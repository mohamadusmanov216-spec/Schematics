package sbuild.materials;

import sbuild.schematic.LoadedSchematic;
import sbuild.schematic.PlacementController;

import java.util.Map;

/**
 * Material analysis entrypoint for commands/planner/UI.
 */
public final class MaterialAnalysisService {
    private final ItemResolver itemResolver;
    private final MaterialTracker materialTracker;

    public MaterialAnalysisService() {
        this.itemResolver = new ItemResolver();
        this.materialTracker = new MaterialTracker(itemResolver);
    }

    public MaterialReport analyze(
        PlacementController placement,
        Map<LoadedSchematic.BlockPosition, String> placedWorldBlocks,
        MaterialAvailability availability
    ) {
        return materialTracker.analyze(placement, placedWorldBlocks, availability);
    }

    public MaterialReport analyzeRequiredOnly(PlacementController placement, MaterialAvailability availability) {
        return materialTracker.analyzeRequiredOnly(placement, availability);
    }

    public ItemResolver itemResolver() {
        return itemResolver;
    }
}
