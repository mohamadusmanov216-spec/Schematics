package sbuild.command;

import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;

import java.util.Locale;

/**
 * Allows users to type ".sbuild ..." and ".ai_help ..." in chat.
 */
public final class DotCommandBridge {
    private DotCommandBridge() {
    }

    public static void register() {
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            String raw = message == null ? "" : message.trim();
            if (raw.isEmpty() || raw.charAt(0) != '.') {
                return true;
            }

            String withoutDot = raw.substring(1);
            String normalized = withoutDot.toLowerCase(Locale.ROOT);
            if (!normalized.startsWith("sbuild") && !normalized.startsWith("ai_help")) {
                return true;
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.getNetworkHandler() == null) {
                return true;
            }

            client.getNetworkHandler().sendChatCommand(withoutDot);
            return false;
        });
    }
}
