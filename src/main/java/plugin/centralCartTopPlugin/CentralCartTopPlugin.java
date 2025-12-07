package plugin.centralCartTopPlugin;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import plugin.centralCartTopPlugin.command.CacheInfoCommand;
import plugin.centralCartTopPlugin.command.ReloadCommand;
import plugin.centralCartTopPlugin.command.RemoveTopNpcsCommand;
import plugin.centralCartTopPlugin.command.ScheduleInfoCommand;
import plugin.centralCartTopPlugin.command.SpawnTopNpcsCommand;
import plugin.centralCartTopPlugin.command.TestRewardsCommand;
import plugin.centralCartTopPlugin.command.TestScheduleCommand;
import plugin.centralCartTopPlugin.command.TopDonadoresCommand;
import plugin.centralCartTopPlugin.listener.PlayerJoinListener;
import plugin.centralCartTopPlugin.service.CentralCartApiService;
import plugin.centralCartTopPlugin.service.RewardsManager;
import plugin.centralCartTopPlugin.service.TopNpcManager;
import plugin.centralCartTopPlugin.task.MonthlyNpcUpdateTask;

import java.util.logging.Level;

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

        // Carrega NPCs salvos após um pequeno delay (garante que Citizens e mundos estejam prontos)
        Bukkit.getScheduler().runTaskLater(this, () -> {
            try {
                if (npcManager != null && npcManager.isCitizensEnabled()) {
                    npcManager.loadExistingNPCs();

                    // Se não havia NPCs salvos e a opção auto_spawn_on_start estiver ativa, busca o top3 e cria NPCs automaticamente
                    if (npcManager.getNpcIds().isEmpty() && getConfig().getBoolean("npcs.auto_spawn_on_start", true)) {
                        getLogger().info("§e[CentralCartTopPlugin] Nenhum NPC salvo encontrado — buscando top3 para spawn automático.");
                        apiService.getTop3DonatorsPreviousMonth().thenAccept(top3 -> {
                            if (top3 != null && !top3.isEmpty()) {
                                // Executa criação na thread principal
                                Bukkit.getScheduler().runTask(this, () -> {
                                    try {
                                        npcManager.createOrUpdateNPCs(top3);
                                        saveConfig();
                                        getLogger().info("§a[CentralCartTopPlugin] NPCs criados automaticamente no início do servidor.");
                                    } catch (Exception ex) {
                                        getLogger().severe("Erro ao criar NPCs no startup: " + ex.getMessage());
                                        getLogger().log(Level.SEVERE, "Stack trace:", ex);
                                    }
                                });
                            } else {
                                getLogger().warning("§e[CentralCartTopPlugin] Não foi possível obter top3 no startup para spawn automático.");
                            }
                        }).exceptionally(t -> {
                            getLogger().severe("Erro ao buscar top3 no startup: " + t.getMessage());
                            return null;
                        });
                    }

                } else {
                    getLogger().warning("§e[CentralCartTopPlugin] Citizens não detectado no momento do carregamento de NPCs.");
                }
            } catch (Exception e) {
                getLogger().severe("Erro ao carregar NPCs no onEnable: " + e.getMessage());
                getLogger().log(Level.SEVERE, "Stack trace:", e);
            }
        }, 20L);

        // Inicia a tarefa mensal automática
        startMonthlyUpdateTask();

        getLogger().info("§a[CentralCartTopPlugin] Comandos registrados!");
        getLogger().info("§a[CentralCartTopPlugin] API URL: " + getConfig().getString("api.url"));
    }

    @Override
    public void onDisable() {
        // Cancela a tarefa de atualização mensal
        if (monthlyUpdateTask != null) {
            monthlyUpdateTask.cancel();
        }

        // Remove NPCs salvos para evitar duplicação em reinícios
        if (npcManager != null && npcManager.isCitizensEnabled()) {
            try {
                npcManager.removeAllNPCs();
                // Salva config para persistir remoção
                saveConfig();
            } catch (Exception e) {
                getLogger().severe("Erro ao remover NPCs no onDisable: " + e.getMessage());
                getLogger().log(Level.SEVERE, "Stack trace:", e);
            }
        }

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
        getCommand("testrewards").setExecutor(new TestRewardsCommand(this));
        getCommand("cacheinfo").setExecutor(new CacheInfoCommand(this));
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

        // Recria o serviço da API (para pegar novo token/config)
        apiService = new CentralCartApiService(getLogger(), getConfig());

        // Recarrega o manager de NPCs com a nova config (mantendo a instância para preservar registry)
        if (npcManager != null) {
            npcManager.reload(getConfig());
        } else {
            npcManager = new TopNpcManager(getLogger(), getConfig());
        }

        // Recarrega recompensas
        if (rewardsManager != null) {
            rewardsManager.reload();
        } else {
            rewardsManager = new RewardsManager(this);
        }

        // Re-registra comandos para usar os novos serviços e config
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
