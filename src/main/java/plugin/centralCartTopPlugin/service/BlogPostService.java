package plugin.centralCartTopPlugin.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.bukkit.configuration.file.FileConfiguration;
import plugin.centralCartTopPlugin.model.BlogPost;
import plugin.centralCartTopPlugin.util.Constants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BlogPostService {

    private final Gson gson;
    private final Logger logger;
    private final int timeout;
    private final String authToken;
    private final int retryAttempts;
    private final int retryDelay;

    public BlogPostService(Logger logger, FileConfiguration config) {
        this.gson = new Gson();
        this.logger = logger;
        this.timeout = config.getInt("api.timeout", Constants.DEFAULT_TIMEOUT);
        this.authToken = config.getString("api.token", "");
        this.retryAttempts = config.getInt("api.retry_attempts", Constants.DEFAULT_RETRY_ATTEMPTS);
        this.retryDelay = config.getInt("api.retry_delay", Constants.DEFAULT_RETRY_DELAY);
    }

    /**
     * Busca o último post publicado no blog de forma assíncrona
     */
    public CompletableFuture<Optional<BlogPost>> getLatestPost() {
        return CompletableFuture.supplyAsync(() -> {
            for (int attempt = 1; attempt <= retryAttempts; attempt++) {
                try {
                    if (attempt > 1) {
                        logger.log(Level.INFO, "[Blog] Tentativa {0} de {1}...", new Object[]{attempt, retryAttempts});
                    }

                    return Optional.ofNullable(fetchLatestPost());

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
                        logger.log(Level.SEVERE, "[Blog] Erro ao buscar último post após {0} tentativas: {1}",
                                new Object[]{retryAttempts, e.getMessage()});
                    } else {
                        sleepRetry();
                    }
                }
            }

            return Optional.empty();
        });
    }

    private BlogPost fetchLatestPost() throws Exception {
        URL url = new URL(Constants.BLOG_API_URL);
        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("x-store-domain", authToken);
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);

            int responseCode = connection.getResponseCode();

            if (responseCode == 401) {
                throw new Exception("[Blog] Autenticação inválida (401). Verifique o api.token no config.yml.");
            }
            if (responseCode != 200) {
                throw new Exception("[Blog] HTTP error code: " + responseCode);
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
            if (!root.has("data") || !root.get("data").isJsonArray()) {
                throw new Exception("[Blog] Resposta sem campo 'data' esperado.");
            }

            JsonArray data = root.getAsJsonArray("data");
            if (data.isEmpty()) {
                logger.info("[Blog] Nenhum post encontrado no blog.");
                return null;
            }

            // O primeiro elemento é o post mais recente
            JsonObject obj = data.get(0).getAsJsonObject();

            BlogPost post = new BlogPost();

            // id é numérico na API
            if (obj.has("id")) post.setId(String.valueOf(obj.get("id").getAsLong()));
            if (obj.has("title")) post.setTitle(obj.get("title").getAsString());

            // url é montada a partir do campo "path"
            if (obj.has("path")) post.setUrl(obj.get("path").getAsString());

            if (obj.has("created_at")) {
                post.setCreatedAt(obj.get("created_at").getAsString());
            } else if (obj.has("createdAt")) {
                post.setCreatedAt(obj.get("createdAt").getAsString());
            }

            logger.log(Level.INFO, "[Blog] Post obtido: {0}", post.getTitle());
            return post;

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

    private void sleepRetry() {
        try {
            Thread.sleep(retryDelay);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
