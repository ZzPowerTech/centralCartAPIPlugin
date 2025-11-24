package plugin.centralCartTopPlugin.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import plugin.centralCartTopPlugin.model.TopCustomer;
import plugin.centralCartTopPlugin.service.CentralCartApiService;

import java.util.List;

public class TopDonadoresCommand implements CommandExecutor {

    private final CentralCartApiService apiService;

    public TopDonadoresCommand(CentralCartApiService apiService) {
        this.apiService = apiService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        sender.sendMessage("§e§l[CentralCart] §aBuscando top doadores do mês anterior...");

        apiService.getTop3DonatorsPreviousMonth().thenAccept(top3 -> {
            if (top3.isEmpty()) {
                sender.sendMessage("§c§l[CentralCart] §cNão foi possível buscar os dados. Verifique os logs.");
                return;
            }

            sender.sendMessage("§6§l========================================");
            sender.sendMessage("§e§l      TOP 3 DOADORES DO MÊS ANTERIOR");
            sender.sendMessage("§6§l========================================");
            sender.sendMessage("");

            for (TopCustomer customer : top3) {
                String medal = getMedal(customer.getPosition());
                sender.sendMessage(String.format("§f%s §6#%d §f- §e%s §7(R$ %.2f)",
                        medal,
                        customer.getPosition(),
                        customer.getName(),
                        customer.getTotal()));
            }

            sender.sendMessage("");
            sender.sendMessage("§6§l========================================");

        }).exceptionally(throwable -> {
            sender.sendMessage("§c§l[CentralCart] §cErro ao buscar dados: " + throwable.getMessage());
            throwable.printStackTrace();
            return null;
        });

        return true;
    }

    private String getMedal(int position) {
        switch (position) {
            case 1:
                return "§6🥇";
            case 2:
                return "§7🥈";
            case 3:
                return "§c🥉";
            default:
                return "§f";
        }
    }
}

