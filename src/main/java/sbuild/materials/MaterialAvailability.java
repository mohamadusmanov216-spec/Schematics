package sbuild.materials;

import java.util.Map;

/**
 * Снимок доступных материалов (например, игрок + склад).
 */
public record MaterialAvailability(Map<String, Long> availableCounts) {
    public MaterialAvailability {
        availableCounts = Map.copyOf(availableCounts);
    }

    public static MaterialAvailability empty() {
        return new MaterialAvailability(Map.of());
    }

    public long countOf(String itemKey) {
        return Math.max(0L, availableCounts.getOrDefault(itemKey, 0L));
    }
}
