package sbuild.redstone;

import net.minecraft.block.enums.ComparatorMode;
import net.minecraft.state.property.Properties;

/**
 * Resolves comparator mode (compare/subtract).
 */
public final class ComparatorModeResolver {
    public ComparatorMode resolve(RedstoneNode node) {
        if (node.type() != RedstoneNode.Type.COMPARATOR || !node.state().contains(Properties.COMPARATOR_MODE)) {
            return ComparatorMode.COMPARE;
        }
        return node.state().get(Properties.COMPARATOR_MODE);
    }
}
