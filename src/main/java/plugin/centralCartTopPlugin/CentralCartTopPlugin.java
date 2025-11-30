package plugin.centralCartTopPlugin;

import org.bukkit.plugin.java.JavaPlugin;
import plugin.centralCartTopPlugin.command.ReloadCommand;
import plugin.centralCartTopPlugin.command.RemoveTopNpcsCommand;
import plugin.centralCartTopPlugin.command.ScheduleInfoCommand;
import plugin.centralCartTopPlugin.command.SpawnTopNpcsCommand;
import plugin.centralCartTopPlugin.command.TestScheduleCommand;
import plugin.centralCartTopPlugin.command.TopDonadoresCommand;
import plugin.centralCartTopPlugin.listener.PlayerJoinListener;
import plugin.centralCartTopPlugin.service.CentralCartApiService;
import plugin.centralCartTopPlugin.service.RewardsManager;
import plugin.centralCartTopPlugin.service.TopNpcManager;
import plugin.centralCartTopPlugin.task.MonthlyNpcUpdateTask;

public final class CentralCartTopPlugin extends JavaPlugin {

    private CentralCartApiService apiService;
    private TopNpcManager npcManager;
    private RewardsManager rewardsManager;
    private MonthlyNpcUpdateTask monthlyUpdateTask;

    @Override
    public void onEnable() {
        getLogger().info("§a[CentralCartTopPlugin] Plugin iniciado com sucesso!");
        
        // Salva o config.yml padrão se não existir
        saveDefaultConfig();
        
        // Inicializa os serviços
        initializeServices();

        // Registra os comandos
        registerCommands();

        // Registra os listeners
        registerListeners();

        getLogger().info("§a[CentralCartTopPlugin] Comandos registrados!");
        getLogger().info("§a[CentralCartTopPlugin] API URL: " + getConfig().getString("api.url"));
    }

    @Override
    public void onDisable() {
        // Cancela a tarefa de atualização mensal
        if (monthlyUpdateTask != null) {
            monthlyUpdateTask.cancel();
        }

        // NPCs não são removidos ao desabilitar para persistirem após restart
        // Se quiser remover automaticamente, descomente a linha abaixo:
        // if (npcManager != null) {
        //     npcManager.removeAllNPCs();
        // }

        getLogger().info("§c[CentralCartTopPlugin] Plugin desabilitado!");
    }

    /**
     * Inicializa os serviços do plugin
     */
    private void initializeServices() {
        // Inicializa o serviço da API
        apiService = new CentralCartApiService(getLogger(), getConfig());
        
        // Inicializa o gerenciador de NPCs
        npcManager = new TopNpcManager(getLogger(), getConfig());

        // Inicializa o gerenciador de recompensas
        rewardsManager = new RewardsManager(this);

        // Verifica se Citizens está disponível
        if (npcManager.isCitizensEnabled()) {
            getLogger().info("§a[CentralCartTopPlugin] Citizens detectado! Sistema de NPCs habilitado.");
        } else {
            getLogger().warning("§e[CentralCartTopPlugin] Citizens não encontrado. Sistema de NPCs desabilitado.");
            getLogger().warning("§e[CentralCartTopPlugin] Para usar NPCs, instale Citizens: https://www.spigotmc.org/resources/citizens.13811/");
        }
    }

    /**
     * Registra todos os comandos do plugin
     */
    private void registerCommands() {
        getCommand("topdonadores").setExecutor(new TopDonadoresCommand(this));
        getCommand("spawntopnpcs").setExecutor(new SpawnTopNpcsCommand(this, apiService, npcManager));
        getCommand("removetopnpcs").setExecutor(new RemoveTopNpcsCommand(this, npcManager));
        getCommand("centralcartreload").setExecutor(new ReloadCommand(this));
        getCommand("testschedule").setExecutor(new TestScheduleCommand(this));
        getCommand("scheduleinfo").setExecutor(new ScheduleInfoCommand(this));
    }

    /**
     * Registra todos os listeners do plugin
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getLogger().info("§a[CentralCartTopPlugin] Listeners registrados!");
    }

    /**
     * Inicia a tarefa de atualização mensal automática
     */
    private void startMonthlyUpdateTask() {
        if (!getConfig().getBoolean("npcs.auto_update_enabled", true)) {
            getLogger().info("§e[CentralCartTopPlugin] Atualização automática mensal desabilitada.");
            return;
        }

        monthlyUpdateTask = new MonthlyNpcUpdateTask(this);

        // Executa a cada hora (72000 ticks = 1 hora)
        // Verifica se é dia 1º do mês
        monthlyUpdateTask.runTaskTimerAsynchronously(this, 20L, 72000L);

        getLogger().info("§a[CentralCartTopPlugin] Atualização automática mensal ativada!");
        getLogger().info("§a[CentralCartTopPlugin] Os NPCs serão atualizados automaticamente todo dia 1º às 00:00h");
    }

    /**
     * Recarrega os serviços do plugin com a nova configuração
     */
    public void reloadServices() {
        getLogger().info("§e[CentralCartTopPlugin] Reinicializando serviços...");

        // Cancela a tarefa antiga
        if (monthlyUpdateTask != null) {
            monthlyUpdateTask.cancel();
        }

        // Reinicializa serviços com nova config
        initializeServices();

        // Re-registra comandos para usar novos serviços e config
        registerCommands();

        // Reinicia a tarefa de atualização mensal
        startMonthlyUpdateTask();

        getLogger().info("§a[CentralCartTopPlugin] Serviços reinicializados!");
    }

    public CentralCartApiService getApiService() {
        return apiService;
    }

    public TopNpcManager getNpcManager() {
        return npcManager;
    }

    public RewardsManager getRewardsManager() {
        return rewardsManager;
    }
}
