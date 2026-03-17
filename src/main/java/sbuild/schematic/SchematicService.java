package sbuild.schematic;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Schematic discovery/loading APIs used by commands and planner.
 */
public final class SchematicService {
    private final SchematicRepository repository;

    public SchematicService() {
        this(new SchematicRepository(new SchematicLoader()));
    }

    public SchematicService(SchematicRepository repository) {
        this.repository = repository;
    }

    public Path rootDirectory() {
        return repository.rootDirectory();
    }

    public List<Path> scanSchematics() {
        return repository.scan();
    }

    public LoadedSchematic load(Path path) {
        return repository.load(path);
    }

    public Optional<LoadedSchematic> loadByName(String name) {
        return repository.loadByName(name);
    }

    public List<LoadedSchematic> loadAll() {
        return repository.loadAll();
    }

    public PlacementController createPlacementController(LoadedSchematic schematic, SchematicTransform transform) {
        return new PlacementController(schematic, transform);
    }
}
