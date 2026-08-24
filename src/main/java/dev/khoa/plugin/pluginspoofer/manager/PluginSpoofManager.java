package dev.khoa.plugin.pluginspoofer.manager;

import dev.khoa.plugin.pluginspoofer.config.FakePluginModel;
import dev.khoa.plugin.pluginspoofer.config.SpoofConfig;
import org.bukkit.ChatColor;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Core engine handling spoofing formatting, tab-completion filtering,
 * command tree cloaking, permission oracle normalization, and command blacklist filtering.
 */
public class PluginSpoofManager {

    private static final Set<String> VERSION_COMMAND_ROOTS = Set.of(
            "ver", "version", "about",
            "bukkit:ver", "bukkit:version", "bukkit:about"
    );

    private static final Set<String> PLUGINS_COMMAND_ROOTS = Set.of(
            "plugins", "pl",
            "bukkit:plugins", "bukkit:pl"
    );

    private static final Set<String> ALL_INFO_COMMAND_ROOTS = Set.of(
            "plugins", "pl", "bukkit:plugins", "bukkit:pl",
            "ver", "version", "about", "bukkit:ver", "bukkit:version", "bukkit:about",
            "?", "bukkit:?", "help", "bukkit:help"
    );

    private static final Pattern EXECUTE_RUN_PATTERN = Pattern.compile("(?i)(?:^|\\s)run\\s+(.+)");

    public static String colorize(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Builds the formatted spoofed plugin list string (e.g. "Plugins (4): WorldEdit, Essentials, Vault, LuckPerms").
     */
    public String buildSpoofedPluginsMessage(SpoofConfig config) {
        List<FakePluginModel> fakePlugins = config.getFakePlugins();
        String formattedPlugins = fakePlugins.stream()
                .map(plugin -> config.getPluginNameFormat().replace("%name%", plugin.name()))
                .collect(Collectors.joining(config.getPluginSeparator()));

        String message = config.getSpoofPluginsFormat()
                .replace("%count%", String.valueOf(fakePlugins.size()))
                .replace("%plugins%", formattedPlugins);

        return colorize(message);
    }

    /**
     * Builds the multiline fake version message for a specific fake plugin.
     */
    public List<String> buildFakeVersionMessage(SpoofConfig config, FakePluginModel model) {
        List<String> lines = new ArrayList<>();
        for (String lineTemplate : config.getFakeVersionFormat()) {
            String line = lineTemplate
                    .replace("%name%", model.name())
                    .replace("%version%", model.version())
                    .replace("%authors%", model.formattedAuthors())
                    .replace("%description%", model.description());
            lines.add(colorize(line));
        }
        return lines;
    }

    /**
     * Finds a fake plugin definition by name (case-insensitive).
     */
    public Optional<FakePluginModel> findFakePlugin(SpoofConfig config, String query) {
        if (query == null || query.isBlank()) return Optional.empty();
        String cleanQuery = query.trim();
        return config.getFakePlugins().stream()
                .filter(plugin -> plugin.name().equalsIgnoreCase(cleanQuery))
                .findFirst();
    }

    /**
     * Resolves tab completions for version and information commands.
     */
    public List<String> getTabCompletions(SpoofConfig config, String buffer) {
        if (config.getMode() == SpoofConfig.SpoofMode.HIDE) {
            return Collections.emptyList();
        }

        if (buffer == null) return Collections.emptyList();
        String trimmed = buffer.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }

        String[] parts = trimmed.split("\\s+", 2);
        String command = parts[0].toLowerCase(Locale.ROOT);
        String query = parts.length > 1 ? parts[1].toLowerCase(Locale.ROOT) : "";

        if (VERSION_COMMAND_ROOTS.contains(command)) {
            return config.getFakePlugins().stream()
                    .map(FakePluginModel::name)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(query))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    public boolean isPluginsCommand(String commandRoot) {
        if (commandRoot == null) return false;
        String clean = stripSlash(commandRoot).toLowerCase(Locale.ROOT);
        return PLUGINS_COMMAND_ROOTS.contains(clean);
    }

    public boolean isVersionCommand(String commandRoot) {
        if (commandRoot == null) return false;
        String clean = stripSlash(commandRoot).toLowerCase(Locale.ROOT);
        return VERSION_COMMAND_ROOTS.contains(clean);
    }

    public boolean isAllInfoCommand(String commandRoot) {
        if (commandRoot == null) return false;
        String clean = stripSlash(commandRoot).toLowerCase(Locale.ROOT);
        return ALL_INFO_COMMAND_ROOTS.contains(clean);
    }

    /**
     * Recursively unwraps command wrappers such as `/execute as @p run ver WorldEdit`.
     */
    public String unwrapCommand(String rawCommand) {
        if (rawCommand == null || rawCommand.isBlank()) return "";
        String current = stripSlash(rawCommand).trim();

        while (true) {
            String lower = current.toLowerCase(Locale.ROOT);
            if (lower.startsWith("execute ") || lower.startsWith("minecraft:execute ")) {
                Matcher matcher = EXECUTE_RUN_PATTERN.matcher(current);
                if (matcher.find()) {
                    current = stripSlash(matcher.group(1)).trim();
                    continue;
                }
            }
            break;
        }

        return current;
    }

    /**
     * Checks if a command is explicitly blocked by the command filter or hidden plugin list.
     */
    public boolean isBlockedCommand(SpoofConfig config, String rawCommand) {
        if (rawCommand == null || rawCommand.isBlank()) return false;
        
        String clean = config.isUnwrapExecuteCommands() ? unwrapCommand(rawCommand) : stripSlash(rawCommand).trim();
        if (clean.isEmpty()) return false;

        String[] parts = clean.split("\\s+");
        String commandRoot = parts[0].toLowerCase(Locale.ROOT);

        if (config.isCommandFilterEnabled()) {
            // Check exact blocked command names or roots
            if (config.getBlockedCommands().contains(commandRoot) || config.getBlockedCommands().contains(clean.toLowerCase(Locale.ROOT))) {
                return true;
            }

            // Check blocked regex patterns
            for (Pattern pattern : config.getBlockedPatterns()) {
                if (pattern.matcher(commandRoot).matches() || pattern.matcher(clean).matches()) {
                    return true;
                }
            }
        }

        // Check if command belongs to hidden plugins
        return isHiddenCommandOrPlugin(config, clean);
    }

    /**
     * Checks if a chat message starts with a blocked hacked client prefix (e.g. '.', '#', '@', ',').
     */
    public boolean hasBlockedPrefix(SpoofConfig config, String message) {
        if (!config.isCommandFilterEnabled() || message == null || message.isBlank()) {
            return false;
        }

        for (String prefix : config.getBlockedPrefixes()) {
            if (message.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the formatted block response message based on the configured mode.
     */
    public String getBlockResponseMessage(SpoofConfig config) {
        if (config.getBlockResponseMode() == SpoofConfig.BlockResponseMode.CUSTOM) {
            return colorize(config.getCustomBlockMessage());
        }
        return colorize(config.getUnknownCommandMessage());
    }

    /**
     * Determines whether a command or sub-command matches a hidden plugin name or sensitive probe.
     */
    public boolean isHiddenCommandOrPlugin(SpoofConfig config, String rawCommand) {
        if (rawCommand == null || rawCommand.isBlank()) return false;
        String clean = stripSlash(rawCommand).trim();
        String commandRoot = clean.split("\\s+")[0].toLowerCase(Locale.ROOT);

        if (commandRoot.contains(":")) {
            String namespace = commandRoot.split(":")[0];
            if (isMatchingHidden(config, namespace)) {
                return true;
            }
            if (config.isFilterColonCommands()) {
                // If it's a colon command not belonging to our fake namespaces
                boolean isFakeNamespace = config.getFakePlugins().stream()
                        .anyMatch(p -> p.fakeCommands().stream()
                                .anyMatch(fc -> fc.equalsIgnoreCase(commandRoot)));
                if (!isFakeNamespace) {
                    return true;
                }
            }
        }

        return isMatchingHidden(config, commandRoot);
    }

    private boolean isMatchingHidden(SpoofConfig config, String query) {
        if (query == null || query.isBlank()) return false;
        String lower = query.toLowerCase(Locale.ROOT);
        return config.getHiddenPluginNames().stream()
                .anyMatch(hidden -> lower.equals(hidden)
                        || lower.startsWith(hidden)
                        || (lower.length() >= 3 && hidden.startsWith(lower)));
    }

    /**
     * Filters the Brigadier command tree packet (PlayerCommandSendEvent).
     */
    public void filterCommandTree(SpoofConfig config, Collection<String> commands) {
        if (commands == null) return;

        commands.removeIf(command -> {
            String lower = command.toLowerCase(Locale.ROOT);

            // Hide version/plugins commands
            if (config.isHideVersionCommands() && ALL_INFO_COMMAND_ROOTS.contains(lower)) {
                return true;
            }

            // Filter colon commands (e.g. antiopsec:asp, grimac:grim)
            if (config.isFilterColonCommands() && lower.contains(":")) {
                return true;
            }

            // Filter hidden plugin names / command roots
            if (isMatchingHidden(config, lower)) {
                return true;
            }

            // Filter blocked commands from command-filter
            if (config.isCommandFilterEnabled()) {
                if (config.getBlockedCommands().contains(lower)) {
                    return true;
                }
                for (Pattern pattern : config.getBlockedPatterns()) {
                    if (pattern.matcher(lower).matches()) {
                        return true;
                    }
                }
            }

            return false;
        });

        // Inject fake namespace commands in SPOOF mode
        if (config.getMode() == SpoofConfig.SpoofMode.SPOOF && config.isSpoofFakeNamespaces()) {
            for (FakePluginModel fakePlugin : config.getFakePlugins()) {
                commands.addAll(fakePlugin.fakeCommands());
            }
        }
    }

    private static String stripSlash(String str) {
        if (str != null && str.startsWith("/")) {
            return str.substring(1);
        }
        return str;
    }
}
