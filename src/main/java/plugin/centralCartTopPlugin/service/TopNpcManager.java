package plugin.centralCartTopPlugin.service;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.trait.SkinTrait;
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
    private FileConfiguration config; // tornamos não-final para permitir reload
    private final Map<Integer, Integer> npcIds; // posição -> NPC ID
    private final Map<String, Location> locationCache; // Cache de localizações parseadas

    public TopNpcManager(Logger logger, FileConfiguration config) {
        this.logger = logger;
        this.config = config;
        this.npcIds = new HashMap<>();
        this.locationCache = new HashMap<>();

        // Carrega IDs salvos dos NPCs
        loadNpcIds();

        // NÃO carregamos os NPCs aqui para evitar problemas com Citizens ainda não inicializado.
        // A carga dos NPCs será feita explicitamente pelo plugin quando for seguro (onEnable).
    }

    /**
     * Carrega e valida NPCs existentes na inicialização (síncrono) - público para ser chamado pelo plugin
     */
    public void loadExistingNPCs() {
        if (!isCitizensEnabled()) {
            return;
        }

        if (npcIds.isEmpty()) {
            logger.info("Nenhum NPC salvo encontrado.");
            return;
        }

        NPCRegistry registry = CitizensAPI.getNPCRegistry();
        int loaded = 0;
        int missing = 0;

        for (Map.Entry<Integer, Integer> entry : new HashMap<>(npcIds).entrySet()) {
            int position = entry.getKey();
            int npcId = entry.getValue();

            NPC npc = registry.getById(npcId);
            if (npc != null) {
                // NPC existe, verifica se está spawnado
                String positionKey = getPositionKey(position);
                Location location = getLocationFromConfig(positionKey);

                if (location != null && !npc.isSpawned()) {
                    npc.spawn(location);
                    logger.info("NPC carregado e spawnado: " + npc.getName() + " (posição " + position + ")");
                    loaded++;
                } else if (location != null && npc.isSpawned()) {
                    logger.info("NPC já estava spawnado: " + npc.getName() + " (posição " + position + ")");
                    // Mesmo se já estiver spawnado, podemos teleportá-lo caso a localização tenha sido alterada
                    npc.teleport(location, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
                    loaded++;
                }
            } else {
                logger.warning("NPC salvo (ID: " + npcId + ") não encontrado na posição " + position);
                npcIds.remove(position);
                missing++;
            }
        }

        if (loaded > 0) {
            logger.info("§a[CentralCart] " + loaded + " NPC(s) carregado(s) com sucesso!");
        }
        if (missing > 0) {
            logger.warning("§e[CentralCart] " + missing + " NPC(s) não encontrado(s) e foram removidos da lista.");
        }
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

        for (TopCustomer customer : topCustomers) {
            try {
                updateOrCreateNPC(registry, customer);
            } catch (Exception e) {
                logger.severe("Erro ao atualizar/criar NPC para " + customer.getName() + ": " + e.getMessage());
                logger.log(java.util.logging.Level.SEVERE, "Stack trace:", e);
            }
        }

        // Salva os IDs no config
        saveNpcIds();

        logger.info("NPCs dos top doadores atualizados com sucesso!");
    }

    /**
     * Atualiza um NPC existente ou cria um novo
     */
    private void updateOrCreateNPC(NPCRegistry registry, TopCustomer customer) {
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

        // Verifica se já existe um NPC nesta posição
        NPC npc = null;
        if (npcIds.containsKey(position)) {
            npc = registry.getById(npcIds.get(position));
        }

        if (npc != null) {
            // Atualiza NPC existente
            logger.info("Atualizando NPC na posição " + position + " para " + customer.getName());

            // Atualiza o nome exibido
            npc.setName(displayName);

            // Atualiza a skin do jogador
            if (npc.hasTrait(SkinTrait.class)) {
                SkinTrait skinTrait = npc.getOrAddTrait(SkinTrait.class);
                skinTrait.setSkinName(customer.getName());
            }

            // Move o NPC para a nova localização (teleporta)
            if (npc.isSpawned()) {
                npc.teleport(location, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
            } else {
                npc.spawn(location);
            }

            logger.info("NPC atualizado: " + displayName + " movido para " + formatLocation(location));
        } else {
            // Cria novo NPC
            logger.info("Criando novo NPC na posição " + position + " para " + customer.getName());

            npc = registry.createNPC(EntityType.PLAYER, customer.getName());
            npc.setName(displayName);

            // Define a skin do jogador
            SkinTrait skinTrait = npc.getOrAddTrait(SkinTrait.class);
            skinTrait.setSkinName(customer.getName());

            // Spawn do NPC
            try {
                npc.spawn(location);

                // Salva o ID do NPC
                npcIds.put(position, npc.getId());

                logger.info("NPC criado: " + displayName + " na posição " + formatLocation(location));
            } catch (Exception e) {
                logger.severe("Erro ao spawnar NPC: " + e.getMessage());
                logger.log(java.util.logging.Level.SEVERE, "Stack trace:", e);
            }
        }
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
     * Formata localização para log
     */
    private String formatLocation(Location loc) {
        return String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ());
    }

    /**
     * Obtém a localização do config (com cache para performance)
     */
    private Location getLocationFromConfig(String positionKey) {
        // Verifica se está em cache
        if (locationCache.containsKey(positionKey)) {
            return locationCache.get(positionKey).clone();
        }

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

        Location location = new Location(world, x, y, z, yaw, pitch);

        // Armazena em cache
        locationCache.put(positionKey, location.clone());

        return location;
    }

    /**
     * Carrega os IDs dos NPCs salvos no config
     */
    private void loadNpcIds() {
        if (config.contains("npcs.saved_ids")) {
            ConfigurationSection idsSection = config.getConfigurationSection("npcs.saved_ids");
            if (idsSection != null) {
                for (String key : idsSection.getKeys(false)) {
                    try {
                        int position = Integer.parseInt(key);
                        int npcId = idsSection.getInt(key);
                        npcIds.put(position, npcId);
                        logger.info("NPC ID carregado: posição " + position + " -> ID " + npcId);
                    } catch (NumberFormatException e) {
                        logger.warning("Chave inválida no saved_ids: " + key);
                        logger.log(java.util.logging.Level.WARNING, "Stack trace:", e);
                    }
                }
            }
        }
    }

    /**
     * Salva os IDs dos NPCs no config
     */
    private void saveNpcIds() {
        // Limpa seção antiga para evitar dados órfãos
        config.set("npcs.saved_ids", null);
        for (Map.Entry<Integer, Integer> entry : npcIds.entrySet()) {
            config.set("npcs.saved_ids." + entry.getKey(), entry.getValue());
        }
    }

    /**
     * Retorna os IDs dos NPCs ativos
     */
    public Map<Integer, Integer> getNpcIds() {
        return new HashMap<>(npcIds);
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

        // Limpa os IDs salvos no config
        config.set("npcs.saved_ids", null);

        if (removed > 0) {
            logger.info("Removidos " + removed + " NPCs dos top doadores.");
        }
    }

    /**
     * Recarrega a configuração do manager (para ser chamado durante reload do plugin)
     */
    public void reload(FileConfiguration newConfig) {
        this.config = newConfig;
        this.npcIds.clear();
        this.locationCache.clear(); // Limpa cache de localizações
        loadNpcIds();
        // Carrega/spawna NPCs com base nos IDs carregados (chamado de forma síncrona pelo plugin)
        loadExistingNPCs();
    }

}
