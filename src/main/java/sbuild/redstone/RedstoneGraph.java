package sbuild.redstone;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mutable graph optimized for adjacency traversal.
 */
public final class RedstoneGraph {
    private final Map<BlockPos, RedstoneNode> nodesByPos = new HashMap<>();
    private final Map<BlockPos, List<RedstoneEdge>> outgoing = new HashMap<>();

    public void addNode(RedstoneNode node) {
        nodesByPos.put(node.pos(), node);
    }

    public void addEdge(RedstoneEdge edge) {
        outgoing.computeIfAbsent(edge.from().pos(), ignored -> new ArrayList<>()).add(edge);
    }

    public RedstoneNode nodeAt(BlockPos pos) {
        return nodesByPos.get(pos);
    }

    public Collection<RedstoneNode> nodes() {
        return Collections.unmodifiableCollection(nodesByPos.values());
    }

    public List<RedstoneEdge> outgoing(BlockPos pos) {
        return outgoing.getOrDefault(pos, List.of());
    }

    public int size() {
        return nodesByPos.size();
    }
}
