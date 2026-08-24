package dev.khoa.plugin.pluginspoofer.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Encapsulates all configuration values for PluginSpoofer.
 */
public class SpoofConfig {

    public enum SpoofMode {
        SPOOF,
        HIDE
    }

    public enum BlockResponseMode {
        VANILLA_UNKNOWN,
        CUSTOM
    }

    private final boolean enabled;
    private final SpoofMode mode;
    private final String bypassPermission;
    private final List<FakePluginModel> fakePlugins;
    private final String spoofPluginsFormat;
    private final String pluginNameFormat;
    private final String pluginSeparator;
    private final List<String> fakeVersionFormat;
    private final String unknownPluginVersionMessage;
    private final Set<String> hiddenPluginNames;
    private final boolean filterColonCommands;
    private final boolean hideVersionCommands;
    private final boolean spoofFakeNamespaces;
    private final String unknownCommandMessage;

    // Command Filter settings
    private final boolean commandFilterEnabled;
    private final BlockResponseMode blockResponseMode;
    private final String customBlockMessage;
    private final Set<String> blockedCommands;
    private final List<Pattern> blockedPatterns;
    private final List<String> blockedPrefixes;
    private final boolean unwrapExecuteCommands;

    public SpoofConfig(
            boolean enabled,
            SpoofMode mode,
            String bypassPermission,
            List<FakePluginModel> fakePlugins,
            String spoofPluginsFormat,
            String pluginNameFormat,
            String pluginSeparator,
            List<String> fakeVersionFormat,
            String unknownPluginVersionMessage,
            Set<String> hiddenPluginNames,
            boolean filterColonCommands,
            boolean hideVersionCommands,
            boolean spoofFakeNamespaces,
            String unknownCommandMessage,
            boolean commandFilterEnabled,
            BlockResponseMode blockResponseMode,
            String customBlockMessage,
            Set<String> blockedCommands,
            List<Pattern> blockedPatterns,
            List<String> blockedPrefixes,
            boolean unwrapExecuteCommands
    ) {
        this.enabled = enabled;
        this.mode = mode != null ? mode : SpoofMode.SPOOF;
        this.bypassPermission = bypassPermission != null ? bypassPermission : "pluginspoofer.bypass";
        this.fakePlugins = fakePlugins != null ? List.copyOf(fakePlugins) : Collections.emptyList();
        this.spoofPluginsFormat = spoofPluginsFormat != null ? spoofPluginsFormat : "&fPlugins (%count%): %plugins%";
        this.pluginNameFormat = pluginNameFormat != null ? pluginNameFormat : "&a%name%";
        this.pluginSeparator = pluginSeparator != null ? pluginSeparator : "&f, ";
        this.fakeVersionFormat = fakeVersionFormat != null ? List.copyOf(fakeVersionFormat) : Collections.emptyList();
        this.unknownPluginVersionMessage = unknownPluginVersionMessage != null ? unknownPluginVersionMessage : "&cThis server is not running any plugin by that name.\n&cUse /plugins to get a list of plugins.";
        this.hiddenPluginNames = hiddenPluginNames != null ? Set.copyOf(hiddenPluginNames) : Collections.emptySet();
        this.filterColonCommands = filterColonCommands;
        this.hideVersionCommands = hideVersionCommands;
        this.spoofFakeNamespaces = spoofFakeNamespaces;
        this.unknownCommandMessage = unknownCommandMessage != null ? unknownCommandMessage : "&cUnknown or incomplete command, see below for error\n&c<--[HERE]";
        this.commandFilterEnabled = commandFilterEnabled;
        this.blockResponseMode = blockResponseMode != null ? blockResponseMode : BlockResponseMode.VANILLA_UNKNOWN;
        this.customBlockMessage = customBlockMessage != null ? customBlockMessage : "&cYou do not have permission to execute this command.";
        this.blockedCommands = blockedCommands != null ? Set.copyOf(blockedCommands) : Collections.emptySet();
        this.blockedPatterns = blockedPatterns != null ? List.copyOf(blockedPatterns) : Collections.emptyList();
        this.blockedPrefixes = blockedPrefixes != null ? List.copyOf(blockedPrefixes) : Collections.emptyList();
        this.unwrapExecuteCommands = unwrapExecuteCommands;
    }

    public static SpoofConfig fromBukkitConfig(FileConfiguration config) {
        boolean enabled = config.getBoolean("enabled", true);
        String rawMode = config.getString("mode", "SPOOF");
        SpoofMode mode;
        try {
            mode = SpoofMode.valueOf(rawMode.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            mode = SpoofMode.SPOOF;
        }

        String bypassPermission = config.getString("bypass-permission", "pluginspoofer.bypass");

        List<FakePluginModel> fakePlugins = new ArrayList<>();
        List<Map<?, ?>> fakePluginMaps = config.getMapList("fake-plugins");
        for (Map<?, ?> entry : fakePluginMaps) {
            String name = entry.get("name") != null ? entry.get("name").toString() : "Unknown";
            String version = entry.get("version") != null ? entry.get("version").toString() : "1.0";

            List<String> authors = new ArrayList<>();
            Object rawAuthors = entry.get("authors");
            if (rawAuthors instanceof List<?> list) {
                for (Object author : list) {
                    if (author != null) authors.add(author.toString());
                }
            } else if (rawAuthors != null) {
                authors.add(rawAuthors.toString());
            }

            String description = entry.get("description") != null ? entry.get("description").toString() : "";

            List<String> fakeCommands = new ArrayList<>();
            Object rawCommands = entry.get("fake-commands");
            if (rawCommands instanceof List<?> list) {
                for (Object cmd : list) {
                    if (cmd != null) fakeCommands.add(cmd.toString());
                }
            }

            fakePlugins.add(new FakePluginModel(name, version, authors, description, fakeCommands));
        }

        String spoofPluginsFormat = config.getString("spoof-plugins-format", "&fPlugins (%count%): %plugins%");
        String pluginNameFormat = config.getString("plugin-name-format", "&a%name%");
        String pluginSeparator = config.getString("plugin-separator", "&f, ");
        List<String> fakeVersionFormat = config.getStringList("fake-version-format");
        if (fakeVersionFormat.isEmpty()) {
            fakeVersionFormat = List.of(
                    "&a%name% version %version% by %authors%",
                    "&f%description%"
            );
        }

        String unknownPluginVersionMessage = config.getString("unknown-plugin-version-message",
                "&cThis server is not running any plugin by that name.\n&cUse /plugins to get a list of plugins.");

        Set<String> hiddenPluginNames = new HashSet<>();
        for (String hidden : config.getStringList("hidden-plugin-names")) {
            if (hidden != null && !hidden.isBlank()) {
                hiddenPluginNames.add(hidden.trim().toLowerCase(Locale.ROOT));
            }
        }

        boolean filterColonCommands = config.getBoolean("command-tree.filter-colon-commands", true);
        boolean hideVersionCommands = config.getBoolean("command-tree.hide-version-commands", true);
        boolean spoofFakeNamespaces = config.getBoolean("command-tree.spoof-fake-namespaces", true);

        String unknownCommandMessage = config.getString("unknown-command-message",
                "&cUnknown or incomplete command, see below for error\n&c<--[HERE]");

        // Command Filter parsing
        boolean commandFilterEnabled = config.getBoolean("command-filter.enabled", true);
        String rawResponseMode = config.getString("command-filter.block-response-mode", "VANILLA_UNKNOWN");
        BlockResponseMode blockResponseMode;
        try {
            blockResponseMode = BlockResponseMode.valueOf(rawResponseMode.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            blockResponseMode = BlockResponseMode.VANILLA_UNKNOWN;
        }

        String customBlockMessage = config.getString("command-filter.custom-block-message",
                "&cYou do not have permission to execute this command.");

        Set<String> blockedCommands = new HashSet<>();
        for (String cmd : config.getStringList("command-filter.blocked-commands")) {
            if (cmd != null && !cmd.isBlank()) {
                String cleanCmd = cmd.trim().toLowerCase(Locale.ROOT);
                if (cleanCmd.startsWith("/")) {
                    cleanCmd = cleanCmd.substring(1);
                }
                blockedCommands.add(cleanCmd);
            }
        }

        List<Pattern> blockedPatterns = new ArrayList<>();
        for (String patternStr : config.getStringList("command-filter.blocked-patterns")) {
            if (patternStr != null && !patternStr.isBlank()) {
                try {
                    blockedPatterns.add(Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE));
                } catch (PatternSyntaxException ignored) {
                }
            }
        }

        List<String> blockedPrefixes = new ArrayList<>();
        for (String prefix : config.getStringList("command-filter.blocked-prefixes")) {
            if (prefix != null && !prefix.isEmpty()) {
                blockedPrefixes.add(prefix);
            }
        }

        boolean unwrapExecuteCommands = config.getBoolean("command-filter.unwrap-execute-commands", true);

        return new SpoofConfig(
                enabled,
                mode,
                bypassPermission,
                fakePlugins,
                spoofPluginsFormat,
                pluginNameFormat,
                pluginSeparator,
                fakeVersionFormat,
                unknownPluginVersionMessage,
                hiddenPluginNames,
                filterColonCommands,
                hideVersionCommands,
                spoofFakeNamespaces,
                unknownCommandMessage,
                commandFilterEnabled,
                blockResponseMode,
                customBlockMessage,
                blockedCommands,
                blockedPatterns,
                blockedPrefixes,
                unwrapExecuteCommands
        );
    }

    public boolean isEnabled() {
        return enabled;
    }

    public SpoofMode getMode() {
        return mode;
    }

    public String getBypassPermission() {
        return bypassPermission;
    }

    public List<FakePluginModel> getFakePlugins() {
        return fakePlugins;
    }

    public String getSpoofPluginsFormat() {
        return spoofPluginsFormat;
    }

    public String getPluginNameFormat() {
        return pluginNameFormat;
    }

    public String getPluginSeparator() {
        return pluginSeparator;
    }

    public List<String> getFakeVersionFormat() {
        return fakeVersionFormat;
    }

    public String getUnknownPluginVersionMessage() {
        return unknownPluginVersionMessage;
    }

    public Set<String> getHiddenPluginNames() {
        return hiddenPluginNames;
    }

    public boolean isFilterColonCommands() {
        return filterColonCommands;
    }

    public boolean isHideVersionCommands() {
        return hideVersionCommands;
    }

    public boolean isSpoofFakeNamespaces() {
        return spoofFakeNamespaces;
    }

    public String getUnknownCommandMessage() {
        return unknownCommandMessage;
    }

    public boolean isCommandFilterEnabled() {
        return commandFilterEnabled;
    }

    public BlockResponseMode getBlockResponseMode() {
        return blockResponseMode;
    }

    public String getCustomBlockMessage() {
        return customBlockMessage;
    }

    public Set<String> getBlockedCommands() {
        return blockedCommands;
    }

    public List<Pattern> getBlockedPatterns() {
        return blockedPatterns;
    }

    public List<String> getBlockedPrefixes() {
        return blockedPrefixes;
    }

    public boolean isUnwrapExecuteCommands() {
        return unwrapExecuteCommands;
    }
}
