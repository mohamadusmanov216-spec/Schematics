package sbuild.bot;

import sbuild.planner.BuildPlannerService;
import sbuild.state.BuildStateService;
import sbuild.storage.StorageService;
import sbuild.world.WorldService;

/**
 * Facade over autonomous builder runtime.
 */
public final class BuildBotService {
    private final BotController botController;
    private final BotTickLoop botTickLoop;
    private final BotRuntime botRuntime;

    public BuildBotService(
        BuildStateService buildState,
        WorldService worldService,
        BuildPlannerService planner,
        StorageService storageService
    ) {
        MovementController movementController = new MovementController();
        LookController lookController = new LookController();
        InventoryController inventoryController = new InventoryController();
        PlaceController placeController = new PlaceController(inventoryController, lookController);
        BreakController breakController = new BreakController();
        JumpScaffoldController jumpScaffoldController = new JumpScaffoldController(placeController);
        TowerBuilder towerBuilder = new TowerBuilder(jumpScaffoldController);
        SafetyController safetyController = new SafetyController();
        RecoveryController recoveryController = new RecoveryController();
        PathExecutor pathExecutor = new PathExecutor(movementController, lookController);

        this.botController = new BotController(
            pathExecutor,
            lookController,
            inventoryController,
            placeController,
            breakController,
            jumpScaffoldController,
            towerBuilder,
            safetyController,
            recoveryController
        );
        this.botTickLoop = new BotTickLoop(botController);
        this.botRuntime = new BotRuntime(
            buildState,
            worldService,
            planner,
            storageService,
            pathExecutor,
            placeController,
            inventoryController
        );
    }

    public void initialize() {
        botTickLoop.register();
        botRuntime.register();
    }


    public void start() {
        botRuntime.start();
    }

    public void stop() {
        botRuntime.stop();
    }

    public boolean isRunning() {
        return botRuntime.isRunning();
    }

    public BotController botController() {
        return botController;
    }
}
