package plugin.centralCartTopPlugin.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import plugin.centralCartTopPlugin.CentralCartTopPlugin;
import plugin.centralCartTopPlugin.manager.MessagesManager;
import plugin.centralCartTopPlugin.model.TopCustomer;

import java.util.HashMap;
import java.util.Map;

public class TopDonadoresCommand implements CommandExecutor {

    private final CentralCartTopPlugin plugin;
    private final MessagesManager messages;

    public TopDonadoresCommand(CentralCartTopPlugin plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessagesManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        sender.sendMessage(messages.getMessageWithPrefix("top_donators.loading"));

        plugin.getApiService().getTop3DonatorsPreviousMonth().thenAccept(top3 -> {
            if (top3.isEmpty()) {
                sender.sendMessage(messages.getMessageWithPrefix("top_donators.error"));
                return;
            }

            sender.sendMessage(messages.getMessage("top_donators.header"));
            sender.sendMessage(messages.getMessage("top_donators.title"));
            sender.sendMessage(messages.getMessage("top_donators.header"));
            sender.sendMessage("");

            // Configurações de exibição
            boolean showTotal = plugin.getConfig().getBoolean("display.show-total", true);
            String currencySymbol = plugin.getConfig().getString("display.currency-symbol", "R$");

            for (TopCustomer customer : top3) {
                String medal = getMedal(customer.getPosition());

                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("medal", medal);
                placeholders.put("position", String.valueOf(customer.getPosition()));
                placeholders.put("player", customer.getName());
                placeholders.put("currency", currencySymbol);
                placeholders.put("total", String.format("%.2f", customer.getTotal()));

                String format = showTotal ? "top_donators.format_with_total" : "top_donators.format_without_total";
                sender.sendMessage(messages.getMessage(format, placeholders));
            }

            sender.sendMessage("");
            sender.sendMessage(messages.getMessage("top_donators.footer"));

        }).exceptionally(throwable -> {
            Map<String, String> errorPlaceholders = new HashMap<>();
            errorPlaceholders.put("message", throwable.getMessage());
            sender.sendMessage(messages.getMessageWithPrefix("top_donators.error_with_message", errorPlaceholders));
            return null;
        });

        return true;
    }

    private String getMedal(int position) {
        switch (position) {
            case 1:
                return messages.getMessage("top_donators.medals.first");
            case 2:
                return messages.getMessage("top_donators.medals.second");
            case 3:
                return messages.getMessage("top_donators.medals.third");
            default:
                return "§f";
        }
    }
}

