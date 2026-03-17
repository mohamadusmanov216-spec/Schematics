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
import sbuild.schematic.SchematicBlockState;
import sbuild.state.BuildStateService;

import java.util.Locale;
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

        VertexConsumer fill = consumers.getBuffer(RenderLayer.getDebugFilledBox());
        VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());

        try {
            int rendered = 0;
            boolean fillAvailable = true;
            for (Map.Entry<LoadedSchematic.BlockPosition, SchematicBlockState> entry : placement.transformedEntries()) {
                if (rendered >= MAX_BLOCKS_PER_FRAME) {
                    break;
                }
                LoadedSchematic.BlockPosition pos = entry.getKey();
                if (client.player.getBlockPos().getSquaredDistance(pos.x(), pos.y(), pos.z()) > MAX_RENDER_DISTANCE_SQ) {
                    continue;
                }

                float[] color = colorForState(entry.getValue());
                if (fillAvailable) {
                    try {
                        VertexRendering.drawFilledBox(matrices, fill,
                            pos.x() + 0.06, pos.y() + 0.06, pos.z() + 0.06,
                            pos.x() + 0.94, pos.y() + 0.94, pos.z() + 0.94,
                            color[0], color[1], color[2], 0.20f);
                    } catch (IllegalStateException ignored) {
                        fillAvailable = false;
                    }
                }
                VertexRendering.drawBox(matrices, lines,
                    pos.x() + 0.02, pos.y() + 0.02, pos.z() + 0.02,
                    pos.x() + 0.98, pos.y() + 0.98, pos.z() + 0.98,
                    color[0], color[1], color[2], 1.00f);
                rendered++;
            }
        } finally {
            matrices.pop();
        }
    }

    private static float[] colorForState(SchematicBlockState state) {
        if (state == null || state.isAir()) {
            return new float[]{0.30f, 0.75f, 1.00f};
        }

        String block = state.blockName().toLowerCase(Locale.ROOT);
        if (block.contains("redstone") || block.contains("repeater") || block.contains("comparator") || block.contains("observer")) {
            return new float[]{1.00f, 0.25f, 0.25f};
        }
        if (block.contains("water") || block.contains("ice")) {
            return new float[]{0.35f, 0.65f, 1.00f};
        }
        if (block.contains("lava") || block.contains("magma") || block.contains("fire")) {
            return new float[]{1.00f, 0.45f, 0.12f};
        }
        if (block.contains("glass")) {
            return new float[]{0.45f, 0.95f, 1.00f};
        }
        if (block.contains("leaf") || block.contains("moss") || block.contains("grass") || block.contains("vine")) {
            return new float[]{0.35f, 0.95f, 0.35f};
        }
        if (block.contains("log") || block.contains("wood") || block.contains("plank")) {
            return new float[]{0.74f, 0.54f, 0.31f};
        }
        if (block.contains("deepslate") || block.contains("stone") || block.contains("cobblestone") || block.contains("brick")) {
            return new float[]{0.68f, 0.78f, 1.00f};
        }
        if (block.contains("ore") || block.contains("iron") || block.contains("gold") || block.contains("diamond")) {
            return new float[]{1.00f, 0.87f, 0.35f};
        }
        return new float[]{0.25f, 0.85f, 1.00f};
    }
}
