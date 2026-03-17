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
    private final LitematicParser litematicParser;
    private final SchemParser schemParser;

    public SchematicLoader() {
        this(new LitematicParser(), new SchemParser());
    }

    SchematicLoader(LitematicParser litematicParser, SchemParser schemParser) {
        this.litematicParser = litematicParser;
        this.schemParser = schemParser;
    }

    public LoadedSchematic load(Path path) throws IOException {
        Path normalized = path.toAbsolutePath().normalize();
        validatePath(normalized);

        String fileName = normalized.getFileName().toString();
        String extension = detectExtension(fileName);
        String baseName = fileName.substring(0, fileName.length() - extension.length());
        long fileSize = Files.size(normalized);
        FileTime lastModifiedTime = Files.getLastModifiedTime(normalized);
        Instant lastModified = lastModifiedTime.toInstant();

        ParsePayload parsed = parseByExtension(normalized, extension);
        SchematicBoundingBox boundingBox = SchematicBoundingBox.fromPositions(parsed.blocks().keySet());

        Map<String, String> metadata = new LinkedHashMap<>(parsed.metadata());
        metadata.put("fileName", fileName);
        metadata.put("format", parsed.format());
        metadata.put("sizeBytes", Long.toString(fileSize));
        metadata.put("lastModified", lastModified.toString());

        return new LoadedSchematic(
            normalized.toString(),
            baseName,
            parsed.format(),
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

    private ParsePayload parseByExtension(Path path, String extension) throws IOException {
        Objects.requireNonNull(path, "path");
        return switch (extension) {
            case ".litematic" -> {
                LitematicParser.ParseResult parsed = litematicParser.parse(path);
                yield new ParsePayload(parsed.blocks(), parsed.metadata(), parsed.stats(), "litematic");
            }
            case ".schem", ".schematic" -> {
                SchemParser.ParseResult parsed = schemParser.parse(path);
                String format = ".schem".equals(extension) ? "schem" : "schematic";
                yield new ParsePayload(parsed.blocks(), parsed.metadata(), parsed.stats(), format);
            }
            default -> throw new IllegalArgumentException("Unsupported schematic format: " + path.getFileName());
        };
    }

    private String detectExtension(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".litematic")) {
            return ".litematic";
        }
        if (lower.endsWith(".schem")) {
            return ".schem";
        }
        if (lower.endsWith(".schematic")) {
            return ".schematic";
        }
        throw new IllegalArgumentException("Unsupported schematic format: " + fileName);
    }

    private record ParsePayload(
        Map<LoadedSchematic.BlockPosition, SchematicBlockState> blocks,
        Map<String, String> metadata,
        LoadedSchematic.SchematicStats stats,
        String format
    ) {
    }
}
