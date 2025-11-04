package fr.skytasul.glowingentities.listeners;

import fr.skytasul.glowingentities.GlowingBlocks;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class EntityInteractionListener implements Listener {
   private final GlowingBlocks glowingBlocks;

   public EntityInteractionListener(GlowingBlocks glowingBlocks) {
      this.glowingBlocks = glowingBlocks;
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
      if (event.getRightClicked() instanceof Slime) {
         Location blockLocation = this.glowingBlocks.getGlowingBlockLocation(event.getRightClicked().getEntityId());
         if (blockLocation != null) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            Block block = blockLocation.getBlock();
            PlayerInteractEvent blockEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, player.getInventory().getItemInMainHand(), block, BlockFace.UP);
            player.getServer().getPluginManager().callEvent(blockEvent);
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
      if (event.getEntity() instanceof Slime) {
         Location blockLocation = this.glowingBlocks.getGlowingBlockLocation(event.getEntity().getEntityId());
         if (blockLocation != null) {
            event.setCancelled(true);
            if (event.getDamager() instanceof Player player) {
               Block block = blockLocation.getBlock();
               PlayerInteractEvent blockEvent = new PlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, player.getInventory().getItemInMainHand(), block, BlockFace.UP);
               player.getServer().getPluginManager().callEvent(blockEvent);
            }
         }
      }
   }
}
