package dev.khoa.plugin.pluginspoofer;

import dev.khoa.plugin.pluginspoofer.config.SpoofConfig;
import dev.khoa.plugin.pluginspoofer.listeners.ChannelFilterListener;
import dev.khoa.plugin.pluginspoofer.listeners.ChatFilterListener;
import dev.khoa.plugin.pluginspoofer.listeners.CommandPreprocessListener;
import dev.khoa.plugin.pluginspoofer.listeners.CommandSendListener;
import dev.khoa.plugin.pluginspoofer.listeners.TabCompleteListener;
import dev.khoa.plugin.pluginspoofer.manager.PluginSpoofManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Main plugin class for PluginSpoofer.
 * Provides complete plugin cloaking, fake plugin spoofing, and reconnaissance defense.
 */
public final class PluginSpoofer extends JavaPlugin implements CommandExecutor, TabCompleter {

    private volatile SpoofConfig spoofConfig;
    private PluginSpoofManager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.manager = new PluginSpoofManager();
        reloadSpoofConfig();

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new CommandSendListener(this), this);
        pm.registerEvents(new TabCompleteListener(this), this);
        pm.registerEvents(new CommandPreprocessListener(this), this);
        pm.registerEvents(new ChannelFilterListener(this), this);
        pm.registerEvents(new ChatFilterListener(this), this);

        var cmd = getCommand("pluginspoofer");
        if (cmd != null) {
            cmd.setExecutor(this);
            cmd.setTabCompleter(this);
        }

        printBanner();
    }

    @Override
    public void onDisable() {
        getLogger().info("PluginSpoofer disabled.");
    }

    private void printBanner() {
        var logger = getLogger();
        logger.info("");
        logger.info("  ██████╗  ██████╗   ██████╗  ██╗  ██╗ ██╗ ███████╗");
        logger.info(" ██╔════╝ ██╔═══██╗ ██╔═══██╗ ██║ ██╔╝ ██║ ██╔════╝");
        logger.info(" ██║      ██║   ██║ ██║   ██║ █████═╝  ██║ █████╗  ");
        logger.info(" ██║      ██║   ██║ ██║   ██║ ██╔═██╗  ██║ ██╔══╝  ");
        logger.info(" ╚██████╗ ╚██████╔╝ ╚██████╔╝ ██║ ╚██╗ ██║ ███████╗");
        logger.info("  ╚═════╝  ╚═════╝   ╚═════╝  ╚═╝  ╚═╝ ╚═╝ ╚══════╝");
        logger.info("  ──────────────────────────────────────────────────");
        logger.info("   • Plugin: PluginSpoofer [V1]");
        logger.info("   • Author: Cookieee | Platform: Paper, Folia 1.21+");
        logger.info("   • System: Active | Mode: " + spoofConfig.getMode());
        logger.info("  ──────────────────────────────────────────────────");
        logger.info("");
    }

    public synchronized void reloadSpoofConfig() {
        reloadConfig();
        this.spoofConfig = SpoofConfig.fromBukkitConfig(getConfig());
    }

    public SpoofConfig getSpoofConfig() {
        return spoofConfig;
    }

    public PluginSpoofManager getManager() {
        return manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("pluginspoofer.admin")) {
            sender.sendMessage(PluginSpoofManager.colorize("&cYou do not have permission to execute this command."));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            reloadSpoofConfig();
            sender.sendMessage(PluginSpoofManager.colorize(
                    "&a[PluginSpoofer] Configuration reloaded successfully! Mode: &f" + spoofConfig.getMode() +
                            "&a, Fake plugins: &f" + spoofConfig.getFakePlugins().size()
            ));
            return true;
        }

        sender.sendMessage(PluginSpoofManager.colorize(
                "&b[PluginSpoofer] &7Version: &f" + getPluginMeta().getVersion() +
                        " &7| Mode: &e" + spoofConfig.getMode() +
                        " &7| Enabled: &a" + spoofConfig.isEnabled() + "\n" +
                        "&7Use &f/pluginspoofer reload &7to reload configuration."
        ));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("pluginspoofer.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            String query = args[0].toLowerCase(Locale.ROOT);
            return List.of("reload", "info").stream()
                    .filter(s -> s.startsWith(query))
                    .toList();
        }

        return Collections.emptyList();
    }
}
