package sbuild.bot;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import sbuild.planner.BuildPlannerService;
import sbuild.schematic.LoadedSchematic;
import sbuild.schematic.PlacementController;
import sbuild.state.BuildStateService;
import sbuild.storage.StoragePoint;
import sbuild.storage.StorageService;
import sbuild.world.WorldService;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Event-driven autonomous builder runtime.
 */
public final class BotRuntime {
    private static final int BUILD_COOLDOWN_TICKS = 5;

    private final BuildStateService buildState;
    private final WorldService worldService;
    private final BuildPlannerService planner;
    private final StorageService storageService;
    private final PathExecutor pathExecutor;
    private final PlaceController placeController;
    private final InventoryController inventoryController;

    private final BuildQueue buildQueue = new BuildQueue();
    private RuntimeState state = RuntimeState.IDLE;
    private Deque<BlockPos> activePath = new ArrayDeque<>();
    private long nextBuildTick;
    private boolean registered;

    public BotRuntime(
        BuildStateService buildState,
        WorldService worldService,
        BuildPlannerService planner,
        StorageService storageService,
        PathExecutor pathExecutor,
        PlaceController placeController,
        InventoryController inventoryController
    ) {
        this.buildState = buildState;
        this.worldService = worldService;
        this.planner = planner;
        this.storageService = storageService;
        this.pathExecutor = pathExecutor;
        this.placeController = placeController;
        this.inventoryController = inventoryController;
    }

    public void register() {
        if (registered) {
            return;
        }
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
        registered = true;
    }

    private void onServerTick(MinecraftServer server) {
        ServerPlayerEntity player = server.getPlayerManager().getPlayerList().stream().findFirst().orElse(null);
        if (player == null) {
            state = RuntimeState.IDLE;
            return;
        }

        ServerWorld world = player.getServerWorld();
        switch (state) {
            case IDLE -> state = RuntimeState.ANALYZING;
            case ANALYZING -> analyze(player);
            case PLANNING -> plan(player);
            case MOVING -> followPath(player);
            case BUILDING -> buildStep(world, player);
            case COLLECTING -> collectFromNearestChest(world, player);
            case CHECKING -> verifyAndContinue(player);
        }
    }

    private void analyze(ServerPlayerEntity player) {
        Optional<PlacementController> placement = buildState.placement();
        if (placement.isEmpty()) {
            state = RuntimeState.IDLE;
            return;
        }
        state = RuntimeState.PLANNING;
    }

    private void plan(ServerPlayerEntity player) {
        PlacementController placement = buildState.placement().orElse(null);
        if (placement == null) {
            state = RuntimeState.IDLE;
            return;
        }

        Map<LoadedSchematic.BlockPosition, String> worldStates = worldService.snapshotBlockStates(
            player.getServerWorld(),
            placement.transformedEntries().stream().map(Map.Entry::getKey).toList()
        );

        BuildPlannerService.BuildPlan plan = planner.createPlan(placement, worldStates);
        buildQueue.replaceWith(plan.tasks().stream()
            .map(task -> new BuildTask(
                new BlockPos(task.position().x(), task.position().y(), task.position().z()),
                parseBlock(task.requiredState())
            ))
            .filter(task -> task.block() != null)
            .toList());

        state = buildQueue.isEmpty() ? RuntimeState.CHECKING : RuntimeState.BUILDING;
    }

    private void followPath(ServerPlayerEntity player) {
        if (activePath.isEmpty()) {
            state = RuntimeState.COLLECTING;
            return;
        }
        boolean done = pathExecutor.executeStep(player, activePath);
        if (done) {
            state = RuntimeState.COLLECTING;
        }
    }

    private void buildStep(ServerWorld world, ServerPlayerEntity player) {
        BuildTask task = buildQueue.peek();
        if (task == null) {
            state = RuntimeState.CHECKING;
            return;
        }

        if (world.getTime() < nextBuildTick) {
            return;
        }

        if (!hasMaterial(player, task.block())) {
            state = RuntimeState.COLLECTING;
            return;
        }

        if (placeController.placeBlock(world, player, task.position(), task.block())) {
            buildQueue.poll();
            nextBuildTick = world.getTime() + BUILD_COOLDOWN_TICKS;
        }

        state = buildQueue.isEmpty() ? RuntimeState.CHECKING : RuntimeState.BUILDING;
    }

    private void collectFromNearestChest(ServerWorld world, ServerPlayerEntity player) {
        StoragePoint closest = storageService.listStoragePoints().stream()
            .min(Comparator.comparingDouble(point -> point.blockPos().getSquaredDistance(player.getBlockPos())))
            .orElse(null);
        if (closest == null) {
            state = RuntimeState.BUILDING;
            return;
        }

        if (!player.getBlockPos().isWithinDistance(closest.blockPos(), 2.0D)) {
            activePath = pathExecutor.createPath(world, player.getBlockPos(), closest.blockPos());
            state = RuntimeState.MOVING;
            return;
        }

        BlockEntity blockEntity = world.getBlockEntity(closest.blockPos());
        if (blockEntity instanceof ChestBlockEntity) {
            // Hook for future chest transfer implementation.
        }
        state = RuntimeState.BUILDING;
    }

    private void verifyAndContinue(ServerPlayerEntity player) {
        state = buildQueue.isEmpty() ? RuntimeState.ANALYZING : RuntimeState.BUILDING;
    }

    private boolean hasMaterial(ServerPlayerEntity player, Block block) {
        return inventoryController.selectHotbarBlock(player, block);
    }

    private Block parseBlock(String requiredState) {
        String blockKey = requiredState;
        int bracket = requiredState.indexOf('[');
        if (bracket >= 0) {
            blockKey = requiredState.substring(0, bracket);
        }
        Identifier id = Identifier.tryParse(blockKey);
        if (id == null || !Registries.BLOCK.containsId(id)) {
            return null;
        }
        return Registries.BLOCK.get(id);
    }

    enum RuntimeState {
        IDLE,
        ANALYZING,
        PLANNING,
        MOVING,
        BUILDING,
        COLLECTING,
        CHECKING
    }

    record BuildTask(BlockPos position, Block block) {
    }

    static final class BuildQueue {
        private final Deque<BuildTask> tasks = new ArrayDeque<>();

        void replaceWith(List<BuildTask> nextTasks) {
            tasks.clear();
            tasks.addAll(nextTasks);
        }

        BuildTask peek() {
            return tasks.peekFirst();
        }

        BuildTask poll() {
            return tasks.pollFirst();
        }

        boolean isEmpty() {
            return tasks.isEmpty();
        }
    }
}
