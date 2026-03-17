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
    private static final int MAX_BLOCKS_PER_FRAME = 350;

    private GhostPreviewService() {
    }

    public static void register(BuildStateService buildState) {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> render(context, buildState));
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

        Vec3d camera = context.camera().getPos();
        MatrixStack matrices = context.matrixStack();
        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        VertexConsumerProvider.Immediate consumers = client.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer fill = consumers.getBuffer(RenderLayer.getDebugFilledBox());
        VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());

        int rendered = 0;
        for (Map.Entry<LoadedSchematic.BlockPosition, ?> entry : placement.transformedEntries()) {
            if (rendered >= MAX_BLOCKS_PER_FRAME) {
                break;
            }
            LoadedSchematic.BlockPosition pos = entry.getKey();
            if (client.player.getBlockPos().getSquaredDistance(pos.x(), pos.y(), pos.z()) > 56 * 56) {
                continue;
            }

            float[] color = colorForBlock(pos);
            VertexRendering.drawFilledBox(matrices, fill,
                pos.x() + 0.04, pos.y() + 0.04, pos.z() + 0.04,
                pos.x() + 0.96, pos.y() + 0.96, pos.z() + 0.96,
                color[0], color[1], color[2], 0.22f);
            VertexRendering.drawBox(matrices, lines,
                pos.x() + 0.02, pos.y() + 0.02, pos.z() + 0.02,
                pos.x() + 0.98, pos.y() + 0.98, pos.z() + 0.98,
                color[0], color[1], color[2], 0.9f);
            rendered++;
        }

        consumers.draw();
        matrices.pop();
    }

    private static float[] colorForBlock(LoadedSchematic.BlockPosition pos) {
        int seed = Math.abs((pos.x() * 734287 + pos.y() * 912271 + pos.z() * 438289));
        float hue = (seed % 360) / 360.0f;
        return hsvToRgb(hue, 0.45f, 1.0f);
    }

    private static float[] hsvToRgb(float h, float s, float v) {
        int i = (int) (h * 6.0f);
        float f = h * 6.0f - i;
        float p = v * (1.0f - s);
        float q = v * (1.0f - f * s);
        float t = v * (1.0f - (1.0f - f) * s);
        return switch (i % 6) {
            case 0 -> new float[]{v, t, p};
            case 1 -> new float[]{q, v, p};
            case 2 -> new float[]{p, v, t};
            case 3 -> new float[]{p, q, v};
            case 4 -> new float[]{t, p, v};
            default -> new float[]{v, p, q};
        };
    }
}
