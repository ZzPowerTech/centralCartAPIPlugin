package plugin.centralCartTopPlugin.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import plugin.centralCartTopPlugin.CentralCartTopPlugin;
import plugin.centralCartTopPlugin.manager.MessagesManager;
import plugin.centralCartTopPlugin.model.TopCustomer;
import plugin.centralCartTopPlugin.service.CentralCartApiService;
import plugin.centralCartTopPlugin.service.TopNpcManager;

import java.util.HashMap;
import java.util.Map;

public class SpawnTopNpcsCommand implements CommandExecutor {

    private final CentralCartTopPlugin plugin;
    private final CentralCartApiService apiService;
    private final TopNpcManager npcManager;
    private final MessagesManager messages;

    public SpawnTopNpcsCommand(CentralCartTopPlugin plugin, CentralCartApiService apiService, TopNpcManager npcManager) {
        this.plugin = plugin;
        this.apiService = apiService;
        this.npcManager = npcManager;
        this.messages = plugin.getMessagesManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("centralcart.admin")) {
            sender.sendMessage(messages.getMessageWithPrefix("general.no_permission"));
            return true;
        }

        if (!npcManager.isCitizensEnabled()) {
            sender.sendMessage(messages.getMessageWithPrefix("spawn_npcs.no_citizens"));
            sender.sendMessage(messages.getMessageWithPrefix("spawn_npcs.citizens_download"));
            return true;
        }

        sender.sendMessage(messages.getMessageWithPrefix("spawn_npcs.searching"));

        apiService.getTop3DonatorsPreviousMonth().thenAccept(top3 -> {
            if (top3.isEmpty()) {
                sender.sendMessage(messages.getMessageWithPrefix("spawn_npcs.error_fetch"));
                sender.sendMessage(messages.getMessageWithPrefix("spawn_npcs.error_check_logs"));
                sender.sendMessage(messages.getMessageWithPrefix("spawn_npcs.possible_causes"));
                sender.sendMessage(messages.getMessage("spawn_npcs.cause_api_offline"));

                Map<String, String> timeoutPlaceholder = new HashMap<>();
                timeoutPlaceholder.put("timeout", String.valueOf(plugin.getConfig().getInt("api.timeout", 5000)));
                sender.sendMessage(messages.getMessage("spawn_npcs.cause_timeout", timeoutPlaceholder));

                sender.sendMessage(messages.getMessage("spawn_npcs.cause_token"));
                sender.sendMessage(messages.getMessage("spawn_npcs.cause_connection"));
                return;
            }

            // Executa a criação de NPCs na thread principal (sincronamente)
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    npcManager.createOrUpdateNPCs(top3);
                    plugin.saveConfig();

                    sender.sendMessage(messages.getMessageWithPrefix("spawn_npcs.success"));
                    sender.sendMessage(messages.getMessage("spawn_npcs.success_header"));
                    sender.sendMessage(messages.getMessage("spawn_npcs.success_title"));
                    sender.sendMessage(messages.getMessage("spawn_npcs.success_header"));

                    String currencySymbol = plugin.getConfig().getString("display.currency-symbol", "R$");

                    for (TopCustomer customer : top3) {
                        String medal = getMedal(customer.getPosition());

                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("medal", medal);
                        placeholders.put("position", String.valueOf(customer.getPosition()));
                        placeholders.put("player", customer.getName());
                        placeholders.put("currency", currencySymbol);
                        placeholders.put("total", String.format("%.2f", customer.getTotal()));

                        sender.sendMessage(messages.getMessage("spawn_npcs.success_line", placeholders));
                    }

                    sender.sendMessage(messages.getMessage("spawn_npcs.success_header"));
                    sender.sendMessage(messages.getMessage("spawn_npcs.success_info"));
                } catch (Exception e) {
                    Map<String, String> errorPlaceholders = new HashMap<>();
                    errorPlaceholders.put("error", e.getMessage());
                    sender.sendMessage(messages.getMessageWithPrefix("spawn_npcs.error_create", errorPlaceholders));
                    plugin.getLogger().severe("Erro ao criar NPCs: " + e.getMessage());
                }
            });
        }).exceptionally(throwable -> {
            Map<String, String> errorPlaceholders = new HashMap<>();
            errorPlaceholders.put("error", throwable.getMessage());
            sender.sendMessage(messages.getMessageWithPrefix("spawn_npcs.error_create", errorPlaceholders));
            return null;
        });

        return true;
    }

    private String getMedal(int position) {
        switch (position) {
            case 1:
                return messages.getMessage("top_donators.medals.first");
            case 2:
                return messages.getMessage("top_donators.medals.second");
            case 3:
                return messages.getMessage("top_donators.medals.third");
            default:
                return "§f";
        }
    }
}

