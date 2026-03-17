package sbuild;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sbuild.ai.AiService;
import sbuild.command.SBuildCommand;
import sbuild.command.SBuildCommandHandler;
import sbuild.materials.MaterialAnalysisService;
import sbuild.planner.BuildPlannerService;
import sbuild.schematic.SchematicService;
import sbuild.state.BuildStateService;
import sbuild.storage.StorageService;
import sbuild.world.WorldService;

public final class SBuildMod implements ModInitializer {
    public static final String MOD_ID = "sbuild";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        BuildStateService buildState = new BuildStateService();
        SchematicService schematics = new SchematicService();
        WorldService world = new WorldService();
        MaterialAnalysisService materials = new MaterialAnalysisService();
        StorageService storage = new StorageService();
        BuildPlannerService planner = new BuildPlannerService();
        AiService ai = new AiService();

        SBuildCommandHandler handler = new SBuildCommandHandler(
            buildState,
            schematics,
            world,
            materials,
            storage,
            planner,
            ai
        );

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            SBuildCommand.register(dispatcher, handler)
        );

        LOGGER.info("SBuild initialized.");
    }
}
