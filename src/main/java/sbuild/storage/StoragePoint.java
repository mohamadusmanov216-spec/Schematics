package sbuild.storage;

import net.minecraft.util.math.BlockPos;

import java.util.Set;

/**
 * Точка хранения материалов для бота-строителя.
 */
public record StoragePoint(
    BlockPos blockPos,
    String name,
    int priority,
    Set<String> filterTags
) {
    public StoragePoint {
        filterTags = Set.copyOf(filterTags);
    }

    public StoragePoint withPriority(int newPriority) {
        return new StoragePoint(blockPos, name, newPriority, filterTags);
    }

    public StoragePoint withFilterTags(Set<String> tags) {
        return new StoragePoint(blockPos, name, priority, tags);
    }
}
