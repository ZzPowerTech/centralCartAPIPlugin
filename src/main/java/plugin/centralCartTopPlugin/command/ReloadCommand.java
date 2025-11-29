package plugin.centralCartTopPlugin.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import plugin.centralCartTopPlugin.CentralCartTopPlugin;

public class ReloadCommand implements CommandExecutor {

    private final CentralCartTopPlugin plugin;

    public ReloadCommand(CentralCartTopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("centralcart.admin")) {
            sender.sendMessage("§c§l[CentralCart] §cVocê não tem permissão para usar este comando.");
            return true;
        }

        sender.sendMessage("§e§l[CentralCart] §eRecarregando configurações...");

        try {
            // Recarrega o config.yml
            plugin.reloadConfig();

            // Reinicializa os serviços com a nova configuração
            plugin.reloadServices();

            sender.sendMessage("§a§l[CentralCart] §aConfiguração recarregada com sucesso!");
            sender.sendMessage("§a§l[CentralCart] §aToken: " + (plugin.getConfig().getString("api.token", "").equals("COLOQUE_SEU_TOKEN_AQUI") ? "§c[NÃO CONFIGURADO]" : "§a[CONFIGURADO]"));
            sender.sendMessage("§a§l[CentralCart] §aNPCs: " + (plugin.getConfig().getBoolean("npcs.enabled", true) ? "§a[ATIVADO]" : "§c[DESATIVADO]"));
            sender.sendMessage("§a§l[CentralCart] §aCitizens: " + (plugin.getNpcManager().isCitizensEnabled() ? "§a[DETECTADO]" : "§c[NÃO ENCONTRADO]"));

        } catch (Exception e) {
            sender.sendMessage("§c§l[CentralCart] §cErro ao recarregar: " + e.getMessage());
            plugin.getLogger().severe("Erro ao recarregar plugin: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }
}

