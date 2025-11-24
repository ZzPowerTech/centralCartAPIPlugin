package plugin.centralCartTopPlugin.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.configuration.file.FileConfiguration;
import plugin.centralCartTopPlugin.model.TopCustomer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class CentralCartApiService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final Gson gson;
    private final Logger logger;
    private final String apiUrl;
    private final int timeout;
    private final String authToken;

    public CentralCartApiService(Logger logger, FileConfiguration config) {
        this.gson = new Gson();
        this.logger = logger;
        this.apiUrl = config.getString("api.url", "https://api.centralcart.com.br/v1/app/widget/top_customers");
        this.timeout = config.getInt("api.timeout", 5000);
        this.authToken = config.getString("api.token", "");

        // Validar se o token foi configurado
        if (authToken.isEmpty() || authToken.equals("COLOQUE_SEU_TOKEN_AQUI")) {
            logger.warning("==========================================");
            logger.warning("ATENÇÃO: Token de autenticação não configurado!");
            logger.warning("Configure o token em: plugins/centralCartTopPlugin/config.yml");
            logger.warning("==========================================");
        }
    }

    /**
     * Busca os top 3 doadores do mês anterior de forma assíncrona
     */
    public CompletableFuture<List<TopCustomer>> getTop3DonatorsPreviousMonth() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Calcula o mês anterior
                YearMonth lastMonth = YearMonth.now().minusMonths(1);
                LocalDate fromDate = lastMonth.atDay(1);
                LocalDate toDate = lastMonth.atEndOfMonth();

                String from = fromDate.format(DATE_FORMATTER);
                String to = toDate.format(DATE_FORMATTER);

                logger.info("Buscando top doadores de " + from + " até " + to);

                // Faz a requisição à API
                List<TopCustomer> allCustomers = fetchTopCustomers(from, to);

                // Retorna apenas os 3 primeiros
                List<TopCustomer> top3 = new ArrayList<>();
                for (int i = 0; i < Math.min(3, allCustomers.size()); i++) {
                    top3.add(allCustomers.get(i));
                }

                return top3;

            } catch (Exception e) {
                logger.severe("Erro ao buscar top doadores: " + e.getMessage());
                return new ArrayList<>();
            }
        });
    }

    /**
     * Faz a requisição HTTP para a API
     */
    private List<TopCustomer> fetchTopCustomers(String from, String to) throws Exception {
        String urlString = apiUrl + "?from=" + from + "&to=" + to;
        URL url = new URL(urlString);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
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

        // Lê a resposta
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        connection.disconnect();

        // Parse JSON
        logger.info("Resposta da API recebida com sucesso");

        JsonObject jsonResponse = gson.fromJson(response.toString(), JsonObject.class);
        JsonArray dataArray = jsonResponse.getAsJsonArray("data");

        List<TopCustomer> customers = new ArrayList<>();
        for (int i = 0; i < dataArray.size(); i++) {
            JsonObject customerObj = dataArray.get(i).getAsJsonObject();
            TopCustomer customer = new TopCustomer();
            customer.setName(customerObj.get("name").getAsString());
            customer.setTotal(customerObj.get("total").getAsDouble());
            customer.setPosition(i + 1);
            customers.add(customer);
        }

        return customers;
    }
}

