package plugin.centralCartTopPlugin.util;

import plugin.centralCartTopPlugin.manager.MessagesManager;

/**
 * Utilitários comuns do plugin para evitar duplicação de código
 */
public final class PluginUtils {

    // Impede instanciação
    private PluginUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Converte posição para chave de configuração
     *
     * @param position Posição (1, 2 ou 3)
     * @return Chave de string ou null se inválida
     */
    public static String getPositionKey(int position) {
        return switch (position) {
            case Constants.FIRST_PLACE -> Constants.POSITION_KEY_FIRST;
            case Constants.SECOND_PLACE -> Constants.POSITION_KEY_SECOND;
            case Constants.THIRD_PLACE -> Constants.POSITION_KEY_THIRD;
            default -> null;
        };
    }

    /**
     * Obtém a medalha para uma posição usando o MessagesManager
     *
     * @param position Posição (1, 2 ou 3)
     * @param messages MessagesManager para buscar as mensagens
     * @return String da medalha
     */
    public static String getMedal(int position, MessagesManager messages) {
        return switch (position) {
            case Constants.FIRST_PLACE -> messages.getMessage("top_donators.medals.first");
            case Constants.SECOND_PLACE -> messages.getMessage("top_donators.medals.second");
            case Constants.THIRD_PLACE -> messages.getMessage("top_donators.medals.third");
            default -> "§f";
        };
    }

    /**
     * Formata o nome do material removendo underscores e capitalizando
     *
     * @param materialName Nome do material
     * @return Nome formatado
     */
    public static String formatMaterialName(String materialName) {
        if (materialName == null || materialName.isEmpty()) {
            return "";
        }
        String name = materialName.toLowerCase().replace("_", " ");
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    /**
     * Valida se um timeout está em um range razoável
     *
     * @param timeout Timeout em milissegundos
     * @return true se válido
     */
    public static boolean isValidTimeout(int timeout) {
        return timeout >= 1000 && timeout <= 120000; // Entre 1 segundo e 2 minutos
    }

    /**
     * Valida se um número de tentativas de retry é razoável
     *
     * @param retryAttempts Número de tentativas
     * @return true se válido
     */
    public static boolean isValidRetryAttempts(int retryAttempts) {
        return retryAttempts >= 1 && retryAttempts <= 10;
    }

    /**
     * Valida se um delay de retry é razoável
     *
     * @param retryDelay Delay em milissegundos
     * @return true se válido
     */
    public static boolean isValidRetryDelay(int retryDelay) {
        return retryDelay >= 100 && retryDelay <= 30000; // Entre 100ms e 30 segundos
    }

    /**
     * Valida se a duração do cache é razoável
     *
     * @param cacheDuration Duração em minutos
     * @return true se válido
     */
    public static boolean isValidCacheDuration(long cacheDuration) {
        return cacheDuration >= 1 && cacheDuration <= 1440; // Entre 1 minuto e 24 horas
    }

    /**
     * Formata uma localização para log
     *
     * @param x Coordenada X
     * @param y Coordenada Y
     * @param z Coordenada Z
     * @return String formatada
     */
    public static String formatLocation(double x, double y, double z) {
        return String.format("%.1f, %.1f, %.1f", x, y, z);
    }
}
