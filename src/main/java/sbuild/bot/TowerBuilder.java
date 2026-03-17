package sbuild.bot;

import net.minecraft.block.Block;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Executes simple vertical tower/scaffold growth.
 */
public final class TowerBuilder {
    private final JumpScaffoldController jumpScaffoldController;

    public TowerBuilder(JumpScaffoldController jumpScaffoldController) {
        this.jumpScaffoldController = jumpScaffoldController;
    }

    public boolean buildStepUp(ServerWorld world, ServerPlayerEntity player, int targetY, Block scaffoldBlock) {
        if (player.getBlockY() >= targetY) {
            return true;
        }
        if (!jumpScaffoldController.placeScaffoldBelow(world, player, scaffoldBlock)) {
            return false;
        }
        player.jump();
        return player.getBlockY() >= targetY;
    }
}
