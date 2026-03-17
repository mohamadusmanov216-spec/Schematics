package sbuild.redstone;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

import java.util.Map;

/**
 * Facade for redstone dependency analysis and validation.
 */
public final class RedstoneService {
    private final RedstoneDependencyAnalyzer dependencyAnalyzer;
    private final RedstoneValidationService validationService;

    public RedstoneService() {
        this.dependencyAnalyzer = new RedstoneDependencyAnalyzer();
        this.validationService = new RedstoneValidationService(
            new WireStrengthCalculator(),
            new RepeaterDelayResolver(),
            new ComparatorModeResolver(),
            new ObserverPulseAnalyzer()
        );
    }

    public void initialize() {
        // No-op for now.
    }

    public RedstoneGraph buildGraph(Map<BlockPos, BlockState> blocks) {
        return dependencyAnalyzer.analyze(blocks);
    }

    public RedstoneValidationService.Result validateCircuit(Map<BlockPos, BlockState> blocks) {
        RedstoneGraph graph = buildGraph(blocks);
        return validationService.validate(graph);
    }
}
