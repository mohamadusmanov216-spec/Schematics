package sbuild.materials;

import sbuild.schematic.LoadedSchematic;
import sbuild.schematic.PlacementController;
import sbuild.schematic.SchematicBlockState;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Calculates required/built/remaining materials from transformed placement data.
 */
public final class MaterialTracker {
    private static final String AIR_ITEM = "minecraft:air";

    private final ItemResolver itemResolver;

    public MaterialTracker(ItemResolver itemResolver) {
        this.itemResolver = itemResolver;
    }

    public MaterialReport analyze(
        PlacementController placement,
        Map<LoadedSchematic.BlockPosition, String> placedWorldBlocks,
        MaterialAvailability availability
    ) {
        Objects.requireNonNull(placement, "placement");
        Objects.requireNonNull(placedWorldBlocks, "placedWorldBlocks");
        Objects.requireNonNull(availability, "availability");

        Map<String, Long> requiredByMaterial = new LinkedHashMap<>();
        Map<String, Long> builtByMaterial = new LinkedHashMap<>();

        for (Map.Entry<LoadedSchematic.BlockPosition, SchematicBlockState> requiredEntry : placement.transformedEntries()) {
            SchematicBlockState requiredState = requiredEntry.getValue();
            String requiredMaterial = itemResolver.resolveItemKey(requiredState.key());
            if (requiredState.isAir() || AIR_ITEM.equals(requiredMaterial)) {
                continue;
            }
            increment(requiredByMaterial, requiredMaterial, 1L);

            String worldBlockState = placedWorldBlocks.get(requiredEntry.getKey());
            if (worldBlockState == null) {
                continue;
            }

            String worldMaterial = itemResolver.resolveItemKey(worldBlockState);
            if (requiredMaterial.equals(worldMaterial)) {
                increment(builtByMaterial, requiredMaterial, 1L);
            }
        }

        if (requiredByMaterial.isEmpty()) {
            return MaterialReport.empty();
        }

        return buildReport(requiredByMaterial, builtByMaterial, availability);
    }

    public MaterialReport analyzeRequiredOnly(PlacementController placement, MaterialAvailability availability) {
        return analyze(placement, Map.of(), availability);
    }

    private MaterialReport buildReport(
        Map<String, Long> requiredByMaterial,
        Map<String, Long> builtByMaterial,
        MaterialAvailability availability
    ) {
        Map<String, MaterialRequirement> rows = new LinkedHashMap<>();
        long totalRequired = 0L;
        long totalBuilt = 0L;
        long totalRemaining = 0L;
        long totalAvailable = 0L;

        for (Map.Entry<String, Long> requiredEntry : requiredByMaterial.entrySet()) {
            String material = requiredEntry.getKey();
            long required = requiredEntry.getValue();
            long built = Math.min(required, builtByMaterial.getOrDefault(material, 0L));
            long remaining = required - built;
            long available = availability.countOf(material);

            rows.put(material, new MaterialRequirement(material, required, built, remaining, available));
            totalRequired += required;
            totalBuilt += built;
            totalRemaining += remaining;
            totalAvailable += available;
        }

        return new MaterialReport(rows, totalRequired, totalBuilt, totalRemaining, totalAvailable);
    }

    private void increment(Map<String, Long> target, String key, long delta) {
        target.merge(key, delta, Long::sum);
    }
}
