package sbuild.materials;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Итоговый отчёт material analysis в GUI-friendly виде.
 */
public record MaterialReport(
    Map<String, MaterialRequirement> requirementsByMaterial,
    long totalRequired,
    long totalAlreadyBuilt,
    long totalRemaining,
    long totalAvailableInInventory
) {
    public MaterialReport {
        requirementsByMaterial = Map.copyOf(requirementsByMaterial);
    }

    public static MaterialReport empty() {
        return new MaterialReport(Map.of(), 0L, 0L, 0L, 0L);
    }

    /**
     * Сортированное представление для таблиц GUI.
     */
    public List<MaterialRequirement> rows() {
        return requirementsByMaterial.values().stream()
            .sorted(Comparator.comparing(MaterialRequirement::materialKey))
            .toList();
    }
}
