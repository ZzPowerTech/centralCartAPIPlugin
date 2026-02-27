package plugin.centralCartTopPlugin.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import plugin.centralCartTopPlugin.CentralCartTopPlugin;
import plugin.centralCartTopPlugin.manager.MessagesManager;
import plugin.centralCartTopPlugin.util.Constants;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class ReloadCommand implements CommandExecutor {

    private final CentralCartTopPlugin plugin;
    private final MessagesManager messages;

    public ReloadCommand(CentralCartTopPlugin plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessagesManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission(Constants.PERMISSION_ADMIN)) {
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
            String tokenValue = plugin.getConfig().getString("api.token", "");
            if (tokenValue == null) {
                tokenValue = "";
            }
            String statusNotConfigured = messages.getMessage("reload.status_not_configured");
            String statusConfigured = messages.getMessage("reload.status_configured");
            tokenStatus.put("status", tokenValue.equals(Constants.PLACEHOLDER_TOKEN)
                ? (statusNotConfigured != null ? statusNotConfigured : "Não configurado")
                : (statusConfigured != null ? statusConfigured : "Configurado"));
            sender.sendMessage(messages.getMessageWithPrefix("reload.info_token", tokenStatus));

            Map<String, String> npcsStatus = new HashMap<>();
            String statusEnabled = messages.getMessage("reload.status_enabled");
            String statusDisabled = messages.getMessage("reload.status_disabled");
            npcsStatus.put("status", plugin.getConfig().getBoolean("npcs.enabled", true)
                ? (statusEnabled != null ? statusEnabled : "Ativado")
                : (statusDisabled != null ? statusDisabled : "Desativado"));
            sender.sendMessage(messages.getMessageWithPrefix("reload.info_npcs", npcsStatus));

            Map<String, String> citizensStatus = new HashMap<>();
            citizensStatus.put("status", plugin.getNpcManager() != null && plugin.getNpcManager().isCitizensEnabled()
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
            plugin.getLogger().log(Level.SEVERE, "Erro ao recarregar plugin", e);
        }

        return true;
    }
}

