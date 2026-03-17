package sbuild.bot;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Builds and executes lightweight block-to-block paths.
 */
public final class PathExecutor {
    private static final int MAX_EXPANDED_NODES = 2048;
    private static final int MAX_STEP_DISTANCE = 96;

    private final MovementController movementController;
    private final LookController lookController;

    public PathExecutor(MovementController movementController, LookController lookController) {
        this.movementController = movementController;
        this.lookController = lookController;
    }

    public Deque<BlockPos> createPath(ServerWorld world, BlockPos from, BlockPos to) {
        if (from.getSquaredDistance(to) > (double) MAX_STEP_DISTANCE * MAX_STEP_DISTANCE) {
            return directPath(from, to);
        }

        Map<BlockPos, BlockPos> parent = new HashMap<>();
        Map<BlockPos, Integer> gScore = new HashMap<>();
        PriorityQueue<PathNode> open = new PriorityQueue<>(Comparator.comparingInt(PathNode::fScore));

        BlockPos start = from.toImmutable();
        BlockPos goal = to.toImmutable();
        gScore.put(start, 0);
        open.add(new PathNode(start, heuristic(start, goal), 0));

        int expanded = 0;
        while (!open.isEmpty() && expanded < MAX_EXPANDED_NODES) {
            PathNode current = open.poll();
            if (current.pos().equals(goal)) {
                return reconstructPath(parent, goal, start);
            }

            if (current.gScore() > gScore.getOrDefault(current.pos(), Integer.MAX_VALUE)) {
                continue;
            }

            expanded++;
            for (BlockPos neighbor : neighbors(current.pos())) {
                if (!isWalkable(world, neighbor)) {
                    continue;
                }

                int tentativeG = current.gScore() + movementCost(current.pos(), neighbor);
                if (tentativeG >= gScore.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                    continue;
                }

                parent.put(neighbor, current.pos());
                gScore.put(neighbor, tentativeG);
                int fScore = tentativeG + heuristic(neighbor, goal);
                open.add(new PathNode(neighbor, fScore, tentativeG));
            }
        }

        return directPath(from, to);
    }

    public boolean executeStep(ServerPlayerEntity player, Deque<BlockPos> path) {
        BlockPos next = path.peekFirst();
        if (next == null) {
            return true;
        }

        lookController.lookAt(player, Vec3d.ofCenter(next));
        boolean reached = movementController.moveTowards(player, next, 0.22D);
        if (reached) {
            path.pollFirst();
        }
        return path.isEmpty();
    }

    private Deque<BlockPos> reconstructPath(Map<BlockPos, BlockPos> parent, BlockPos goal, BlockPos start) {
        ArrayDeque<BlockPos> reversed = new ArrayDeque<>();
        BlockPos current = goal;
        while (!current.equals(start)) {
            reversed.push(current);
            current = parent.get(current);
            if (current == null) {
                return directPath(start, goal);
            }
        }
        return reversed;
    }

    private Deque<BlockPos> directPath(BlockPos from, BlockPos to) {
        List<BlockPos> points = new ArrayList<>();
        int x = from.getX();
        int y = from.getY();
        int z = from.getZ();

        while (x != to.getX()) {
            x += Integer.compare(to.getX(), x);
            points.add(new BlockPos(x, y, z));
        }
        while (z != to.getZ()) {
            z += Integer.compare(to.getZ(), z);
            points.add(new BlockPos(x, y, z));
        }
        while (y != to.getY()) {
            y += Integer.compare(to.getY(), y);
            points.add(new BlockPos(x, y, z));
        }
        return new ArrayDeque<>(points);
    }

    private int heuristic(BlockPos from, BlockPos to) {
        int dx = Math.abs(from.getX() - to.getX());
        int dy = Math.abs(from.getY() - to.getY());
        int dz = Math.abs(from.getZ() - to.getZ());
        return (dx + dz) * 10 + dy * 14;
    }

    private int movementCost(BlockPos from, BlockPos to) {
        int dy = Math.abs(to.getY() - from.getY());
        return dy == 0 ? 10 : 14;
    }

    private List<BlockPos> neighbors(BlockPos pos) {
        List<BlockPos> result = new ArrayList<>(14);
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        result.add(new BlockPos(x + 1, y, z));
        result.add(new BlockPos(x - 1, y, z));
        result.add(new BlockPos(x, y, z + 1));
        result.add(new BlockPos(x, y, z - 1));
        result.add(new BlockPos(x, y + 1, z));
        result.add(new BlockPos(x, y - 1, z));
        result.add(new BlockPos(x + 1, y + 1, z));
        result.add(new BlockPos(x - 1, y + 1, z));
        result.add(new BlockPos(x, y + 1, z + 1));
        result.add(new BlockPos(x, y + 1, z - 1));
        result.add(new BlockPos(x + 1, y - 1, z));
        result.add(new BlockPos(x - 1, y - 1, z));
        result.add(new BlockPos(x, y - 1, z + 1));
        result.add(new BlockPos(x, y - 1, z - 1));

        return result;
    }

    private boolean isWalkable(ServerWorld world, BlockPos pos) {
        if (!world.isChunkLoaded(pos) || !world.getWorldBorder().contains(pos)) {
            return false;
        }
        if (!world.isAir(pos) || !world.isAir(pos.up())) {
            return false;
        }
        return world.getBlockState(pos.down()).isSolidBlock(world, pos.down());
    }

    private record PathNode(BlockPos pos, int fScore, int gScore) {
    }
}
