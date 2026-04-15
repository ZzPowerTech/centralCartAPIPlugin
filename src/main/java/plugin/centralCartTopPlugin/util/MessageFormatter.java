package plugin.centralCartTopPlugin.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.Map;

/**
 * Utilitário para parsear mensagens com suporte misto: códigos legados (&) e MiniMessage.
 */
public final class MessageFormatter {

    private MessageFormatter() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Converte uma string com placeholders, códigos & e tags MiniMessage em um Component.
     *
     * @param text         Texto original (pode misturar &X e tags MiniMessage)
     * @param placeholders Mapa de {placeholder} → valor
     * @return Component pronto para broadcast
     */
    public static Component parse(String text, Map<String, String> placeholders) {
        // Passo 1: substituir placeholders
        String result = text;
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                result = result.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }

        // Passo 2: converter códigos & legados para tags MiniMessage
        result = convertLegacyToMiniMessage(result);

        // Passo 3: parsear com MiniMessage
        return MiniMessage.miniMessage().deserialize(result);
    }

    /**
     * Converte códigos de cor/formatação legados (&X) para tags MiniMessage equivalentes.
     * Suporta todos os 16 códigos de cor + formatação + reset.
     */
    private static String convertLegacyToMiniMessage(String text) {
        // Cores
        text = text.replace("&0", "<black>");
        text = text.replace("&1", "<dark_blue>");
        text = text.replace("&2", "<dark_green>");
        text = text.replace("&3", "<dark_aqua>");
        text = text.replace("&4", "<dark_red>");
        text = text.replace("&5", "<dark_purple>");
        text = text.replace("&6", "<gold>");
        text = text.replace("&7", "<gray>");
        text = text.replace("&8", "<dark_gray>");
        text = text.replace("&9", "<blue>");
        text = text.replace("&a", "<green>");
        text = text.replace("&A", "<green>");
        text = text.replace("&b", "<aqua>");
        text = text.replace("&B", "<aqua>");
        text = text.replace("&c", "<red>");
        text = text.replace("&C", "<red>");
        text = text.replace("&d", "<light_purple>");
        text = text.replace("&D", "<light_purple>");
        text = text.replace("&e", "<yellow>");
        text = text.replace("&E", "<yellow>");
        text = text.replace("&f", "<white>");
        text = text.replace("&F", "<white>");

        // Formatação
        text = text.replace("&l", "<bold>");
        text = text.replace("&L", "<bold>");
        text = text.replace("&m", "<strikethrough>");
        text = text.replace("&M", "<strikethrough>");
        text = text.replace("&n", "<underlined>");
        text = text.replace("&N", "<underlined>");
        text = text.replace("&o", "<italic>");
        text = text.replace("&O", "<italic>");
        text = text.replace("&k", "<obfuscated>");
        text = text.replace("&K", "<obfuscated>");

        // Reset
        text = text.replace("&r", "<reset>");
        text = text.replace("&R", "<reset>");

        return text;
    }
}
