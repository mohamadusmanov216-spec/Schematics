package sbuild.redstone;

import net.minecraft.state.property.Properties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Minimal, reliable redstone validation: component shape and connectivity sanity checks.
 */
public final class RedstoneValidationService {
    private final WireStrengthCalculator wireStrengthCalculator;
    private final RepeaterDelayResolver repeaterDelayResolver;

    public RedstoneValidationService(
        WireStrengthCalculator wireStrengthCalculator,
        RepeaterDelayResolver repeaterDelayResolver,
        ComparatorModeResolver comparatorModeResolver,
        ObserverPulseAnalyzer observerPulseAnalyzer
    ) {
        this.wireStrengthCalculator = wireStrengthCalculator;
        this.repeaterDelayResolver = repeaterDelayResolver;
    }

    public Result validate(RedstoneGraph graph) {
        List<String> issues = new ArrayList<>();
        Map<RedstoneNode, Integer> strengths = wireStrengthCalculator.compute(graph);

        for (RedstoneNode node : graph.nodes()) {
            if (node.type() == RedstoneNode.Type.WIRE && strengths.getOrDefault(node, 0) <= 0) {
                issues.add("Unpowered wire at " + node.pos());
            }
            if (node.type() == RedstoneNode.Type.REPEATER) {
                int delay = repeaterDelayResolver.resolve(node);
                if (delay < 1 || delay > 4) {
                    issues.add("Invalid repeater delay at " + node.pos());
                }
                if (!node.state().contains(Properties.HORIZONTAL_FACING)) {
                    issues.add("Repeater has no facing at " + node.pos());
                }
            }
            if (node.type() == RedstoneNode.Type.COMPARATOR && !node.state().contains(Properties.HORIZONTAL_FACING)) {
                issues.add("Comparator has no facing at " + node.pos());
            }
            if (node.type() == RedstoneNode.Type.OBSERVER && !node.state().contains(Properties.FACING)) {
                issues.add("Observer has no facing at " + node.pos());
            }
        }

        return new Result(issues.isEmpty(), List.copyOf(issues), strengths);
    }

    public record Result(boolean valid, List<String> issues, Map<RedstoneNode, Integer> strengths) {
    }
}
