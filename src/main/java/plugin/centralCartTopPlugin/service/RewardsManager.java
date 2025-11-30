package plugin.centralCartTopPlugin.service;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import plugin.centralCartTopPlugin.CentralCartTopPlugin;
import plugin.centralCartTopPlugin.model.TopCustomer;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.logging.Logger;

public class RewardsManager {

    private final CentralCartTopPlugin plugin;
    private final Logger logger;
    private FileConfiguration rewardsConfig;
    private File pendingRewardsFile;
    private FileConfiguration pendingRewardsData;

    public RewardsManager(CentralCartTopPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        loadRewardsConfig();
        loadPendingRewards();
    }

    /**
     * Carrega o arquivo de configuração de recompensas
     */
    private void loadRewardsConfig() {
        File rewardsFile = new File(plugin.getDataFolder(), "rewards.yml");

        if (!rewardsFile.exists()) {
            plugin.saveResource("rewards.yml", false);
        }

        rewardsConfig = YamlConfiguration.loadConfiguration(rewardsFile);
        logger.info("§a[Rewards] Configuração de recompensas carregada!");
    }

    /**
     * Carrega o arquivo de recompensas pendentes
     */
    private void loadPendingRewards() {
        pendingRewardsFile = new File(plugin.getDataFolder(), "pending_rewards.yml");

        if (!pendingRewardsFile.exists()) {
            try {
                if (!pendingRewardsFile.createNewFile()) {
                    logger.warning("§c[Rewards] Não foi possível criar arquivo de recompensas pendentes.");
                }
            } catch (IOException e) {
                logger.severe("§c[Rewards] Erro ao criar arquivo de recompensas pendentes: " + e.getMessage());
            }
        }

        pendingRewardsData = YamlConfiguration.loadConfiguration(pendingRewardsFile);
    }

    /**
     * Recarrega as configurações
     */
    public void reload() {
        loadRewardsConfig();
        loadPendingRewards();
        logger.info("§a[Rewards] Configurações recarregadas!");
    }

    /**
     * Distribui recompensas para os top 3 doadores
     */
    public void distributeRewards(List<TopCustomer> top3) {
        if (!rewardsConfig.getBoolean("enabled", true)) {
            logger.info("§e[Rewards] Sistema de recompensas desabilitado.");
            return;
        }

        if (top3 == null || top3.isEmpty()) {
            logger.warning("§c[Rewards] Lista de top doadores vazia!");
            return;
        }

        String month = getMonthName();

        // Envia broadcast
        sendBroadcast(top3, month);

        // Distribui recompensas para cada posição
        for (TopCustomer customer : top3) {
            distributeRewardForPlayer(customer, month);
        }

        logger.info("§a[Rewards] Recompensas distribuídas com sucesso!");
    }

    /**
     * Distribui recompensa para um jogador específico
     */
    private void distributeRewardForPlayer(TopCustomer customer, String month) {
        String playerName = customer.getName();
        int position = customer.getPosition();

        Player player = Bukkit.getPlayerExact(playerName);

        if (player != null && player.isOnline()) {
            // Jogador está online, entrega imediatamente
            giveRewards(player, position, month, false);
        } else {
            // Jogador offline, salva para entregar quando logar
            savePendingReward(playerName, position, month);
            logger.info("§e[Rewards] Recompensa salva para " + playerName + " (offline)");
        }
    }

    /**
     * Entrega as recompensas para um jogador
     */
    private void giveRewards(Player player, int position, String month, boolean isPending) {
        String positionKey = getPositionKey(position);
        String positionName = position + "º";

        List<String> rewardsList = new ArrayList<>();

        // Executa comandos
        if (rewardsConfig.getBoolean("settings.commands_first", true)) {
            executeCommands(player, positionKey, rewardsList);
        }

        // Entrega itens
        giveItems(player, positionKey, month, rewardsList);

        // Se não executou comandos antes, executa agora
        if (!rewardsConfig.getBoolean("settings.commands_first", true)) {
            executeCommands(player, positionKey, rewardsList);
        }

        // Envia mensagem para o jogador
        String messageKey = isPending ? "messages.pending_rewards" : "messages.player_received";
        List<String> messages = rewardsConfig.getStringList(messageKey);

        for (String message : messages) {
            String formatted = message
                .replace("{player}", player.getName())
                .replace("{position}", positionName)
                .replace("{month}", month)
                .replace("{rewards_list}", String.join("\n", rewardsList));

            player.sendMessage(formatted);
        }

        logger.info("§a[Rewards] Recompensas entregues para " + player.getName() + " (" + positionName + " lugar)");
    }

    /**
     * Executa os comandos de recompensa
     */
    private void executeCommands(Player player, String positionKey, List<String> rewardsList) {
        List<String> commands = rewardsConfig.getStringList("rewards." + positionKey + ".commands");
        int delay = rewardsConfig.getInt("settings.command_delay", 5);

        for (int i = 0; i < commands.size(); i++) {
            String command = commands.get(i).replace("{player}", player.getName());
            int tickDelay = i * delay;

            Bukkit.getScheduler().runTaskLater(plugin, () ->
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
            , tickDelay);
        }

        if (!commands.isEmpty()) {
            rewardsList.add("§a✓ §fComandos executados");
        }
    }

    /**
     * Entrega os itens de recompensa
     */
    private void giveItems(Player player, String positionKey, String month, List<String> rewardsList) {
        ConfigurationSection itemsSection = rewardsConfig.getConfigurationSection("rewards." + positionKey + ".items");

        if (itemsSection == null) {
            return;
        }

        List<?> itemsList = rewardsConfig.getList("rewards." + positionKey + ".items");
        if (itemsList == null) return;

        for (Object obj : itemsList) {
            if (!(obj instanceof Map)) continue;

            @SuppressWarnings("unchecked")
            Map<String, Object> itemMap = (Map<String, Object>) obj;

            ItemStack item = createItemFromConfig(itemMap, month);
            if (item != null) {
                giveItemToPlayer(player, item);
                rewardsList.add("§a✓ §f" + item.getAmount() + "x " + formatMaterialName(item.getType()));
            }
        }
    }

    /**
     * Cria um item a partir da configuração
     */
    private ItemStack createItemFromConfig(Map<String, Object> config, String month) {
        try {
            String materialName = (String) config.get("material");
            Material material = Material.getMaterial(materialName);

            if (material == null) {
                logger.warning("§c[Rewards] Material inválido: " + materialName);
                return null;
            }

            int amount = config.containsKey("amount") ? (int) config.get("amount") : 1;
            ItemStack item = new ItemStack(material, amount);
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                // Nome do item
                if (config.containsKey("name")) {
                    String name = ((String) config.get("name")).replace("{month}", month);
                    meta.displayName(net.kyori.adventure.text.Component.text(name));
                }

                // Lore
                if (config.containsKey("lore")) {
                    @SuppressWarnings("unchecked")
                    List<String> lore = (List<String>) config.get("lore");
                    List<net.kyori.adventure.text.Component> formattedLore = new ArrayList<>();
                    for (String line : lore) {
                        formattedLore.add(net.kyori.adventure.text.Component.text(line.replace("{month}", month)));
                    }
                    meta.lore(formattedLore);
                }

                // Encantamentos
                if (config.containsKey("enchantments")) {
                    @SuppressWarnings("unchecked")
                    List<String> enchantments = (List<String>) config.get("enchantments");
                    for (String enchant : enchantments) {
                        String[] parts = enchant.split(":");
                        if (parts.length == 2) {
                            Enchantment enchantment = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(parts[0].toLowerCase()));
                            int level = Integer.parseInt(parts[1]);
                            if (enchantment != null) {
                                meta.addEnchant(enchantment, level, true);
                            }
                        }
                    }
                }

                item.setItemMeta(meta);
            }

            return item;
        } catch (Exception e) {
            logger.severe("§c[Rewards] Erro ao criar item: " + e.getMessage());
            return null;
        }
    }

    /**
     * Entrega um item ao jogador
     */
    private void giveItemToPlayer(Player player, ItemStack item) {
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);

        if (!leftover.isEmpty() && rewardsConfig.getBoolean("settings.drop_if_full", true)) {
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItem(player.getLocation(), drop);
            }

            List<String> fullMessages = rewardsConfig.getStringList("messages.inventory_full");
            for (String msg : fullMessages) {
                player.sendMessage(msg);
            }
        }
    }

    /**
     * Salva uma recompensa pendente
     */
    private void savePendingReward(String playerName, int position, String month) {
        String uuid = playerName.toLowerCase();
        pendingRewardsData.set(uuid + ".position", position);
        pendingRewardsData.set(uuid + ".month", month);
        pendingRewardsData.set(uuid + ".timestamp", System.currentTimeMillis());

        try {
            pendingRewardsData.save(pendingRewardsFile);
        } catch (IOException e) {
            logger.severe("§c[Rewards] Erro ao salvar recompensa pendente: " + e.getMessage());
        }
    }

    /**
     * Verifica e entrega recompensas pendentes ao jogador
     */
    public void checkPendingRewards(Player player) {
        String uuid = player.getName().toLowerCase();

        if (pendingRewardsData.contains(uuid)) {
            int position = pendingRewardsData.getInt(uuid + ".position");
            String month = pendingRewardsData.getString(uuid + ".month");

            // Aguarda 2 segundos antes de entregar (para o jogador carregar completamente)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                giveRewards(player, position, month, true);

                // Remove das pendências
                pendingRewardsData.set(uuid, null);
                try {
                    pendingRewardsData.save(pendingRewardsFile);
                } catch (IOException e) {
                    logger.severe("§c[Rewards] Erro ao remover recompensa pendente: " + e.getMessage());
                }
            }, 40L); // 2 segundos
        }
    }

    /**
     * Envia broadcast das recompensas
     */
    private void sendBroadcast(List<TopCustomer> top3, String month) {
        List<String> messages = rewardsConfig.getStringList("messages.broadcast");

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{month}", month);

        for (int i = 0; i < top3.size() && i < 3; i++) {
            TopCustomer customer = top3.get(i);
            String key = i == 0 ? "first" : i == 1 ? "second" : "third";
            placeholders.put("{" + key + "_player}", customer.getName());
        }

        for (String message : messages) {
            String formatted = message;
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                formatted = formatted.replace(entry.getKey(), entry.getValue());
            }
            Bukkit.getServer().broadcast(net.kyori.adventure.text.Component.text(formatted));
        }
    }

    /**
     * Obtém o nome do mês anterior
     */
    private String getMonthName() {
        LocalDate lastMonth = LocalDate.now().minusMonths(1);
        String monthName = lastMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.of("pt", "BR"));
        return monthName.substring(0, 1).toUpperCase() + monthName.substring(1);
    }

    /**
     * Converte posição para chave de configuração
     */
    private String getPositionKey(int position) {
        return switch (position) {
            case 1 -> "first";
            case 2 -> "second";
            case 3 -> "third";
            default -> "first";
        };
    }

    /**
     * Formata o nome do material
     */
    private String formatMaterialName(Material material) {
        String name = material.name().toLowerCase().replace("_", " ");
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    /**
     * Verifica se o sistema está habilitado
     */
    public boolean isEnabled() {
        return rewardsConfig.getBoolean("enabled", true);
    }
}

