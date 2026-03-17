package sbuild.command;

import com.mojang.brigadier.CommandDispatcher;
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

            .then(CommandManager.literal("schematic")
                .then(CommandManager.literal("scan").executes(handler::handleSchematicScan))
                .then(CommandManager.literal("list").executes(handler::handleSchematicList))
                .then(CommandManager.literal("info").executes(handler::handleSchematicInfo))
                .then(CommandManager.literal("load")
                    .then(CommandManager.argument("name", StringArgumentType.greedyString())
                        .executes(ctx -> handler.handleSchematicLoad(ctx, StringArgumentType.getString(ctx, "name"))))))

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
}
