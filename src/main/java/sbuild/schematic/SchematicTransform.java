package sbuild.schematic;

import java.util.List;

/**
 * Иммутабельное описание трансформации схематики:
 * поворот, зеркалирование и смещение.
 */
public record SchematicTransform(
    Rotation rotation,
    Mirror mirror,
    int offsetX,
    int offsetY,
    int offsetZ
) {
    public static SchematicTransform identity() {
        return new SchematicTransform(Rotation.NONE, Mirror.NONE, 0, 0, 0);
    }

    public LoadedSchematic.BlockPosition apply(LoadedSchematic.BlockPosition position, SchematicBoundingBox bounds) {
        LocalFrame frame = LocalFrame.from(bounds);
        LocalPoint local = frame.normalize(position);

        LocalPoint mirrored = applyMirror(local, frame);
        LocalPoint rotated = applyRotation(mirrored, frame);

        return frame.denormalize(rotated, offsetX, offsetY, offsetZ);
    }

    public SchematicBoundingBox transformBounds(SchematicBoundingBox bounds) {
        List<LoadedSchematic.BlockPosition> transformedCorners = List.of(
            apply(new LoadedSchematic.BlockPosition(bounds.min().x(), bounds.min().y(), bounds.min().z()), bounds),
            apply(new LoadedSchematic.BlockPosition(bounds.min().x(), bounds.min().y(), bounds.max().z()), bounds),
            apply(new LoadedSchematic.BlockPosition(bounds.max().x(), bounds.min().y(), bounds.min().z()), bounds),
            apply(new LoadedSchematic.BlockPosition(bounds.max().x(), bounds.min().y(), bounds.max().z()), bounds),
            apply(new LoadedSchematic.BlockPosition(bounds.min().x(), bounds.max().y(), bounds.min().z()), bounds),
            apply(new LoadedSchematic.BlockPosition(bounds.min().x(), bounds.max().y(), bounds.max().z()), bounds),
            apply(new LoadedSchematic.BlockPosition(bounds.max().x(), bounds.max().y(), bounds.min().z()), bounds),
            apply(new LoadedSchematic.BlockPosition(bounds.max().x(), bounds.max().y(), bounds.max().z()), bounds)
        );
        return SchematicBoundingBox.fromPositions(transformedCorners);
    }

    private LocalPoint applyMirror(LocalPoint point, LocalFrame frame) {
        int maxX = frame.sizeX() - 1;
        int maxZ = frame.sizeZ() - 1;
        return switch (mirror) {
            case NONE -> point;
            case X -> new LocalPoint(maxX - point.x(), point.y(), point.z());
            case Z -> new LocalPoint(point.x(), point.y(), maxZ - point.z());
        };
    }

    private LocalPoint applyRotation(LocalPoint point, LocalFrame frame) {
        int maxX = frame.sizeX() - 1;
        int maxZ = frame.sizeZ() - 1;
        return switch (rotation) {
            case NONE -> point;
            case CLOCKWISE_90 -> new LocalPoint(maxZ - point.z(), point.y(), point.x());
            case CLOCKWISE_180 -> new LocalPoint(maxX - point.x(), point.y(), maxZ - point.z());
            case CLOCKWISE_270 -> new LocalPoint(point.z(), point.y(), maxX - point.x());
        };
    }

    private record LocalFrame(LoadedSchematic.BlockPosition min, int sizeX, int sizeY, int sizeZ) {
        static LocalFrame from(SchematicBoundingBox bounds) {
            return new LocalFrame(bounds.min(), bounds.sizeX(), bounds.sizeY(), bounds.sizeZ());
        }

        LocalPoint normalize(LoadedSchematic.BlockPosition position) {
            return new LocalPoint(position.x() - min.x(), position.y() - min.y(), position.z() - min.z());
        }

        LoadedSchematic.BlockPosition denormalize(LocalPoint local, int offsetX, int offsetY, int offsetZ) {
            return new LoadedSchematic.BlockPosition(
                local.x() + min.x() + offsetX,
                local.y() + min.y() + offsetY,
                local.z() + min.z() + offsetZ
            );
        }
    }

    private record LocalPoint(int x, int y, int z) {}

    public enum Rotation {
        NONE,
        CLOCKWISE_90,
        CLOCKWISE_180,
        CLOCKWISE_270
    }

    public enum Mirror {
        NONE,
        X,
        Z
    }
}
