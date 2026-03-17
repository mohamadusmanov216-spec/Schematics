package sbuild.storage;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.Map;

public final class StorageScanner {
    public Map<String, Long> scanChestInventory(ServerWorld world, StoragePoint point) {
        BlockEntity blockEntity = world.getBlockEntity(point.blockPos());
        if (!(blockEntity instanceof Inventory inventory)) {
            return Map.of();
        }
        return scanInventory(inventory);
    }

    public Map<String, Long> scanInventory(Inventory inventory) {
        Map<String, Long> counts = new HashMap<>();
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (stack.isEmpty()) {
                continue;
            }
            String itemId = Registries.ITEM.getId(stack.getItem()).toString();
            counts.merge(itemId, (long) stack.getCount(), Long::sum);
        }
        return Map.copyOf(counts);
    }
}
