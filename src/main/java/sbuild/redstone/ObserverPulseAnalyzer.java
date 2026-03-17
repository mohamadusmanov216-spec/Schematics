package sbuild.redstone;

import net.minecraft.state.property.Properties;

/**
 * Detects observer pulse behavior in the graph.
 */
public final class ObserverPulseAnalyzer {
    public boolean emitsPulse(RedstoneNode node) {
        return node.type() == RedstoneNode.Type.OBSERVER
            && node.state().contains(Properties.POWERED)
            && node.state().get(Properties.POWERED);
    }
}
