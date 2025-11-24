package plugin.centralCartTopPlugin;

import org.bukkit.plugin.java.JavaPlugin;
import plugin.centralCartTopPlugin.command.TopDonadoresCommand;
import plugin.centralCartTopPlugin.service.CentralCartApiService;

public final class CentralCartTopPlugin extends JavaPlugin {

    private CentralCartApiService apiService;

    @Override
    public void onEnable() {
        getLogger().info("§a[CentralCartTopPlugin] Plugin iniciado com sucesso!");

        // Salva o config.yml padrão se não existir
        saveDefaultConfig();

        // Inicializa o serviço da API
        apiService = new CentralCartApiService(getLogger(), getConfig());

        // Registra o comando
        getCommand("topdonadores").setExecutor(new TopDonadoresCommand(apiService, getConfig()));

        getLogger().info("§a[CentralCartTopPlugin] Comando /topdonadores registrado!");
        getLogger().info("§a[CentralCartTopPlugin] API URL: " + getConfig().getString("api.url"));
    }

    @Override
    public void onDisable() {
        getLogger().info("§c[CentralCartTopPlugin] Plugin desabilitado!");
    }
}
