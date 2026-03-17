package sbuild.bot;

import net.minecraft.block.Block;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Places temporary scaffold blocks for vertical reach.
 */
public final class JumpScaffoldController {
    private final PlaceController placeController;

    public JumpScaffoldController(PlaceController placeController) {
        this.placeController = placeController;
    }

    public boolean placeScaffoldBelow(ServerWorld world, ServerPlayerEntity player, Block scaffoldBlock) {
        BlockPos below = player.getBlockPos().down();
        if (!world.isAir(below)) {
            return true;
        }
        return placeController.placeBlock(world, player, below, scaffoldBlock);
    }
}
