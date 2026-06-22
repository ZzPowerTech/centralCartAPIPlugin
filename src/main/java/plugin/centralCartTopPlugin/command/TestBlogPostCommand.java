package plugin.centralCartTopPlugin.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import plugin.centralCartTopPlugin.CentralCartTopPlugin;
import plugin.centralCartTopPlugin.model.BlogPost;
import plugin.centralCartTopPlugin.util.BlogNotifier;
import plugin.centralCartTopPlugin.util.Constants;

import java.util.logging.Level;

/**
 * Testa o broadcast de novo post do blog usando o MESMO caminho da task automática
 * (via {@link BlogNotifier}). Não altera {@code last_seen_post_id}.
 */
public class TestBlogPostCommand implements CommandExecutor {

    private final CentralCartTopPlugin plugin;

    public TestBlogPostCommand(CentralCartTopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission(Constants.PERMISSION_ADMIN)) {
            sender.sendMessage("§c§l[CentralCart] §cVocê não tem permissão para usar este comando.");
            return true;
        }

        if (!plugin.getBlogPostService().isConfigured()) {
            sender.sendMessage("§c§l[Blog] §capi.store_domain não está configurado no config.yml.");
            sender.sendMessage("§7Configure §fapi.store_domain: \"loja.austv.net\" §7e use §f/centralcartreload§7.");
            return true;
        }

        sender.sendMessage("§e§l[Blog] §eBuscando último post na API...");

        plugin.getBlogPostService().getLatestPost().thenAccept(optPost -> {
            if (optPost.isEmpty()) {
                Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage("§c§l[Blog] §cNenhum post retornado pela API. Verifique os logs.")
                );
                return;
            }

            BlogPost post = optPost.get();

            if (post.getId() == null || post.getId().isEmpty()) {
                Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage("§c§l[Blog] §cPost retornado sem ID — não é possível processar.")
                );
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage("§a§l[Blog] §aPost obtido com sucesso!");
                sender.sendMessage("§7ID: §f" + post.getId());
                sender.sendMessage("§7Título: §f" + post.getTitle());
                sender.sendMessage("§7URL: §f" + post.getUrl());
                sender.sendMessage("§7Data: §f" + post.getCreatedAt());
                sender.sendMessage("§e§l[Blog] §eDisparando broadcast...");

                int sent = BlogNotifier.broadcast(plugin, post);

                sender.sendMessage("§a§l[Blog] §aBroadcast enviado (" + sent
                        + " linha(s))! §7(last_seen_post_id NÃO foi alterado)");
            });

        }).exceptionally(throwable -> {
            plugin.getLogger().log(Level.SEVERE, "[Blog] Erro no teste de broadcast", throwable);
            Bukkit.getScheduler().runTask(plugin, () ->
                sender.sendMessage("§c§l[Blog] §cErro ao buscar post: " + throwable.getMessage())
            );
            return null;
        });

        return true;
    }
}
