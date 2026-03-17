package sbuild.schematic;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Canonical block state representation used by schematic pipeline.
 */
public record SchematicBlockState(String blockName, Map<String, String> properties) {
    public static final String AIR_BLOCK = "minecraft:air";
    public static final SchematicBlockState AIR = new SchematicBlockState(AIR_BLOCK, Map.of());

    public SchematicBlockState {
        Objects.requireNonNull(blockName, "blockName");
        if (blockName.isBlank()) {
            throw new IllegalArgumentException("blockName cannot be blank");
        }
        Objects.requireNonNull(properties, "properties");
        Map<String, String> normalized = new TreeMap<>();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String key = Objects.requireNonNull(entry.getKey(), "property key");
            String value = Objects.requireNonNull(entry.getValue(), "property value");
            normalized.put(key, value);
        }
        properties = Map.copyOf(normalized);
    }

    public static SchematicBlockState of(String blockName, Map<String, String> properties) {
        if (AIR_BLOCK.equals(blockName) && properties.isEmpty()) {
            return AIR;
        }
        return new SchematicBlockState(blockName, properties);
    }

    public String key() {
        if (properties.isEmpty()) {
            return blockName;
        }
        String props = properties.entrySet().stream()
            .sorted(Comparator.comparing(Map.Entry::getKey))
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .collect(Collectors.joining(","));
        return blockName + "[" + props + "]";
    }

    public boolean isAir() {
        return AIR_BLOCK.equals(blockName);
    }
}
