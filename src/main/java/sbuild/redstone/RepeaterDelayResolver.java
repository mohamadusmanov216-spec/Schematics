package sbuild.redstone;

import net.minecraft.state.property.Properties;

/**
 * Resolves repeater delay in redstone ticks.
 */
public final class RepeaterDelayResolver {
    public int resolve(RedstoneNode node) {
        if (node.type() != RedstoneNode.Type.REPEATER || !node.state().contains(Properties.DELAY)) {
            return 0;
        }
        return node.state().get(Properties.DELAY);
    }
}
