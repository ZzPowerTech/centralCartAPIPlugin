package plugin.centralCartTopPlugin.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import plugin.centralCartTopPlugin.CentralCartTopPlugin;
import plugin.centralCartTopPlugin.service.TopNpcManager;

public class RemoveTopNpcsCommand implements CommandExecutor {

    private final CentralCartTopPlugin plugin;
    private final TopNpcManager npcManager;

    public RemoveTopNpcsCommand(CentralCartTopPlugin plugin, TopNpcManager npcManager) {
        this.plugin = plugin;
        this.npcManager = npcManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("centralcart.admin")) {
            sender.sendMessage("§c§l[CentralCart] §cVocê não tem permissão para usar este comando.");
            return true;
        }

        if (!npcManager.isCitizensEnabled()) {
            sender.sendMessage("§c§l[CentralCart] §cO plugin Citizens não está instalado ou habilitado!");
            return true;
        }

        npcManager.removeAllNPCs();

        // Salva o config para persistir a remoção dos IDs
        plugin.saveConfig();

        sender.sendMessage("§a§l[CentralCart] §aTodos os NPCs dos top doadores foram removidos!");

        return true;
    }
}

