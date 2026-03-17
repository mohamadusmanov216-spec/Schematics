package sbuild.storage;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class StorageController {
    private static final double DEFAULT_REACH = 6.0D;

    private final StorageRegistry registry;
    private final StorageScanner scanner;

    public StorageController(StorageRegistry registry, StorageScanner scanner) {
        this.registry = registry;
        this.scanner = scanner;
    }

    public StoragePoint registerLookedAtChest(ServerWorld world, PlayerEntity player, String rawName) {
        String name = validateName(rawName);
        BlockPos pos = findLookedAtChest(world, player);

        StoragePoint existing = registry.findByPos(pos).orElse(null);
        if (existing != null) {
            StoragePoint updated = new StoragePoint(pos.toImmutable(), name, existing.priority(), existing.filterTags());
            return registry.register(updated);
        }

        return registry.register(new StoragePoint(pos.toImmutable(), name, 0, Set.of()));
    }

    public List<StoragePoint> listStoragePoints() {
        return registry.list();
    }

    public Map<String, Long> scan(StoragePoint point, ServerWorld world) {
        return scanner.scanChestInventory(world, point);
    }

    private String validateName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            throw new IllegalArgumentException("Имя хранилища не может быть пустым.");
        }
        String name = rawName.trim().toLowerCase();
        if (name.length() > 32) {
            throw new IllegalArgumentException("Имя хранилища слишком длинное (максимум 32). ");
        }
        return name;
    }

    private BlockPos findLookedAtChest(ServerWorld world, PlayerEntity player) {
        Vec3d eye = player.getCameraPosVec(1.0F);
        Vec3d look = player.getRotationVec(1.0F);
        Vec3d target = eye.add(look.multiply(DEFAULT_REACH));

        BlockHitResult hit = world.raycast(new RaycastContext(
            eye,
            target,
            RaycastContext.ShapeType.OUTLINE,
            RaycastContext.FluidHandling.NONE,
            player
        ));

        if (hit.getType() != HitResult.Type.BLOCK) {
            throw new IllegalStateException("Вы не смотрите на блок сундука.");
        }

        BlockPos hitPos = hit.getBlockPos();
        BlockState state = world.getBlockState(hitPos);
        if (!isChestBlock(state.getBlock())) {
            throw new IllegalStateException("Целевой блок не является сундуком/бочкой.");
        }
        return hitPos;
    }

    private boolean isChestBlock(Block block) {
        return block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST || block == Blocks.BARREL;
    }
}
