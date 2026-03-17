package sbuild.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import sbuild.ai.AiService;
import sbuild.materials.MaterialAnalysisService;
import sbuild.materials.MaterialAvailability;
import sbuild.materials.MaterialReport;
import sbuild.materials.MaterialRequirement;
import sbuild.planner.BuildPlannerService;
import sbuild.schematic.LoadedSchematic;
import sbuild.schematic.PlacementController;
import sbuild.schematic.SchematicService;
import sbuild.state.BuildStateService;
import sbuild.storage.StoragePoint;
import sbuild.storage.StorageService;
import sbuild.world.WorldService;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class SBuildCommandHandler {
    private static final int MATERIAL_ROWS_PREVIEW = 12;
    private static final int PLAN_ROWS_PREVIEW = 10;

    private final BuildStateService buildState;
    private final SchematicService schematics;
    private final WorldService worldService;
    private final MaterialAnalysisService materials;
    private final StorageService storage;
    private final BuildPlannerService planner;
    private final AiService aiService;

    public SBuildCommandHandler(
        BuildStateService buildState,
        SchematicService schematics,
        WorldService worldService,
        MaterialAnalysisService materials,
        StorageService storage,
        BuildPlannerService planner,
        AiService aiService
    ) {
        this.buildState = buildState;
        this.schematics = schematics;
        this.worldService = worldService;
        this.materials = materials;
        this.storage = storage;
        this.planner = planner;
        this.aiService = aiService;
    }

    public int handleRoot(CommandContext<ServerCommandSource> ctx) {
        sendInfo(ctx.getSource(), Text.translatable("command.sbuild.root"));
        return 1;
    }

    public int handleStatus(CommandContext<ServerCommandSource> ctx) {
        String schematic = buildState.loadedSchematic().map(LoadedSchematic::name).orElse("none");
        int blocks = buildState.loadedSchematic().map(LoadedSchematic::blockCount).orElse(0);
        sendInfo(ctx.getSource(), Text.translatable("command.sbuild.status.summary", schematic, blocks));
        return 1;
    }

    public int handleHelp(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        sendInfo(source, Text.translatable("command.sbuild.help.header"));
        List<Text> lines = List.of(
            Text.translatable("command.sbuild.help.status"),
            Text.translatable("command.sbuild.help.schematic"),
            Text.translatable("command.sbuild.help.materials"),
            Text.translatable("command.sbuild.help.chest"),
            Text.translatable("command.sbuild.help.planner"),
            Text.translatable("command.sbuild.help.ai")
        );
        lines.forEach(line -> sendInfo(source, line));
        return 1;
    }

    public int handleAiHelp(CommandContext<ServerCommandSource> ctx, String query) {
        AiService.AssistantReply reply = aiService.respond(query, buildState, storage);
        sendInfo(ctx.getSource(), Text.translatable("command.sbuild.ai.prefix", Text.translatable(reply.key(), reply.args())));
        return 1;
    }

    public int handleSchematicScan(CommandContext<ServerCommandSource> ctx) {
        List<Path> paths = schematics.scanSchematics();
        sendInfo(ctx.getSource(), Text.translatable("command.sbuild.schematic.scan.result", paths.size(), schematics.rootDirectory().toString()));
        return 1;
    }

    public int handleSchematicList(CommandContext<ServerCommandSource> ctx) {
        List<Path> paths = schematics.scanSchematics();
        if (paths.isEmpty()) {
            sendInfo(ctx.getSource(), Text.translatable("command.sbuild.schematic.list.empty"));
            return 1;
        }

        sendInfo(ctx.getSource(), Text.translatable("command.sbuild.schematic.list.header", paths.size()));
        for (Path path : paths) {
            sendInfo(ctx.getSource(), Text.translatable("command.sbuild.schematic.list.entry", path.getFileName().toString()));
        }
        return 1;
    }

    public int handleSchematicLoad(CommandContext<ServerCommandSource> ctx, String name) {
        return schematics.loadByName(name)
            .map(loaded -> {
                buildState.setLoadedSchematic(loaded);
                sendInfo(ctx.getSource(), Text.translatable("command.sbuild.schematic.load.success", loaded.name(), loaded.blockCount()));
                return 1;
            })
            .orElseGet(() -> {
                sendError(ctx.getSource(), Text.translatable("command.sbuild.schematic.load.not_found", name));
                return 0;
            });
    }

    public int handleSchematicInfo(CommandContext<ServerCommandSource> ctx) {
        return buildState.loadedSchematic().map(schematic -> {
            sendInfo(ctx.getSource(), Text.translatable("command.sbuild.schematic.info.summary", schematic.name(), schematic.format(), schematic.blockCount()));
            sendInfo(ctx.getSource(), Text.translatable(
                "command.sbuild.schematic.info.bounds",
                schematic.boundingBox().min().toString(),
                schematic.boundingBox().max().toString(),
                schematic.stats().regionCount(),
                schematic.stats().paletteEntries()
            ));
            return 1;
        }).orElseGet(() -> {
            sendError(ctx.getSource(), Text.translatable("command.sbuild.error.no_loaded_schematic"));
            return 0;
        });
    }

    public int handleMaterialsReport(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = requirePlayer(source);
        if (player == null) {
            return 0;
        }

        PlacementController placement = requirePlacement(source);
        if (placement == null) {
            return 0;
        }

        Map<LoadedSchematic.BlockPosition, String> worldStates = snapshotWorldStates(player, placement);
        MaterialAvailability availability = storage.aggregateAvailability(player.getWorld());
        MaterialReport report = materials.analyze(placement, worldStates, availability);

        sendInfo(source, Text.translatable(
            "command.sbuild.materials.report.summary",
            report.totalRequired(),
            report.totalAlreadyBuilt(),
            report.totalRemaining(),
            report.totalAvailableInInventory()
        ));

        List<MaterialRequirement> rows = report.rows();
        rows.stream().limit(MATERIAL_ROWS_PREVIEW).forEach(row ->
            sendInfo(source, Text.translatable("command.sbuild.materials.report.row", row.materialKey(), row.remaining(), row.availableInInventory()))
        );
        if (rows.size() > MATERIAL_ROWS_PREVIEW) {
            sendInfo(source, Text.translatable("command.sbuild.preview.truncated", rows.size() - MATERIAL_ROWS_PREVIEW));
        }
        return 1;
    }

    public int handlePlannerPreview(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = requirePlayer(ctx.getSource());
        if (player == null) {
            return 0;
        }

        PlacementController placement = requirePlacement(ctx.getSource());
        if (placement == null) {
            return 0;
        }

        Map<LoadedSchematic.BlockPosition, String> worldStates = snapshotWorldStates(player, placement);
        BuildPlannerService.BuildPlan plan = planner.createPlan(placement, worldStates);

        sendInfo(ctx.getSource(), Text.translatable("command.sbuild.planner.preview.summary", plan.tasks().size(), plan.skippedAlreadyCorrect()));
        plan.tasks().stream().limit(PLAN_ROWS_PREVIEW).forEach(task ->
            sendInfo(ctx.getSource(), Text.translatable("command.sbuild.planner.preview.row", task.position().toString(), task.requiredState()))
        );
        if (plan.tasks().size() > PLAN_ROWS_PREVIEW) {
            sendInfo(ctx.getSource(), Text.translatable("command.sbuild.preview.truncated", plan.tasks().size() - PLAN_ROWS_PREVIEW));
        }
        return 1;
    }

    public int handleChestSet(CommandContext<ServerCommandSource> ctx, String name) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = requirePlayer(source);
        if (player == null) {
            return 0;
        }
        try {
            ServerWorld world = player.getWorld();
            StoragePoint point = storage.registerLookedAtChest(world, player, name);
            sendInfo(source, Text.translatable("command.sbuild.chest.set.success", point.name(), formatPos(point)));
            return 1;
        } catch (Exception e) {
            sendError(source, Text.translatable("command.sbuild.chest.set.failed", e.getMessage()));
            return 0;
        }
    }

    public int handleChestList(CommandContext<ServerCommandSource> ctx) {
        List<StoragePoint> points = storage.listStoragePoints();
        if (points.isEmpty()) {
            sendInfo(ctx.getSource(), Text.translatable("command.sbuild.chest.list.empty"));
            return 1;
        }

        sendInfo(ctx.getSource(), Text.translatable("command.sbuild.chest.list.header", points.size()));
        for (StoragePoint point : points) {
            sendInfo(ctx.getSource(), Text.translatable("command.sbuild.chest.list.entry", point.name(), formatPos(point), point.priority()));
        }
        return 1;
    }

    private PlacementController requirePlacement(ServerCommandSource source) {
        PlacementController placement = buildState.placement().orElse(null);
        if (placement == null) {
            sendError(source, Text.translatable("command.sbuild.error.no_loaded_schematic"));
        }
        return placement;
    }

    private Map<LoadedSchematic.BlockPosition, String> snapshotWorldStates(ServerPlayerEntity player, PlacementController placement) {
        return worldService.snapshotBlockStates(
            player.getWorld(),
            placement.transformedEntries().stream().map(Map.Entry::getKey).toList()
        );
    }

    private ServerPlayerEntity requirePlayer(ServerCommandSource source) {
        try {
            return source.getPlayerOrThrow();
        } catch (Exception e) {
            sendError(source, Text.translatable("command.sbuild.error.player_only"));
            return null;
        }
    }

    private String formatPos(StoragePoint point) {
        return point.blockPos().getX() + "," + point.blockPos().getY() + "," + point.blockPos().getZ();
    }

    private void sendInfo(ServerCommandSource source, Text text) {
        source.sendFeedback(() -> text, false);
    }

    private void sendError(ServerCommandSource source, Text text) {
        source.sendError(text);
    }
}
