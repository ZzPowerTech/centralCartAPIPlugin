package plugin.centralCartTopPlugin.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.bukkit.configuration.file.FileConfiguration;
import plugin.centralCartTopPlugin.model.BlogPost;
import plugin.centralCartTopPlugin.util.Constants;
import plugin.centralCartTopPlugin.util.PluginUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Acessa a API de posts da loja (webstore) da CentralCart.
 *
 * <p>A API é multi-tenant: cada requisição precisa do header {@code x-store-domain} identificando
 * a loja. Sem ele, a API responde {@code 404 "Store not found"}. Por isso o serviço aborta cedo
 * (com erro explícito) quando {@code api.store_domain} não está configurado.
 */
public class BlogPostService {

    private final Gson gson;
    private final Logger logger;
    private final int timeout;
    private final String storeDomain;
    private final int retryAttempts;
    private final int retryDelay;

    public BlogPostService(Logger logger, FileConfiguration config) {
        this.gson = new Gson();
        this.logger = logger;
        this.timeout = config.getInt("api.timeout", Constants.DEFAULT_TIMEOUT);
        this.retryAttempts = config.getInt("api.retry_attempts", Constants.DEFAULT_RETRY_ATTEMPTS);
        this.retryDelay = config.getInt("api.retry_delay", Constants.DEFAULT_RETRY_DELAY);

        // Aceita URL completa (https://loja.austv.net/) ou apenas o domínio (loja.austv.net)
        this.storeDomain = PluginUtils.normalizeStoreDomain(config.getString("api.store_domain", ""));

        if (!PluginUtils.isStoreDomainConfigured(storeDomain)) {
            logger.warning("==========================================");
            logger.warning("[Blog] api.store_domain NÃO configurado!");
            logger.warning("[Blog] As notificações de blog ficarão indisponíveis (a API responde 404 'Store not found').");
            logger.warning("[Blog] Configure em config.yml -> api.store_domain: \"loja.austv.net\"");
            logger.warning("==========================================");
        }
    }

    /**
     * @return true se o domínio da loja está configurado e o serviço pode operar
     */
    public boolean isConfigured() {
        return PluginUtils.isStoreDomainConfigured(storeDomain);
    }

    /**
     * Busca o post mais recente publicado no blog de forma assíncrona (conveniência).
     */
    public CompletableFuture<Optional<BlogPost>> getLatestPost() {
        return getRecentPosts()
                .thenApply(posts -> posts.isEmpty() ? Optional.empty() : Optional.of(posts.get(0)));
    }

    /**
     * Busca os posts recentes do blog, ordenados do mais recente para o mais antigo
     * (mesma ordem retornada pela API). Retorna lista vazia em caso de falha.
     */
    public CompletableFuture<List<BlogPost>> getRecentPosts() {
        return CompletableFuture.supplyAsync(() -> {
            if (!PluginUtils.isStoreDomainConfigured(storeDomain)) {
                logger.severe("[Blog] Requisição abortada: api.store_domain não configurado em config.yml.");
                return Collections.<BlogPost>emptyList();
            }

            for (int attempt = 1; attempt <= retryAttempts; attempt++) {
                try {
                    if (attempt > 1) {
                        logger.log(Level.INFO, "[Blog] Tentativa {0} de {1}...", new Object[]{attempt, retryAttempts});
                    }

                    return fetchRecentPosts();

                } catch (java.net.SocketTimeoutException e) {
                    logger.log(Level.WARNING, "[Blog] Timeout na tentativa {0} de {1}", new Object[]{attempt, retryAttempts});
                    if (attempt == retryAttempts) {
                        logger.log(Level.SEVERE, "[Blog] Erro de timeout após {0} tentativas!", retryAttempts);
                    } else {
                        sleepRetry();
                    }
                } catch (java.net.UnknownHostException e) {
                    logger.severe("[Blog] Não foi possível conectar à API CentralCart para buscar posts.");
                    break;
                } catch (Exception e) {
                    logger.log(Level.WARNING, "[Blog] Erro na tentativa {0}: {1}", new Object[]{attempt, e.getMessage()});
                    if (attempt == retryAttempts) {
                        logger.log(Level.SEVERE, "[Blog] Erro ao buscar posts após {0} tentativas: {1}",
                                new Object[]{retryAttempts, e.getMessage()});
                    } else {
                        sleepRetry();
                    }
                }
            }

            return Collections.emptyList();
        });
    }

    private List<BlogPost> fetchRecentPosts() throws Exception {
        URL url = new URL(Constants.BLOG_API_URL);
        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("x-store-domain", storeDomain);
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);

            logger.log(Level.INFO, "[Blog] GET {0} | x-store-domain: {1}",
                    new Object[]{Constants.BLOG_API_URL, storeDomain});

            int responseCode = connection.getResponseCode();

            if (responseCode != 200) {
                String errBody = PluginUtils.readErrorBody(connection);
                throw new Exception("[Blog] HTTP " + responseCode + " — body: " + errBody);
            }

            reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            JsonObject root;
            try {
                root = gson.fromJson(response.toString(), JsonObject.class);
            } catch (JsonSyntaxException e) {
                throw new Exception("[Blog] Resposta JSON inválida: " + e.getMessage());
            }

            // A API retorna { "meta": {...}, "data": [ posts ] }
            if (root == null || !root.has("data") || !root.get("data").isJsonArray()) {
                throw new Exception("[Blog] Resposta sem campo 'data' esperado.");
            }

            JsonArray data = root.getAsJsonArray("data");
            if (data.isEmpty()) {
                logger.info("[Blog] Nenhum post encontrado no blog.");
                return Collections.emptyList();
            }

            List<BlogPost> posts = new ArrayList<>(data.size());
            for (JsonElement element : data) {
                if (element.isJsonObject()) {
                    posts.add(parsePost(element.getAsJsonObject()));
                }
            }

            logger.log(Level.INFO, "[Blog] {0} post(s) obtido(s). Mais recente: {1}",
                    new Object[]{posts.size(), posts.isEmpty() ? "(nenhum)" : posts.get(0).getTitle()});
            return posts;

        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "[Blog] Erro ao fechar BufferedReader", e);
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Converte um objeto JSON de post em {@link BlogPost}, montando a URL completa
     * (domínio + path) e sanitizando o título.
     */
    private BlogPost parsePost(JsonObject obj) {
        BlogPost post = new BlogPost();

        // id é numérico na API
        if (hasValue(obj, "id")) {
            post.setId(String.valueOf(obj.get("id").getAsLong()));
        }
        if (hasValue(obj, "title")) {
            post.setTitle(sanitizeTitle(obj.get("title").getAsString()));
        }
        // url completa = domínio da loja + path
        if (hasValue(obj, "path")) {
            post.setUrl("https://" + storeDomain + obj.get("path").getAsString());
        }
        if (hasValue(obj, "created_at")) {
            post.setCreatedAt(obj.get("created_at").getAsString());
        } else if (hasValue(obj, "createdAt")) {
            post.setCreatedAt(obj.get("createdAt").getAsString());
        }

        return post;
    }

    private boolean hasValue(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull();
    }

    /**
     * Limpa o título vindo da API: remove prefixo markdown de heading ("# ") e caracteres
     * {@code < >} que quebrariam o parsing do MiniMessage ao serem injetados via placeholder.
     */
    private String sanitizeTitle(String raw) {
        if (raw == null) {
            return null;
        }
        return raw.replaceAll("^#+\\s*", "")
                .replace("<", "")
                .replace(">", "")
                .trim();
    }

    private void sleepRetry() {
        try {
            Thread.sleep(retryDelay);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
