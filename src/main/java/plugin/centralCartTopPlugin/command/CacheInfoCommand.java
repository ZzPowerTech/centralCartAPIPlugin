package plugin.centralCartTopPlugin.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import plugin.centralCartTopPlugin.CentralCartTopPlugin;

public class CacheInfoCommand implements CommandExecutor {

    private final CentralCartTopPlugin plugin;

    public CacheInfoCommand(CentralCartTopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("centralcart.admin")) {
            sender.sendMessage("§c§l[CentralCart] §cVocê não tem permissão para usar este comando.");
            return true;
        }

        // Verifica argumentos
        if (args.length > 0 && args[0].equalsIgnoreCase("clear")) {
            // Limpa o cache
            plugin.getApiService().invalidateCache();
            sender.sendMessage("§a§l[CentralCart] §aCache limpo com sucesso!");
            sender.sendMessage("§e§l[CentralCart] §eA próxima consulta buscará dados novos da API.");
            return true;
        }

        // Mostra informações do cache
        sender.sendMessage("§6§l========================================");
        sender.sendMessage("§e§l     INFORMAÇÕES DO CACHE");
        sender.sendMessage("§6§l========================================");
        sender.sendMessage("");

        String cacheInfo = plugin.getApiService().getCacheInfo();
        sender.sendMessage("§fStatus: " + cacheInfo);

        sender.sendMessage("");
        sender.sendMessage("§e§lℹ Benefícios do Cache:");
        sender.sendMessage("§a  ✓ §fReduz chamadas à API");
        sender.sendMessage("§a  ✓ §fMelhora tempo de resposta");
        sender.sendMessage("§a  ✓ §fReduz carga no servidor");
        sender.sendMessage("§a  ✓ §fFunciona mesmo com API offline");
        sender.sendMessage("");
        sender.sendMessage("§e§lComandos:");
        sender.sendMessage("§f  /cacheinfo §7- Ver status do cache");
        sender.sendMessage("§f  /cacheinfo clear §7- Limpar cache");
        sender.sendMessage("§6§l========================================");

        return true;
    }
}

