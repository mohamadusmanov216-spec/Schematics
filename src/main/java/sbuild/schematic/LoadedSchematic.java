package sbuild.schematic;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record LoadedSchematic(
    String id,
    String name,
    String format,
    Path sourcePath,
    long fileSizeBytes,
    Instant lastModified,
    SchematicBoundingBox boundingBox,
    Map<BlockPosition, SchematicBlockState> blocks,
    BlockDomain domain,
    SchematicStats stats,
    Map<String, String> metadata
) {
    public LoadedSchematic {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(sourcePath, "sourcePath");
        Objects.requireNonNull(lastModified, "lastModified");
        Objects.requireNonNull(boundingBox, "boundingBox");
        Objects.requireNonNull(blocks, "blocks");
        Objects.requireNonNull(stats, "stats");
        Objects.requireNonNull(metadata, "metadata");

        if (id.isBlank() || name.isBlank() || format.isBlank()) {
            throw new IllegalArgumentException("id/name/format must be non-blank");
        }
        if (fileSizeBytes < 0L) {
            throw new IllegalArgumentException("fileSizeBytes cannot be negative");
        }

        blocks = Map.copyOf(blocks);
        domain = domain == null ? BlockDomain.fromBlocks(blocks) : domain;
        metadata = Map.copyOf(metadata);
    }

    public Optional<SchematicBlockState> blockAt(BlockPosition position) {
        return Optional.ofNullable(blocks.get(position));
    }

    public Optional<String> blockStateKeyAt(BlockPosition position) {
        return blockAt(position).map(SchematicBlockState::key);
    }

    public int blockCount() {
        return blocks.size();
    }

    public Map<SchematicBlockState, Long> requiredBlockStates() {
        return domain.requiredStates();
    }

    public Map<String, Long> requiredBlockStateKeys() {
        return domain.requiredStateKeys();
    }

    public Collection<Map.Entry<BlockPosition, SchematicBlockState>> entries() {
        return List.copyOf(blocks.entrySet());
    }

    public Collection<Map.Entry<BlockPosition, SchematicBlockState>> layerEntries(int y) {
        return domain.layerEntries(y);
    }

    public List<BlockPosition> positionsForStateKey(String stateKey) {
        return domain.positionsForStateKey(stateKey);
    }

    public record BlockDomain(
        Map<Integer, Map<BlockPosition, SchematicBlockState>> layers,
        Map<SchematicBlockState, Long> requiredStates,
        Map<String, Long> requiredStateKeys,
        Map<String, List<BlockPosition>> positionsByStateKey
    ) {
        static BlockDomain fromBlocks(Map<BlockPosition, SchematicBlockState> blocks) {
            Map<Integer, Map<BlockPosition, SchematicBlockState>> layers = new LinkedHashMap<>();
            Map<SchematicBlockState, Long> requiredStates = new LinkedHashMap<>();
            Map<String, Long> requiredStateKeys = new LinkedHashMap<>();
            Map<String, List<BlockPosition>> positionsByStateKey = new LinkedHashMap<>();

            for (Map.Entry<BlockPosition, SchematicBlockState> entry : blocks.entrySet()) {
                BlockPosition position = entry.getKey();
                SchematicBlockState state = entry.getValue();

                layers.computeIfAbsent(position.y(), ignored -> new LinkedHashMap<>()).put(position, state);
                if (state.isAir()) {
                    continue;
                }

                requiredStates.merge(state, 1L, Long::sum);
                requiredStateKeys.merge(state.key(), 1L, Long::sum);
                positionsByStateKey.computeIfAbsent(state.key(), ignored -> new java.util.ArrayList<>()).add(position);
            }

            Map<Integer, Map<BlockPosition, SchematicBlockState>> immutableLayers = new LinkedHashMap<>();
            for (Map.Entry<Integer, Map<BlockPosition, SchematicBlockState>> entry : layers.entrySet()) {
                immutableLayers.put(entry.getKey(), Map.copyOf(entry.getValue()));
            }

            Map<String, List<BlockPosition>> immutablePositions = new LinkedHashMap<>();
            for (Map.Entry<String, List<BlockPosition>> entry : positionsByStateKey.entrySet()) {
                immutablePositions.put(entry.getKey(), List.copyOf(entry.getValue()));
            }

            return new BlockDomain(
                Map.copyOf(immutableLayers),
                Map.copyOf(requiredStates),
                Map.copyOf(requiredStateKeys),
                Map.copyOf(immutablePositions)
            );
        }

        Collection<Map.Entry<BlockPosition, SchematicBlockState>> layerEntries(int y) {
            Map<BlockPosition, SchematicBlockState> layer = layers.get(y);
            return layer == null ? List.of() : List.copyOf(layer.entrySet());
        }

        List<BlockPosition> positionsForStateKey(String stateKey) {
            return positionsByStateKey.getOrDefault(stateKey, List.of());
        }
    }

    public record BlockPosition(int x, int y, int z) {
        public BlockPosition add(int dx, int dy, int dz) {
            return new BlockPosition(x + dx, y + dy, z + dz);
        }

        public int manhattanDistance(BlockPosition other) {
            return Math.abs(x - other.x) + Math.abs(y - other.y) + Math.abs(z - other.z);
        }
    }

    public record SchematicStats(int regionCount, int paletteEntries, int airBlocks, int solidBlocks) {
        public SchematicStats {
            if (regionCount < 0 || paletteEntries < 0 || airBlocks < 0 || solidBlocks < 0) {
                throw new IllegalArgumentException("Schematic stats cannot be negative");
            }
        }
    }
}
