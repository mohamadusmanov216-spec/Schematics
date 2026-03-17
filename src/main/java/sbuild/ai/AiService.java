package sbuild.ai;

import sbuild.schematic.LoadedSchematic;
import sbuild.state.BuildStateService;
import sbuild.storage.StorageService;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * AI assistant layer for /ai_help and .ai_help commands.
 */
public final class AiService {
    private final AiApiClient apiClient;

    public AiService() {
        this(new AiApiClient());
    }

    public AiService(AiApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public boolean isApiEnabled() {
        return apiClient.isConfigured();
    }

    public AssistantReply respond(String rawQuery, BuildStateService state, StorageService storageService) {
        String query = normalize(rawQuery);
        LoadedSchematic loaded = state.loadedSchematic().orElse(null);
        boolean hasLoadedSchematic = loaded != null;
        int storageCount = storageService.listStoragePoints().size();
        String stateSummary = hasLoadedSchematic
            ? "схема=" + loaded.name() + ", блоков=" + loaded.blockCount() + ", складов=" + storageCount
            : "схема не загружена, складов=" + storageCount;

        if (!query.isBlank()) {
            String apiAnswer = apiClient.askInRussian(query, stateSummary).orElse(null);
            if (apiAnswer != null) {
                return AssistantReply.raw(apiAnswer);
            }
        }

        if (query.isBlank()) {
            if (apiClient.isConfigured()) {
                return hasLoadedSchematic
                    ? reply("command.sbuild.ai.reply.idle.loaded.api", loaded.name(), storageCount)
                    : reply("command.sbuild.ai.reply.idle.no_schematic.api", storageCount);
            }
            return hasLoadedSchematic
                ? reply("command.sbuild.ai.reply.idle.loaded", loaded.name(), storageCount)
                : reply("command.sbuild.ai.reply.idle.no_schematic", storageCount);
        }

        Intent intent = classify(query);

        return switch (intent) {
            case STATUS -> hasLoadedSchematic
                ? reply("command.sbuild.ai.reply.status.loaded", loaded.name(), loaded.blockCount(), storageCount)
                : reply("command.sbuild.ai.reply.status.no_schematic", storageCount);
            case LOAD -> reply("command.sbuild.ai.reply.load_howto");
            case NEXT -> {
                if (!hasLoadedSchematic) {
                    yield reply("command.sbuild.ai.reply.next_step.no_schematic");
                }
                if (storageCount == 0) {
                    yield reply("command.sbuild.ai.reply.next_step.no_storage");
                }
                yield reply("command.sbuild.ai.reply.next_step.ready");
            }
            case STORAGE -> storageCount == 0
                ? reply("command.sbuild.ai.reply.storage.none")
                : reply("command.sbuild.ai.reply.storage.with_count", storageCount);
            case MATERIALS -> hasLoadedSchematic
                ? reply("command.sbuild.ai.reply.materials.ready")
                : reply("command.sbuild.ai.reply.materials.no_schematic");
            case PLANNER -> hasLoadedSchematic
                ? reply("command.sbuild.ai.reply.planner.ready")
                : reply("command.sbuild.ai.reply.planner.no_schematic");
            case HELP -> reply("command.sbuild.ai.reply.help");
            case UNKNOWN -> hasLoadedSchematic
                ? reply("command.sbuild.ai.reply.unknown.loaded", storageCount)
                : reply("command.sbuild.ai.reply.unknown.no_schematic", storageCount);
        };
    }

    private Intent classify(String text) {
        Map<Intent, Integer> scores = new LinkedHashMap<>();
        for (Intent intent : Intent.values()) {
            if (intent == Intent.UNKNOWN) {
                continue;
            }
            scores.put(intent, score(text, Words.forIntent(intent)));
        }

        Intent bestIntent = Intent.UNKNOWN;
        int bestScore = 0;
        for (Intent intent : Intent.PRIORITY_ORDER) {
            int score = scores.getOrDefault(intent, 0);
            if (score > bestScore) {
                bestScore = score;
                bestIntent = intent;
            }
        }
        return bestIntent;
    }

    private int score(String text, Set<String> needles) {
        int score = 0;
        for (String needle : needles) {
            if (text.contains(needle)) {
                score += needle.length() >= 6 ? 2 : 1;
            }
        }
        return score;
    }

    private AssistantReply reply(String key, Object... args) {
        return new AssistantReply(key, args, null);
    }

    private String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    public record AssistantReply(String key, Object[] args, String rawText) {
        public static AssistantReply raw(String text) {
            return new AssistantReply(null, new Object[0], text);
        }

        public boolean isRaw() {
            return rawText != null && !rawText.isBlank();
        }
    }

    private enum Intent {
        STATUS,
        LOAD,
        NEXT,
        MATERIALS,
        PLANNER,
        STORAGE,
        HELP,
        UNKNOWN;

        private static final Intent[] PRIORITY_ORDER = {
            NEXT,
            MATERIALS,
            PLANNER,
            STORAGE,
            STATUS,
            LOAD,
            HELP
        };
    }

    private static final class Words {
        private static final Map<Intent, Set<String>> MAP = Map.of(
            Intent.STATUS, setOf("status", "статус", "progress", "прогресс", "state", "состояние", "what now", "что сейчас"),
            Intent.LOAD, setOf("load", "loaded", "загруз", "подгруз", "откры", "схем", "litematic", "schematic"),
            Intent.NEXT, setOf("next", "дальше", "what to do", "что делать", "следующий", "step", "шаг"),
            Intent.STORAGE, setOf("storage", "chest", "сундук", "склад", "хранил", "restock", "сундуки", "склады"),
            Intent.MATERIALS, setOf("material", "материал", "resources", "ресурсы", "need", "нужно", "хват", "enough", "required"),
            Intent.PLANNER, setOf("planner", "plan", "план", "preview", "очеред", "order"),
            Intent.HELP, setOf("help", "помощ", "команды", "commands", "что умеешь", "what can")
        );

        private static Set<String> forIntent(Intent intent) {
            return MAP.getOrDefault(intent, Set.of());
        }

        private static Set<String> setOf(String... words) {
            Set<String> out = new LinkedHashSet<>();
            for (String word : words) {
                out.add(word);
            }
            return Set.copyOf(out);
        }
    }
}
