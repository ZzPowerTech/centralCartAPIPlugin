package plugin.centralCartTopPlugin.service;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import plugin.centralCartTopPlugin.model.TopCustomer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class TopNpcManager {

    private final Logger logger;
    private final FileConfiguration config;
    private final Map<Integer, Integer> npcIds; // posição -> NPC ID

    public TopNpcManager(Logger logger, FileConfiguration config) {
        this.logger = logger;
        this.config = config;
        this.npcIds = new HashMap<>();
    }

    /**
     * Verifica se o Citizens está disponível
     */
    public boolean isCitizensEnabled() {
        return Bukkit.getPluginManager().getPlugin("Citizens") != null &&
               Bukkit.getPluginManager().isPluginEnabled("Citizens");
    }

    /**
     * Cria ou atualiza os NPCs dos top doadores
     */
    public void createOrUpdateNPCs(List<TopCustomer> topCustomers) {
        if (!isCitizensEnabled()) {
            logger.warning("Citizens não está instalado ou habilitado. NPCs não serão criados.");
            return;
        }

        if (!config.getBoolean("npcs.enabled", true)) {
            logger.info("NPCs desabilitados na configuração.");
            return;
        }

        NPCRegistry registry = CitizensAPI.getNPCRegistry();

        // Remove NPCs antigos primeiro
        removeAllNPCs();

        for (TopCustomer customer : topCustomers) {
            try {
                createNPC(registry, customer);
            } catch (Exception e) {
                logger.severe("Erro ao criar NPC para " + customer.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        logger.info("NPCs dos top doadores criados/atualizados com sucesso!");
    }

    /**
     * Cria um NPC individual
     */
    private void createNPC(NPCRegistry registry, TopCustomer customer) {
        int position = customer.getPosition();
        String positionKey = getPositionKey(position);

        if (positionKey == null) {
            logger.warning("Posição inválida: " + position);
            return;
        }

        // Busca a localização do config
        Location location = getLocationFromConfig(positionKey);
        if (location == null) {
            logger.warning("Localização não configurada para posição: " + position);
            return;
        }

        // Busca o nome formatado
        String displayName = config.getString("npcs.names." + positionKey,
            "§e{player} §7- " + position + "º Lugar");
        displayName = displayName.replace("{player}", customer.getName());

        // Cria o NPC
        NPC npc = registry.createNPC(EntityType.PLAYER, customer.getName());
        npc.setName(displayName);

        // Spawn do NPC
        npc.spawn(location);

        // Salva o ID do NPC
        npcIds.put(position, npc.getId());

        logger.info("NPC criado: " + displayName + " na posição " + position);
    }

    /**
     * Remove todos os NPCs dos top doadores
     */
    public void removeAllNPCs() {
        if (!isCitizensEnabled()) {
            return;
        }

        NPCRegistry registry = CitizensAPI.getNPCRegistry();
        int removed = 0;

        for (Integer npcId : new ArrayList<>(npcIds.values())) {
            NPC npc = registry.getById(npcId);
            if (npc != null) {
                npc.destroy();
                removed++;
            }
        }

        npcIds.clear();

        if (removed > 0) {
            logger.info("Removidos " + removed + " NPCs dos top doadores.");
        }
    }

    /**
     * Obtém a localização do config
     */
    private Location getLocationFromConfig(String positionKey) {
        ConfigurationSection locationSection = config.getConfigurationSection("npcs.locations." + positionKey);

        if (locationSection == null) {
            return null;
        }

        String worldName = locationSection.getString("world", "world");
        double x = locationSection.getDouble("x", 0.0);
        double y = locationSection.getDouble("y", 64.0);
        double z = locationSection.getDouble("z", 0.0);
        float yaw = (float) locationSection.getDouble("yaw", 0.0);
        float pitch = (float) locationSection.getDouble("pitch", 0.0);

        org.bukkit.World world = Bukkit.getWorld(worldName);
        if (world == null) {
            logger.warning("Mundo não encontrado: " + worldName);
            return null;
        }

        return new Location(world, x, y, z, yaw, pitch);
    }

    /**
     * Converte número da posição para key do config
     */
    private String getPositionKey(int position) {
        switch (position) {
            case 1:
                return "first";
            case 2:
                return "second";
            case 3:
                return "third";
            default:
                return null;
        }
    }

    /**
     * Retorna os IDs dos NPCs ativos
     */
    public Map<Integer, Integer> getNpcIds() {
        return new HashMap<>(npcIds);
    }
}

