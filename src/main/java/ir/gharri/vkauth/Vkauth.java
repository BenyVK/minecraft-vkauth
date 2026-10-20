package ir.gharri.vkauth;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;

public final class Vkauth extends JavaPlugin {

    private StorageManager storage;
    private AuthManager auth;

    @Override
    public void onEnable() {
        ConfigVk cfg = new ConfigVk(this);

        storage = new StorageManager(this, cfg);
        if (!storage.init()) {
            getLogger().severe("No storage backend could be started. Disabling vkauth.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        auth = new AuthManager(this, cfg, storage);
        getServer().getPluginManager().registerEvents(new AuthListener(auth, cfg), this);

        AuthCommand command = new AuthCommand(auth, cfg);
        getCommand("register").setExecutor(command);
        getCommand("login").setExecutor(command);
        getCommand("logout").setExecutor(command);

        auth.startTask();

        for (Player player : new ArrayList<>(getServer().getOnlinePlayers())) {
            auth.handleJoin(player);
        }
    }

    @Override
    public void onDisable() {
        if (auth != null) auth.shutdown();
        if (storage != null) storage.close();
    }
}
