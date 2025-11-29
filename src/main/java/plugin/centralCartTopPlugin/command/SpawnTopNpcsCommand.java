package plugin.centralCartTopPlugin.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import plugin.centralCartTopPlugin.CentralCartTopPlugin;
import plugin.centralCartTopPlugin.model.TopCustomer;
import plugin.centralCartTopPlugin.service.CentralCartApiService;
import plugin.centralCartTopPlugin.service.TopNpcManager;

public class SpawnTopNpcsCommand implements CommandExecutor {

    private final CentralCartTopPlugin plugin;
    private final CentralCartApiService apiService;
    private final TopNpcManager npcManager;

    public SpawnTopNpcsCommand(CentralCartTopPlugin plugin, CentralCartApiService apiService, TopNpcManager npcManager) {
        this.plugin = plugin;
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

            // Executa a criação de NPCs na thread principal (sincronamente)
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    npcManager.createOrUpdateNPCs(top3);
                    sender.sendMessage("§a§l[CentralCart] §aNPCs atualizados com sucesso!");
                    sender.sendMessage("§6§l========================================");
                    sender.sendMessage("§e§l        NPCs DOS TOP DOADORES");
                    sender.sendMessage("§6§l========================================");

                    for (TopCustomer customer : top3) {
                        String medal = customer.getPosition() == 1 ? "§6🥇" :
                                      customer.getPosition() == 2 ? "§7🥈" : "§c🥉";
                        sender.sendMessage(String.format("§f%s §6#%d §f- §e%s §7(§aR$ %.2f§7)",
                                medal,
                                customer.getPosition(),
                                customer.getName(),
                                customer.getTotal()));
                    }

                    sender.sendMessage("§6§l========================================");
                    sender.sendMessage("§a§lℹ §aOs NPCs foram movidos/atualizados nas coordenadas configuradas!");
                } catch (Exception e) {
                    sender.sendMessage("§c§l[CentralCart] §cErro ao criar NPCs: " + e.getMessage());
                    plugin.getLogger().severe("Erro ao criar NPCs: " + e.getMessage());
                }
            });
        }).exceptionally(throwable -> {
            sender.sendMessage("§c§l[CentralCart] §cErro ao buscar dados: " + throwable.getMessage());
            return null;
        });

        return true;
    }
}

