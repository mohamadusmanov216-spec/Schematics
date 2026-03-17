package sbuild.schematic;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

/**
 * Parser for Sponge .schem and legacy .schematic structures.
 */
final class SchemParser {
    ParseResult parse(Path path) throws IOException {
        byte[] raw = Files.readAllBytes(path);
        Map<String, Object> rawRoot = readRootCompound(raw);
        Map<String, Object> root = unwrapRoot(rawRoot);

        ParseResult sponge = tryParseSponge(root);
        if (sponge != null) {
            return sponge;
        }

        ParseResult legacy = tryParseLegacySchematic(root);
        if (legacy != null) {
            return legacy;
        }

        throw new IllegalArgumentException("Unsupported .schem/.schematic structure");
    }

    private Map<String, Object> unwrapRoot(Map<String, Object> root) {
        Object schematic = getIgnoreCase(root, "Schematic");
        if (schematic instanceof Map<?, ?> map) {
            return castMap(map);
        }
        return root;
    }

    private ParseResult tryParseSponge(Map<String, Object> root) {
        int width = asInt(getIgnoreCase(root, "Width"));
        int height = asInt(getIgnoreCase(root, "Height"));
        int length = asInt(getIgnoreCase(root, "Length"));
        if (width <= 0 || height <= 0 || length <= 0) {
            return null;
        }

        Map<String, Object> blockContainer = root;
        Object blocksObj = getIgnoreCase(root, "Blocks");
        if (blocksObj instanceof Map<?, ?> nested) {
            blockContainer = castMap(nested);
        }

        Object paletteRaw = getIgnoreCase(blockContainer, "Palette");
        Object blockDataRaw = getIgnoreCase(blockContainer, "BlockData");
        if (blockDataRaw == null) {
            blockDataRaw = getIgnoreCase(blockContainer, "Data");
        }

        byte[] blockDataBytes = asByteArray(blockDataRaw);
        if (!(paletteRaw instanceof Map<?, ?> paletteMap) || blockDataBytes == null) {
            return null;
        }

        Map<Integer, SchematicBlockState> palette = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : paletteMap.entrySet()) {
            if (!(entry.getKey() instanceof String blockKey) || !(entry.getValue() instanceof Number idNum)) {
                continue;
            }
            palette.put(idNum.intValue(), parseBlockState(blockKey));
        }
        if (palette.isEmpty()) {
            throw new IllegalArgumentException("Schem palette is empty");
        }

        int expected = width * height * length;
        int[] paletteIndices = decodeVarIntArray(blockDataBytes, expected);
        if (paletteIndices.length < expected) {
            throw new IllegalArgumentException("Schem block data is truncated");
        }

        Map<LoadedSchematic.BlockPosition, SchematicBlockState> blocks = new LinkedHashMap<>();
        int airBlocks = 0;
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    int index = (y * length + z) * width + x;
                    SchematicBlockState state = palette.getOrDefault(paletteIndices[index], SchematicBlockState.AIR);
                    if (state.isAir()) {
                        airBlocks++;
                        continue;
                    }
                    blocks.put(new LoadedSchematic.BlockPosition(x, y, z), state);
                }
            }
        }

        return new ParseResult(
            Map.copyOf(blocks),
            Map.of("format", "schem", "width", Integer.toString(width), "height", Integer.toString(height), "length", Integer.toString(length)),
            new LoadedSchematic.SchematicStats(1, palette.size(), airBlocks, blocks.size())
        );
    }

    private ParseResult tryParseLegacySchematic(Map<String, Object> root) {
        int width = asInt(getIgnoreCase(root, "Width"));
        int height = asInt(getIgnoreCase(root, "Height"));
        int length = asInt(getIgnoreCase(root, "Length"));
        if (width <= 0 || height <= 0 || length <= 0) {
            return null;
        }

        byte[] blocksArr = asByteArray(getIgnoreCase(root, "Blocks"));
        byte[] dataArr = asByteArray(getIgnoreCase(root, "Data"));
        if (blocksArr == null || dataArr == null) {
            return null;
        }

        int expected = width * height * length;
        if (blocksArr.length < expected || dataArr.length < expected) {
            throw new IllegalArgumentException("Legacy schematic arrays are truncated");
        }

        Map<LoadedSchematic.BlockPosition, SchematicBlockState> blocks = new LinkedHashMap<>();
        int airBlocks = 0;
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    int index = (y * length + z) * width + x;
                    int id = blocksArr[index] & 0xFF;
                    if (id == 0) {
                        airBlocks++;
                        continue;
                    }
                    blocks.put(new LoadedSchematic.BlockPosition(x, y, z), legacyIdToState(id));
                }
            }
        }

        return new ParseResult(
            Map.copyOf(blocks),
            Map.of("format", "schematic", "legacy", "true", "width", Integer.toString(width), "height", Integer.toString(height), "length", Integer.toString(length)),
            new LoadedSchematic.SchematicStats(1, 0, airBlocks, blocks.size())
        );
    }

    private SchematicBlockState legacyIdToState(int id) {
        return switch (id) {
            case 1 -> SchematicBlockState.of("minecraft:stone", Map.of());
            case 2 -> SchematicBlockState.of("minecraft:grass_block", Map.of());
            case 3 -> SchematicBlockState.of("minecraft:dirt", Map.of());
            case 4 -> SchematicBlockState.of("minecraft:cobblestone", Map.of());
            case 5 -> SchematicBlockState.of("minecraft:oak_planks", Map.of());
            default -> SchematicBlockState.of("minecraft:stone", Map.of("legacy_id", Integer.toString(id)));
        };
    }

    private SchematicBlockState parseBlockState(String key) {
        String trimmed = key.trim().toLowerCase(Locale.ROOT);
        int bracket = trimmed.indexOf('[');
        if (bracket < 0 || !trimmed.endsWith("]")) {
            return SchematicBlockState.of(trimmed, Map.of());
        }

        String blockName = trimmed.substring(0, bracket).trim();
        String propsChunk = trimmed.substring(bracket + 1, trimmed.length() - 1);
        if (propsChunk.isBlank()) {
            return SchematicBlockState.of(blockName, Map.of());
        }

        Map<String, String> props = new TreeMap<>();
        for (String pair : propsChunk.split(",")) {
            String[] kv = pair.split("=", 2);
            if (kv.length != 2) {
                continue;
            }
            props.put(kv[0].trim(), kv[1].trim());
        }
        return SchematicBlockState.of(blockName, Map.copyOf(props));
    }

    private int[] decodeVarIntArray(byte[] data, int expectedValues) {
        List<Integer> out = new ArrayList<>(expectedValues);
        int index = 0;
        while (index < data.length && out.size() < expectedValues) {
            int value = 0;
            int position = 0;
            byte current;
            do {
                if (index >= data.length || position >= 35) {
                    return out.stream().mapToInt(Integer::intValue).toArray();
                }
                current = data[index++];
                value |= (current & 0x7F) << position;
                position += 7;
            } while ((current & 0x80) != 0);
            out.add(value);
        }
        return out.stream().mapToInt(Integer::intValue).toArray();
    }

    private Map<String, Object> readRootCompound(byte[] raw) throws IOException {
        IOException last = null;
        for (boolean gzipped : new boolean[]{true, false}) {
            try (InputStream base = new ByteArrayInputStream(raw);
                 InputStream in = gzipped ? new GZIPInputStream(base) : base;
                 DataInputStream data = new DataInputStream(in)) {
                byte tagId = data.readByte();
                if (tagId != 10) {
                    throw new IllegalArgumentException("Root tag is not COMPOUND");
                }
                readString(data);
                return readCompound(data);
            } catch (IOException | RuntimeException e) {
                last = e instanceof IOException io ? io : new IOException(e);
            }
        }
        throw last == null ? new IOException("Unable to read schematic NBT") : last;
    }

    private Map<String, Object> readCompound(DataInputStream in) throws IOException {
        Map<String, Object> out = new LinkedHashMap<>();
        while (true) {
            byte type = in.readByte();
            if (type == 0) {
                return out;
            }
            String name = readString(in);
            out.put(name, readPayload(in, type));
        }
    }

    private Object readPayload(DataInputStream in, byte type) throws IOException {
        return switch (type) {
            case 1 -> in.readByte();
            case 2 -> in.readShort();
            case 3 -> in.readInt();
            case 4 -> in.readLong();
            case 5 -> in.readFloat();
            case 6 -> in.readDouble();
            case 7 -> {
                int len = in.readInt();
                byte[] arr = new byte[len];
                in.readFully(arr);
                yield arr;
            }
            case 8 -> readString(in);
            case 9 -> readList(in);
            case 10 -> readCompound(in);
            case 11 -> {
                int len = in.readInt();
                int[] arr = new int[len];
                for (int i = 0; i < len; i++) arr[i] = in.readInt();
                yield arr;
            }
            case 12 -> {
                int len = in.readInt();
                long[] arr = new long[len];
                for (int i = 0; i < len; i++) arr[i] = in.readLong();
                yield arr;
            }
            default -> throw new IllegalArgumentException("Unsupported NBT tag type: " + type);
        };
    }

    private List<Object> readList(DataInputStream in) throws IOException {
        byte elementType = in.readByte();
        int len = in.readInt();
        List<Object> out = new ArrayList<>(Math.max(0, len));
        for (int i = 0; i < len; i++) {
            out.add(readPayload(in, elementType));
        }
        return out;
    }

    private String readString(DataInputStream in) throws IOException {
        int len = in.readUnsignedShort();
        byte[] bytes = new byte[len];
        in.readFully(bytes);
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    private Object getIgnoreCase(Map<String, Object> map, String key) {
        if (map.containsKey(key)) {
            return map.get(key);
        }
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private byte[] asByteArray(Object value) {
        if (value instanceof byte[] b) {
            return b;
        }
        if (value instanceof int[] ints) {
            byte[] out = new byte[ints.length];
            for (int i = 0; i < ints.length; i++) {
                out[i] = (byte) ints[i];
            }
            return out;
        }
        if (value instanceof List<?> list) {
            byte[] out = new byte[list.size()];
            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                out[i] = (byte) (item instanceof Number n ? n.intValue() : 0);
            }
            return out;
        }
        return null;
    }

    private Map<String, Object> castMap(Map<?, ?> source) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() instanceof String key) {
                out.put(key, entry.getValue());
            }
        }
        return out;
    }

    private int asInt(Object value) {
        return value instanceof Number n ? n.intValue() : 0;
    }

    record ParseResult(
        Map<LoadedSchematic.BlockPosition, SchematicBlockState> blocks,
        Map<String, String> metadata,
        LoadedSchematic.SchematicStats stats
    ) {
    }
}
