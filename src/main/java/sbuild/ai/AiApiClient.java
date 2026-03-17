package sbuild.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * OpenAI-compatible API client used by /ai_help and .ai_help commands.
 *
 * Defaults to Cloudflare Worker proxy so requests can be routed from regions where direct OpenAI access is blocked.
 */
public final class AiApiClient {
    private static final String DEFAULT_PROXY_ENDPOINT = "https://star-proxy-bridge.mohamadusmanov216.workers.dev";
    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    private final HttpClient httpClient;
    private final String apiKey;
    private final List<URI> endpoints;
    private final String model;

    public AiApiClient() {
        this(HttpClient.newHttpClient(), readApiKey(), readEndpoints(), readModel());
    }

    AiApiClient(HttpClient httpClient, String apiKey, List<URI> endpoints, String model) {
        this.httpClient = httpClient;
        this.apiKey = apiKey;
        this.endpoints = endpoints;
        this.model = model;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public Optional<String> askInRussian(String query, String stateSummary) {
        if (!isConfigured() || query == null || query.isBlank()) {
            return Optional.empty();
        }

        String payload = createPayload(query, stateSummary).toString();
        for (URI endpoint : endpoints) {
            Optional<String> result = send(payload, endpoint);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

    private Optional<String> send(String payload, URI endpoint) {
        HttpRequest request = HttpRequest.newBuilder(endpoint)
            .timeout(TIMEOUT)
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .header("X-SBuild-AI-Key", apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Optional.empty();
            }
            return extractContent(response.body());
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (IOException ignored) {
            return Optional.empty();
        }
    }

    private JsonObject createPayload(String query, String stateSummary) {
        JsonObject payload = new JsonObject();
        payload.addProperty("model", model);

        JsonArray messages = new JsonArray();
        messages.add(message("system", "Ты ассистент SBuild для Minecraft. Всегда отвечай только на русском языке, кратко и по делу."));
        messages.add(message("system", "Текущее состояние: " + stateSummary));
        messages.add(message("user", query));
        payload.add("messages", messages);
        payload.addProperty("temperature", 0.25);
        return payload;
    }

    private Optional<String> extractContent(String body) {
        JsonElement rootElement = JsonParser.parseString(body);
        if (!rootElement.isJsonObject()) {
            return Optional.empty();
        }

        JsonObject root = rootElement.getAsJsonObject();

        Optional<String> common = extractCommonText(root, "response", "text", "answer", "content");
        if (common.isPresent()) {
            return common;
        }

        if (!root.has("choices") || !root.get("choices").isJsonArray()) {
            return Optional.empty();
        }

        JsonArray choices = root.getAsJsonArray("choices");
        if (choices.isEmpty()) {
            return Optional.empty();
        }

        JsonObject first = choices.get(0).getAsJsonObject();
        if (first.has("message") && first.get("message").isJsonObject()) {
            Optional<String> fromMessage = extractCommonText(first.getAsJsonObject("message"), "content", "text");
            if (fromMessage.isPresent()) {
                return fromMessage;
            }
        }

        return extractCommonText(first, "text", "content");
    }

    private Optional<String> extractCommonText(JsonObject object, String... keys) {
        for (String key : keys) {
            if (!object.has(key)) {
                continue;
            }
            JsonElement value = object.get(key);
            if (value.isJsonPrimitive()) {
                String text = value.getAsString().trim();
                if (!text.isEmpty()) {
                    return Optional.of(text);
                }
            }
        }
        return Optional.empty();
    }

    private JsonObject message(String role, String content) {
        JsonObject message = new JsonObject();
        message.addProperty("role", role);
        message.addProperty("content", content);
        return message;
    }

    private static String readApiKey() {
        String property = System.getProperty("sbuild.ai.api.key", "");
        if (!property.isBlank()) {
            return property;
        }
        return System.getenv("SBUILD_AI_API_KEY");
    }

    private static List<URI> readEndpoints() {
        String configured = System.getProperty("sbuild.ai.api.url", System.getenv().getOrDefault("SBUILD_AI_API_URL", ""));
        List<URI> resolved = new ArrayList<>();
        if (!configured.isBlank()) {
            resolved.add(URI.create(configured));
        }

        resolved.add(URI.create(DEFAULT_PROXY_ENDPOINT));

        String directFallback = System.getProperty("sbuild.ai.api.direct_fallback", System.getenv().getOrDefault("SBUILD_AI_DIRECT_FALLBACK", ""));
        if (!directFallback.isBlank()) {
            resolved.add(URI.create(directFallback));
        }

        return List.copyOf(resolved.stream().distinct().toList());
    }

    private static String readModel() {
        String property = System.getProperty("sbuild.ai.model", "");
        if (!property.isBlank()) {
            return property;
        }
        String fromEnv = System.getenv("SBUILD_AI_MODEL");
        return (fromEnv == null || fromEnv.isBlank()) ? "gpt-4o-mini" : fromEnv;
    }
}
