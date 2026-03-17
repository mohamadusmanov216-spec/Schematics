package sbuild.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import sbuild.ai.AiService;
import sbuild.state.BuildStateService;
import sbuild.storage.StorageService;

/**
 * Lightweight HUD with current SBuild state.
 */
public final class SBuildHudOverlay implements HudRenderCallback {
    private final BuildStateService state;
    private final StorageService storage;
    private final AiService aiService;

    public SBuildHudOverlay(BuildStateService state, StorageService storage, AiService aiService) {
        this.state = state;
        this.storage = storage;
        this.aiService = aiService;
    }

    public static void register(BuildStateService state, StorageService storage, AiService aiService) {
        HudRenderCallback.EVENT.register(new SBuildHudOverlay(state, storage, aiService));
    }

    @Override
    public void onHudRender(DrawContext drawContext, net.minecraft.client.render.RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.options.hudHidden) {
            return;
        }

        int x = 8;
        int y = 8;

        String schematic = state.loadedSchematic().map(s -> s.name() + " (" + s.blockCount() + ")").orElse("нет");
        int storages = storage.listStoragePoints().size();
        String api = aiService.isApiEnabled() ? "proxy ON" : "proxy OFF";

        drawContext.drawText(client.textRenderer, Text.literal("§eSBuild HUD"), x, y, 0xFFFFFF, true);
        drawContext.drawText(client.textRenderer, Text.literal("§7Схема: §f" + schematic), x, y + 12, 0xFFFFFF, true);
        drawContext.drawText(client.textRenderer, Text.literal("§7Склады: §f" + storages), x, y + 24, 0xFFFFFF, true);
        drawContext.drawText(client.textRenderer, Text.literal("§7AI: §f" + api), x, y + 36, 0xFFFFFF, true);
    }
}
