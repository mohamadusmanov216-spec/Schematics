package sbuild.block;

import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Resolves and executes block placement strategies.
 */
public final class BlockPlacementService {
    private final List<BlockPlacementStrategy> strategies;
    private final BlockPlacementStrategy fallbackStrategy;

    public BlockPlacementService() {
        this.strategies = List.of(
            new ObserverPlacementStrategy(),
            new HopperPlacementStrategy(),
            new RepeaterPlacementStrategy(),
            new ComparatorPlacementStrategy(),
            new StairsPlacementStrategy(),
            new SlabPlacementStrategy(),
            new TrapdoorPlacementStrategy(),
            new PistonPlacementStrategy(),
            new RedstoneWirePlacementStrategy()
        );
        this.fallbackStrategy = new DefaultPlacementStrategy();
    }

    public void initialize() {
        // No-op for now. Strategies are stateless and eagerly initialized.
    }

    public boolean place(ServerWorld world, ServerPlayerEntity player, BlockPos targetPos, BlockState desiredState) {
        PlacementContext context = new PlacementContext(world, player, targetPos, desiredState);
        BlockPlacementStrategy strategy = resolveStrategy(context);
        return strategy.place(context);
    }

    private BlockPlacementStrategy resolveStrategy(PlacementContext context) {
        for (BlockPlacementStrategy strategy : strategies) {
            if (strategy.supports(context)) {
                return strategy;
            }
        }
        return fallbackStrategy;
    }

    private static final class DefaultPlacementStrategy extends AbstractBlockPlacementStrategy {
        @Override
        public boolean supports(PlacementContext context) {
            return true;
        }

        @Override
        public net.minecraft.util.math.Direction determinePlayerLookDirection(PlacementContext context) {
            return context.player().getHorizontalFacing();
        }

        @Override
        public PlacementPlan createPlan(
            PlacementContext context,
            net.minecraft.util.math.Direction lookDirection,
            net.minecraft.util.math.Direction faceToClick
        ) {
            return new PlacementPlan(context.desiredState(), lookDirection, faceToClick);
        }
    }
}
