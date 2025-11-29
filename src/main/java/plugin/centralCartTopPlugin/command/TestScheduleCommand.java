package plugin.centralCartTopPlugin.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import plugin.centralCartTopPlugin.CentralCartTopPlugin;
import plugin.centralCartTopPlugin.model.TopCustomer;

public class TestScheduleCommand implements CommandExecutor {

    private final CentralCartTopPlugin plugin;

    public TestScheduleCommand(CentralCartTopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("centralcart.admin")) {
            sender.sendMessage("§c§l[CentralCart] §cVocê não tem permissão para usar este comando.");
            return true;
        }

        if (!plugin.getNpcManager().isCitizensEnabled()) {
            sender.sendMessage("§c§l[CentralCart] §cO plugin Citizens não está instalado ou habilitado!");
            return true;
        }

        sender.sendMessage("§e§l[CentralCart] §eTestando atualização automática mensal...");
        sender.sendMessage("§e§l[CentralCart] §eSimulando que hoje é dia 1º do mês...");

        // Simula a atualização automática
        plugin.getApiService().getTop3DonatorsPreviousMonth().thenAccept(top3 -> {
            if (top3.isEmpty()) {
                sender.sendMessage("§c§l[CentralCart] §cNão foi possível buscar os dados dos top doadores.");
                plugin.getLogger().warning("§c[CentralCart] Falha no teste de atualização automática.");
                return;
            }

            // Executa na thread principal (sincronamente)
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    plugin.getNpcManager().createOrUpdateNPCs(top3);
                    plugin.saveConfig();

                    sender.sendMessage("§a§l[CentralCart] §a✓ Teste de atualização automática bem-sucedido!");
                    sender.sendMessage("§6§l========================================");
                    sender.sendMessage("§e§l   SIMULAÇÃO DE ATUALIZAÇÃO MENSAL");
                    sender.sendMessage("§6§l========================================");
                    sender.sendMessage("§a§lℹ §aOs NPCs foram atualizados como se fosse dia 1º do mês!");
                    sender.sendMessage("");

                    for (TopCustomer customer : top3) {
                        String medal = customer.getPosition() == 1 ? "§6🥇" :
                                      customer.getPosition() == 2 ? "§7🥈" : "§c🥉";
                        sender.sendMessage(String.format("§f%s §6#%d §f- §e%s §7(§aR$ %.2f§7)",
                                medal,
                                customer.getPosition(),
                                customer.getName(),
                                customer.getTotal()));
                    }

                    sender.sendMessage("");
                    sender.sendMessage("§6§l========================================");
                    sender.sendMessage("§a§lℹ §aEsta é uma simulação. No servidor real, isso");
                    sender.sendMessage("§a   acontecerá automaticamente todo dia 1º às 00:00h");
                    sender.sendMessage("§6§l========================================");

                    plugin.getLogger().info("§a[CentralCart] Teste de atualização automática executado com sucesso!");

                } catch (Exception e) {
                    sender.sendMessage("§c§l[CentralCart] §cErro ao testar atualização: " + e.getMessage());
                    plugin.getLogger().severe("Erro no teste de atualização automática: " + e.getMessage());
                }
            });
        }).exceptionally(throwable -> {
            sender.sendMessage("§c§l[CentralCart] §cErro ao buscar dados: " + throwable.getMessage());
            return null;
        });

        return true;
    }
}

