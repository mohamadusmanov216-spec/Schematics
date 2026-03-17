package sbuild.bot;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/**
 * Hooks bot processing to the server tick loop.
 */
public final class BotTickLoop {
    private final BotController botController;
    private boolean registered;

    public BotTickLoop(BotController botController) {
        this.botController = botController;
    }

    public void register() {
        if (registered) {
            return;
        }
        ServerTickEvents.END_SERVER_TICK.register(botController::tickAll);
        registered = true;
    }
}
