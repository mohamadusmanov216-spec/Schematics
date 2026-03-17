package sbuild.bot;

import net.minecraft.block.Block;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Handles block placement with inventory and look synchronization.
 */
public final class PlaceController {
    private final InventoryController inventoryController;
    private final LookController lookController;

    public PlaceController(InventoryController inventoryController, LookController lookController) {
        this.inventoryController = inventoryController;
        this.lookController = lookController;
    }

    public boolean placeBlock(ServerWorld world, ServerPlayerEntity player, BlockPos pos, Block block) {
        if (!world.isAir(pos)) {
            return false;
        }
        lookController.lookAtBlock(player, pos);
        if (!inventoryController.selectHotbarBlock(player, block)) {
            return false;
        }
        if (!inventoryController.consumeSelectedBlock(player)) {
            return false;
        }
        return world.setBlockState(pos, block.getDefaultState());
    }
}
