package sbuild.materials;

/**
 * Агрегированная строка отчёта по конкретному материалу.
 */
public record MaterialRequirement(
    String materialKey,
    long totalRequired,
    long alreadyBuilt,
    long remaining,
    long availableInInventory
) {
    public MaterialRequirement {
        totalRequired = Math.max(0L, totalRequired);
        alreadyBuilt = Math.max(0L, alreadyBuilt);
        remaining = Math.max(0L, remaining);
        availableInInventory = Math.max(0L, availableInInventory);
    }

    public boolean isComplete() {
        return remaining == 0;
    }

    public long missingInInventory() {
        return Math.max(0L, remaining - availableInInventory);
    }
}
