package fr.skytasul.glowingentities.animation;

import fr.skytasul.glowingentities.GlowingEntitiesPlugin;
import fr.skytasul.glowingentities.citizens.CitizensIntegration;
import fr.skytasul.glowingentities.citizens.NPCGlowData;
import fr.skytasul.glowingentities.data.GlowingBlockData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class RainbowAnimator {
   private final GlowingEntitiesPlugin plugin;
   private final CitizensIntegration citizensIntegration;
   private BukkitRunnable animationTask;
   private static final ChatColor[] RAINBOW_COLORS;
   private int currentColorIndex = 0;

   public RainbowAnimator(GlowingEntitiesPlugin plugin, CitizensIntegration citizensIntegration) {
      this.plugin = plugin;
      this.citizensIntegration = citizensIntegration;
   }

   public void start(long intervalTicks) {
      if (this.animationTask != null) {
         this.animationTask.cancel();
      }

      this.animationTask = new BukkitRunnable() {
         public void run() {
            RainbowAnimator.this.currentColorIndex = (RainbowAnimator.this.currentColorIndex + 1) % RainbowAnimator.RAINBOW_COLORS.length;
            ChatColor nextColor = RainbowAnimator.RAINBOW_COLORS[RainbowAnimator.this.currentColorIndex];
            for (GlowingBlockData blockData : RainbowAnimator.this.plugin.getDataManager().getAnimatedBlocks()) {
               try {
                  blockData.setColor(nextColor);
                  List<Player> viewers = new ArrayList<>();
                  if (blockData.isGlobal()) {
                     viewers.addAll(Bukkit.getOnlinePlayers());
                  } else {
                     Player owner = Bukkit.getPlayer(UUID.fromString(blockData.getOwnerUUID()));
                     if (owner != null && owner.isOnline()) {
                        viewers.add(owner);
                     }
                  }

                  for (Player viewer : viewers) {
                     RainbowAnimator.this.plugin.getGlowingBlocks().setGlowing(blockData.getLocation().getBlock(), viewer, nextColor);
                  }
               } catch (Exception e) {
                  RainbowAnimator.this.plugin.getLogger().warning("Failed to animate block at " + blockData.getLocationString());
               }
            }

            if (RainbowAnimator.this.citizensIntegration != null && RainbowAnimator.this.citizensIntegration.isEnabled()) {
               Collection<NPCGlowData> animatedNPCs = RainbowAnimator.this.citizensIntegration.getAnimatedNPCs();
               for (NPCGlowData npcData : animatedNPCs) {
                  try {
                     RainbowAnimator.this.citizensIntegration.updateNPCColor(npcData.getNpcId(), nextColor);
                  } catch (Exception e) {
                     Logger logger = RainbowAnimator.this.plugin.getLogger();
                     logger.warning("Failed to animate NPC " + npcData.getNpcName() + " (ID: " + npcData.getNpcId() + ")");
                     e.printStackTrace();
                  }
               }
            }

         }
      };
      this.animationTask.runTaskTimer(this.plugin, 0L, intervalTicks);
      this.plugin.getLogger().info("Rainbow animator started (interval: " + intervalTicks + " ticks)");
   }

   public void stop() {
      if (this.animationTask != null) {
         this.animationTask.cancel();
         this.animationTask = null;
         this.plugin.getLogger().info("Rainbow animator stopped");
      }

   }

   public ChatColor getCurrentColor() {
      return RAINBOW_COLORS[this.currentColorIndex];
   }

   public static ChatColor[] getRainbowColors() {
      return RAINBOW_COLORS;
   }

   static {
      RAINBOW_COLORS = new ChatColor[]{ChatColor.RED, ChatColor.GOLD, ChatColor.YELLOW, ChatColor.GREEN, ChatColor.AQUA, ChatColor.BLUE, ChatColor.LIGHT_PURPLE};
   }
}
