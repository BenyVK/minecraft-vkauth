package ir.gharri.vkauth;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public final class YamlStorage implements Storage {

    private final File file;
    private YamlConfiguration yml;

    public YamlStorage(File dataFolder) {
        this.file = new File(dataFolder, "accounts.yml");
    }

    @Override
    public String name() {
        return "yaml";
    }

    @Override
    public synchronized void init() throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        if (!file.exists()) file.createNewFile();
        yml = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public synchronized Account getAccount(UUID uuid) {
        String base = "accounts." + uuid;
        String hash = yml.getString(base + ".hash");
        if (hash == null) return null;
        return new Account(hash, yml.getLong(base + ".registered", 0L), yml.getLong(base + ".last-login", 0L));
    }

    @Override
    public synchronized void saveAccount(UUID uuid, String name, Account account) throws IOException {
        String base = "accounts." + uuid;
        yml.set(base + ".name", name);
        yml.set(base + ".hash", account.hash());
        yml.set(base + ".registered", account.registered() > 0 ? account.registered() : System.currentTimeMillis());
        yml.set(base + ".last-login", account.lastLogin());
        yml.save(file);
    }

    @Override
    public synchronized void updateLastLogin(UUID uuid, long time) throws IOException {
        yml.set("accounts." + uuid + ".last-login", time);
        yml.save(file);
    }

    @Override
    public void close() {
    }
}
