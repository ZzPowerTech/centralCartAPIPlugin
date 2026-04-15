package plugin.centralCartTopPlugin.command;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
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

public class TestBlogPostCommand implements CommandExecutor {

    private static final DateTimeFormatter[] PARSERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
    };

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CentralCartTopPlugin plugin;

    public TestBlogPostCommand(CentralCartTopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("centralcart.admin")) {
            sender.sendMessage("§c§l[CentralCart] §cVocê não tem permissão para usar este comando.");
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

            Map<String, String> placeholders = buildPlaceholders(post);

            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage("§a§l[Blog] §aPost obtido com sucesso!");
                sender.sendMessage("§7ID: §f" + post.getId());
                sender.sendMessage("§7Título: §f" + post.getTitle());
                sender.sendMessage("§7URL: §f" + post.getUrl());
                sender.sendMessage("§7Data: §f" + post.getCreatedAt());
                sender.sendMessage("§e§l[Blog] §eDisparando broadcast...");

                List<String> lines = plugin.getConfig().getStringList("blog.notification.lines");
                for (String line : lines) {
                    Component component = MessageFormatter.parse(line, placeholders);
                    Bukkit.getServer().broadcast(component);
                }

                sender.sendMessage("§a§l[Blog] §aBroadcast enviado! (last_seen_post_id NÃO foi alterado)");
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
