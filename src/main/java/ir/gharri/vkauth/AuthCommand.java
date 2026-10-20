package ir.gharri.vkauth;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class AuthCommand implements CommandExecutor {

    private final AuthManager auth;
    private final ConfigVk cfg;

    public AuthCommand(AuthManager auth, ConfigVk cfg) {
        this.auth = auth;
        this.cfg = cfg;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(cfg.msg("players-only"));
            return true;
        }
        switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "register" -> auth.register(player, args);
            case "login" -> auth.login(player, args);
            case "logout" -> player.kick(cfg.msg("logout-kick"));
            default -> { }
        }
        return true;
    }
}
