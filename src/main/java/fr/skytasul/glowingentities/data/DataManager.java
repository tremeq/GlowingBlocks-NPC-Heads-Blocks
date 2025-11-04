package fr.skytasul.glowingentities.data;

import fr.skytasul.glowingentities.citizens.NPCGlowData;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public class DataManager {
   private final Plugin plugin;
   private final File savesFile;
   private YamlConfiguration config;
   private final Map<String, GlowingBlockData> glowingBlocks;
   private final Map<Integer, NPCGlowData> glowingNPCs;

   public DataManager(Plugin plugin) {
      this.plugin = plugin;
      this.savesFile = new File(plugin.getDataFolder(), "saves.yml");
      this.glowingBlocks = new HashMap<>();
      this.glowingNPCs = new HashMap<>();
      this.load();
   }

   public void load() {
      if (!this.savesFile.exists()) {
         this.plugin.getDataFolder().mkdirs();

         try {
            this.savesFile.createNewFile();
         } catch (IOException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Could not create saves.yml", e);
         }
      }

      this.config = YamlConfiguration.loadConfiguration(this.savesFile);
      this.glowingBlocks.clear();
      this.glowingNPCs.clear();
      ConfigurationSection blocksSection = this.config.getConfigurationSection("blocks");
      if (blocksSection != null) {
         for (String key : blocksSection.getKeys(false)) {
            try {
               ConfigurationSection blockSection = blocksSection.getConfigurationSection(key);
               if (blockSection != null) {
                  String[] parts = key.split(",");
                  if (parts.length == 4) {
                     Location location = new Location(Bukkit.getWorld(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
                     Material blockType = Material.valueOf(blockSection.getString("type", "PLAYER_HEAD"));
                     ChatColor color = ChatColor.valueOf(blockSection.getString("color", "RED"));
                     boolean animated = blockSection.getBoolean("animated", false);
                     boolean global = blockSection.getBoolean("global", false);
                     String ownerUUID = blockSection.getString("owner", "");
                     long createdTime = blockSection.getLong("createdTime", System.currentTimeMillis());
                     GlowingBlockData data = new GlowingBlockData(location, blockType, color, animated, global, ownerUUID, createdTime);
                     this.glowingBlocks.put(key, data);
                  }
               }
            } catch (Exception e) {
               this.plugin.getLogger().warning("Failed to load glowing block: " + key);
            }
         }
      }

      ConfigurationSection npcsSection = this.config.getConfigurationSection("npcs");
      if (npcsSection != null) {
         for (String key : npcsSection.getKeys(false)) {
            try {
               ConfigurationSection npcSection = npcsSection.getConfigurationSection(key);
               if (npcSection != null) {
                  int npcId = Integer.parseInt(key);
                  String npcName = npcSection.getString("name", "Unknown");
                  ChatColor color = ChatColor.valueOf(npcSection.getString("color", "RED"));
                  boolean animated = npcSection.getBoolean("animated", false);
                  boolean global = npcSection.getBoolean("global", true);
                  NPCGlowData data = new NPCGlowData(npcId, npcName, color, animated, global);
                  this.glowingNPCs.put(npcId, data);
               }
            } catch (Exception e) {
               this.plugin.getLogger().warning("Failed to load glowing NPC: " + key);
            }
         }
      }

      Logger logger = this.plugin.getLogger();
      logger.info("Loaded " + this.glowingBlocks.size() + " glowing blocks and " + this.glowingNPCs.size() + " glowing NPCs from saves.yml");
   }

   public void save() {
      this.config = new YamlConfiguration();
      for (Map.Entry<String, GlowingBlockData> entry : this.glowingBlocks.entrySet()) {
         String key = entry.getKey();
         GlowingBlockData data = entry.getValue();
         this.config.set("blocks." + key + ".type", data.getBlockType().name());
         this.config.set("blocks." + key + ".color", data.getColor().name());
         this.config.set("blocks." + key + ".animated", data.isAnimated());
         this.config.set("blocks." + key + ".global", data.isGlobal());
         this.config.set("blocks." + key + ".owner", data.getOwnerUUID());
         this.config.set("blocks." + key + ".createdTime", data.getCreatedTime());
      }

      for (Map.Entry<Integer, NPCGlowData> entry : this.glowingNPCs.entrySet()) {
         int npcId = entry.getKey();
         NPCGlowData data = entry.getValue();
         this.config.set("npcs." + npcId + ".name", data.getNpcName());
         this.config.set("npcs." + npcId + ".color", data.getColor().name());
         this.config.set("npcs." + npcId + ".animated", data.isAnimated());
         this.config.set("npcs." + npcId + ".global", data.isGlobal());
      }

      try {
         this.config.save(this.savesFile);
         Logger logger = this.plugin.getLogger();
         logger.info("Saved " + this.glowingBlocks.size() + " glowing blocks and " + this.glowingNPCs.size() + " glowing NPCs to saves.yml");
      } catch (IOException e) {
         this.plugin.getLogger().log(Level.SEVERE, "Could not save to saves.yml", e);
      }

   }

   public void addBlock(GlowingBlockData data) {
      this.glowingBlocks.put(data.getLocationString(), data);
      this.save();
   }

   public void removeBlock(Location location) {
      String key = this.locationToString(location);
      this.glowingBlocks.remove(key);
      this.save();
   }

   public GlowingBlockData getBlock(Location location) {
      return this.glowingBlocks.get(this.locationToString(location));
   }

   public Collection<GlowingBlockData> getAllBlocks() {
      return this.glowingBlocks.values();
   }

   public Collection<GlowingBlockData> getAnimatedBlocks() {
      List<GlowingBlockData> animated = new ArrayList<>();
      for (GlowingBlockData data : this.glowingBlocks.values()) {
         if (data.isAnimated()) {
            animated.add(data);
         }
      }
      return animated;
   }

   public void addNPC(NPCGlowData data) {
      this.glowingNPCs.put(data.getNpcId(), data);
      this.save();
   }

   public void saveNPC(NPCGlowData data) {
      this.glowingNPCs.put(data.getNpcId(), data);
      this.save();
   }

   public void removeNPC(int npcId) {
      this.glowingNPCs.remove(npcId);
      this.save();
   }

   public Collection<NPCGlowData> getAllNPCs() {
      return this.glowingNPCs.values();
   }

   public NPCGlowData getNPC(int npcId) {
      return this.glowingNPCs.get(npcId);
   }

   private String locationToString(Location loc) {
      return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
   }
}
