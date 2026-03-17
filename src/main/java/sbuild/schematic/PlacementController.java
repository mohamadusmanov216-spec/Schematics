package sbuild.schematic;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Управляет размещением схематики в мире с учетом трансформаций.
 *
 * <p>Класс отдаёт нормализованные данные для следующего этапа:
 * planner/bot смогут забирать готовые world-координаты блоков.</p>
 */
public final class PlacementController {
    private final LoadedSchematic schematic;
    private final SchematicTransform transform;
    private final Map<LoadedSchematic.BlockPosition, SchematicBlockState> transformedBlocks;
    private final SchematicBoundingBox transformedBoundingBox;

    public PlacementController(LoadedSchematic schematic, SchematicTransform transform) {
        this.schematic = schematic;
        this.transform = transform;
        this.transformedBlocks = buildTransformedBlocks(schematic, transform);
        this.transformedBoundingBox = transform.transformBounds(schematic.boundingBox());
    }

    public LoadedSchematic schematic() {
        return schematic;
    }

    public SchematicTransform transform() {
        return transform;
    }

    public Optional<SchematicBlockState> blockAtTransformed(LoadedSchematic.BlockPosition transformedPosition) {
        return Optional.ofNullable(transformedBlocks.get(transformedPosition));
    }

    public Optional<String> blockStateKeyAtTransformed(LoadedSchematic.BlockPosition transformedPosition) {
        return blockAtTransformed(transformedPosition).map(SchematicBlockState::key);
    }

    public List<Map.Entry<LoadedSchematic.BlockPosition, SchematicBlockState>> transformedEntries() {
        return List.copyOf(transformedBlocks.entrySet());
    }

    public SchematicBoundingBox transformedBoundingBox() {
        return transformedBoundingBox;
    }

    public Map<SchematicBlockState, Long> requiredBlockStates() {
        return schematic.requiredBlockStates();
    }

    public Map<String, Long> requiredBlockStateKeys() {
        return schematic.requiredBlockStateKeys();
    }

    private static Map<LoadedSchematic.BlockPosition, SchematicBlockState> buildTransformedBlocks(
        LoadedSchematic schematic,
        SchematicTransform transform
    ) {
        Map<LoadedSchematic.BlockPosition, SchematicBlockState> transformed = new LinkedHashMap<>();
        SchematicBoundingBox bounds = schematic.boundingBox();

        for (Map.Entry<LoadedSchematic.BlockPosition, SchematicBlockState> entry : schematic.entries()) {
            LoadedSchematic.BlockPosition transformedPos = transform.apply(entry.getKey(), bounds);
            transformed.put(transformedPos, entry.getValue());
        }

        return Map.copyOf(transformed);
    }
}
