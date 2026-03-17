package sbuild.redstone;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Map;

/**
 * Builds component dependency graph from block map.
 */
public final class RedstoneDependencyAnalyzer {
    public RedstoneGraph analyze(Map<BlockPos, BlockState> blocks) {
        RedstoneGraph graph = new RedstoneGraph();

        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            RedstoneNode node = new RedstoneNode(entry.getKey(), entry.getValue(), classify(entry.getValue()));
            graph.addNode(node);
        }

        for (RedstoneNode node : graph.nodes()) {
            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = node.pos().offset(direction);
                RedstoneNode neighbor = graph.nodeAt(neighborPos);
                if (neighbor == null) {
                    continue;
                }
                if (!isDependency(node, neighbor, direction)) {
                    continue;
                }

                int weight = node.type() == RedstoneNode.Type.WIRE ? 1 : 0;
                boolean repeaterReset = node.type() == RedstoneNode.Type.REPEATER;
                graph.addEdge(new RedstoneEdge(node, neighbor, direction, weight, repeaterReset));
            }
        }

        return graph;
    }

    private boolean isDependency(RedstoneNode from, RedstoneNode to, Direction direction) {
        if (from.type() == RedstoneNode.Type.REPEATER && from.state().contains(Properties.HORIZONTAL_FACING)) {
            return from.state().get(Properties.HORIZONTAL_FACING) == direction;
        }
        if (from.type() == RedstoneNode.Type.OBSERVER && from.state().contains(Properties.FACING)) {
            return from.state().get(Properties.FACING) == direction;
        }
        return true;
    }

    private RedstoneNode.Type classify(BlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.REDSTONE_WIRE) {
            return RedstoneNode.Type.WIRE;
        }
        if (block == Blocks.REPEATER) {
            return RedstoneNode.Type.REPEATER;
        }
        if (block == Blocks.COMPARATOR) {
            return RedstoneNode.Type.COMPARATOR;
        }
        if (block == Blocks.OBSERVER) {
            return RedstoneNode.Type.OBSERVER;
        }
        if (block == Blocks.REDSTONE_BLOCK || block == Blocks.LEVER || block == Blocks.REDSTONE_TORCH) {
            return RedstoneNode.Type.POWER_SOURCE;
        }
        if (block == Blocks.REDSTONE_LAMP || block == Blocks.PISTON || block == Blocks.STICKY_PISTON) {
            return RedstoneNode.Type.CONSUMER;
        }
        return RedstoneNode.Type.OTHER;
    }
}
