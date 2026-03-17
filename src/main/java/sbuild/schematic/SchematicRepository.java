package sbuild.schematic;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory repository of discovered/loaded schematics.
 */
public final class SchematicRepository {
    private final Path rootDirectory;
    private final SchematicLoader loader;
    private final ConcurrentMap<Path, LoadedSchematic> cacheByPath;
    private final ConcurrentMap<String, Path> pathByName;

    public SchematicRepository(SchematicLoader loader) {
        this(defaultRootDirectory(), loader);
    }

    public SchematicRepository(Path rootDirectory, SchematicLoader loader) {
        this.rootDirectory = rootDirectory;
        this.loader = loader;
        this.cacheByPath = new ConcurrentHashMap<>();
        this.pathByName = new ConcurrentHashMap<>();
    }

    public Path rootDirectory() {
        return rootDirectory;
    }

    public List<Path> scan() {
        ensureRootExists();
        try (var stream = Files.list(rootDirectory)) {
            List<Path> paths = stream
                .filter(Files::isRegularFile)
                .filter(this::isSupportedSchematic)
                .map(path -> path.toAbsolutePath().normalize())
                .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                .toList();
            indexNames(paths);
            return paths;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to scan schematics directory: " + rootDirectory, e);
        }
    }

    public LoadedSchematic load(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        return cacheByPath.compute(normalized, (key, cached) -> {
            if (cached != null && !isFileChanged(cached)) {
                return cached;
            }
            LoadedSchematic loaded = loadUnchecked(key);
            pathByName.put(loaded.name().toLowerCase(Locale.ROOT), key);
            return loaded;
        });
    }

    public Optional<LoadedSchematic> loadByName(String name) {
        String normalized = normalizeName(name);
        Path path = pathByName.get(normalized);
        if (path != null) {
            return Optional.of(load(path));
        }

        for (Path candidate : scan()) {
            if (baseName(candidate).equalsIgnoreCase(normalized)) {
                return Optional.of(load(candidate));
            }
        }
        return Optional.empty();
    }

    public List<LoadedSchematic> loadAll() {
        List<LoadedSchematic> loaded = new ArrayList<>();
        for (Path path : scan()) {
            loaded.add(load(path));
        }
        return List.copyOf(loaded);
    }

    public Optional<LoadedSchematic> findById(String id) {
        return cacheByPath.values().stream().filter(s -> s.id().equals(id)).findFirst();
    }

    public void clearCache() {
        cacheByPath.clear();
        pathByName.clear();
    }

    private LoadedSchematic loadUnchecked(Path key) {
        try {
            return loader.load(key);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load schematic: " + key, e);
        }
    }

    private boolean isFileChanged(LoadedSchematic cached) {
        try {
            Path sourcePath = cached.sourcePath();
            if (!Files.exists(sourcePath)) {
                return true;
            }
            long currentSize = Files.size(sourcePath);
            if (currentSize != cached.fileSizeBytes()) {
                return true;
            }
            FileTime currentModified = Files.getLastModifiedTime(sourcePath);
            return !currentModified.toInstant().equals(cached.lastModified());
        } catch (IOException e) {
            return true;
        }
    }

    private void ensureRootExists() {
        try {
            Files.createDirectories(rootDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create schematic directory: " + rootDirectory, e);
        }
    }

    private void indexNames(List<Path> paths) {
        for (Path path : paths) {
            pathByName.put(baseName(path).toLowerCase(Locale.ROOT), path);
        }
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private String baseName(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private boolean isSupportedSchematic(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".litematic");
    }

    private static Path defaultRootDirectory() {
        return FabricLoader.getInstance().getGameDir().resolve("sbuild").resolve("schematics");
    }
}
