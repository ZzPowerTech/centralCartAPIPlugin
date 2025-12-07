package plugin.centralCartTopPlugin.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import plugin.centralCartTopPlugin.CentralCartTopPlugin;
import plugin.centralCartTopPlugin.model.TopCustomer;

import java.util.logging.Level;

public class TestRewardsCommand implements CommandExecutor {

    private final CentralCartTopPlugin plugin;

    public TestRewardsCommand(CentralCartTopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("centralcart.admin")) {
            sender.sendMessage("§c§l[CentralCart] §cVocê não tem permissão para usar este comando.");
            return true;
        }

        sender.sendMessage("§e§l[CentralCart] §eIniciando teste de recompensas...");
        sender.sendMessage("§e§l[CentralCart] §eBuscando top 3 doadores do mês anterior...");

        // Busca os top 3 doadores
        plugin.getApiService().getTop3DonatorsPreviousMonth().thenAccept(top3 -> {
            if (top3 == null || top3.isEmpty()) {
                sender.sendMessage("§c§l[CentralCart] §cNão foi possível buscar os dados dos top doadores.");
                sender.sendMessage("§c§l[CentralCart] §cVerifique os logs do servidor para mais detalhes.");
                return;
            }

            // Executa na thread principal
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage("§a§l[CentralCart] §aDados obtidos com sucesso!");
                sender.sendMessage("§6§l========================================");
                sender.sendMessage("§e§l     TESTE DE RECOMPENSAS - TOP 3");
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
                sender.sendMessage("§e§l[CentralCart] §eDistribuindo recompensas...");

                try {
                    // Distribui as recompensas
                    plugin.getRewardsManager().distributeRewards(top3);

                    sender.sendMessage("§a§l[CentralCart] §aRecompensas distribuídas com sucesso!");
                    sender.sendMessage("§a§l[CentralCart] §aVerifique:");
                    sender.sendMessage("§a  - §fBroadcast enviado para todos os jogadores online");
                    sender.sendMessage("§a  - §fRecompensas entregues aos jogadores online");
                    sender.sendMessage("§a  - §fRecompensas salvas para jogadores offline");
                    sender.sendMessage("§e§l[CentralCart] §eArquivo: §fplugins/centralCartTopPlugin/pending_rewards.yml");

                } catch (Exception e) {
                    sender.sendMessage("§c§l[CentralCart] §cErro ao distribuir recompensas: " + e.getMessage());
                    plugin.getLogger().severe("Erro ao testar recompensas: " + e.getMessage());
                    plugin.getLogger().log(Level.SEVERE, "Stack trace:", e);
                }
            });
        }).exceptionally(throwable -> {
            sender.sendMessage("§c§l[CentralCart] §cErro ao buscar dados: " + throwable.getMessage());
            plugin.getLogger().severe("Erro ao buscar top doadores para teste: " + throwable.getMessage());
            return null;
        });

        return true;
    }
}

