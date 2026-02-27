package plugin.centralCartTopPlugin.util;

/**
 * Constantes do plugin
 */
public final class Constants {

    // Impede instanciação
    private Constants() {
        throw new UnsupportedOperationException("Utility class");
    }

    // Ticks do Minecraft
    public static final long TICKS_PER_SECOND = 20L;
    public static final long TICKS_PER_MINUTE = TICKS_PER_SECOND * 60L;
    public static final long TICKS_PER_HOUR = TICKS_PER_MINUTE * 60L;

    // Delays do Plugin
    public static final long STARTUP_DELAY_TICKS = TICKS_PER_SECOND; // 1 segundo
    public static final long PLAYER_JOIN_REWARD_DELAY_TICKS = TICKS_PER_SECOND * 2; // 2 segundos
    public static final long MONTHLY_UPDATE_CHECK_INTERVAL = TICKS_PER_HOUR; // 1 hora

    // Posições
    public static final int FIRST_PLACE = 1;
    public static final int SECOND_PLACE = 2;
    public static final int THIRD_PLACE = 3;
    public static final int TOP_POSITIONS_COUNT = 3;

    // Chaves de configuração
    public static final String POSITION_KEY_FIRST = "first";
    public static final String POSITION_KEY_SECOND = "second";
    public static final String POSITION_KEY_THIRD = "third";

    // Permissões
    public static final String PERMISSION_ADMIN = "centralcart.admin";

    // Defaults
    public static final String DEFAULT_API_URL = "https://api.centralcart.com.br/v1/app/widget/top_customers";
    public static final int DEFAULT_TIMEOUT = 15000;
    public static final int DEFAULT_RETRY_ATTEMPTS = 3;
    public static final int DEFAULT_RETRY_DELAY = 2000;
    public static final long DEFAULT_CACHE_DURATION_MINUTES = 30;
    public static final String DEFAULT_CURRENCY_SYMBOL = "R$";
    public static final String PLACEHOLDER_TOKEN = "COLOQUE_SEU_TOKEN_AQUI";

    // Prefixos de log
    public static final String LOG_PREFIX = "[CentralCartTopPlugin]";
    public static final String CACHE_PREFIX = "[Cache]";
    public static final String REWARDS_PREFIX = "[Rewards]";
    public static final String MESSAGES_PREFIX = "[Messages]";
}
