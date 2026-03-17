package sbuild.redstone;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/**
 * Computes redstone strength propagation decay from 15 to 0.
 */
public final class WireStrengthCalculator {
    private static final int MAX_STRENGTH = 15;

    public Map<RedstoneNode, Integer> compute(RedstoneGraph graph) {
        Map<RedstoneNode, Integer> strengths = new HashMap<>();
        ArrayDeque<RedstoneNode> queue = new ArrayDeque<>();

        for (RedstoneNode node : graph.nodes()) {
            if (node.type() == RedstoneNode.Type.POWER_SOURCE) {
                strengths.put(node, MAX_STRENGTH);
                queue.add(node);
            }
        }

        while (!queue.isEmpty()) {
            RedstoneNode current = queue.poll();
            int currentStrength = strengths.getOrDefault(current, 0);
            if (currentStrength <= 0) {
                continue;
            }

            for (RedstoneEdge edge : graph.outgoing(current.pos())) {
                int nextStrength = edge.repeaterReset() ? MAX_STRENGTH : Math.max(0, currentStrength - edge.weight());
                Integer previous = strengths.get(edge.to());
                if (previous == null || nextStrength > previous) {
                    strengths.put(edge.to(), nextStrength);
                    queue.add(edge.to());
                }
            }
        }

        return Map.copyOf(strengths);
    }
}
