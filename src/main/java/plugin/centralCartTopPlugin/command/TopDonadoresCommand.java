package plugin.centralCartTopPlugin.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import plugin.centralCartTopPlugin.model.TopCustomer;
import plugin.centralCartTopPlugin.service.CentralCartApiService;

public class TopDonadoresCommand implements CommandExecutor {

    private final CentralCartApiService apiService;
    private final FileConfiguration config;

    public TopDonadoresCommand(CentralCartApiService apiService, FileConfiguration config) {
        this.apiService = apiService;
        this.config = config;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Mensagem de carregamento configurável
        String loadingMsg = config.getString("messages.loading", "§e§l[CentralCart] §aBuscando top doadores...");
        sender.sendMessage(loadingMsg);

        apiService.getTop3DonatorsPreviousMonth().thenAccept(top3 -> {
            if (top3.isEmpty()) {
                String errorMsg = config.getString("messages.error", "§c§l[CentralCart] §cNão foi possível buscar os dados.");
                sender.sendMessage(errorMsg);
                return;
            }

            // Mensagens configuráveis do header
            String header = config.getString("messages.header", "§6§l========================================");
            String title = config.getString("messages.title", "§e§l      TOP 3 DOADORES DO MÊS ANTERIOR");
            String footer = config.getString("messages.footer", "§6§l========================================");

            sender.sendMessage(header);
            sender.sendMessage(title);
            sender.sendMessage(header);
            sender.sendMessage("");

            // Configurações de exibição
            boolean showTotal = config.getBoolean("display.show-total", true);
            String currencySymbol = config.getString("display.currency-symbol", "R$");

            for (TopCustomer customer : top3) {
                String medal = getMedal(customer.getPosition());

                if (showTotal) {
                    sender.sendMessage(String.format("§f%s §6#%d §f- §e%s §7(%s %.2f)",
                            medal,
                            customer.getPosition(),
                            customer.getName(),
                            currencySymbol,
                            customer.getTotal()));
                } else {
                    sender.sendMessage(String.format("§f%s §6#%d §f- §e%s",
                            medal,
                            customer.getPosition(),
                            customer.getName()));
                }
            }

            sender.sendMessage("");
            sender.sendMessage(footer);

        }).exceptionally(throwable -> {
            String errorMsg = config.getString("messages.error", "§c§l[CentralCart] §cErro ao buscar dados.");
            sender.sendMessage(errorMsg + " " + throwable.getMessage());
            return null;
        });

        return true;
    }

    private String getMedal(int position) {
        String medal;
        switch (position) {
            case 1:
                medal = config.getString("medals.first", "§6🥇");
                break;
            case 2:
                medal = config.getString("medals.second", "§7🥈");
                break;
            case 3:
                medal = config.getString("medals.third", "§c🥉");
                break;
            default:
                medal = "§f";
        }
        return medal;
    }
}

