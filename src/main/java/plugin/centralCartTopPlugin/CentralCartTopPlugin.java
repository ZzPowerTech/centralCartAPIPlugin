package plugin.centralCartTopPlugin;

import org.bukkit.plugin.java.JavaPlugin;
import plugin.centralCartTopPlugin.command.TopDonadoresCommand;
import plugin.centralCartTopPlugin.service.CentralCartApiService;

public final class CentralCartTopPlugin extends JavaPlugin {

    private CentralCartApiService apiService;

    @Override
    public void onEnable() {
        getLogger().info("§a[CentralCartTopPlugin] Plugin iniciado com sucesso!");

        // Inicializa o serviço da API
        apiService = new CentralCartApiService(getLogger());

        // Registra o comando
        getCommand("topdonadores").setExecutor(new TopDonadoresCommand(apiService));

        getLogger().info("§a[CentralCartTopPlugin] Comando /topdonadores registrado!");
    }

    @Override
    public void onDisable() {
        getLogger().info("§c[CentralCartTopPlugin] Plugin desabilitado!");
    }
}
