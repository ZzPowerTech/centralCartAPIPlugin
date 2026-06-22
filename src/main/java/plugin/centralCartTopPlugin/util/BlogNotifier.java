package plugin.centralCartTopPlugin.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import plugin.centralCartTopPlugin.model.BlogPost;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Centraliza a formatação e o broadcast de notificações de novos posts do blog.
 *
 * <p>Garante que a task automática ({@code BlogPostCheckTask}) e o comando manual
 * ({@code /testblogpost}) produzam exatamente a mesma mensagem, evitando divergência.
 */
public final class BlogNotifier {

    private BlogNotifier() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Monta o mapa de placeholders ({@code title}, {@code url}, {@code time}, {@code date})
     * a partir de um post.
     */
    public static Map<String, String> buildPlaceholders(BlogPost post) {
        Map<String, String> map = new HashMap<>();
        map.put("title", post.getTitle() != null ? post.getTitle() : "");
        map.put("url", post.getUrl() != null ? post.getUrl() : "");

        String time = "";
        String date = "";

        String createdAt = post.getCreatedAt();
        if (createdAt != null && !createdAt.isEmpty()) {
            LocalDateTime dt = DateTimeUtil.tryParse(createdAt);
            if (dt != null) {
                time = dt.format(DateTimeUtil.TIME_FORMAT);
                date = dt.format(DateTimeUtil.DATE_FORMAT);
            } else {
                // Não conseguimos parsear: expõe o valor bruto em vez de string vazia
                time = createdAt;
                date = createdAt;
            }
        }

        map.put("time", time);
        map.put("date", date);
        return map;
    }

    /**
     * Dispara o broadcast da notificação do post para todo o servidor.
     *
     * <p>Deve ser chamado na thread principal. Usa {@code blog.notification.lines} do config
     * e, se essa lista estiver ausente/vazia, recorre a {@link Constants#DEFAULT_BLOG_NOTIFICATION_LINES}
     * para nunca enviar uma notificação em branco.
     *
     * @return a quantidade de linhas efetivamente enviadas
     */
    public static int broadcast(Plugin plugin, BlogPost post) {
        Map<String, String> placeholders = buildPlaceholders(post);

        List<String> lines = plugin.getConfig().getStringList("blog.notification.lines");
        if (lines.isEmpty()) {
            lines = Constants.DEFAULT_BLOG_NOTIFICATION_LINES;
            plugin.getLogger().warning("[Blog] 'blog.notification.lines' ausente no config — usando layout padrão.");
        }

        for (String line : lines) {
            Component component = MessageFormatter.parse(line, placeholders);
            Bukkit.getServer().broadcast(component);
        }
        return lines.size();
    }
}
