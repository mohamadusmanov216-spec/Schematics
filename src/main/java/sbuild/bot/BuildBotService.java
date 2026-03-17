package sbuild.bot;

/**
 * Facade over autonomous builder runtime.
 */
public final class BuildBotService {
    private final BotController botController;
    private final BotTickLoop botTickLoop;

    public BuildBotService() {
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
    }

    public void initialize() {
        botTickLoop.register();
    }

    public BotController botController() {
        return botController;
    }
}
