package sbuild.schematic;

import java.util.Collection;

/**
 * Неизменяемый AABB схематики в локальных координатах.
 */
public record SchematicBoundingBox(
    LoadedSchematic.BlockPosition min,
    LoadedSchematic.BlockPosition max
) {
    public SchematicBoundingBox {
        if (min.x() > max.x() || min.y() > max.y() || min.z() > max.z()) {
            throw new IllegalArgumentException("Invalid bounding box: min must be <= max");
        }
    }

    public static SchematicBoundingBox singleBlockOrigin() {
        LoadedSchematic.BlockPosition origin = new LoadedSchematic.BlockPosition(0, 0, 0);
        return new SchematicBoundingBox(origin, origin);
    }

    public static SchematicBoundingBox fromPositions(Collection<LoadedSchematic.BlockPosition> positions) {
        if (positions.isEmpty()) {
            return singleBlockOrigin();
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (LoadedSchematic.BlockPosition position : positions) {
            minX = Math.min(minX, position.x());
            minY = Math.min(minY, position.y());
            minZ = Math.min(minZ, position.z());
            maxX = Math.max(maxX, position.x());
            maxY = Math.max(maxY, position.y());
            maxZ = Math.max(maxZ, position.z());
        }

        return new SchematicBoundingBox(
            new LoadedSchematic.BlockPosition(minX, minY, minZ),
            new LoadedSchematic.BlockPosition(maxX, maxY, maxZ)
        );
    }

    public int sizeX() { return (max.x() - min.x()) + 1; }
    public int sizeY() { return (max.y() - min.y()) + 1; }
    public int sizeZ() { return (max.z() - min.z()) + 1; }

    public boolean contains(LoadedSchematic.BlockPosition position) {
        return position.x() >= min.x() && position.x() <= max.x()
            && position.y() >= min.y() && position.y() <= max.y()
            && position.z() >= min.z() && position.z() <= max.z();
    }
}
