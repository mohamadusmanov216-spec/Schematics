package sbuild.storage;

import sbuild.materials.MaterialReport;

import java.util.Comparator;
import java.util.List;

/**
 * Планирует пополнение ресурсов для строительного бота.
 */
public final class RestockPlanner {
    public RestockPlan buildPlan(MaterialReport report) {
        List<RestockEntry> missing = report.rows().stream()
            .filter(req -> req.missingInInventory() > 0)
            .map(req -> new RestockEntry(req.materialKey(), req.missingInInventory()))
            .sorted(Comparator.comparingLong(RestockEntry::count).reversed())
            .toList();

        return new RestockPlan(missing);
    }

    public record RestockPlan(List<RestockEntry> entries) {
        public RestockPlan {
            entries = List.copyOf(entries);
        }

        public boolean isEmpty() {
            return entries.isEmpty();
        }
    }

    public record RestockEntry(String materialKey, long count) {
    }
}
