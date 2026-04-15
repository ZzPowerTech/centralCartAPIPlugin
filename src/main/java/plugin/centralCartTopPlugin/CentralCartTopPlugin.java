package plugin.centralCartTopPlugin;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import plugin.centralCartTopPlugin.command.CacheInfoCommand;
import plugin.centralCartTopPlugin.command.MessagesCommand;
import plugin.centralCartTopPlugin.command.ReloadCommand;
import plugin.centralCartTopPlugin.command.RemoveTopNpcsCommand;
import plugin.centralCartTopPlugin.command.ScheduleInfoCommand;
import plugin.centralCartTopPlugin.command.SpawnTopNpcsCommand;
import plugin.centralCartTopPlugin.command.TestRewardsCommand;
import plugin.centralCartTopPlugin.command.TestScheduleCommand;
import plugin.centralCartTopPlugin.command.TopDonadoresCommand;
import plugin.centralCartTopPlugin.listener.PlayerJoinListener;
import plugin.centralCartTopPlugin.manager.MessagesManager;
import plugin.centralCartTopPlugin.service.BlogPostService;
import plugin.centralCartTopPlugin.service.CentralCartApiService;
import plugin.centralCartTopPlugin.service.RewardsManager;
import plugin.centralCartTopPlugin.service.TopNpcManager;
import plugin.centralCartTopPlugin.task.BlogPostCheckTask;
import plugin.centralCartTopPlugin.task.MonthlyNpcUpdateTask;
import plugin.centralCartTopPlugin.util.Constants;

import java.util.logging.Level;

public final class CentralCartTopPlugin extends JavaPlugin {

    private CentralCartApiService apiService;
    private TopNpcManager npcManager;
    private RewardsManager rewardsManager;
    private MonthlyNpcUpdateTask monthlyUpdateTask;
    private MessagesManager messagesManager;
    private BlogPostService blogPostService;
    private BlogPostCheckTask blogPostCheckTask;

    @Override
    public void onEnable() {
        getLogger().info("§a[CentralCartTopPlugin] Plugin iniciado com sucesso!");

        // Salva o config.yml padrão se não existir
        saveDefaultConfig();

        // Inicializa o gerenciador de mensagens PRIMEIRO
        messagesManager = new MessagesManager(this);

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
                        getLogger().info("§e" + Constants.LOG_PREFIX + " Nenhum NPC salvo encontrado — buscando top3 para spawn automático.");
                        apiService.getTop3DonatorsPreviousMonth().thenAccept(top3 -> {
                            if (top3 != null && !top3.isEmpty()) {
                                // Executa criação na thread principal
                                Bukkit.getScheduler().runTask(this, () -> {
                                    try {
                                        npcManager.createOrUpdateNPCs(top3);
                                        saveConfig();
                                        getLogger().info("§a" + Constants.LOG_PREFIX + " NPCs criados automaticamente no início do servidor.");
                                    } catch (Exception ex) {
                                        getLogger().log(Level.SEVERE, "Erro ao criar NPCs no startup", ex);
                                    }
                                });
                            } else {
                                getLogger().warning("§e" + Constants.LOG_PREFIX + " Não foi possível obter top3 no startup para spawn automático.");
                            }
                        }).exceptionally(t -> {
                            getLogger().log(Level.SEVERE, "Erro ao buscar top3 no startup", t);
                            return null;
                        });
                    }

                } else {
                    getLogger().warning("§e" + Constants.LOG_PREFIX + " Citizens não detectado no momento do carregamento de NPCs.");
                }
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Erro ao carregar NPCs no onEnable", e);
            }
        }, Constants.STARTUP_DELAY_TICKS);

        // Inicia a tarefa mensal automática
        startMonthlyUpdateTask();

        // Inicia a verificação de novos posts do blog
        startBlogCheckTask();

        getLogger().info("§a[CentralCartTopPlugin] Comandos registrados!");
        getLogger().log(Level.INFO, "§a[CentralCartTopPlugin] API URL: {0}", getConfig().getString("api.url"));
    }

    @Override
    public void onDisable() {
        // Cancela a tarefa de atualização mensal
        if (monthlyUpdateTask != null) {
            monthlyUpdateTask.cancel();
        }

        // Cancela a tarefa de verificação de posts do blog
        if (blogPostCheckTask != null) {
            blogPostCheckTask.cancel();
        }

        // Persiste os IDs dos NPCs para que sejam reutilizados no próximo onEnable.
        // Os NPCs não são destruídos: o Citizens os salva e os recarrega automaticamente
        // entre reinícios do servidor. Destruí-los aqui causaria a criação de novos
        // NPCs a cada restart, em vez de reutilizar os existentes.
        saveConfig();

        getLogger().info("§c[CentralCartTopPlugin] Plugin desabilitado!");
    }

    /**
     * Inicializa os serviços do plugin
     */
    private void initializeServices() {
        // Inicializa o serviço da API
        apiService = new CentralCartApiService(getLogger(), getConfig());

        // Inicializa o serviço de blog
        blogPostService = new BlogPostService(getLogger(), getConfig());

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
        getCommand("messages").setExecutor(new MessagesCommand(this));
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
            getLogger().info("§e" + Constants.LOG_PREFIX + " Atualização automática mensal desabilitada.");
            return;
        }

        monthlyUpdateTask = new MonthlyNpcUpdateTask(this);

        // Executa a cada hora - verifica se é dia 1º do mês
        monthlyUpdateTask.runTaskTimerAsynchronously(this, Constants.STARTUP_DELAY_TICKS, Constants.MONTHLY_UPDATE_CHECK_INTERVAL);

        getLogger().info("§a" + Constants.LOG_PREFIX + " Atualização automática mensal ativada!");
        getLogger().info("§a" + Constants.LOG_PREFIX + " Os NPCs serão atualizados automaticamente todo dia 1º às 00:00h");
    }

    /**
     * Inicia a tarefa de verificação de novos posts do blog
     */
    private void startBlogCheckTask() {
        if (!getConfig().getBoolean("blog.enabled", false)) {
            getLogger().info("§e" + Constants.LOG_PREFIX + " Notificações de blog desabilitadas.");
            return;
        }

        blogPostCheckTask = new BlogPostCheckTask(this);
        blogPostCheckTask.runTaskTimerAsynchronously(this, Constants.STARTUP_DELAY_TICKS, Constants.BLOG_CHECK_INTERVAL_TICKS);

        getLogger().info("§a" + Constants.LOG_PREFIX + " Verificação de novos posts do blog ativada (a cada 5 minutos)!");
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

        // Cancela a tarefa de blog
        if (blogPostCheckTask != null) {
            blogPostCheckTask.cancel();
            blogPostCheckTask = null;
        }

        // Recarrega mensagens
        if (messagesManager != null) {
            messagesManager.reload();
        }

        // Recria o serviço da API (para pegar novo token/config)
        apiService = new CentralCartApiService(getLogger(), getConfig());

        // Recria o serviço de blog
        blogPostService = new BlogPostService(getLogger(), getConfig());

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

        // Reinicia as tarefas
        startMonthlyUpdateTask();
        startBlogCheckTask();

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

    public MessagesManager getMessagesManager() {
        return messagesManager;
    }

    public BlogPostService getBlogPostService() {
        return blogPostService;
    }
}
