package plugin.centralCartTopPlugin.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import plugin.centralCartTopPlugin.CentralCartTopPlugin;
import plugin.centralCartTopPlugin.manager.MessagesManager;
import plugin.centralCartTopPlugin.service.TopNpcManager;

import java.util.HashMap;
import java.util.Map;

public class RemoveTopNpcsCommand implements CommandExecutor {

    private final CentralCartTopPlugin plugin;
    private final TopNpcManager npcManager;
    private final MessagesManager messages;

    public RemoveTopNpcsCommand(CentralCartTopPlugin plugin, TopNpcManager npcManager) {
        this.plugin = plugin;
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
            sender.sendMessage(messages.getMessageWithPrefix("remove_npcs.no_citizens"));
            return true;
        }

        sender.sendMessage(messages.getMessageWithPrefix("remove_npcs.removing"));

        int count = npcManager.getNpcIds().size();
        npcManager.removeAllNPCs();
        plugin.saveConfig();

        if (count > 0) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("count", String.valueOf(count));
            sender.sendMessage(messages.getMessageWithPrefix("remove_npcs.success", placeholders));
        } else {
            sender.sendMessage(messages.getMessageWithPrefix("remove_npcs.no_npcs"));
        }

        return true;
    }
}

