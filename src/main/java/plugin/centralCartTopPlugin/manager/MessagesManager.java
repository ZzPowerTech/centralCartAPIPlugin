package plugin.centralCartTopPlugin.manager;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Gerenciador de mensagens do plugin
 * Carrega e gerencia todas as mensagens de messages.yml
 */
public class MessagesManager {

    private final JavaPlugin plugin;
    private final Logger logger;
    private File messagesFile;
    private FileConfiguration messagesConfig;
    private final Map<String, String> cachedMessages;

    public MessagesManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.cachedMessages = new HashMap<>();
        loadMessages();
    }

    /**
     * Carrega o arquivo de mensagens
     */
    public void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        // Cria o arquivo se não existir
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
            logger.info("§a[Messages] Arquivo de mensagens criado!");
        }

        // Carrega a configuração
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        // Limpa cache
        cachedMessages.clear();

        logger.info("§a[Messages] Mensagens carregadas com sucesso!");
    }

    /**
     * Recarrega o arquivo de mensagens
     */
    public void reload() {
        loadMessages();
        logger.info("§a[Messages] Mensagens recarregadas!");
    }

    /**
     * Obtém uma mensagem do arquivo
     *
     * @param path Caminho da mensagem (ex: "general.prefix")
     * @return Mensagem formatada com cores
     */
    public String getMessage(String path) {
        return getMessage(path, new HashMap<>());
    }

    /**
     * Obtém uma mensagem com placeholders substituídos
     *
     * @param path Caminho da mensagem
     * @param placeholders Map de placeholders e valores
     * @return Mensagem formatada
     */
    public String getMessage(String path, Map<String, String> placeholders) {
        // Verifica cache primeiro
        String cacheKey = path + placeholders.toString();
        if (cachedMessages.containsKey(cacheKey)) {
            return cachedMessages.get(cacheKey);
        }

        String message = messagesConfig.getString(path, "§cMensagem não encontrada: " + path);

        // Substitui placeholders
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        // Formata cores
        message = formatColors(message);

        // Salva no cache
        cachedMessages.put(cacheKey, message);

        return message;
    }

    /**
     * Obtém uma mensagem com um único placeholder
     */
    public String getMessage(String path, String placeholder, String value) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put(placeholder, value);
        return getMessage(path, placeholders);
    }

    /**
     * Obtém uma mensagem com dois placeholders
     */
    public String getMessage(String path, String p1, String v1, String p2, String v2) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put(p1, v1);
        placeholders.put(p2, v2);
        return getMessage(path, placeholders);
    }

    /**
     * Obtém uma mensagem com três placeholders
     */
    public String getMessage(String path, String p1, String v1, String p2, String v2, String p3, String v3) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put(p1, v1);
        placeholders.put(p2, v2);
        placeholders.put(p3, v3);
        return getMessage(path, placeholders);
    }

    /**
     * Formata códigos de cor (&) para Minecraft
     */
    private String formatColors(String message) {
        return message.replace("&", "§");
    }

    /**
     * Obtém o prefixo do plugin
     */
    public String getPrefix() {
        return getMessage("general.prefix");
    }

    /**
     * Obtém uma mensagem com o prefixo
     */
    public String getMessageWithPrefix(String path) {
        return getPrefix() + " " + getMessage(path);
    }

    /**
     * Obtém uma mensagem com prefixo e placeholders
     */
    public String getMessageWithPrefix(String path, Map<String, String> placeholders) {
        return getPrefix() + " " + getMessage(path, placeholders);
    }

    /**
     * Verifica se uma mensagem existe
     */
    public boolean hasMessage(String path) {
        return messagesConfig.contains(path);
    }

    /**
     * Obtém o arquivo de configuração de mensagens
     */
    public FileConfiguration getConfig() {
        return messagesConfig;
    }

    /**
     * Salva o arquivo de mensagens
     */
    public void save() {
        try {
            messagesConfig.save(messagesFile);
        } catch (IOException e) {
            logger.severe("Erro ao salvar messages.yml: " + e.getMessage());
        }
    }

    /**
     * Limpa o cache de mensagens
     */
    public void clearCache() {
        cachedMessages.clear();
    }

    /**
     * Obtém estatísticas do cache
     */
    public int getCacheSize() {
        return cachedMessages.size();
    }
}

