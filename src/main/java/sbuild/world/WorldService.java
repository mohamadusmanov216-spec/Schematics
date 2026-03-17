package sbuild.world;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import sbuild.schematic.LoadedSchematic;
import sbuild.util.BlockStateKeyCodec;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * World query primitives used by planner/material analysis.
 */
public final class WorldService {
    private final BlockStateKeyCodec stateKeyCodec = new BlockStateKeyCodec();

    public Map<LoadedSchematic.BlockPosition, String> snapshotBlockStates(
        ServerWorld world,
        Collection<LoadedSchematic.BlockPosition> positions
    ) {
        Map<LoadedSchematic.BlockPosition, String> out = new HashMap<>();
        for (LoadedSchematic.BlockPosition position : positions) {
            BlockPos blockPos = new BlockPos(position.x(), position.y(), position.z());
            out.put(position, stateKeyCodec.toKey(world.getBlockState(blockPos)));
        }
        return Map.copyOf(out);
    }
}
