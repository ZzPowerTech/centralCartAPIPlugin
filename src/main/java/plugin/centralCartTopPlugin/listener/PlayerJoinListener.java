package plugin.centralCartTopPlugin.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import plugin.centralCartTopPlugin.CentralCartTopPlugin;

public class PlayerJoinListener implements Listener {

    private final CentralCartTopPlugin plugin;

    public PlayerJoinListener(CentralCartTopPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Verifica se há recompensas pendentes para o jogador
        plugin.getRewardsManager().checkPendingRewards(event.getPlayer());
    }
}

