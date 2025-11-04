package fr.skytasul.glowingentities.citizens;

import fr.skytasul.glowingentities.GlowingEntities;
import fr.skytasul.glowingentities.GlowingEntitiesPlugin;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class CitizensIntegration {
   private final GlowingEntitiesPlugin plugin;
   private final Map<Integer, NPCGlowData> glowingNPCs;
   private GlowingEntities glowingEntities;
   private boolean enabled = false;

   public CitizensIntegration(GlowingEntitiesPlugin plugin) {
      this.plugin = plugin;
      this.glowingNPCs = new ConcurrentHashMap<>();
   }

   public void setGlowingEntities(GlowingEntities glowingEntities) {
      this.glowingEntities = glowingEntities;
   }

   private String getTeamID(Entity entity) {
      return entity instanceof Player ? entity.getName() : entity.getUniqueId().toString();
   }

   private byte getFlagsWithoutName(Entity entity) {
      try {
         byte flags = this.glowingEntities.getEntityFlags(entity);
         flags &= ~8;
         return flags;
      } catch (Exception e) {
         return 0;
      }
   }

   public boolean initialize() {
      if (!Bukkit.getPluginManager().isPluginEnabled("Citizens")) {
         this.plugin.getLogger().info("Citizens not found - NPC glowing disabled");
         return false;
      } else {
         this.plugin.getLogger().info("Citizens found - NPC glowing enabled!");
         this.enabled = true;
         return true;
      }
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public boolean setNPCGlowing(int npcId, ChatColor color, boolean animated, boolean global) {
      NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);
      if (npc == null || !npc.isSpawned()) return false;

      npc.data().setPersistent(NPC.Metadata.NAMEPLATE_VISIBLE, false);
      npc.data().setPersistent(NPC.Metadata.ALWAYS_USE_NAME_HOLOGRAM, false);
      Entity entity = npc.getEntity();
      if (entity == null) return false;

      try {
         String teamID = this.getTeamID(entity);
         byte flags = this.getFlagsWithoutName(entity);
         for (Player player : Bukkit.getOnlinePlayers()) {
            this.glowingEntities.setGlowing(entity.getEntityId(), teamID, player, color, flags, true);
         }

         NPCGlowData glowData = new NPCGlowData(npcId, npc.getName(), color, animated, global);
         this.glowingNPCs.put(npcId, glowData);
         this.plugin.getDataManager().addNPC(glowData);
         return true;
      } catch (ReflectiveOperationException e) {
         this.plugin.getLogger().warning("Failed to make NPC glow: " + e.getMessage());
         return false;
      }
   }

   public boolean removeNPCGlowing(int npcId) {
      NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);
      if (npc == null || !npc.isSpawned()) {
         this.glowingNPCs.remove(npcId);
         return false;
      }

      Entity entity = npc.getEntity();
      if (entity == null) {
         this.glowingNPCs.remove(npcId);
         return false;
      }

      try {
         for (Player player : Bukkit.getOnlinePlayers()) {
            this.glowingEntities.unsetGlowing(entity, player);
         }

         this.glowingNPCs.remove(npcId);
         this.plugin.getDataManager().removeNPC(npcId);
         return true;
      } catch (ReflectiveOperationException e) {
         this.plugin.getLogger().warning("Failed to remove NPC glow: " + e.getMessage());
         return false;
      }
   }

   public void updateNPCColor(int npcId, ChatColor newColor) {
      NPCGlowData glowData = this.glowingNPCs.get(npcId);
      if (glowData == null) return;

      NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);
      if (npc == null || !npc.isSpawned()) return;

      npc.data().setPersistent(NPC.Metadata.NAMEPLATE_VISIBLE, false);
      npc.data().setPersistent(NPC.Metadata.ALWAYS_USE_NAME_HOLOGRAM, false);
      Entity entity = npc.getEntity();
      if (entity == null) return;

      try {
         String teamID = this.getTeamID(entity);
         byte flags = this.getFlagsWithoutName(entity);
         for (Player player : Bukkit.getOnlinePlayers()) {
            this.glowingEntities.setGlowing(entity.getEntityId(), teamID, player, newColor, flags, true);
         }
         glowData.setColor(newColor);
      } catch (ReflectiveOperationException e) {
         this.plugin.getLogger().warning("Failed to update NPC color: " + e.getMessage());
         e.printStackTrace();
      }
   }

   public void applyGlowingToPlayer(Player player) {
      for (NPCGlowData glowData : this.glowingNPCs.values()) {
         if (glowData.isGlobal()) {
            NPC npc = CitizensAPI.getNPCRegistry().getById(glowData.getNpcId());
            if (npc != null && npc.isSpawned()) {
               Entity entity = npc.getEntity();
               if (entity != null) {
                  try {
                     String teamID = this.getTeamID(entity);
                     byte flags = this.getFlagsWithoutName(entity);
                     this.glowingEntities.setGlowing(entity.getEntityId(), teamID, player, glowData.getColor(), flags, true);
                  } catch (ReflectiveOperationException e) {
                     this.plugin.getLogger().warning("Failed to apply NPC glow to player: " + e.getMessage());
                  }
               }
            }
         }
      }
   }

   public NPC getNPC(String identifier) {
      try {
         int id = Integer.parseInt(identifier);
         return CitizensAPI.getNPCRegistry().getById(id);
      } catch (NumberFormatException e) {
         for (NPC npc : CitizensAPI.getNPCRegistry()) {
            if (npc.getName().equalsIgnoreCase(identifier)) {
               return npc;
            }
         }
         return null;
      }
   }

   public Collection<NPCGlowData> getAllGlowingNPCs() {
      return this.glowingNPCs.values();
   }

   public Collection<NPCGlowData> getAnimatedNPCs() {
      List<NPCGlowData> animated = new ArrayList<>();
      for (NPCGlowData data : this.glowingNPCs.values()) {
         if (data.isAnimated()) {
            animated.add(data);
         }
      }
      return animated;
   }

   public boolean isNPCGlowing(int npcId) {
      return this.glowingNPCs.containsKey(npcId);
   }

   public NPCGlowData getGlowData(int npcId) {
      return this.glowingNPCs.get(npcId);
   }

   public void clearAll() {
      for (int npcId : new HashSet<>(this.glowingNPCs.keySet())) {
         this.removeNPCGlowing(npcId);
      }
   }

   public void loadNPCData(int npcId, String npcName, ChatColor color, boolean animated, boolean global) {
      NPCGlowData glowData = new NPCGlowData(npcId, npcName, color, animated, global);
      this.glowingNPCs.put(npcId, glowData);
      NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);
      if (npc != null && npc.isSpawned() && npc.getEntity() != null) {
         npc.data().setPersistent(NPC.Metadata.NAMEPLATE_VISIBLE, false);
         npc.data().setPersistent(NPC.Metadata.ALWAYS_USE_NAME_HOLOGRAM, false);

         try {
            Entity entity = npc.getEntity();
            String teamID = this.getTeamID(entity);
            byte flags = this.getFlagsWithoutName(entity);
            for (Player player : Bukkit.getOnlinePlayers()) {
               this.glowingEntities.setGlowing(entity.getEntityId(), teamID, player, color, flags, true);
            }
         } catch (ReflectiveOperationException e) {
            this.plugin.getLogger().warning("Failed to apply saved NPC glow: " + e.getMessage());
         }
      }
   }
}
