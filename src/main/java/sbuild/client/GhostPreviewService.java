package sbuild.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.particle.ParticleTypes;
import sbuild.schematic.LoadedSchematic;
import sbuild.schematic.PlacementController;
import sbuild.state.BuildStateService;

import java.util.Map;

/**
 * Simple ghost preview via particles.
 */
public final class GhostPreviewService {
    private static final int MAX_PARTICLES_PER_TICK = 80;

    private GhostPreviewService() {
    }

    public static void register(BuildStateService buildState) {
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick(client, buildState));
    }

    private static void tick(MinecraftClient client, BuildStateService buildState) {
        if (!buildState.isGhostEnabled() || client.world == null) {
            return;
        }
        ClientPlayerEntity player = client.player;
        if (player == null || player.age % 6 != 0) {
            return;
        }

        PlacementController placement = buildState.placement().orElse(null);
        if (placement == null) {
            return;
        }

        int count = 0;
        for (Map.Entry<LoadedSchematic.BlockPosition, ?> entry : placement.transformedEntries()) {
            if (count >= MAX_PARTICLES_PER_TICK) {
                break;
            }
            LoadedSchematic.BlockPosition pos = entry.getKey();
            if (player.getBlockPos().getSquaredDistance(pos.x(), pos.y(), pos.z()) > 40 * 40) {
                continue;
            }

            client.world.addParticle(
                ParticleTypes.END_ROD,
                pos.x() + 0.5,
                pos.y() + 0.55,
                pos.z() + 0.5,
                0.0,
                0.005,
                0.0
            );
            count++;
        }
    }
}
