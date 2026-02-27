package plugin.centralCartTopPlugin.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import plugin.centralCartTopPlugin.CentralCartTopPlugin;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class ScheduleInfoCommand implements CommandExecutor {

    private final CentralCartTopPlugin plugin;

    public ScheduleInfoCommand(CentralCartTopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("centralcart.admin")) {
            sender.sendMessage("§c§l[CentralCart] §cVocê não tem permissão para usar este comando.");
            return true;
        }

        boolean autoUpdateEnabled = plugin.getConfig().getBoolean("npcs.auto_update_enabled", true);
        String lastUpdate = plugin.getConfig().getString("npcs.last_auto_update", "Nunca");

        sender.sendMessage("§6§l========================================");
        sender.sendMessage("§e§l   INFORMAÇÕES DE ATUALIZAÇÃO MENSAL");
        sender.sendMessage("§6§l========================================");
        sender.sendMessage("");

        // Status
        if (autoUpdateEnabled) {
            sender.sendMessage("§a§l✓ Status: §aAtivado");
        } else {
            sender.sendMessage("§c§l✗ Status: §cDesativado");
        }

        // Última atualização
        sender.sendMessage("§e§l📅 Última atualização: §f" + lastUpdate);

        // Próxima atualização
        if (autoUpdateEnabled) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime nextFirst = calculateNextFirstDay(now);

            long daysUntil = ChronoUnit.DAYS.between(now.toLocalDate(), nextFirst.toLocalDate());
            long hoursUntil = ChronoUnit.HOURS.between(now, nextFirst);
            long minutesUntil = ChronoUnit.MINUTES.between(now, nextFirst);

            sender.sendMessage("§e§l⏰ Próxima atualização: §f" + nextFirst.toLocalDate() + " às 00:00h");

            if (daysUntil > 0) {
                sender.sendMessage("§e§l⌛ Tempo restante: §f" + daysUntil + " dia(s) e " + (hoursUntil % 24) + " hora(s)");
            } else if (hoursUntil > 0) {
                sender.sendMessage("§e§l⌛ Tempo restante: §f" + hoursUntil + " hora(s) e " + (minutesUntil % 60) + " minuto(s)");
            } else {
                sender.sendMessage("§e§l⌛ Tempo restante: §f" + minutesUntil + " minuto(s)");
            }
        } else {
            sender.sendMessage("§c§l⏰ Próxima atualização: §cDesativada");
        }

        sender.sendMessage("");

        // Configurações
        sender.sendMessage("§e§l⚙ Configurações:");
        sender.sendMessage("§f  • NPCs habilitados: " + (plugin.getConfig().getBoolean("npcs.enabled", true) ? "§aSim" : "§cNão"));
        sender.sendMessage("§f  • Citizens detectado: " + (plugin.getNpcManager().isCitizensEnabled() ? "§aSim" : "§cNão"));
        sender.sendMessage("§f  • Timeout da API: §f" + plugin.getConfig().getInt("api.timeout", 15000) + "ms");
        sender.sendMessage("§f  • Tentativas de retry: §f" + plugin.getConfig().getInt("api.retry_attempts", 3));

        sender.sendMessage("");
        sender.sendMessage("§6§l========================================");
        sender.sendMessage("§7Use §e/testschedule §7para testar a atualização agora");
        sender.sendMessage("§6§l========================================");

        return true;
    }

    private LocalDateTime calculateNextFirstDay(LocalDateTime now) {
        LocalDateTime nextFirst = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

        // Se já passou o dia 1º deste mês, vai para o próximo mês
        if (now.getDayOfMonth() > 1 || (now.getDayOfMonth() == 1 && now.getHour() > 0)) {
            nextFirst = nextFirst.plusMonths(1);
        }

        return nextFirst;
    }
}

