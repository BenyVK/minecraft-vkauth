package ir.gharri.vkauth;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import java.util.Locale;
import java.util.Set;

public final class AuthListener implements Listener {

    private static final Set<String> ALLOWED_COMMANDS = Set.of("reg", "register", "login", "log", "logout");

    private final AuthManager auth;
    private final ConfigVk cfg;

    public AuthListener(AuthManager auth, ConfigVk cfg) {
        this.auth = auth;
        this.cfg = cfg;
    }

    private void block(Player player, Cancellable event) {
        if (auth.isLocked(player)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent e) {
        auth.handleJoin(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent e) {
        auth.handleQuit(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMove(PlayerMoveEvent e) {
        if (!auth.isLocked(e.getPlayer())) return;
        Location from = e.getFrom();
        Location to = e.getTo();
        if (to == null) return;
        if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
            Location fixed = from.clone();
            fixed.setYaw(to.getYaw());
            fixed.setPitch(to.getPitch());
            e.setTo(fixed);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent e) {
        if (!auth.isLocked(e.getPlayer())) return;
        e.setCancelled(true);
        e.getPlayer().sendMessage(cfg.msg("chat-blocked"));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (!auth.isLocked(e.getPlayer())) return;

        String message = e.getMessage();
        String label = message.substring(1).split(" ", 2)[0].toLowerCase(Locale.ROOT);

        int colon = label.indexOf(':');
        if (colon >= 0) {
            if (!label.substring(0, colon).equals("vkauth")) {
                deny(e);
                return;
            }
            label = label.substring(colon + 1);
        }
        if (!ALLOWED_COMMANDS.contains(label)) deny(e);
    }

    private void deny(PlayerCommandPreprocessEvent e) {
        e.setCancelled(true);
        e.getPlayer().sendMessage(cfg.msg("commands-blocked"));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent e) {
        block(e.getPlayer(), e);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        block(e.getPlayer(), e);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent e) {
        block(e.getPlayer(), e);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBreak(BlockBreakEvent e) {
        block(e.getPlayer(), e);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlace(BlockPlaceEvent e) {
        block(e.getPlayer(), e);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrop(PlayerDropItemEvent e) {
        block(e.getPlayer(), e);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player p) block(p, e);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player p) block(p, e);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (e.getPlayer() instanceof Player p) block(p, e);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSwapHands(PlayerSwapHandItemsEvent e) {
        block(e.getPlayer(), e);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onConsume(PlayerItemConsumeEvent e) {
        block(e.getPlayer(), e);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        block(e.getPlayer(), e);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBucketFill(PlayerBucketFillEvent e) {
        block(e.getPlayer(), e);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamaged(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p) block(p, e);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAttack(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p) block(p, e);
    }
}
