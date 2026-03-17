package sbuild.schematic;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
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
        try (InputStream fileInput = Files.newInputStream(path);
             InputStream gzipInput = new GZIPInputStream(fileInput);
             DataInputStream input = new DataInputStream(gzipInput)) {

            NbtTag root = readNamedTag(input);
            if (root.type() != NbtType.COMPOUND || !(root.value() instanceof Map<?, ?> rootMap)) {
                throw new IllegalArgumentException("Invalid litematic root for " + path.getFileName());
            }

            Map<String, String> metadata = readMetadata(rootMap.get("Metadata"));
            List<RegionModel> regions = extractRegions(rootMap.get("Regions"));

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
    }

    List<RegionModel> extractRegions(Object regionsRaw) {
        if (!(regionsRaw instanceof Map<?, ?> regionsMap) || regionsMap.isEmpty()) {
            throw new IllegalArgumentException("Litematic has no regions");
        }

        List<RegionModel> regions = new ArrayList<>();
        for (Map.Entry<?, ?> regionEntry : regionsMap.entrySet()) {
            if (!(regionEntry.getValue() instanceof Map<?, ?> regionRaw)) {
                continue;
            }
            regions.add(readRegionModel(regionEntry.getKey(), regionRaw));
        }

        if (regions.isEmpty()) {
            throw new IllegalArgumentException("Litematic regions are malformed");
        }

        return List.copyOf(regions);
    }

    RegionModel readRegionModel(Object regionName, Map<?, ?> region) {
        Vec3 position = readVec3(region.get("Position"));
        Vec3 size = readVec3(region.get("Size"));
        RegionVolume volume = RegionVolume.from(position, size);
        PaletteModel palette = readPaletteModel(regionName, region.get("BlockStatePalette"));
        long[] blockStates = readLongArray(region.get("BlockStates"));

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
        int requiredLongs = (int) ((packedBits + 63L) / 64L);
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

    private NbtTag readNamedTag(DataInputStream in) throws IOException {
        byte typeId = in.readByte();
        if (typeId == 0) {
            return new NbtTag(NbtType.END, "", null);
        }
        NbtType type = NbtType.fromId(typeId);
        String name = in.readUTF();
        Object payload = readPayload(in, type);
        return new NbtTag(type, name, payload);
    }

    private Object readPayload(DataInputStream in, NbtType type) throws IOException {
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
            case STRING -> in.readUTF();
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

    private List<Object> readList(DataInputStream in) throws IOException {
        NbtType elementType = NbtType.fromId(in.readByte());
        int len = in.readInt();
        List<Object> out = new ArrayList<>(Math.max(0, len));
        for (int i = 0; i < len; i++) {
            out.add(readPayload(in, elementType));
        }
        return out;
    }

    private Map<String, Object> readCompound(DataInputStream in) throws IOException {
        Map<String, Object> out = new LinkedHashMap<>();
        while (true) {
            byte typeId = in.readByte();
            NbtType type = NbtType.fromId(typeId);
            if (type == NbtType.END) {
                break;
            }
            String key = in.readUTF();
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
            return Math.max(2, 32 - Integer.numberOfLeadingZeros(Math.max(1, entries.size() - 1)));
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

    private record Vec3(int x, int y, int z) {}
}
