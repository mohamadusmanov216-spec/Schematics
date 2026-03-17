package sbuild.util;

import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;

import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Converts Minecraft BlockState to stable string key: namespace:block[prop=value,...].
 */
public final class BlockStateKeyCodec {
    public String toKey(BlockState state) {
        String id = Registries.BLOCK.getId(state.getBlock()).toString();
        if (state.getProperties().isEmpty()) {
            return id;
        }

        String props = state.getProperties().stream()
            .sorted(Comparator.comparing(Property::getName))
            .map(property -> property.getName() + "=" + valueName(state, property))
            .collect(Collectors.joining(","));

        return id + "[" + props + "]";
    }

    private <T extends Comparable<T>> String valueName(BlockState state, Property<T> property) {
        return property.name(state.get(property));
    }
}
