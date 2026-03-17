package sbuild.schematic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class SchematicLoader {
    private static final String SUPPORTED_EXTENSION = ".litematic";
    private final LitematicParser parser;

    public SchematicLoader() {
        this(new LitematicParser());
    }

    SchematicLoader(LitematicParser parser) {
        this.parser = parser;
    }

    public LoadedSchematic load(Path path) throws IOException {
        Path normalized = path.toAbsolutePath().normalize();
        validatePath(normalized);
        validateFormat(normalized);

        String fileName = normalized.getFileName().toString();
        String baseName = fileName.substring(0, fileName.length() - SUPPORTED_EXTENSION.length());
        long fileSize = Files.size(normalized);
        FileTime lastModifiedTime = Files.getLastModifiedTime(normalized);
        Instant lastModified = lastModifiedTime.toInstant();

        LitematicParser.ParseResult parsed = parse(normalized);
        SchematicBoundingBox boundingBox = SchematicBoundingBox.fromPositions(parsed.blocks().keySet());

        Map<String, String> metadata = new LinkedHashMap<>(parsed.metadata());
        metadata.put("fileName", fileName);
        metadata.put("format", "litematic");
        metadata.put("sizeBytes", Long.toString(fileSize));
        metadata.put("lastModified", lastModified.toString());

        return new LoadedSchematic(
            normalized.toString(),
            baseName,
            "litematic",
            normalized,
            fileSize,
            lastModified,
            boundingBox,
            parsed.blocks(),
            null,
            parsed.stats(),
            metadata
        );
    }

    private void validatePath(Path path) {
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Schematic file does not exist: " + path);
        }
        if (!Files.isReadable(path)) {
            throw new IllegalArgumentException("Schematic file is not readable: " + path);
        }
    }

    private void validateFormat(Path path) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!fileName.endsWith(SUPPORTED_EXTENSION)) {
            throw new IllegalArgumentException("Unsupported schematic format: " + fileName);
        }
    }

    private LitematicParser.ParseResult parse(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        return parser.parse(path);
    }
}
