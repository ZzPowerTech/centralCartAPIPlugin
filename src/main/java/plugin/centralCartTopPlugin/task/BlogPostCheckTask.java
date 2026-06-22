package plugin.centralCartTopPlugin.task;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import plugin.centralCartTopPlugin.CentralCartTopPlugin;
import plugin.centralCartTopPlugin.model.BlogPost;
import plugin.centralCartTopPlugin.util.BlogNotifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * Verifica periodicamente novos posts no blog e dispara o broadcast.
 *
 * <p>Estratégia de detecção:
 * <ul>
 *   <li><b>Seeding</b>: na primeira execução (sem {@code last_seen_post_id}), apenas marca o post
 *       mais recente como visto, sem anunciar — evita spammar posts antigos ao subir o servidor.</li>
 *   <li><b>Múltiplos posts</b>: anuncia todos os posts mais novos que o último visto (do mais antigo
 *       para o mais recente), e não apenas o primeiro da lista.</li>
 * </ul>
 */
public class BlogPostCheckTask extends BukkitRunnable {

    private final CentralCartTopPlugin plugin;

    public BlogPostCheckTask(CentralCartTopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        plugin.getBlogPostService().getRecentPosts().thenAccept(posts -> {
            List<BlogPost> valid = new ArrayList<>();
            for (BlogPost post : posts) {
                if (post.getId() != null && !post.getId().isEmpty()) {
                    valid.add(post);
                }
            }

            if (valid.isEmpty()) {
                plugin.getLogger().log(Level.FINE, "[Blog] Nenhum post válido retornado pela API.");
                return;
            }

            BlogPost newest = valid.get(0);
            String lastSeen = plugin.getConfig().getString("blog.last_seen_post_id", "");

            // Primeira execução / estado limpo: faz seeding sem anunciar.
            if (lastSeen.isEmpty()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getConfig().set("blog.last_seen_post_id", newest.getId());
                    plugin.saveConfig();
                    plugin.getLogger().info("[Blog] Inicialização: post mais recente (ID " + newest.getId()
                            + ") marcado como visto. Próximos posts serão anunciados.");
                });
                return;
            }

            // Coleta os posts mais novos que o último visto. A API entrega em ordem decrescente,
            // então paramos assim que encontramos um post que não é mais novo.
            List<BlogPost> novos = new ArrayList<>();
            for (BlogPost post : valid) {
                if (isNewer(post.getId(), lastSeen)) {
                    novos.add(post);
                } else {
                    break;
                }
            }

            if (novos.isEmpty()) {
                plugin.getLogger().log(Level.FINE, "[Blog] Nenhum post novo (último visto: {0}).", lastSeen);
                return;
            }

            // Anuncia do mais antigo para o mais recente, para manter a ordem cronológica no chat.
            Collections.reverse(novos);

            Bukkit.getScheduler().runTask(plugin, () -> {
                for (BlogPost post : novos) {
                    plugin.getLogger().info("[Blog] Novo post detectado: " + post.getTitle());
                    BlogNotifier.broadcast(plugin, post);
                }
                plugin.getConfig().set("blog.last_seen_post_id", newest.getId());
                plugin.saveConfig();
                plugin.getLogger().info("[Blog] " + novos.size() + " post(s) anunciado(s). last_seen_post_id = "
                        + newest.getId());
            });

        }).exceptionally(throwable -> {
            plugin.getLogger().log(Level.SEVERE, "[Blog] Erro ao verificar novos posts", throwable);
            return null;
        });
    }

    /**
     * Compara IDs de post. IDs são numéricos (auto-increment) na API; usa comparação numérica
     * e degrada para comparação textual de igualdade caso não sejam parseáveis.
     */
    private boolean isNewer(String candidateId, String lastSeenId) {
        try {
            return Long.parseLong(candidateId) > Long.parseLong(lastSeenId);
        } catch (NumberFormatException e) {
            return !candidateId.equals(lastSeenId);
        }
    }
}
