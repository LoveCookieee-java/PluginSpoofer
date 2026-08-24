package dev.khoa.plugin.pluginspoofer.listeners;

import dev.khoa.plugin.pluginspoofer.PluginSpoofer;
import dev.khoa.plugin.pluginspoofer.config.SpoofConfig;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Intercepts chat messages starting with hacked client prefixes (e.g. '.', '#', '@', ',', ';', '!').
 */
public class ChatFilterListener implements Listener {

    private final PluginSpoofer plugin;

    public ChatFilterListener(PluginSpoofer plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPaperChat(AsyncChatEvent event) {
        SpoofConfig config = plugin.getSpoofConfig();
        if (!config.isEnabled() || !config.isCommandFilterEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        if (player.hasPermission(config.getBypassPermission())) {
            return;
        }

        String plainText = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        if (plugin.getManager().hasBlockedPrefix(config, plainText)) {
            event.setCancelled(true);
            player.sendMessage(plugin.getManager().getBlockResponseMessage(config));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLegacyChat(AsyncPlayerChatEvent event) {
        SpoofConfig config = plugin.getSpoofConfig();
        if (!config.isEnabled() || !config.isCommandFilterEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        if (player.hasPermission(config.getBypassPermission())) {
            return;
        }

        String message = event.getMessage() != null ? event.getMessage().trim() : "";
        if (plugin.getManager().hasBlockedPrefix(config, message)) {
            event.setCancelled(true);
            player.sendMessage(plugin.getManager().getBlockResponseMessage(config));
        }
    }
}
