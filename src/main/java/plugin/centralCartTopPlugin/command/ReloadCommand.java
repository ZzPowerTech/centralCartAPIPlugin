package plugin.centralCartTopPlugin.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import plugin.centralCartTopPlugin.CentralCartTopPlugin;
import plugin.centralCartTopPlugin.manager.MessagesManager;

import java.util.HashMap;
import java.util.Map;

public class ReloadCommand implements CommandExecutor {

    private final CentralCartTopPlugin plugin;
    private final MessagesManager messages;

    public ReloadCommand(CentralCartTopPlugin plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessagesManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("centralcart.admin")) {
            sender.sendMessage(messages.getMessageWithPrefix("general.no_permission"));
            return true;
        }

        sender.sendMessage(messages.getMessageWithPrefix("reload.reloading"));

        try {
            // Recarrega o config.yml
            plugin.reloadConfig();

            // Reinicializa os serviços com a nova configuração
            plugin.reloadServices();

            // Recarrega as recompensas
            if (plugin.getRewardsManager() != null) {
                plugin.getRewardsManager().reload();
            }

            sender.sendMessage(messages.getMessageWithPrefix("reload.success"));

            // Informações de status
            Map<String, String> tokenStatus = new HashMap<>();
            tokenStatus.put("status", plugin.getConfig().getString("api.token", "").equals("COLOQUE_SEU_TOKEN_AQUI")
                ? messages.getMessage("reload.status_not_configured")
                : messages.getMessage("reload.status_configured"));
            sender.sendMessage(messages.getMessageWithPrefix("reload.info_token", tokenStatus));

            Map<String, String> npcsStatus = new HashMap<>();
            npcsStatus.put("status", plugin.getConfig().getBoolean("npcs.enabled", true)
                ? messages.getMessage("reload.status_enabled")
                : messages.getMessage("reload.status_disabled"));
            sender.sendMessage(messages.getMessageWithPrefix("reload.info_npcs", npcsStatus));

            Map<String, String> citizensStatus = new HashMap<>();
            citizensStatus.put("status", plugin.getNpcManager().isCitizensEnabled()
                ? messages.getMessage("reload.status_detected")
                : messages.getMessage("reload.status_not_found"));
            sender.sendMessage(messages.getMessageWithPrefix("reload.info_citizens", citizensStatus));

            Map<String, String> rewardsStatus = new HashMap<>();
            rewardsStatus.put("status", (plugin.getRewardsManager() != null && plugin.getRewardsManager().isEnabled())
                ? messages.getMessage("reload.status_enabled")
                : messages.getMessage("reload.status_disabled"));
            sender.sendMessage(messages.getMessageWithPrefix("reload.info_rewards", rewardsStatus));

        } catch (Exception e) {
            Map<String, String> errorPlaceholders = new HashMap<>();
            errorPlaceholders.put("error", e.getMessage());
            sender.sendMessage(messages.getMessageWithPrefix("reload.error", errorPlaceholders));
            plugin.getLogger().severe("Erro ao recarregar plugin: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }
}

