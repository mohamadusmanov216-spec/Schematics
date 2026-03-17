package sbuild.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import sbuild.SBuildClientMod;
import sbuild.ai.AiService;
import sbuild.bot.BuildBotService;
import sbuild.client.MaterialListScreen;
import sbuild.materials.MaterialAnalysisService;
import sbuild.materials.MaterialAvailability;
import sbuild.materials.MaterialReport;
import sbuild.materials.MaterialRequirement;
import sbuild.planner.BuildPlannerService;
import sbuild.schematic.LoadedSchematic;
import sbuild.schematic.PlacementController;
import sbuild.schematic.SchematicService;
import sbuild.schematic.SchematicTransform;
import sbuild.state.BuildStateService;
import sbuild.storage.StoragePoint;
import sbuild.storage.StorageService;
import sbuild.world.WorldService;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
    private final BuildBotService buildBotService;

    public SBuildCommandHandler(
        BuildStateService buildState,
        SchematicService schematics,
        WorldService worldService,
        MaterialAnalysisService materials,
        StorageService storage,
        BuildPlannerService planner,
        AiService aiService,
        BuildBotService buildBotService
    ) {
        this.buildState = buildState;
        this.schematics = schematics;
        this.worldService = worldService;
        this.materials = materials;
        this.storage = storage;
        this.planner = planner;
        this.aiService = aiService;
        this.buildBotService = buildBotService;
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
            Text.translatable("command.sbuild.help.schematic.storage"),
            Text.translatable("command.sbuild.help.materials"),
            Text.translatable("command.sbuild.help.chest"),
            Text.translatable("command.sbuild.help.planner"),
            Text.translatable("command.sbuild.help.bot"),
            Text.translatable("command.sbuild.help.ghost"),
            Text.translatable("command.sbuild.help.ai")
        );
        lines.forEach(line -> sendInfo(source, line));
        return 1;
    }

    public int handleAiHelp(CommandContext<ServerCommandSource> ctx, String query) {
        AiService.AssistantReply reply = aiService.respond(query, buildState, storage);
        if (reply.isRaw()) {
            sendInfo(ctx.getSource(), Text.translatable("command.sbuild.ai.prefix", Text.literal(reply.rawText())));
            return 1;
        }
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

    public int handleSchematicStorage(CommandContext<ServerCommandSource> ctx) {
        Path root = schematics.rootDirectory();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            sendError(ctx.getSource(), Text.translatable("command.sbuild.schematic.storage.unavailable", root.toString()));
            return 0;
        }
        client.execute(() -> {
            try {
                Util.getOperatingSystem().open(root.toFile());
            } catch (Exception e) {
                SBuildClientMod.LOGGER.warn("Failed to open schematic storage folder {}", root, e);
            }
        });

        sendInfo(ctx.getSource(), Text.translatable("command.sbuild.schematic.storage.opened", root.toString()));
        return 1;
    }

    public CompletableFuture<Suggestions> suggestSchematicNames(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
        List<String> names = schematics.scanSchematics().stream()
            .map(path -> path.getFileName() == null ? "" : path.getFileName().toString())
            .filter(name -> !name.isBlank())
            .toList();
        return CommandSource.suggestMatching(names, builder);
    }

    public int handleSchematicLoad(CommandContext<ServerCommandSource> ctx, String name) {
        try {
            return schematics.loadByName(name)
                .map(loaded -> {
                    buildState.setLoadedSchematic(loaded);
                    alignLoadedSchematicToPlayerY(ctx.getSource(), loaded);
                    sendInfo(ctx.getSource(), Text.translatable("command.sbuild.schematic.load.success", loaded.name(), loaded.blockCount()));
                    return 1;
                })
                .orElseGet(() -> {
                    sendError(ctx.getSource(), Text.translatable("command.sbuild.schematic.load.not_found", name));
                    return 0;
                });
        } catch (Exception e) {
            SBuildClientMod.LOGGER.error("Failed to load schematic '{}'", name, e);
            String reason = extractSafeReason(e);
            sendError(ctx.getSource(), Text.translatable("command.sbuild.schematic.load.failed", name, reason));
            return 0;
        }
    }

    public int handleSchematicShiftY(CommandContext<ServerCommandSource> ctx, int deltaY) {
        return applySchematicShift(ctx.getSource(), 0, deltaY, 0, "Y");
    }

    public int handleSchematicShiftX(CommandContext<ServerCommandSource> ctx, int deltaX) {
        return applySchematicShift(ctx.getSource(), deltaX, 0, 0, "X");
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
        buildState.setLastMaterialReport(report);

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

    public int handleMaterialsGui(CommandContext<ServerCommandSource> ctx) {
        MaterialReport report = resolveReportForGui(ctx.getSource());
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            sendError(ctx.getSource(), Text.literal("Клиент Minecraft недоступен."));
            return 0;
        }
        client.execute(() -> client.setScreen(new MaterialListScreen(report)));
        sendInfo(ctx.getSource(), Text.literal("Открыт GUI материалов."));
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

    public int handleBotStart(CommandContext<ServerCommandSource> ctx) {
        if (buildState.loadedSchematic().isEmpty()) {
            sendError(ctx.getSource(), Text.translatable("command.sbuild.error.no_loaded_schematic"));
            return 0;
        }
        buildBotService.start();
        sendInfo(ctx.getSource(), Text.translatable("command.sbuild.bot.started"));
        return 1;
    }

    public int handleBotStop(CommandContext<ServerCommandSource> ctx) {
        buildBotService.stop();
        sendInfo(ctx.getSource(), Text.translatable("command.sbuild.bot.stopped"));
        return 1;
    }

    public int handleBotStatus(CommandContext<ServerCommandSource> ctx) {
        sendInfo(ctx.getSource(), Text.translatable("command.sbuild.bot.status", buildBotService.isRunning() ? "ON" : "OFF"));
        return 1;
    }

    public int handleGhostOn(CommandContext<ServerCommandSource> ctx) {
        buildState.setGhostEnabled(true);
        sendInfo(ctx.getSource(), Text.translatable("command.sbuild.ghost.on"));
        return 1;
    }

    public int handleGhostOff(CommandContext<ServerCommandSource> ctx) {
        buildState.setGhostEnabled(false);
        sendInfo(ctx.getSource(), Text.translatable("command.sbuild.ghost.off"));
        return 1;
    }

    public int handleGhostStatus(CommandContext<ServerCommandSource> ctx) {
        sendInfo(ctx.getSource(), Text.translatable("command.sbuild.ghost.status", buildState.isGhostEnabled() ? "ON" : "OFF"));
        return 1;
    }

    private PlacementController requirePlacement(ServerCommandSource source) {
        PlacementController placement = buildState.placement().orElse(null);
        if (placement == null) {
            sendError(source, Text.translatable("command.sbuild.error.no_loaded_schematic"));
        }
        return placement;
    }

    private int applySchematicShift(ServerCommandSource source, int dx, int dy, int dz, String axis) {
        if (buildState.loadedSchematic().isEmpty()) {
            sendError(source, Text.translatable("command.sbuild.error.no_loaded_schematic"));
            return 0;
        }
        SchematicTransform current = buildState.transform();
        SchematicTransform next = new SchematicTransform(
            current.rotation(),
            current.mirror(),
            current.offsetX() + dx,
            current.offsetY() + dy,
            current.offsetZ() + dz
        );
        buildState.updateTransform(next);
        sendInfo(source, Text.literal("Сдвиг схематики по " + axis + ": " + (axis.equals("X") ? next.offsetX() : next.offsetY())));
        return 1;
    }

    private void alignLoadedSchematicToPlayerY(ServerCommandSource source, LoadedSchematic loaded) {
        ServerPlayerEntity player = requirePlayer(source);
        if (player == null) {
            return;
        }
        int desiredMinY = player.getBlockY();
        int baseMinY = loaded.boundingBox().min().y();
        int offsetY = desiredMinY - baseMinY;

        SchematicTransform current = buildState.transform();
        buildState.updateTransform(new SchematicTransform(
            current.rotation(),
            current.mirror(),
            current.offsetX(),
            offsetY,
            current.offsetZ()
        ));
    }

    private MaterialReport resolveReportForGui(ServerCommandSource source) {
        MaterialReport cached = buildState.lastMaterialReport();
        if (!cached.rows().isEmpty()) {
            return cached;
        }
        ServerPlayerEntity player = requirePlayer(source);
        PlacementController placement = requirePlacement(source);
        if (player == null || placement == null) {
            return cached;
        }
        Map<LoadedSchematic.BlockPosition, String> worldStates = snapshotWorldStates(player, placement);
        MaterialAvailability availability = storage.aggregateAvailability(player.getWorld());
        MaterialReport report = materials.analyze(placement, worldStates, availability);
        buildState.setLastMaterialReport(report);
        return report;
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


    private String extractSafeReason(Throwable error) {
        if (error == null) {
            return "unknown";
        }
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            Throwable cause = error.getCause();
            if (cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()) {
                message = cause.getMessage();
            }
        }
        if (message == null || message.isBlank()) {
            return "unknown";
        }
        return message.length() > 160 ? message.substring(0, 160) + "..." : message;
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
