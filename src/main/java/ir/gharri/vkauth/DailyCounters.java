package ir.gharri.vkauth;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.logging.Level;

/** "Today's" login / register counters (used for the TAB names). Reset every day, saved in counters.yml. */
public final class DailyCounters {

    private static final Object IO = new Object();

    private final Vkauth plugin;
    private final ConfigVk cfg;
    private final File file;

    private LocalDate date;
    private int login;
    private int register;

    public DailyCounters(Vkauth plugin, ConfigVk cfg) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.file = new File(plugin.getDataFolder(), "counters.yml");
        load();
    }

    private LocalDate today() {
        return LocalDate.now(cfg.zone());
    }

    private void load() {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        try {
            date = LocalDate.parse(y.getString("date", ""));
            login = y.getInt("login");
            register = y.getInt("register");
        } catch (DateTimeParseException e) {
            date = today();
            login = 0;
            register = 0;
        }
        rollover();
    }

    private void rollover() {
        LocalDate t = today();
        if (!t.equals(date)) {
            date = t;
            login = 0;
            register = 0;
        }
    }

    public synchronized int nextLogin() {
        rollover();
        login++;
        persist();
        return login;
    }

    public synchronized int nextRegister() {
        rollover();
        register++;
        persist();
        return register;
    }

    private void persist() {
        final String d = date.toString();
        final int l = login;
        final int r = register;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (IO) {
                YamlConfiguration y = new YamlConfiguration();
                y.set("date", d);
                y.set("login", l);
                y.set("register", r);
                try {
                    y.save(file);
                } catch (IOException e) {
                    plugin.getLogger().log(Level.WARNING, "Could not save counters.yml: " + e.getMessage());
                }
            }
        });
    }
}
