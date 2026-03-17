package sbuild.client;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import sbuild.schematic.LoadedSchematic;
import sbuild.schematic.PlacementController;
import sbuild.state.BuildStateService;

import java.util.Map;

/**
 * Solid ghost preview renderer.
 */
public final class GhostPreviewService {
    private static final int MAX_BLOCKS_PER_FRAME = 900;
    private static final int MAX_RENDER_DISTANCE_SQ = 128 * 128;

    private GhostPreviewService() {
    }

    public static void register(BuildStateService buildState) {
        WorldRenderEvents.LAST.register(context -> render(context, buildState));
    }

    private static void render(WorldRenderContext context, BuildStateService buildState) {
        if (!buildState.isGhostEnabled()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        PlacementController placement = buildState.placement().orElse(null);
        if (placement == null) {
            return;
        }

        MatrixStack matrices = context.matrixStack();
        if (matrices == null) {
            return;
        }

        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) {
            return;
        }

        Vec3d camera = context.camera().getPos();
        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());

        try {
            int rendered = 0;
            for (Map.Entry<LoadedSchematic.BlockPosition, ?> entry : placement.transformedEntries()) {
                if (rendered >= MAX_BLOCKS_PER_FRAME) {
                    break;
                }
                LoadedSchematic.BlockPosition pos = entry.getKey();
                if (client.player.getBlockPos().getSquaredDistance(pos.x(), pos.y(), pos.z()) > MAX_RENDER_DISTANCE_SQ) {
                    continue;
                }

                VertexRendering.drawBox(matrices, lines,
                    pos.x() + 0.02, pos.y() + 0.02, pos.z() + 0.02,
                    pos.x() + 0.98, pos.y() + 0.98, pos.z() + 0.98,
                    0.20f, 0.90f, 1.00f, 1.00f);
                rendered++;
            }
        } finally {
            matrices.pop();
        }
    }
}
