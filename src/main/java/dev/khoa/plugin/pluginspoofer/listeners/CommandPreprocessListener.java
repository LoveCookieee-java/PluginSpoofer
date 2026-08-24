package dev.khoa.plugin.pluginspoofer.listeners;

import dev.khoa.plugin.pluginspoofer.PluginSpoofer;
import dev.khoa.plugin.pluginspoofer.config.FakePluginModel;
import dev.khoa.plugin.pluginspoofer.config.SpoofConfig;
import dev.khoa.plugin.pluginspoofer.manager.PluginSpoofManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Layer 3 & 4: Direct Command Interceptor, Permission Oracle Normalizer,
 * Strict Permission Lockdown, and Admin Alerts.
 */
public class CommandPreprocessListener implements Listener {

    private final PluginSpoofer plugin;

    public CommandPreprocessListener(PluginSpoofer plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        SpoofConfig config = plugin.getSpoofConfig();
        if (!config.isEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        if (player.hasPermission(config.getBypassPermission())) {
            return;
        }

        String rawMessage = event.getMessage();
        if (rawMessage == null || !rawMessage.startsWith("/")) {
            return;
        }

        String clean = rawMessage.substring(1).trim();
        if (clean.isEmpty()) {
            return;
        }

        String targetCommand = config.isUnwrapExecuteCommands()
                ? plugin.getManager().unwrapCommand(clean)
                : clean;

        String[] parts = targetCommand.split("\\s+");
        String commandRoot = parts[0].toLowerCase(Locale.ROOT);

        // Layer 3: Direct /plugins or /pl (including unwrapped /execute run plugins)
        if (plugin.getManager().isPluginsCommand(commandRoot)) {
            event.setCancelled(true);
            if (config.getMode() == SpoofConfig.SpoofMode.SPOOF) {
                player.sendMessage(plugin.getManager().buildSpoofedPluginsMessage(config));
            } else {
                player.sendMessage(plugin.getManager().getBlockResponseMessage(config));
            }
            return;
        }

        // Layer 3: Direct /ver, /version, /about (including unwrapped /execute run ver <plugin>)
        if (plugin.getManager().isVersionCommand(commandRoot)) {
            event.setCancelled(true);
            if (config.getMode() == SpoofConfig.SpoofMode.SPOOF) {
                if (parts.length > 1) {
                    String targetPlugin = parts[1];
                    Optional<FakePluginModel> fakeOpt = plugin.getManager().findFakePlugin(config, targetPlugin);
                    if (fakeOpt.isPresent()) {
                        List<String> lines = plugin.getManager().buildFakeVersionMessage(config, fakeOpt.get());
                        for (String line : lines) {
                            player.sendMessage(line);
                        }
                    } else {
                        player.sendMessage(PluginSpoofManager.colorize(config.getUnknownPluginVersionMessage()));
                    }
                } else {
                    player.sendMessage(plugin.getManager().buildSpoofedPluginsMessage(config));
                }
            } else {
                player.sendMessage(plugin.getManager().getBlockResponseMessage(config));
            }
            return;
        }

        // Layer 4 & Command Filter: Check if command is blocked or matches hidden probe
        if (plugin.getManager().isBlockedCommand(config, clean) || plugin.getManager().isBlockedCommand(config, targetCommand)) {
            event.setCancelled(true);
            player.sendMessage(plugin.getManager().getBlockResponseMessage(config));
            if (config.isAdminAlertsEnabled()) {
                plugin.getManager().alertStaff(plugin.getManager().formatBlockedCommandAlert(config, player.getName(), rawMessage));
            }
            return;
        }

        // Strict Permission Cloaking: Check if real command exists on server but player has no permission
        if (config.isMaskNoPermissionErrors()) {
            try {
                CommandMap commandMap = Bukkit.getCommandMap();
                if (commandMap != null) {
                    Command registeredCmd = commandMap.getCommand(commandRoot);
                    if (registeredCmd != null && !registeredCmd.testPermissionSilent(player)) {
                        event.setCancelled(true);
                        player.sendMessage(plugin.getManager().getBlockResponseMessage(config));
                        if (config.isAdminAlertsEnabled()) {
                            plugin.getManager().alertStaff(plugin.getManager().formatBlockedCommandAlert(config, player.getName(), rawMessage));
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
