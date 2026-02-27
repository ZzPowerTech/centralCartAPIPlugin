package plugin.centralCartTopPlugin.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import plugin.centralCartTopPlugin.cache.TopDonatorsCache;
import plugin.centralCartTopPlugin.model.TopCustomer;
import plugin.centralCartTopPlugin.util.Constants;
import plugin.centralCartTopPlugin.util.PluginUtils;

public class CentralCartApiService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final Gson gson;
    private final Logger logger;
    private final String apiUrl;
    private final int timeout;
    private final String authToken;
    private final int retryAttempts;
    private final int retryDelay;
    private final TopDonatorsCache cache;

    public CentralCartApiService(Logger logger, FileConfiguration config) {
        this.gson = new Gson();
        this.logger = logger;
        this.apiUrl = config.getString("api.url", Constants.DEFAULT_API_URL);
        this.timeout = config.getInt("api.timeout", Constants.DEFAULT_TIMEOUT);
        this.authToken = config.getString("api.token", "");
        this.retryAttempts = config.getInt("api.retry_attempts", Constants.DEFAULT_RETRY_ATTEMPTS);
        this.retryDelay = config.getInt("api.retry_delay", Constants.DEFAULT_RETRY_DELAY);

        // Validação de configurações
        if (!PluginUtils.isValidTimeout(this.timeout)) {
            logger.log(Level.WARNING, "Timeout configurado inválido ({0}ms). Usando padrão de {1}ms", new Object[]{this.timeout, Constants.DEFAULT_TIMEOUT});
        }
        if (!PluginUtils.isValidRetryAttempts(this.retryAttempts)) {
            logger.log(Level.WARNING, "Número de tentativas inválido ({0}). Usando padrão de {1}", new Object[]{this.retryAttempts, Constants.DEFAULT_RETRY_ATTEMPTS});
        }

        // Inicializa cache com duração configurável (padrão: 30 minutos)
        long cacheDuration = config.getLong("api.cache_duration_minutes", Constants.DEFAULT_CACHE_DURATION_MINUTES);
        if (!PluginUtils.isValidCacheDuration(cacheDuration)) {
            logger.log(Level.WARNING, "Duração de cache inválida ({0} minutos). Usando padrão de {1} minutos", new Object[]{cacheDuration, Constants.DEFAULT_CACHE_DURATION_MINUTES});
            cacheDuration = Constants.DEFAULT_CACHE_DURATION_MINUTES;
        }
        this.cache = new TopDonatorsCache(cacheDuration);

        // Validar se o token foi configurado
        if (authToken.isEmpty() || authToken.equals(Constants.PLACEHOLDER_TOKEN)) {
            logger.warning("==========================================");
            logger.warning("ATENÇÃO: Token de autenticação não configurado!");
            logger.warning("Configure o token em: plugins/centralCartTopPlugin/config.yml");
            logger.warning("==========================================");
        }
    }

    /**
     * Busca os top 3 doadores do mês anterior de forma assíncrona com cache
     */
    public CompletableFuture<List<TopCustomer>> getTop3DonatorsPreviousMonth() {
        return getTop3DonatorsPreviousMonth(false);
    }

    /**
     * Busca os top 3 doadores do mês anterior com opção de forçar atualização
     *
     * @param forceRefresh Se true, ignora o cache e busca dados novos
     */
    public CompletableFuture<List<TopCustomer>> getTop3DonatorsPreviousMonth(boolean forceRefresh) {
        // Verifica se pode usar cache
        if (!forceRefresh && cache.isValid()) {
            logger.log(Level.INFO, "§a[Cache] Usando dados em cache (válido por mais {0} minutos)", cache.getRemainingValidityMinutes());
            return CompletableFuture.completedFuture(cache.getData());
        }

        return CompletableFuture.supplyAsync(() -> {
            // Calcula o mês anterior
            YearMonth lastMonth = YearMonth.now().minusMonths(1);
            LocalDate fromDate = lastMonth.atDay(1);
            LocalDate toDate = lastMonth.atEndOfMonth();

            String from = fromDate.format(DATE_FORMATTER);
            String to = toDate.format(DATE_FORMATTER);

            logger.log(Level.INFO, "Buscando top doadores de {0} até {1}", new Object[]{from, to});

            // Tenta buscar com retry
            for (int attempt = 1; attempt <= retryAttempts; attempt++) {
                try {
                    if (attempt > 1) {
                        logger.log(Level.INFO, "Tentativa {0} de {1}...", new Object[]{attempt, retryAttempts});
                    }

                    // Faz a requisição à API
                    List<TopCustomer> allCustomers = fetchTopCustomers(from, to);

                    // Retorna apenas os 3 primeiros
                    List<TopCustomer> top3 = new ArrayList<>();
                    for (int i = 0; i < Math.min(3, allCustomers.size()); i++) {
                        top3.add(allCustomers.get(i));
                    }

                    // Atualiza o cache
                    if (!top3.isEmpty()) {
                        cache.update(top3);
                        logger.info("§a[Cache] Dados atualizados (válido por 30 minutos)");
                    }

                    return top3;

                } catch (java.net.SocketTimeoutException e) {
                    logger.log(Level.WARNING, "Timeout na tentativa {0} de {1}", new Object[]{attempt, retryAttempts});
                    if (attempt == retryAttempts) {
                        logger.log(Level.SEVERE, "Erro de timeout após {0} tentativas!", retryAttempts);
                        logger.log(Level.SEVERE, "A API demorou mais que {0} segundos para responder.", timeout / 1000);
                        logger.severe("Tente aumentar o timeout em config.yml: api.timeout");

                        // Em caso de erro, retorna cache antigo se disponível
                        if (cache.hasData()) {
                            logger.warning("§e[Cache] Retornando dados em cache antigos devido a erro na API");
                            return cache.getData();
                        }
                    } else {
                        try {
                            Thread.sleep(retryDelay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                } catch (java.net.UnknownHostException e) {
                    logger.severe("Erro: Não foi possível conectar à API CentralCart");
                    logger.severe("Verifique sua conexão com a internet");

                    // Retorna cache se disponível
                    if (cache.hasData()) {
                        logger.warning("§e[Cache] Retornando dados em cache antigos devido a erro de conexão");
                        return cache.getData();
                    }
                    break; // Não tenta novamente em caso de erro de DNS
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Erro na tentativa {0}: {1}", new Object[]{attempt, e.getMessage()});
                    if (attempt == retryAttempts) {
                        logger.log(Level.SEVERE, "Erro ao buscar top doadores após {0} tentativas: {1}", new Object[]{retryAttempts, e.getMessage()});

                        // Retorna cache se disponível
                        if (cache.hasData()) {
                            logger.warning("§e[Cache] Retornando dados em cache antigos devido a erro");
                            return cache.getData();
                        }
                    } else {
                        try {
                            Thread.sleep(retryDelay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }

            return new ArrayList<>();
        });
    }

    /**
     * Invalida o cache forçando uma nova busca na próxima chamada
     */
    public void invalidateCache() {
        cache.invalidate();
        logger.info("§e[Cache] Cache invalidado manualmente");
    }

    /**
     * Obtém informações sobre o estado do cache
     */
    public String getCacheInfo() {
        if (!cache.hasData()) {
            return "§cCache vazio";
        }

        if (cache.isValid()) {
            return "§aCache válido (expira em " + cache.getRemainingValidityMinutes() + " minutos)";
        } else {
            return "§eCache expirado (última atualização há " + cache.getMinutesSinceLastUpdate() + " minutos)";
        }
    }

    /**
     * Faz a requisição HTTP para a API usando try-with-resources para gerenciamento adequado de recursos
     */
    private List<TopCustomer> fetchTopCustomers(String from, String to) throws Exception {
        String urlString = apiUrl + "?from=" + from + "&to=" + to;
        URL url = new URL(urlString);

        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + authToken);
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);

            int responseCode = connection.getResponseCode();

            if (responseCode == 401) {
                logger.severe("Erro de autenticação (401 Unauthorized)");
                logger.severe("Verifique se o token está configurado corretamente em config.yml");
                throw new Exception("Token de autenticação inválido ou ausente");
            }

            if (responseCode != 200) {
                throw new Exception("HTTP error code: " + responseCode);
            }

            // Lê a resposta usando try-with-resources
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            // Parse JSON com tratamento de erros melhorado
            logger.info("Resposta da API recebida com sucesso");

            JsonObject jsonResponse;
            try {
                jsonResponse = gson.fromJson(response.toString(), JsonObject.class);
            } catch (JsonSyntaxException e) {
                logger.log(Level.SEVERE, "Erro ao parsear resposta JSON da API: {0}", e.getMessage());
                throw new Exception("Resposta da API inválida: " + e.getMessage());
            }

            if (!jsonResponse.has("data")) {
                throw new Exception("Resposta da API não contém o campo 'data'");
            }

            JsonArray dataArray = jsonResponse.getAsJsonArray("data");

            List<TopCustomer> customers = new ArrayList<>();
            for (int i = 0; i < dataArray.size(); i++) {
                JsonObject customerObj = dataArray.get(i).getAsJsonObject();
                TopCustomer customer = new TopCustomer();

                // A API retorna "username" ao invés de "name"
                if (!customerObj.has("username")) {
                    logger.warning("Objeto do cliente não contém 'username', pulando...");
                    continue;
                }

                String username = customerObj.get("username").getAsString();
                customer.setName(username);

                // A API retorna "spent" como string formatada (ex: "R$ 1.139,99")
                if (!customerObj.has("spent")) {
                    logger.warning("Objeto do cliente não contém 'spent', pulando...");
                    continue;
                }

                String spentStr = customerObj.get("spent").getAsString();
                double total = parseSpentValue(spentStr);
                customer.setTotal(total);

                customer.setPosition(i + 1);
                customers.add(customer);

                logger.log(Level.INFO, "Top {0}: {1} - {2}", new Object[]{i + 1, username, spentStr});
            }

            return customers;

        } finally {
            // Garante que os recursos sejam fechados
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Erro ao fechar BufferedReader", e);
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * Converte o valor formatado "R$ 1.139,99" para double 1139.99
     */
    private double parseSpentValue(String spent) {
        try {
            // Remove "R$", espaços e pontos (milhares)
            // Substitui vírgula por ponto (decimal)
            String cleaned = spent
                .replace("R$", "")
                .replace(" ", "")
                .replace(".", "")
                .replace(",", ".");
            return Double.parseDouble(cleaned);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Erro ao parsear valor: {0}", spent);
            return 0.0;
        }
    }
}

