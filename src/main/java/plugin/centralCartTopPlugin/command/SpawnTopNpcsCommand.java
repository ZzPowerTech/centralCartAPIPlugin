package plugin.centralCartTopPlugin.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import plugin.centralCartTopPlugin.model.TopCustomer;
import plugin.centralCartTopPlugin.service.CentralCartApiService;
import plugin.centralCartTopPlugin.service.TopNpcManager;

import java.util.List;

public class SpawnTopNpcsCommand implements CommandExecutor {

    private final CentralCartApiService apiService;
    private final TopNpcManager npcManager;

    public SpawnTopNpcsCommand(CentralCartApiService apiService, TopNpcManager npcManager) {
        this.apiService = apiService;
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
            sender.sendMessage("§c§l[CentralCart] §cBaixe em: https://www.spigotmc.org/resources/citizens.13811/");
            return true;
        }

        sender.sendMessage("§e§l[CentralCart] §aBuscando top doadores para criar os NPCs...");

        apiService.getTop3DonatorsPreviousMonth().thenAccept(top3 -> {
            if (top3.isEmpty()) {
                sender.sendMessage("§c§l[CentralCart] §cNão foi possível buscar os dados dos top doadores.");
                return;
            }

            try {
                npcManager.createOrUpdateNPCs(top3);
                sender.sendMessage("§a§l[CentralCart] §aNPCs dos top doadores criados com sucesso!");
                sender.sendMessage("§a§l[CentralCart] §aTop doadores:");

                for (TopCustomer customer : top3) {
                    sender.sendMessage("§f  " + customer.getPosition() + "º - §e" + customer.getName());
                }
            } catch (Exception e) {
                sender.sendMessage("§c§l[CentralCart] §cErro ao criar NPCs: " + e.getMessage());
            }
        }).exceptionally(throwable -> {
            sender.sendMessage("§c§l[CentralCart] §cErro ao buscar dados: " + throwable.getMessage());
            return null;
        });

        return true;
    }
}

