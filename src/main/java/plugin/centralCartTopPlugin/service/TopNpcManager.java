package plugin.centralCartTopPlugin.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.trait.SkinTrait;
import plugin.centralCartTopPlugin.model.TopCustomer;
import plugin.centralCartTopPlugin.util.PluginUtils;

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
                    logger.log(Level.INFO, "NPC carregado e spawnado: {0} (posição {1})", new Object[]{npc.getName(), position});
                    loaded++;
                } else if (location != null && npc.isSpawned()) {
                    logger.log(Level.INFO, "NPC já estava spawnado: {0} (posição {1})", new Object[]{npc.getName(), position});
                    // Mesmo se já estiver spawnado, podemos teleportá-lo caso a localização tenha sido alterada
                    npc.teleport(location, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
                    loaded++;
                }
            } else {
                logger.log(Level.WARNING, "NPC salvo (ID: {0}) não encontrado na posição {1}", new Object[]{npcId, position});
                npcIds.remove(position);
                missing++;
            }
        }

        if (loaded > 0) {
            logger.log(Level.INFO, "§a[CentralCart] {0} NPC(s) carregado(s) com sucesso!", loaded);
        }
        if (missing > 0) {
            logger.log(Level.WARNING, "§e[CentralCart] {0} NPC(s) não encontrado(s) e foram removidos da lista.", missing);
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
                logger.log(Level.SEVERE, "Erro ao atualizar/criar NPC para " + customer.getName(), e);
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
            logger.log(Level.WARNING, "Posição inválida: {0}", position);
            return;
        }

        // Busca a localização do config
        Location location = getLocationFromConfig(positionKey);
        if (location == null) {
            logger.log(Level.WARNING, "Localização não configurada para posição: {0}", position);
            return;
        }

        // Busca o nome formatado
        String displayName = config.getString("npcs.names." + positionKey,
            "§e{player} §7- " + position + "º Lugar");
        if (displayName == null) {
            displayName = "§e{player} §7- " + position + "º Lugar";
        }
        displayName = displayName.replace("{player}", customer.getName());

        // Verifica se já existe um NPC nesta posição
        NPC npc = null;
        if (npcIds.containsKey(position)) {
            npc = registry.getById(npcIds.get(position));
        }

        if (npc != null) {
            // Atualiza NPC existente
            logger.log(Level.INFO, "Atualizando NPC na posição {0} para {1}", new Object[]{position, customer.getName()});

            // Atualiza o nome exibido
            npc.setName(displayName);

            // Atualiza a skin — getOrAddTrait garante que a trait existe antes de atualizar,
            // sem depender de hasTrait que deixaria NPCs sem trait herdada sem skin atualizada
            SkinTrait skinTrait = npc.getOrAddTrait(SkinTrait.class);
            skinTrait.setSkinName(customer.getName(), true);

            // Move o NPC para a nova localização (teleporta)
            if (npc.isSpawned()) {
                npc.teleport(location, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
            } else {
                npc.spawn(location);
            }

            logger.log(Level.INFO, "NPC atualizado: {0} movido para {1}", new Object[]{displayName, formatLocation(location)});
        } else {
            // Cria novo NPC
            logger.log(Level.INFO, "Criando novo NPC na posição {0} para {1}", new Object[]{position, customer.getName()});

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

                logger.log(Level.INFO, "NPC criado: {0} na posição {1}", new Object[]{displayName, formatLocation(location)});
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Erro ao spawnar NPC", e);
            }
        }
    }

    /**
     * Converte número da posição para key do config usando PluginUtils
     */
    private String getPositionKey(int position) {
        return PluginUtils.getPositionKey(position);
    }

    /**
     * Formata localização para log usando PluginUtils
     */
    private String formatLocation(Location loc) {
        if (loc == null) {
            return "null";
        }
        return PluginUtils.formatLocation(loc.getX(), loc.getY(), loc.getZ());
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
        if (worldName == null || worldName.isEmpty()) {
            worldName = "world";
        }
        double x = locationSection.getDouble("x", 0.0);
        double y = locationSection.getDouble("y", 64.0);
        double z = locationSection.getDouble("z", 0.0);
        float yaw = (float) locationSection.getDouble("yaw", 0.0);
        float pitch = (float) locationSection.getDouble("pitch", 0.0);

        org.bukkit.World world = Bukkit.getWorld(worldName);
        if (world == null) {
            logger.log(Level.WARNING, "Mundo não encontrado: {0}", worldName);
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
                        logger.log(Level.INFO, "NPC ID carregado: posição {0} -> ID {1}", new Object[]{position, npcId});
                    } catch (NumberFormatException e) {
                        logger.log(Level.WARNING, "Chave inválida no saved_ids: {0}", key);
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
            logger.log(Level.INFO, "Removidos {0} NPCs dos top doadores.", removed);
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
