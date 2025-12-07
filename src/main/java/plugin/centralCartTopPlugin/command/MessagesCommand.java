package plugin.centralCartTopPlugin.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import plugin.centralCartTopPlugin.CentralCartTopPlugin;
import plugin.centralCartTopPlugin.manager.MessagesManager;

import java.io.File;

public class MessagesCommand implements CommandExecutor {

    private final CentralCartTopPlugin plugin;
    private final MessagesManager messages;

    public MessagesCommand(CentralCartTopPlugin plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessagesManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("centralcart.admin")) {
            sender.sendMessage(messages.getMessage("general.no_permission"));
            return true;
        }

        // Subcomando: reload
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            messages.reload();
            sender.sendMessage("§a§l[CentralCart] §aMensagens recarregadas com sucesso!");
            sender.sendMessage("§a§l[CentralCart] §aCache limpo: " + messages.getCacheSize() + " mensagens");
            return true;
        }

        // Subcomando: clear
        if (args.length > 0 && args[0].equalsIgnoreCase("clear")) {
            int oldSize = messages.getCacheSize();
            messages.clearCache();
            sender.sendMessage("§a§l[CentralCart] §aCache de mensagens limpo!");
            sender.sendMessage("§a§l[CentralCart] §a" + oldSize + " mensagens removidas do cache");
            return true;
        }

        // Informações gerais
        sender.sendMessage("§6§l========================================");
        sender.sendMessage("§e§l   SISTEMA DE MENSAGENS");
        sender.sendMessage("§6§l========================================");
        sender.sendMessage("");

        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        sender.sendMessage("§fArquivo: §e" + messagesFile.getName());
        sender.sendMessage("§fExiste: " + (messagesFile.exists() ? "§a✓ Sim" : "§c✗ Não"));

        if (messagesFile.exists()) {
            long size = messagesFile.length();
            String sizeStr = size < 1024 ? size + " bytes" :
                            size < 1024*1024 ? (size/1024) + " KB" :
                            (size/(1024*1024)) + " MB";
            sender.sendMessage("§fTamanho: §e" + sizeStr);
        }

        sender.sendMessage("§fCache: §e" + messages.getCacheSize() + " mensagens");
        sender.sendMessage("");

        sender.sendMessage("§e§lℹ Funcionalidades:");
        sender.sendMessage("§a  ✓ §fMensagens personalizáveis");
        sender.sendMessage("§a  ✓ §fSuporte a placeholders");
        sender.sendMessage("§a  ✓ §fReload sem reiniciar servidor");
        sender.sendMessage("§a  ✓ §fSistema de cache para performance");
        sender.sendMessage("§a  ✓ §fCores com código & ou §");
        sender.sendMessage("");

        sender.sendMessage("§e§lComandos:");
        sender.sendMessage("§f  /messages §7- Ver informações");
        sender.sendMessage("§f  /messages reload §7- Recarregar messages.yml");
        sender.sendMessage("§f  /messages clear §7- Limpar cache");
        sender.sendMessage("");

        sender.sendMessage("§e§lArquivo de Edição:");
        sender.sendMessage("§f  plugins/centralCartTopPlugin/messages.yml");
        sender.sendMessage("");

        sender.sendMessage("§e§lExemplo de Uso no messages.yml:");
        sender.sendMessage("§7  general:");
        sender.sendMessage("§7    prefix: \"&6&l[CentralCart]\"");
        sender.sendMessage("§7    no_permission: \"&cSem permissão!\"");
        sender.sendMessage("");

        sender.sendMessage("§6§l========================================");

        return true;
    }
}

