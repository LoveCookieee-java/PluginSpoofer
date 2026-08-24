package dev.khoa.plugin.pluginspoofer.listeners;

import dev.khoa.plugin.pluginspoofer.PluginSpoofer;
import dev.khoa.plugin.pluginspoofer.config.SpoofConfig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRegisterChannelEvent;

import java.util.Locale;
import java.util.Set;

/**
 * Layer 5: Plugin Messaging Channel Masking.
 * Monitors and filters suspicious plugin messaging channels (e.g. WDL|INIT, wdl:init)
 * registered by probing clients without bypass permission.
 */
public class ChannelFilterListener implements Listener {

    private static final Set<String> SENSITIVE_CHANNELS = Set.of(
            "wdl:init",
            "wdl|init",
            "worlddownloader"
    );

    private final PluginSpoofer plugin;

    public ChannelFilterListener(PluginSpoofer plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRegisterChannel(PlayerRegisterChannelEvent event) {
        SpoofConfig config = plugin.getSpoofConfig();
        if (!config.isEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        if (player.hasPermission(config.getBypassPermission())) {
            return;
        }

        String channel = event.getChannel().toLowerCase(Locale.ROOT);
        if (SENSITIVE_CHANNELS.contains(channel)) {
            // Unregister sensitive probing channels from the player session
            player.getListeningPluginChannels().remove(event.getChannel());
        }
    }
}
