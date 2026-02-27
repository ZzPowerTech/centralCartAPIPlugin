package plugin.centralCartTopPlugin.command;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import plugin.centralCartTopPlugin.CentralCartTopPlugin;
import plugin.centralCartTopPlugin.manager.MessagesManager;
import plugin.centralCartTopPlugin.model.TopCustomer;
import plugin.centralCartTopPlugin.util.Constants;
import plugin.centralCartTopPlugin.util.PluginUtils;

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
            String currencySymbol = plugin.getConfig().getString("display.currency-symbol", Constants.DEFAULT_CURRENCY_SYMBOL);

            for (TopCustomer customer : top3) {
                String medal = PluginUtils.getMedal(customer.getPosition(), messages);

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
}

