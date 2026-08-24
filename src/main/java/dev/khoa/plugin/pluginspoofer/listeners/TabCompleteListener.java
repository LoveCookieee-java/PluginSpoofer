package dev.khoa.plugin.pluginspoofer.listeners;

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import dev.khoa.plugin.pluginspoofer.PluginSpoofer;
import dev.khoa.plugin.pluginspoofer.config.SpoofConfig;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.TabCompleteEvent;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Layer 2: Tab-Completion Spoofing.
 * Intercepts tab-completion packets (/ver <tab>, /plugins <tab>, /<hidden>: <tab>),
 * returning fake plugin suggestions in SPOOF mode and masking hidden commands.
 */
public class TabCompleteListener implements Listener {

    private final PluginSpoofer plugin;

    public TabCompleteListener(PluginSpoofer plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncTabComplete(AsyncTabCompleteEvent event) {
        SpoofConfig config = plugin.getSpoofConfig();
        if (!config.isEnabled()) {
            return;
        }

        CommandSender sender = event.getSender();
        if (sender.hasPermission(config.getBypassPermission())) {
            return;
        }

        String buffer = event.getBuffer();
        if (buffer == null || !buffer.startsWith("/")) {
            return;
        }

        String clean = buffer.substring(1).trim();
        String[] parts = clean.split("\\s+", 2);
        String commandRoot = parts[0].toLowerCase(Locale.ROOT);

        // Case 1: /ver <tab>, /version <tab>, /about <tab>
        if (plugin.getManager().isVersionCommand(commandRoot)) {
            if (config.getMode() == SpoofConfig.SpoofMode.SPOOF) {
                List<String> suggestions = plugin.getManager().getTabCompletions(config, buffer);
                event.setCompletions(suggestions);
                event.setHandled(true);
            } else {
                event.setCompletions(Collections.emptyList());
                event.setHandled(true);
            }
            return;
        }

        // Case 2: /plugins <tab>, /pl <tab>
        if (plugin.getManager().isPluginsCommand(commandRoot)) {
            event.setCompletions(Collections.emptyList());
            event.setHandled(true);
            return;
        }

        // Case 3: Colon commands or hidden plugins tab completion
        if (plugin.getManager().isHiddenCommandOrPlugin(config, clean)) {
            event.setCompletions(Collections.emptyList());
            event.setHandled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSyncTabComplete(TabCompleteEvent event) {
        SpoofConfig config = plugin.getSpoofConfig();
        if (!config.isEnabled()) {
            return;
        }

        CommandSender sender = event.getSender();
        if (sender.hasPermission(config.getBypassPermission())) {
            return;
        }

        String buffer = event.getBuffer();
        if (buffer == null || !buffer.startsWith("/")) {
            return;
        }

        String clean = buffer.substring(1).trim();
        String[] parts = clean.split("\\s+", 2);
        String commandRoot = parts[0].toLowerCase(Locale.ROOT);

        if (plugin.getManager().isVersionCommand(commandRoot)) {
            if (config.getMode() == SpoofConfig.SpoofMode.SPOOF) {
                List<String> suggestions = plugin.getManager().getTabCompletions(config, buffer);
                event.setCompletions(suggestions);
            } else {
                event.setCompletions(Collections.emptyList());
            }
            event.setCancelled(true);
            return;
        }

        if (plugin.getManager().isPluginsCommand(commandRoot)) {
            event.setCompletions(Collections.emptyList());
            event.setCancelled(true);
            return;
        }

        if (plugin.getManager().isHiddenCommandOrPlugin(config, clean)) {
            event.setCompletions(Collections.emptyList());
            event.setCancelled(true);
        }
    }
}
