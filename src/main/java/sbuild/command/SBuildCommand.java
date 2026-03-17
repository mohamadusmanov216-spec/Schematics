package sbuild.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public final class SBuildCommand {
    private SBuildCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, SBuildCommandHandler handler) {
        dispatcher.register(CommandManager.literal("ai_help")
            .executes(ctx -> handler.handleAiHelp(ctx, ""))
            .then(CommandManager.argument("query", StringArgumentType.greedyString())
                .executes(ctx -> handler.handleAiHelp(ctx, StringArgumentType.getString(ctx, "query")))));

        dispatcher.register(CommandManager.literal("sbuild")
            .executes(handler::handleRoot)
            .then(CommandManager.literal("help").executes(handler::handleHelp))
            .then(CommandManager.literal("status").executes(handler::handleStatus))
            .then(CommandManager.literal("ai")
                .executes(ctx -> handler.handleAiHelp(ctx, ""))
                .then(CommandManager.argument("query", StringArgumentType.greedyString())
                    .executes(ctx -> handler.handleAiHelp(ctx, StringArgumentType.getString(ctx, "query")))))

            .then(buildSchematicNode(handler, "schematic"))
            .then(buildSchematicNode(handler, "shematic"))

            .then(CommandManager.literal("materials")
                .executes(handler::handleMaterialsReport)
                .then(CommandManager.literal("report").executes(handler::handleMaterialsReport))
                .then(CommandManager.literal("gui").executes(handler::handleMaterialsGui)))
            .then(CommandManager.literal("material")
                .executes(handler::handleMaterialsReport)
                .then(CommandManager.literal("report").executes(handler::handleMaterialsReport))
                .then(CommandManager.literal("gui").executes(handler::handleMaterialsGui)))

            .then(CommandManager.literal("planner")
                .then(CommandManager.literal("preview").executes(handler::handlePlannerPreview)))

            .then(CommandManager.literal("bot")
                .then(CommandManager.literal("start").executes(handler::handleBotStart))
                .then(CommandManager.literal("stop").executes(handler::handleBotStop))
                .then(CommandManager.literal("status").executes(handler::handleBotStatus)))

            .then(CommandManager.literal("ghost")
                .then(CommandManager.literal("on").executes(handler::handleGhostOn))
                .then(CommandManager.literal("off").executes(handler::handleGhostOff))
                .then(CommandManager.literal("status").executes(handler::handleGhostStatus)))
            .then(CommandManager.literal("chest")
                .then(CommandManager.literal("set")
                    .executes(ctx -> handler.handleChestSet(ctx, "chest"))
                    .then(CommandManager.argument("name", StringArgumentType.greedyString())
                        .executes(ctx -> handler.handleChestSet(ctx, StringArgumentType.getString(ctx, "name")))))
                .then(CommandManager.literal("list").executes(handler::handleChestList))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> buildSchematicNode(
        SBuildCommandHandler handler,
        String literal
    ) {
        return CommandManager.literal(literal)
            .then(CommandManager.literal("scan").executes(handler::handleSchematicScan))
            .then(CommandManager.literal("list").executes(handler::handleSchematicList))
            .then(CommandManager.literal("info").executes(handler::handleSchematicInfo))
            .then(CommandManager.literal("storage").executes(handler::handleSchematicStorage))
            .then(CommandManager.literal("up")
                .then(CommandManager.argument("value", IntegerArgumentType.integer(1, 256))
                    .executes(ctx -> handler.handleSchematicShiftY(ctx, IntegerArgumentType.getInteger(ctx, "value")))))
            .then(CommandManager.literal("down")
                .then(CommandManager.argument("value", IntegerArgumentType.integer(1, 256))
                    .executes(ctx -> handler.handleSchematicShiftY(ctx, -IntegerArgumentType.getInteger(ctx, "value")))))
            .then(CommandManager.literal("left")
                .then(CommandManager.argument("value", IntegerArgumentType.integer(1, 256))
                    .executes(ctx -> handler.handleSchematicShiftX(ctx, -IntegerArgumentType.getInteger(ctx, "value")))))
            .then(CommandManager.literal("right")
                .then(CommandManager.argument("value", IntegerArgumentType.integer(1, 256))
                    .executes(ctx -> handler.handleSchematicShiftX(ctx, IntegerArgumentType.getInteger(ctx, "value")))))
            .then(CommandManager.literal("load")
                .then(CommandManager.argument("name", StringArgumentType.greedyString())
                    .suggests(handler::suggestSchematicNames)
                    .executes(ctx -> handler.handleSchematicLoad(ctx, StringArgumentType.getString(ctx, "name")))));
    }
}
