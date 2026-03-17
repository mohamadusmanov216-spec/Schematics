package sbuild.schematic;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

final class LitematicParser {
    ParseResult parse(Path path) throws IOException {
        byte[] nbtPayload;
        try (InputStream fileInput = Files.newInputStream(path);
             InputStream gzipInput = new GZIPInputStream(fileInput)) {
            nbtPayload = gzipInput.readAllBytes();
        }

        try {
            return parseNbtPayload(path, nbtPayload, ByteOrder.LITTLE_ENDIAN);
        } catch (RuntimeException | IOException littleEndianError) {
            return parseNbtPayload(path, nbtPayload, ByteOrder.BIG_ENDIAN);
        }
    }

    private ParseResult parseNbtPayload(Path path, byte[] nbtPayload, ByteOrder byteOrder) throws IOException {
        NbtInput input = new NbtInput(nbtPayload, byteOrder);
        NbtTag root = readNamedTag(input);
            if (root.type() != NbtType.COMPOUND || !(root.value() instanceof Map<?, ?> rootMap)) {
                throw new IllegalArgumentException("Invalid litematic root for " + path.getFileName());
            }

            Map<String, String> metadata = readMetadata(rootMap.get("Metadata"));
            List<RegionModel> regions = extractRegions(rootMap);

            ParseAccumulator acc = new ParseAccumulator();
            for (RegionModel region : regions) {
                decodeRegionBlocks(region, acc);
                acc.regionCount++;
            }

            if (acc.blocks.isEmpty()) {
                throw new IllegalArgumentException("Litematic contains zero non-air blocks: " + path.getFileName());
            }

            return new ParseResult(
                Map.copyOf(acc.blocks),
                metadata,
                new LoadedSchematic.SchematicStats(acc.regionCount, acc.paletteEntries, acc.airBlocks, acc.blocks.size())
            );
    }

    List<RegionModel> extractRegions(Map<?, ?> rootMap) {
        Object regionsRaw = getIgnoreCase(rootMap, "Regions");
        if (regionsRaw instanceof Map<?, ?> regionsMap && !regionsMap.isEmpty()) {
            List<RegionModel> regions = new ArrayList<>();
            for (Map.Entry<?, ?> regionEntry : regionsMap.entrySet()) {
                if (!(regionEntry.getValue() instanceof Map<?, ?> regionRaw)) {
                    continue;
                }
                regions.add(readRegionModel(regionEntry.getKey(), regionRaw));
            }
            if (!regions.isEmpty()) {
                return List.copyOf(regions);
            }
        }

        Map<String, Object> syntheticRegion = readSyntheticSingleRegion(rootMap);
        if (syntheticRegion != null) {
            return List.of(readRegionModel("main", syntheticRegion));
        }

        throw new IllegalArgumentException("Формат схемы не поддерживается: не найдены данные Regions/Position/Size/Palette");
    }

    private Map<String, Object> readSyntheticSingleRegion(Map<?, ?> rootMap) {
        Object position = getIgnoreCase(rootMap, "Position");
        Object size = getIgnoreCase(rootMap, "Size");
        Object palette = getIgnoreCase(rootMap, "BlockStatePalette");
        Object states = getIgnoreCase(rootMap, "BlockStates");
        if (position == null || size == null || palette == null || states == null) {
            return null;
        }

        Map<String, Object> region = new LinkedHashMap<>();
        region.put("Position", position);
        region.put("Size", size);
        region.put("BlockStatePalette", palette);
        region.put("BlockStates", states);
        return region;
    }

    private Object getIgnoreCase(Map<?, ?> map, String key) {
        if (map.containsKey(key)) {
            return map.get(key);
        }
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() instanceof String k && k.equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    RegionModel readRegionModel(Object regionName, Map<?, ?> region) {
        Vec3 position = readVec3(getIgnoreCase(region, "Position"));
        Vec3 size = readVec3(getIgnoreCase(region, "Size"));
        RegionVolume volume = RegionVolume.from(position, size);
        PaletteModel palette = readPaletteModel(regionName, getIgnoreCase(region, "BlockStatePalette"));
        long[] blockStates = readLongArray(getIgnoreCase(region, "BlockStates"));

        validatePackedData(regionName, volume, palette, blockStates);
        return new RegionModel(String.valueOf(regionName), volume, palette, blockStates);
    }

    PaletteModel readPaletteModel(Object regionName, Object value) {
        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException("Region has invalid palette: " + regionName);
        }

        List<SchematicBlockState> entries = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> state)) {
                continue;
            }
            Object nameRaw = state.get("Name");
            if (!(nameRaw instanceof String name)) {
                continue;
            }
            Object propertiesRaw = state.get("Properties");
            Map<String, String> props = propertiesRaw instanceof Map<?, ?> propsMap ? readProperties(propsMap) : Map.of();
            entries.add(SchematicBlockState.of(name, props));
        }

        if (entries.isEmpty()) {
            throw new IllegalArgumentException("Region has empty palette: " + regionName);
        }

        return new PaletteModel(List.copyOf(entries));
    }

    void decodeRegionBlocks(RegionModel region, ParseAccumulator out) {
        out.paletteEntries += region.palette().size();

        for (int index = 0; index < region.volume().blockCount(); index++) {
            int paletteIndex = readPackedValue(region.blockStates(), index, region.palette().bitsPerBlock());
            if (paletteIndex < 0 || paletteIndex >= region.palette().size()) {
                throw new IllegalArgumentException("Palette index out of range in region: " + region.name());
            }

            LoadedSchematic.BlockPosition worldPos = region.volume().worldPosition(index);
            SchematicBlockState blockState = region.palette().entry(paletteIndex);
            if (blockState.isAir()) {
                out.airBlocks++;
                continue;
            }
            out.blocks.put(worldPos, blockState);
        }
    }

    private void validatePackedData(Object regionName, RegionVolume volume, PaletteModel palette, long[] blockStates) {
        if (volume.isEmpty()) {
            throw new IllegalArgumentException("Region has empty size: " + regionName);
        }

        long packedBits = (long) volume.blockCount() * palette.bitsPerBlock();
        if (packedBits < 0 || packedBits > (long) Integer.MAX_VALUE * 64L) {
            throw new IllegalArgumentException("Region is too large to decode safely: " + regionName);
        }
        int requiredLongs = (int) ((packedBits + 63L) >>> 6);
        if (blockStates.length < requiredLongs) {
            throw new IllegalArgumentException("Region has truncated block states: " + regionName);
        }
    }

    private int readPackedValue(long[] data, int index, int bitsPerValue) {
        int bitIndex = index * bitsPerValue;
        int startLong = bitIndex >>> 6;
        int startOffset = bitIndex & 63;
        if (startLong >= data.length) {
            return -1;
        }

        long value = data[startLong] >>> startOffset;
        int endOffset = startOffset + bitsPerValue;
        if (endOffset > 64 && (startLong + 1) < data.length) {
            value |= data[startLong + 1] << (64 - startOffset);
        }

        long mask = bitsPerValue == 64 ? -1L : (1L << bitsPerValue) - 1L;
        return (int) (value & mask);
    }

    private Map<String, String> readProperties(Map<?, ?> properties) {
        Map<String, String> out = new TreeMap<>();
        for (Map.Entry<?, ?> entry : properties.entrySet()) {
            if (entry.getKey() instanceof String key && entry.getValue() instanceof String value) {
                out.put(key, value);
            }
        }
        return Map.copyOf(out);
    }

    private long[] readLongArray(Object value) {
        return value instanceof long[] longArray ? longArray : new long[0];
    }

    private Vec3 readVec3(Object value) {
        if (!(value instanceof Map<?, ?> compound)) {
            return new Vec3(0, 0, 0);
        }
        return new Vec3(asInt(compound.get("x")), asInt(compound.get("y")), asInt(compound.get("z")));
    }

    private Map<String, String> readMetadata(Object value) {
        if (!(value instanceof Map<?, ?> metadataRaw)) {
            return Map.of();
        }
        Map<String, String> metadata = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : metadataRaw.entrySet()) {
            if (entry.getKey() instanceof String key && entry.getValue() != null) {
                metadata.put(key, String.valueOf(entry.getValue()));
            }
        }
        return Map.copyOf(metadata);
    }

    private int asInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private NbtTag readNamedTag(NbtInput in) throws IOException {
        byte typeId = in.readByte();
        if (typeId == 0) {
            return new NbtTag(NbtType.END, "", null);
        }
        NbtType type = NbtType.fromId(typeId);
        String name = in.readString();
        Object payload = readPayload(in, type);
        return new NbtTag(type, name, payload);
    }

    private Object readPayload(NbtInput in, NbtType type) throws IOException {
        return switch (type) {
            case END -> null;
            case BYTE -> in.readByte();
            case SHORT -> in.readShort();
            case INT -> in.readInt();
            case LONG -> in.readLong();
            case FLOAT -> in.readFloat();
            case DOUBLE -> in.readDouble();
            case BYTE_ARRAY -> {
                int len = in.readInt();
                byte[] data = new byte[len];
                in.readFully(data);
                yield data;
            }
            case STRING -> in.readString();
            case LIST -> readList(in);
            case COMPOUND -> readCompound(in);
            case INT_ARRAY -> {
                int len = in.readInt();
                int[] data = new int[len];
                for (int i = 0; i < len; i++) {
                    data[i] = in.readInt();
                }
                yield data;
            }
            case LONG_ARRAY -> {
                int len = in.readInt();
                long[] data = new long[len];
                for (int i = 0; i < len; i++) {
                    data[i] = in.readLong();
                }
                yield data;
            }
        };
    }

    private List<Object> readList(NbtInput in) throws IOException {
        NbtType elementType = NbtType.fromId(in.readByte());
        int len = in.readInt();
        List<Object> out = new ArrayList<>(Math.max(0, len));
        for (int i = 0; i < len; i++) {
            out.add(readPayload(in, elementType));
        }
        return out;
    }

    private Map<String, Object> readCompound(NbtInput in) throws IOException {
        Map<String, Object> out = new LinkedHashMap<>();
        while (true) {
            byte typeId = in.readByte();
            NbtType type = NbtType.fromId(typeId);
            if (type == NbtType.END) {
                break;
            }
            String key = in.readString();
            Object value = readPayload(in, type);
            out.put(key, value);
        }
        return out;
    }

    record ParseResult(Map<LoadedSchematic.BlockPosition, SchematicBlockState> blocks, Map<String, String> metadata, LoadedSchematic.SchematicStats stats) {}

    static final class ParseAccumulator {
        final Map<LoadedSchematic.BlockPosition, SchematicBlockState> blocks = new LinkedHashMap<>();
        int regionCount;
        int paletteEntries;
        int airBlocks;
    }

    record RegionModel(String name, RegionVolume volume, PaletteModel palette, long[] blockStates) {}

    record PaletteModel(List<SchematicBlockState> entries) {
        int size() {
            return entries.size();
        }

        int bitsPerBlock() {
            return Math.max(2, Integer.toBinaryString(Math.max(1, entries.size() - 1)).length());
        }

        SchematicBlockState entry(int index) {
            return entries.get(index);
        }
    }

    record RegionVolume(int startX, int startY, int startZ, int sizeX, int sizeY, int sizeZ) {
        static RegionVolume from(Vec3 position, Vec3 size) {
            int sx = Math.abs(size.x());
            int sy = Math.abs(size.y());
            int sz = Math.abs(size.z());

            int startX = size.x() >= 0 ? position.x() : position.x() + size.x() + 1;
            int startY = size.y() >= 0 ? position.y() : position.y() + size.y() + 1;
            int startZ = size.z() >= 0 ? position.z() : position.z() + size.z() + 1;
            return new RegionVolume(startX, startY, startZ, sx, sy, sz);
        }

        boolean isEmpty() {
            return sizeX == 0 || sizeY == 0 || sizeZ == 0;
        }

        int blockCount() {
            return sizeX * sizeY * sizeZ;
        }

        LoadedSchematic.BlockPosition worldPosition(int index) {
            int x = index % sizeX;
            int z = (index / sizeX) % sizeZ;
            int y = index / (sizeX * sizeZ);
            return new LoadedSchematic.BlockPosition(startX + x, startY + y, startZ + z);
        }
    }

    private record NbtTag(NbtType type, String name, Object value) {}

    private enum NbtType {
        END(0), BYTE(1), SHORT(2), INT(3), LONG(4), FLOAT(5), DOUBLE(6), BYTE_ARRAY(7), STRING(8), LIST(9), COMPOUND(10), INT_ARRAY(11), LONG_ARRAY(12);
        private final int id;

        NbtType(int id) {
            this.id = id;
        }

        static NbtType fromId(int id) {
            for (NbtType type : values()) {
                if (type.id == id) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unsupported NBT tag id: " + id);
        }
    }

    private static final class NbtInput {
        private final ByteBuffer buffer;

        private NbtInput(byte[] data, ByteOrder byteOrder) {
            this.buffer = ByteBuffer.wrap(data).order(byteOrder);
        }

        byte readByte() throws IOException {
            requireRemaining(1);
            return buffer.get();
        }

        short readShort() throws IOException {
            requireRemaining(2);
            return buffer.getShort();
        }

        int readInt() throws IOException {
            requireRemaining(4);
            return buffer.getInt();
        }

        long readLong() throws IOException {
            requireRemaining(8);
            return buffer.getLong();
        }

        float readFloat() throws IOException {
            return Float.intBitsToFloat(readInt());
        }

        double readDouble() throws IOException {
            return Double.longBitsToDouble(readLong());
        }

        void readFully(byte[] data) throws IOException {
            requireRemaining(data.length);
            buffer.get(data);
        }

        String readString() throws IOException {
            int length = Short.toUnsignedInt(readShort());
            byte[] utf = new byte[length];
            readFully(utf);
            return new String(utf, StandardCharsets.UTF_8);
        }

        private void requireRemaining(int bytes) throws IOException {
            if (buffer.remaining() < bytes) {
                throw new IOException("NBT stream ended unexpectedly");
            }
        }
    }

    private record Vec3(int x, int y, int z) {}
}
