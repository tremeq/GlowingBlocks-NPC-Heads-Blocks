package fr.skytasul.glowingentities.listeners;

import fr.skytasul.glowingentities.GlowingBlocks;
import fr.skytasul.glowingentities.GlowingEntitiesPlugin;
import fr.skytasul.glowingentities.citizens.CitizensIntegration;
import fr.skytasul.glowingentities.data.DataManager;
import fr.skytasul.glowingentities.data.GlowingBlockData;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {
   private final GlowingEntitiesPlugin plugin;
   private final GlowingBlocks glowingBlocks;
   private final DataManager dataManager;
   private final CitizensIntegration citizensIntegration;

   public PlayerJoinListener(GlowingEntitiesPlugin plugin, GlowingBlocks glowingBlocks, DataManager dataManager, CitizensIntegration citizensIntegration) {
      this.plugin = plugin;
      this.glowingBlocks = glowingBlocks;
      this.dataManager = dataManager;
      this.citizensIntegration = citizensIntegration;
   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   public void onPlayerJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
         this.loadBlocksForPlayer(player);
         this.loadNPCsForPlayer(player);
      }, 1L);
   }

   private void loadBlocksForPlayer(Player player) {
      int globalBlocks = 0;
      int privateBlocks = 0;
      for (GlowingBlockData blockData : this.dataManager.getAllBlocks()) {
         try {
            if (blockData.getLocation().isWorldLoaded()) {
               boolean shouldLoad = false;
               if (blockData.isGlobal()) {
                  shouldLoad = true;
                  globalBlocks++;
               } else if (blockData.getOwnerUUID().equals(player.getUniqueId().toString())) {
                  shouldLoad = true;
                  privateBlocks++;
               }

               if (shouldLoad) {
                  this.glowingBlocks.setGlowing(blockData.getLocation().getBlock(), player, blockData.getColor());
               }
            }
         } catch (Exception e) {
            Logger logger = this.plugin.getLogger();
            logger.warning("Failed to load glowing block at " + blockData.getLocationString() + " for player " + player.getName());
            e.printStackTrace();
         }
      }

      if (globalBlocks > 0 || privateBlocks > 0) {
         this.plugin.getLogger().info("Loaded " + globalBlocks + " global and " + privateBlocks + " private glowing blocks for " + player.getName());
      }

   }

   private void loadNPCsForPlayer(Player player) {
      if (this.citizensIntegration.isEnabled()) {
         this.citizensIntegration.applyGlowingToPlayer(player);
      }
   }
}
