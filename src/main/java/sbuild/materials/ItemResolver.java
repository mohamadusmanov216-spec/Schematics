package sbuild.materials;

import java.util.Locale;
import java.util.Map;

/**
 * Преобразует block state из схематики в material key для инвентаря/отчета.
 *
 * <p>Ключ выбран строковым (`minecraft:...`) для простого GUI-рендеринга
 * и независимости от runtime-объектов мира на текущем этапе.</p>
 */
public final class ItemResolver {
    private static final Map<String, String> BLOCK_TO_ITEM_OVERRIDES = Map.of(
        "minecraft:redstone_wire", "minecraft:redstone",
        "minecraft:wall_torch", "minecraft:torch",
        "minecraft:wall_sign", "minecraft:oak_sign",
        "minecraft:lava", "minecraft:lava_bucket",
        "minecraft:water", "minecraft:water_bucket"
    );

    /**
     * Возвращает material key для block state.
     *
     * @param blockStateKey пример: minecraft:oak_planks или minecraft:oak_stairs[facing=north]
     */
    public String resolveItemKey(String blockStateKey) {
        String normalized = normalizeBlockName(blockStateKey);
        return BLOCK_TO_ITEM_OVERRIDES.getOrDefault(normalized, normalized);
    }

    /**
     * Возвращает canonical block id без state-параметров.
     */
    public String normalizeBlockName(String blockStateKey) {
        String raw = blockStateKey == null ? "minecraft:air" : blockStateKey.trim().toLowerCase(Locale.ROOT);
        int propertyStart = raw.indexOf('[');
        if (propertyStart >= 0) {
            return raw.substring(0, propertyStart);
        }
        return raw;
    }
}
