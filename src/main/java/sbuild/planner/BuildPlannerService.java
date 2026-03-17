package sbuild.planner;

import sbuild.schematic.LoadedSchematic;
import sbuild.schematic.PlacementController;
import sbuild.schematic.SchematicBlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds deterministic placement plans from transformed schematic blocks.
 */
public final class BuildPlannerService {
    public BuildPlan createPlan(
        PlacementController placement,
        Map<LoadedSchematic.BlockPosition, String> worldStates
    ) {
        List<PlacementTask> tasks = new ArrayList<>();
        int skippedAlreadyCorrect = 0;

        for (Map.Entry<LoadedSchematic.BlockPosition, SchematicBlockState> entry : placement.transformedEntries()) {
            SchematicBlockState requiredState = entry.getValue();
            if (requiredState.isAir()) {
                continue;
            }

            String required = requiredState.key();
            String world = worldStates.get(entry.getKey());
            if (required.equals(world)) {
                skippedAlreadyCorrect++;
                continue;
            }
            tasks.add(new PlacementTask(entry.getKey(), required, world));
        }

        Map<LoadedSchematic.BlockPosition, PlacementTask> pendingByPosition = indexByPosition(tasks);
        tasks.sort(Comparator
            .comparingInt((PlacementTask t) -> t.position().y())
            .thenComparingInt(t -> structuralDependencyScore(t, pendingByPosition))
            .thenComparingInt(t -> replacementPriority(t.worldState()))
            .thenComparingInt(t -> t.position().x())
            .thenComparingInt(t -> t.position().z()));

        return new BuildPlan(List.copyOf(tasks), skippedAlreadyCorrect);
    }

    private Map<LoadedSchematic.BlockPosition, PlacementTask> indexByPosition(List<PlacementTask> tasks) {
        Map<LoadedSchematic.BlockPosition, PlacementTask> byPos = new HashMap<>();
        for (PlacementTask task : tasks) {
            byPos.put(task.position(), task);
        }
        return byPos;
    }

    private int structuralDependencyScore(PlacementTask task, Map<LoadedSchematic.BlockPosition, PlacementTask> pendingByPosition) {
        LoadedSchematic.BlockPosition p = task.position();
        int score = 0;
        score += pendingByPosition.containsKey(p.add(0, -1, 0)) ? 4 : 0;
        score += pendingByPosition.containsKey(p.add(1, 0, 0)) ? 1 : 0;
        score += pendingByPosition.containsKey(p.add(-1, 0, 0)) ? 1 : 0;
        score += pendingByPosition.containsKey(p.add(0, 0, 1)) ? 1 : 0;
        score += pendingByPosition.containsKey(p.add(0, 0, -1)) ? 1 : 0;
        return score;
    }

    private int replacementPriority(String worldState) {
        if (worldState == null || worldState.startsWith("minecraft:air")) {
            return 0;
        }
        return 1;
    }

    public record PlacementTask(
        LoadedSchematic.BlockPosition position,
        String requiredState,
        String worldState
    ) {
    }

    public record BuildPlan(List<PlacementTask> tasks, int skippedAlreadyCorrect) {
        public BuildPlan {
            tasks = List.copyOf(tasks);
        }

        public boolean isEmpty() {
            return tasks.isEmpty();
        }
    }
}
