package sbuild.bot;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Selects and consumes inventory resources for bot build actions.
 */
public final class InventoryController {
    public boolean selectHotbarBlock(ServerPlayerEntity player, Block block) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() == block) {
                player.getInventory().setSelectedSlot(slot);
                return true;
            }
        }
        return false;
    }

    public boolean consumeSelectedBlock(ServerPlayerEntity player) {
        ItemStack selected = player.getMainHandStack();
        if (selected.isEmpty()) {
            return false;
        }
        selected.decrement(1);
        return true;
    }
}
