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

    public void clear() {
        loadedSchematic = null;
        transform = SchematicTransform.identity();
    }
}
