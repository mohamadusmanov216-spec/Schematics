package sbuild.storage;

import net.minecraft.util.math.BlockPos;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Реестр зарегистрированных хранилищ (сундуков).
 */
public final class StorageRegistry {
    private final ConcurrentMap<String, StoragePoint> byName = new ConcurrentHashMap<>();

    public StoragePoint register(StoragePoint point) {
        byName.put(point.name(), point);
        return point;
    }

    public Optional<StoragePoint> findByName(String name) {
        return Optional.ofNullable(byName.get(name));
    }

    public Optional<StoragePoint> findByPos(BlockPos pos) {
        return byName.values().stream().filter(point -> point.blockPos().equals(pos)).findFirst();
    }

    public List<StoragePoint> list() {
        return byName.values().stream()
            .sorted(Comparator.comparingInt(StoragePoint::priority).thenComparing(StoragePoint::name))
            .toList();
    }

    public void clear() {
        byName.clear();
    }

    public int size() {
        return byName.size();
    }
}
