package dev.khoa.plugin.pluginspoofer;

import dev.khoa.plugin.pluginspoofer.config.FakePluginModel;
import dev.khoa.plugin.pluginspoofer.config.SpoofConfig;
import dev.khoa.plugin.pluginspoofer.manager.PluginSpoofManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class PluginSpoofManagerTest {

    private PluginSpoofManager manager;
    private SpoofConfig defaultConfig;

    @BeforeEach
    void setUp() {
        manager = new PluginSpoofManager();

        List<FakePluginModel> fakePlugins = List.of(
                new FakePluginModel("WorldEdit", "7.3.0", List.of("sk89q", "EngineHub"), "WorldEdit in-game map editor",
                        List.of("worldedit:wand", "worldedit:set")),
                new FakePluginModel("Essentials", "2.20.1", List.of("Zenexer", "ementalo"), "Provides essential commands",
                        List.of("essentials:spawn", "essentials:tp")),
                new FakePluginModel("Vault", "1.7.3-b131", List.of("cossinater", "Kevlar"), "Vault is a Permissions API",
                        List.of("vault:vault-info")),
                new FakePluginModel("LuckPerms", "5.4.102", List.of("Luck"), "A permissions plugin",
                        List.of("luckperms:lp", "luckperms:luckperms"))
        );

        Set<String> hiddenPlugins = Set.of(
                "antiopsec", "asp", "grimac", "grim", "vulcan", "matrix", "polar", "coreprotect", "co", "authme", "spark"
        );

        Set<String> blockedCommands = Set.of(
                "op", "deop", "stop", "reload", "rl", "restart", "icanhasbukkit",
                "version", "ver", "about", "plugins", "pl",
                "bukkit:ver", "bukkit:plugins", "paper:version", "spigot:version"
        );

        List<Pattern> blockedPatterns = List.of(
                Pattern.compile("^(bukkit|minecraft|spigot|paper):.*", Pattern.CASE_INSENSITIVE)
        );

        List<String> blockedPrefixes = List.of(".", "#", "@", ",", ";", "!");

        defaultConfig = new SpoofConfig(
                true,
                SpoofConfig.SpoofMode.SPOOF,
                "pluginspoofer.bypass",
                fakePlugins,
                "&fPlugins (%count%): %plugins%",
                "&a%name%",
                "&f, ",
                List.of("&a%name% version %version% by %authors%", "&f%description%"),
                "&cThis server is not running any plugin by that name.\n&cUse /plugins to get a list of plugins.",
                hiddenPlugins,
                true,
                true,
                true,
                "&cUnknown or incomplete command, see below for error\n&c<--[HERE]",
                true,
                SpoofConfig.BlockResponseMode.VANILLA_UNKNOWN,
                "&cYou do not have permission to execute this command.",
                blockedCommands,
                blockedPatterns,
                blockedPrefixes,
                true
        );
    }

    @Test
    @DisplayName("TC-01: Verify /plugins formatted output in SPOOF mode")
    void testTC01_SpoofedPluginsMessage() {
        String output = manager.buildSpoofedPluginsMessage(defaultConfig);
        assertNotNull(output);
        assertTrue(output.contains("Plugins (4):"));
        assertTrue(output.contains("WorldEdit"));
        assertTrue(output.contains("Essentials"));
        assertTrue(output.contains("Vault"));
        assertTrue(output.contains("LuckPerms"));
    }

    @Test
    @DisplayName("TC-02: Verify /ver <FakePlugin> formatting")
    void testTC02_FakeVersionFormatting() {
        Optional<FakePluginModel> modelOpt = manager.findFakePlugin(defaultConfig, "WorldEdit");
        assertTrue(modelOpt.isPresent());

        List<String> lines = manager.buildFakeVersionMessage(defaultConfig, modelOpt.get());
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).contains("WorldEdit version 7.3.0 by sk89q, EngineHub"));
        assertTrue(lines.get(1).contains("WorldEdit in-game map editor"));
    }

    @Test
    @DisplayName("TC-03: Verify /ver <HiddenPlugin> returns not found in fake list")
    void testTC03_UnknownOrHiddenPluginQuery() {
        Optional<FakePluginModel> modelOpt = manager.findFakePlugin(defaultConfig, "GrimAC");
        assertTrue(modelOpt.isEmpty());
    }

    @Test
    @DisplayName("TC-04: Verify Tab-completion for /ver and prefix filtering")
    void testTC04_TabCompletionSpoofing() {
        List<String> allCompletions = manager.getTabCompletions(defaultConfig, "/ver ");
        assertEquals(4, allCompletions.size());
        assertTrue(allCompletions.contains("WorldEdit"));
        assertTrue(allCompletions.contains("Essentials"));

        List<String> filteredCompletions = manager.getTabCompletions(defaultConfig, "/ver e");
        assertEquals(1, filteredCompletions.size());
        assertEquals("Essentials", filteredCompletions.get(0));
    }

    @Test
    @DisplayName("TC-05: Verify Brigadier Command Tree cloaking & fake namespace injection")
    void testTC05_CommandTreeCloakingAndInjection() {
        Set<String> commands = new HashSet<>(Set.of(
                "spawn", "plugins", "pl", "ver", "grimac:grim", "vulcan:vulcan", "grim", "customcmd"
        ));

        manager.filterCommandTree(defaultConfig, commands);

        // Real sensitive colon commands & version commands must be stripped
        assertFalse(commands.contains("plugins"));
        assertFalse(commands.contains("pl"));
        assertFalse(commands.contains("ver"));
        assertFalse(commands.contains("grimac:grim"));
        assertFalse(commands.contains("vulcan:vulcan"));
        assertFalse(commands.contains("grim"));

        // Normal commands preserved
        assertTrue(commands.contains("spawn"));
        assertTrue(commands.contains("customcmd"));

        // Fake namespace commands injected
        assertTrue(commands.contains("worldedit:wand"));
        assertTrue(commands.contains("essentials:spawn"));
        assertTrue(commands.contains("vault:vault-info"));
        assertTrue(commands.contains("luckperms:lp"));
    }

    @Test
    @DisplayName("TC-06: Verify Permission Oracle detection on sensitive probes")
    void testTC06_PermissionOracleDetection() {
        assertTrue(manager.isHiddenCommandOrPlugin(defaultConfig, "/grim reload"));
        assertTrue(manager.isHiddenCommandOrPlugin(defaultConfig, "vulcan verbose"));
        assertTrue(manager.isHiddenCommandOrPlugin(defaultConfig, "/antiopsec:asp"));
        assertTrue(manager.isHiddenCommandOrPlugin(defaultConfig, "spark:spark"));
        assertFalse(manager.isHiddenCommandOrPlugin(defaultConfig, "spawn"));
    }

    @Test
    @DisplayName("TC-07: Verify HIDE mode masks tab completion completely")
    void testTC07_HideModeTabCompletion() {
        SpoofConfig hideConfig = new SpoofConfig(
                true,
                SpoofConfig.SpoofMode.HIDE,
                "pluginspoofer.bypass",
                defaultConfig.getFakePlugins(),
                defaultConfig.getSpoofPluginsFormat(),
                defaultConfig.getPluginNameFormat(),
                defaultConfig.getPluginSeparator(),
                defaultConfig.getFakeVersionFormat(),
                defaultConfig.getUnknownPluginVersionMessage(),
                defaultConfig.getHiddenPluginNames(),
                true,
                true,
                false,
                defaultConfig.getUnknownCommandMessage(),
                true,
                SpoofConfig.BlockResponseMode.VANILLA_UNKNOWN,
                defaultConfig.getCustomBlockMessage(),
                defaultConfig.getBlockedCommands(),
                defaultConfig.getBlockedPatterns(),
                defaultConfig.getBlockedPrefixes(),
                true
        );

        List<String> completions = manager.getTabCompletions(hideConfig, "/ver ");
        assertTrue(completions.isEmpty());
    }

    @Test
    @DisplayName("TC-08: Verify /execute command recursive unwrapping")
    void testTC08_ExecuteCommandUnwrapping() {
        assertEquals("ver WorldEdit", manager.unwrapCommand("/execute as @p run ver WorldEdit"));
        assertEquals("grim reload", manager.unwrapCommand("/minecraft:execute if entity @p run grim reload"));
        assertEquals("plugins", manager.unwrapCommand("execute as @e run execute at @s run plugins"));
        assertEquals("spawn", manager.unwrapCommand("/spawn"));
    }

    @Test
    @DisplayName("TC-09: Verify Blocked Commands filtering")
    void testTC09_BlockedCommands() {
        assertTrue(manager.isBlockedCommand(defaultConfig, "/op player123"));
        assertTrue(manager.isBlockedCommand(defaultConfig, "/stop"));
        assertTrue(manager.isBlockedCommand(defaultConfig, "/reload"));
        assertTrue(manager.isBlockedCommand(defaultConfig, "/icanhasbukkit"));
        assertTrue(manager.isBlockedCommand(defaultConfig, "/execute run stop"));
        assertFalse(manager.isBlockedCommand(defaultConfig, "/warp spawn"));
    }

    @Test
    @DisplayName("TC-10: Verify Regex pattern command blocking")
    void testTC10_BlockedPatterns() {
        assertTrue(manager.isBlockedCommand(defaultConfig, "/minecraft:tell @a hello"));
        assertTrue(manager.isBlockedCommand(defaultConfig, "/bukkit:help"));
        assertTrue(manager.isBlockedCommand(defaultConfig, "/paper:version"));
        assertTrue(manager.isBlockedCommand(defaultConfig, "/spigot:version"));
    }

    @Test
    @DisplayName("TC-11: Verify Hacked Client Chat Prefixes detection")
    void testTC11_BlockedClientPrefixes() {
        assertTrue(manager.hasBlockedPrefix(defaultConfig, ".plugins"));
        assertTrue(manager.hasBlockedPrefix(defaultConfig, ".server plugins"));
        assertTrue(manager.hasBlockedPrefix(defaultConfig, "#ver"));
        assertTrue(manager.hasBlockedPrefix(defaultConfig, "@say test"));
        assertTrue(manager.hasBlockedPrefix(defaultConfig, ",pl"));
        assertTrue(manager.hasBlockedPrefix(defaultConfig, "!help"));
        assertFalse(manager.hasBlockedPrefix(defaultConfig, "Hello everyone in chat!"));
    }

    @Test
    @DisplayName("TC-12: Verify Block Response Modes")
    void testTC12_BlockResponseModes() {
        String vanillaMsg = manager.getBlockResponseMessage(defaultConfig);
        assertTrue(vanillaMsg.contains("Unknown or incomplete command"));

        SpoofConfig customConfig = new SpoofConfig(
                true,
                SpoofConfig.SpoofMode.SPOOF,
                "pluginspoofer.bypass",
                defaultConfig.getFakePlugins(),
                defaultConfig.getSpoofPluginsFormat(),
                defaultConfig.getPluginNameFormat(),
                defaultConfig.getPluginSeparator(),
                defaultConfig.getFakeVersionFormat(),
                defaultConfig.getUnknownPluginVersionMessage(),
                defaultConfig.getHiddenPluginNames(),
                true,
                true,
                true,
                defaultConfig.getUnknownCommandMessage(),
                true,
                SpoofConfig.BlockResponseMode.CUSTOM,
                "&cYou do not have permission to execute this command.",
                defaultConfig.getBlockedCommands(),
                defaultConfig.getBlockedPatterns(),
                defaultConfig.getBlockedPrefixes(),
                true
        );

        String customMsg = manager.getBlockResponseMessage(customConfig);
        assertTrue(customMsg.contains("You do not have permission"));
    }
}
