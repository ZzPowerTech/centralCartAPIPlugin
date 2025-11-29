package plugin.centralCartTopPlugin.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import plugin.centralCartTopPlugin.CentralCartTopPlugin;
import plugin.centralCartTopPlugin.model.TopCustomer;

public class TopDonadoresCommand implements CommandExecutor {

    private final CentralCartTopPlugin plugin;

    public TopDonadoresCommand(CentralCartTopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Mensagem de carregamento configurável (sempre busca da config atual)
        String loadingMsg = plugin.getConfig().getString("messages.loading", "§e§l[CentralCart] §aBuscando top doadores...");
        sender.sendMessage(loadingMsg);

        plugin.getApiService().getTop3DonatorsPreviousMonth().thenAccept(top3 -> {
            if (top3.isEmpty()) {
                String errorMsg = plugin.getConfig().getString("messages.error", "§c§l[CentralCart] §cNão foi possível buscar os dados.");
                sender.sendMessage(errorMsg);
                return;
            }

            // Mensagens configuráveis do header
            String header = plugin.getConfig().getString("messages.header", "§6§l========================================");
            String title = plugin.getConfig().getString("messages.title", "§e§l      TOP 3 DOADORES DO MÊS ANTERIOR");
            String footer = plugin.getConfig().getString("messages.footer", "§6§l========================================");

            sender.sendMessage(header);
            sender.sendMessage(title);
            sender.sendMessage(header);
            sender.sendMessage("");

            // Configurações de exibição
            boolean showTotal = plugin.getConfig().getBoolean("display.show-total", true);
            String currencySymbol = plugin.getConfig().getString("display.currency-symbol", "R$");

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
            String errorMsg = plugin.getConfig().getString("messages.error", "§c§l[CentralCart] §cErro ao buscar dados.");
            sender.sendMessage(errorMsg + " " + throwable.getMessage());
            return null;
        });

        return true;
    }

    private String getMedal(int position) {
        String medal;
        switch (position) {
            case 1:
                medal = plugin.getConfig().getString("medals.first", "§6🥇");
                break;
            case 2:
                medal = plugin.getConfig().getString("medals.second", "§7🥈");
                break;
            case 3:
                medal = plugin.getConfig().getString("medals.third", "§c🥉");
                break;
            default:
                medal = "§f";
        }
        return medal;
    }
}

