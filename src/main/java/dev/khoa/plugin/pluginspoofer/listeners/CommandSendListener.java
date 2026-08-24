package dev.khoa.plugin.pluginspoofer.listeners;

import dev.khoa.plugin.pluginspoofer.PluginSpoofer;
import dev.khoa.plugin.pluginspoofer.config.SpoofConfig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;

/**
 * Layer 1: Brigadier Command Tree Cloaking.
 * Intercepts ClientboundCommandsPacket command tree sent to players,
 * stripping sensitive colon commands, unauthorized commands, and injecting fake plugin commands.
 */
public class CommandSendListener implements Listener {

    private final PluginSpoofer plugin;

    public CommandSendListener(PluginSpoofer plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommandSend(PlayerCommandSendEvent event) {
        SpoofConfig config = plugin.getSpoofConfig();
        if (!config.isEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        if (player.hasPermission(config.getBypassPermission())) {
            return;
        }

        plugin.getManager().filterCommandTree(config, event.getCommands(), player);
    }
}
