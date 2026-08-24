package dev.khoa.plugin.pluginspoofer.config;

import java.util.Collections;
import java.util.List;

/**
 * Immutable model representing a fake/spoofed plugin definition.
 */
public record FakePluginModel(
        String name,
        String version,
        List<String> authors,
        String description,
        List<String> fakeCommands
) {
    public FakePluginModel {
        authors = authors != null ? List.copyOf(authors) : Collections.emptyList();
        fakeCommands = fakeCommands != null ? List.copyOf(fakeCommands) : Collections.emptyList();
    }

    public String formattedAuthors() {
        return String.join(", ", authors);
    }
}
