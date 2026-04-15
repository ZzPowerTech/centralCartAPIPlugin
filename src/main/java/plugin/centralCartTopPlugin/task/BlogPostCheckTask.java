package plugin.centralCartTopPlugin.task;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import plugin.centralCartTopPlugin.CentralCartTopPlugin;
import plugin.centralCartTopPlugin.model.BlogPost;
import plugin.centralCartTopPlugin.util.MessageFormatter;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class BlogPostCheckTask extends BukkitRunnable {

    private static final DateTimeFormatter[] PARSERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
    };

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CentralCartTopPlugin plugin;

    public BlogPostCheckTask(CentralCartTopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        plugin.getBlogPostService().getLatestPost().thenAccept(optPost -> {
            if (optPost.isEmpty()) {
                plugin.getLogger().warning("[Blog] Nenhum post retornado pela API.");
                return;
            }

            BlogPost post = optPost.get();

            if (post.getId() == null || post.getId().isEmpty()) {
                plugin.getLogger().warning("[Blog] Post sem ID recebido — ignorando.");
                return;
            }

            String lastSeenId = plugin.getConfig().getString("blog.last_seen_post_id", "");

            if (post.getId().equals(lastSeenId)) {
                plugin.getLogger().log(Level.FINE, "[Blog] Nenhum post novo (ID {0} já visto).", post.getId());
                return;
            }

            plugin.getLogger().info("[Blog] Novo post detectado: " + post.getTitle());

            Map<String, String> placeholders = buildPlaceholders(post);

            // Agenda broadcast na thread principal
            Bukkit.getScheduler().runTask(plugin, () -> {
                List<String> lines = plugin.getConfig().getStringList("blog.notification.lines");
                for (String line : lines) {
                    Component component = MessageFormatter.parse(line, placeholders);
                    Bukkit.getServer().broadcast(component);
                }

                plugin.getConfig().set("blog.last_seen_post_id", post.getId());
                plugin.saveConfig();

                plugin.getLogger().info("[Blog] Broadcast realizado e last_seen_post_id atualizado para " + post.getId());
            });

        }).exceptionally(throwable -> {
            plugin.getLogger().log(Level.SEVERE, "[Blog] Erro ao verificar novos posts", throwable);
            return null;
        });
    }

    private Map<String, String> buildPlaceholders(BlogPost post) {
        Map<String, String> map = new HashMap<>();
        map.put("title", post.getTitle() != null ? post.getTitle() : "");
        map.put("url", post.getUrl() != null ? post.getUrl() : "");

        String time = "";
        String date = "";

        if (post.getCreatedAt() != null && !post.getCreatedAt().isEmpty()) {
            LocalDateTime dt = tryParseDateTime(post.getCreatedAt());
            if (dt != null) {
                time = dt.format(TIME_FORMAT);
                date = dt.format(DATE_FORMAT);
            } else {
                time = post.getCreatedAt();
                date = post.getCreatedAt();
            }
        }

        map.put("time", time);
        map.put("date", date);
        return map;
    }

    private LocalDateTime tryParseDateTime(String raw) {
        // Tenta primeiro como OffsetDateTime (ex: "2024-03-03T23:43:12.000-03:00")
        try {
            return OffsetDateTime.parse(raw, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            // segue para os formatos legados
        }

        for (DateTimeFormatter fmt : PARSERS) {
            try {
                return LocalDateTime.parse(raw, fmt);
            } catch (DateTimeParseException ignored) {
                // tenta próximo formato
            }
        }
        return null;
    }
}
