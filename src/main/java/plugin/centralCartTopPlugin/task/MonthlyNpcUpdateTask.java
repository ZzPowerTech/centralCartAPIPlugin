package plugin.centralCartTopPlugin.task;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import plugin.centralCartTopPlugin.CentralCartTopPlugin;
import plugin.centralCartTopPlugin.model.TopCustomer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class MonthlyNpcUpdateTask extends BukkitRunnable {

    private final CentralCartTopPlugin plugin;
    private LocalDate lastUpdate;

    public MonthlyNpcUpdateTask(CentralCartTopPlugin plugin) {
        this.plugin = plugin;
        this.lastUpdate = LocalDate.now();
    }

    @Override
    public void run() {
        LocalDate today = LocalDate.now();

        // Verifica se é dia 1º do mês e ainda não atualizou hoje
        if (today.getDayOfMonth() == 1 && !today.equals(lastUpdate)) {
            plugin.getLogger().info("§e[CentralCart] Dia 1º do mês detectado! Atualizando NPCs automaticamente...");

            // Atualiza os NPCs automaticamente
            updateNPCs();

            // Marca como atualizado
            lastUpdate = today;

            // Salva a data da última atualização no config
            plugin.getConfig().set("npcs.last_auto_update", today.toString());
            plugin.saveConfig();
        }
    }

    /**
     * Atualiza os NPCs com os top doadores do mês anterior
     */
    private void updateNPCs() {
        if (!plugin.getNpcManager().isCitizensEnabled()) {
            plugin.getLogger().warning("§c[CentralCart] Citizens não está disponível. Atualização automática cancelada.");
            return;
        }

        if (!plugin.getConfig().getBoolean("npcs.auto_update_enabled", true)) {
            plugin.getLogger().info("§e[CentralCart] Atualização automática desabilitada no config.");
            return;
        }

        // Busca os top doadores de forma assíncrona
        plugin.getApiService().getTop3DonatorsPreviousMonth().thenAccept(top3 -> {
            if (top3.isEmpty()) {
                plugin.getLogger().warning("§c[CentralCart] Não foi possível buscar top doadores para atualização automática.");
                return;
            }

            // Executa na thread principal (sincronamente)
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    plugin.getNpcManager().createOrUpdateNPCs(top3);
                    plugin.saveConfig();

                    plugin.getLogger().info("§a[CentralCart] NPCs atualizados automaticamente com sucesso!");
                    plugin.getLogger().info("§a[CentralCart] Novos top doadores:");

                    for (TopCustomer customer : top3) {
                        plugin.getLogger().info("§a  " + customer.getPosition() + "º - " + customer.getName() + " (R$ " + customer.getTotal() + ")");
                    }

                    // Notifica admins online
                    notifyAdmins(top3);

                } catch (Exception e) {
                    plugin.getLogger().severe("§c[CentralCart] Erro na atualização automática de NPCs: " + e.getMessage());
                }
            });
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("§c[CentralCart] Erro ao buscar dados para atualização automática: " + throwable.getMessage());
            return null;
        });
    }

    /**
     * Notifica administradores online sobre a atualização
     */
    private void notifyAdmins(List<TopCustomer> top3) {
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (player.hasPermission("centralcart.admin")) {
                player.sendMessage("§6§l========================================");
                player.sendMessage("§e§l   ATUALIZAÇÃO AUTOMÁTICA DE NPCs");
                player.sendMessage("§6§l========================================");
                player.sendMessage("§a§lℹ §aOs NPCs dos top doadores foram atualizados automaticamente!");
                player.sendMessage("");

                for (TopCustomer customer : top3) {
                    String medal = customer.getPosition() == 1 ? "§6🥇" :
                                  customer.getPosition() == 2 ? "§7🥈" : "§c🥉";
                    player.sendMessage(String.format("§f%s §6#%d §f- §e%s §7(§aR$ %.2f§7)",
                            medal,
                            customer.getPosition(),
                            customer.getName(),
                            customer.getTotal()));
                }

                player.sendMessage("§6§l========================================");
            }
        });
    }

    /**
     * Calcula o tempo até o próximo dia 1º
     */
    public static long getTicksUntilNextFirstDay() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextFirst = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

        // Se já passou o dia 1º deste mês, vai para o próximo mês
        if (now.getDayOfMonth() > 1 || (now.getDayOfMonth() == 1 && now.getHour() > 0)) {
            nextFirst = nextFirst.plusMonths(1);
        }

        long minutesUntil = ChronoUnit.MINUTES.between(now, nextFirst);
        return minutesUntil * 60 * 20; // Converte para ticks (20 ticks = 1 segundo)
    }
}

