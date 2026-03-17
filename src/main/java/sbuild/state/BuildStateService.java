package sbuild.state;

import sbuild.schematic.LoadedSchematic;
import sbuild.schematic.PlacementController;
import sbuild.schematic.SchematicTransform;

import java.util.Optional;

/**
 * Current user-facing build session state.
 */
public final class BuildStateService {
    private LoadedSchematic loadedSchematic;
    private SchematicTransform transform = SchematicTransform.identity();
    private volatile boolean botEnabled;
    private volatile boolean ghostEnabled = true;

    public boolean hasActiveBuild() {
        return loadedSchematic != null;
    }

    public void setLoadedSchematic(LoadedSchematic schematic) {
        this.loadedSchematic = schematic;
        this.transform = SchematicTransform.identity();
    }

    public Optional<LoadedSchematic> loadedSchematic() {
        return Optional.ofNullable(loadedSchematic);
    }

    public SchematicTransform transform() {
        return transform;
    }

    public void updateTransform(SchematicTransform nextTransform) {
        this.transform = nextTransform;
    }

    public Optional<PlacementController> placement() {
        if (loadedSchematic == null) {
            return Optional.empty();
        }
        return Optional.of(new PlacementController(loadedSchematic, transform));
    }

    public boolean isBotEnabled() {
        return botEnabled;
    }

    public void setBotEnabled(boolean botEnabled) {
        this.botEnabled = botEnabled;
    }

    public boolean isGhostEnabled() {
        return ghostEnabled;
    }

    public void setGhostEnabled(boolean ghostEnabled) {
        this.ghostEnabled = ghostEnabled;
    }

    public void clear() {
        loadedSchematic = null;
        transform = SchematicTransform.identity();
    }
}
