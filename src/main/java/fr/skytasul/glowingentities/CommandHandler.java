package fr.skytasul.glowingentities;

import fr.skytasul.glowingentities.animation.RainbowAnimator;
import fr.skytasul.glowingentities.citizens.CitizensIntegration;
import fr.skytasul.glowingentities.citizens.NPCGlowData;
import fr.skytasul.glowingentities.data.DataManager;
import fr.skytasul.glowingentities.data.GlowingBlockData;
import java.util.UUID;
import java.util.logging.Logger;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CommandHandler {
   private final GlowingEntitiesPlugin plugin;
   private final GlowingBlocks glowingBlocks;
   private final DataManager dataManager;
   private final RainbowAnimator rainbowAnimator;
   private final CitizensIntegration citizensIntegration;

   public CommandHandler(GlowingEntitiesPlugin plugin, GlowingBlocks glowingBlocks, DataManager dataManager, RainbowAnimator rainbowAnimator, CitizensIntegration citizensIntegration) {
      this.plugin = plugin;
      this.glowingBlocks = glowingBlocks;
      this.dataManager = dataManager;
      this.rainbowAnimator = rainbowAnimator;
      this.citizensIntegration = citizensIntegration;
   }

   public boolean handleCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      String cmdName = command.getName().toLowerCase();
      switch (cmdName) {
         case "glowblock":
            return this.handleGlowBlock(sender, args);
         case "unglowblock":
            return this.handleUnglowBlock(sender, args);
         case "glownpc":
            return this.handleGlowNPC(sender, args);
         case "unglownpc":
            return this.handleUnglowNPC(sender, args);
         case "glowingentities":
            return this.handleMainCommand(sender, args);
         default:
            return false;
      }
   }

   private boolean handleGlowBlock(CommandSender sender, String[] args) {
      if (!(sender instanceof Player player)) {
         sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
         return true;
      }
      if (!player.hasPermission("glowingblocks.glowblock")) {
         player.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
         return true;
      }
      Block targetBlock = player.getTargetBlockExact(5);
      if (targetBlock == null || targetBlock.getType().isAir()) {
         player.sendMessage(ChatColor.RED + "You must be looking at a block!");
         return true;
      }

      boolean animated = false;
      boolean global = false;
      ChatColor color = ChatColor.RED;
      if (args.length > 0) {
         if (args[0].equalsIgnoreCase("rainbow")) {
            animated = true;
            color = this.rainbowAnimator.getCurrentColor();
         } else {
            try {
               color = ChatColor.valueOf(args[0].toUpperCase());
               if (!color.isColor()) {
                  player.sendMessage(ChatColor.RED + "Invalid color! Use: RED, BLUE, GREEN, YELLOW, AQUA, GOLD, or RAINBOW");
                  return true;
               }
            } catch (IllegalArgumentException e) {
               player.sendMessage(ChatColor.RED + "Invalid color! Use: RED, BLUE, GREEN, YELLOW, AQUA, GOLD, or RAINBOW");
               return true;
            }
         }
      }

      if (args.length > 1 && args[1].equalsIgnoreCase("global")) {
         global = true;
      }

      try {
         if (global) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
               this.glowingBlocks.setGlowing(targetBlock, onlinePlayer, color);
            }
         } else {
            this.glowingBlocks.setGlowing(targetBlock, player, color);
         }

         GlowingBlockData blockData = new GlowingBlockData(targetBlock.getLocation(), targetBlock.getType(), color, animated, global, player.getUniqueId().toString());
         this.dataManager.addBlock(blockData);
         String blockType = this.getBlockDisplayName(targetBlock);
         String mode = animated ? ChatColor.LIGHT_PURPLE + "RAINBOW" : color.toString() + color.name();
         String visibility = global ? ChatColor.GOLD + " (GLOBAL)" : ChatColor.GRAY + " (private)";
         player.sendMessage(ChatColor.GREEN + blockType + " is now glowing with " + mode + ChatColor.GREEN + " color!" + visibility);
         player.sendMessage(ChatColor.GRAY + "Location: " + targetBlock.getX() + ", " + targetBlock.getY() + ", " + targetBlock.getZ());
         if (this.isPlayerHead(targetBlock)) {
            String ownerInfo = this.getHeadOwnerInfo(targetBlock);
            if (ownerInfo != null) {
               player.sendMessage(ChatColor.GRAY + "Head owner: " + ChatColor.YELLOW + ownerInfo);
            }
         }
      } catch (ReflectiveOperationException e) {
         player.sendMessage(ChatColor.RED + "An error occurred while making the block glow!");
         e.printStackTrace();
      }

      return true;
   }

   private boolean handleUnglowBlock(CommandSender sender, String[] args) {
      if (!(sender instanceof Player player)) {
         sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
         return true;
      }
      if (!player.hasPermission("glowingblocks.unglowblock")) {
         player.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
         return true;
      }
      Block targetBlock = player.getTargetBlockExact(5);
      if (targetBlock == null || targetBlock.getType().isAir()) {
         player.sendMessage(ChatColor.RED + "You must be looking at a block!");
         return true;
      }

      try {
         GlowingBlockData blockData = this.dataManager.getBlock(targetBlock.getLocation());
         if (blockData != null && blockData.isGlobal()) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
               this.glowingBlocks.unsetGlowing(targetBlock, onlinePlayer);
            }
         } else {
            this.glowingBlocks.unsetGlowing(targetBlock, player);
         }

         this.dataManager.removeBlock(targetBlock.getLocation());
         String blockType = this.getBlockDisplayName(targetBlock);
         player.sendMessage(ChatColor.GREEN + blockType + " is no longer glowing!");
         player.sendMessage(ChatColor.GRAY + "Location: " + targetBlock.getX() + ", " + targetBlock.getY() + ", " + targetBlock.getZ());
      } catch (ReflectiveOperationException e) {
         player.sendMessage(ChatColor.RED + "An error occurred while removing the glow!");
         e.printStackTrace();
      }

      return true;
   }

   private boolean handleMainCommand(CommandSender sender, String[] args) {
      if (args.length == 0) {
         this.sendHelp(sender);
         return true;
      }
      String subCommand = args[0].toLowerCase();
      switch (subCommand) {
         case "help":
            this.sendHelp(sender);
            return true;
         case "reload":
            if (!sender.hasPermission("glowingblocks.admin")) {
               sender.sendMessage(ChatColor.RED + "You don't have permission!");
               return true;
            }
            this.plugin.reloadConfig();
            this.dataManager.load();
            sender.sendMessage(ChatColor.GREEN + "Reloaded config.yml and saves.yml!");
            return true;
         case "save":
            if (!sender.hasPermission("glowingblocks.admin")) {
               sender.sendMessage(ChatColor.RED + "You don't have permission!");
               return true;
            }
            this.dataManager.save();
            sender.sendMessage(ChatColor.GREEN + "Saved all glowing blocks!");
            return true;
         case "list":
            sender.sendMessage(ChatColor.GOLD + "===== Glowing Blocks =====");
            sender.sendMessage(ChatColor.GRAY + "Total: " + ChatColor.YELLOW + this.dataManager.getAllBlocks().size());
            sender.sendMessage(ChatColor.GRAY + "Animated: " + ChatColor.LIGHT_PURPLE + this.dataManager.getAnimatedBlocks().size());
            return true;
         case "version":
            sender.sendMessage(ChatColor.AQUA + "GlowingEntities v" + this.plugin.getDescription().getVersion());
            return true;
         default:
            this.sendHelp(sender);
            return true;
      }
   }

   private void sendHelp(CommandSender sender) {
      sender.sendMessage(ChatColor.GOLD + "===== " + ChatColor.AQUA + "GlowingEntities Help" + ChatColor.GOLD + " =====");
      sender.sendMessage(ChatColor.YELLOW + "/glowblock [color|rainbow] [global]" + ChatColor.GRAY + " - Make block glow");
      sender.sendMessage(ChatColor.GRAY + "  Examples:");
      sender.sendMessage(ChatColor.GRAY + "    /glowblock RED - Red glow (private)");
      sender.sendMessage(ChatColor.GRAY + "    /glowblock RAINBOW - Animated rainbow");
      sender.sendMessage(ChatColor.GRAY + "    /glowblock GOLD global - Gold glow (everyone sees)");
      sender.sendMessage(ChatColor.YELLOW + "/unglowblock" + ChatColor.GRAY + " - Remove glow");
      sender.sendMessage(ChatColor.YELLOW + "/ge list" + ChatColor.GRAY + " - List all glowing blocks");
      sender.sendMessage(ChatColor.YELLOW + "/ge save" + ChatColor.GRAY + " - Save to saves.yml");
      sender.sendMessage("");
      sender.sendMessage(ChatColor.GRAY + "Colors: RED, BLUE, GREEN, YELLOW, AQUA, GOLD, RAINBOW");
   }

   public void loadAllSavedBlocks() {
      Logger logger = this.plugin.getLogger();
      for (GlowingBlockData blockData : this.dataManager.getAllBlocks()) {
         try {
            if (!blockData.getLocation().isWorldLoaded()) {
               logger.warning("World not loaded for block at " + blockData.getLocationString());
               continue;
            }
            if (blockData.isGlobal()) {
               for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                  this.glowingBlocks.setGlowing(blockData.getLocation().getBlock(), onlinePlayer, blockData.getColor());
               }
            } else {
               String ownerUUID = blockData.getOwnerUUID();
               if (ownerUUID != null && !ownerUUID.isEmpty()) {
                  try {
                     Player owner = Bukkit.getPlayer(UUID.fromString(ownerUUID));
                     if (owner != null && owner.isOnline()) {
                        this.glowingBlocks.setGlowing(blockData.getLocation().getBlock(), owner, blockData.getColor());
                     }
                  } catch (IllegalArgumentException e) {
                     logger.warning("Invalid UUID format for block at " + blockData.getLocationString() + ": " + ownerUUID);
                  }
               } else {
                  logger.warning("Invalid owner UUID for block at " + blockData.getLocationString());
               }
            }
         } catch (Exception e) {
            logger.warning("Failed to load glowing block at " + blockData.getLocationString());
            e.printStackTrace();
         }
      }

      if (this.citizensIntegration.isEnabled()) {
         for (NPCGlowData npcData : this.dataManager.getAllNPCs()) {
            try {
               this.citizensIntegration.loadNPCData(npcData.getNpcId(), npcData.getNpcName(), npcData.getColor(), npcData.isAnimated(), npcData.isGlobal());
            } catch (Exception e) {
               logger.warning("Failed to load glowing NPC: " + npcData.getNpcName() + " (ID: " + npcData.getNpcId() + ")");
            }
         }
      }
   }

   private boolean isPlayerHead(Block block) {
      Material type = block.getType();
      return type == Material.PLAYER_HEAD || type == Material.PLAYER_WALL_HEAD;
   }

   private String getBlockDisplayName(Block block) {
      if (this.isPlayerHead(block)) {
         return "Player Head";
      }
      String name = block.getType().name().toLowerCase().replace('_', ' ');
      String[] words = name.split(" ");
      StringBuilder result = new StringBuilder();
      for (String word : words) {
         if (!word.isEmpty()) {
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
         }
      }
      return result.toString().trim();
   }

   private String getHeadOwnerInfo(Block block) {
      if (!this.isPlayerHead(block)) {
         return null;
      }
      try {
         Skull skull = (Skull) block.getState();
         if (skull.hasOwner() && skull.getOwningPlayer() != null) {
            return skull.getOwningPlayer().getName();
         }
         if (skull.getPlayerProfile() != null && skull.getPlayerProfile().getName() != null) {
            return skull.getPlayerProfile().getName();
         }
      } catch (Exception e) {
         // Ignore
      }
      return "Unknown";
   }

   private boolean handleGlowNPC(CommandSender sender, String[] args) {
      if (!this.citizensIntegration.isEnabled()) {
         sender.sendMessage(ChatColor.RED + "Citizens plugin is not installed or enabled!");
         return true;
      }
      if (!sender.hasPermission("glowingblocks.glownpc")) {
         sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
         return true;
      }
      if (args.length < 1) {
         sender.sendMessage(ChatColor.RED + "Usage: /glownpc <npc_id|name> [color|rainbow] [global]");
         return true;
      }
      NPC npc = this.citizensIntegration.getNPC(args[0]);
      if (npc == null) {
         sender.sendMessage(ChatColor.RED + "NPC not found: " + args[0]);
         return true;
      }
      if (!npc.isSpawned()) {
         sender.sendMessage(ChatColor.RED + "NPC must be spawned first!");
         return true;
      }

      boolean animated = false;
      boolean global = true;
      ChatColor color = ChatColor.RED;
      if (args.length > 1) {
         if (args[1].equalsIgnoreCase("rainbow")) {
            animated = true;
            color = this.rainbowAnimator.getCurrentColor();
         } else {
            try {
               color = ChatColor.valueOf(args[1].toUpperCase());
               if (!color.isColor()) {
                  sender.sendMessage(ChatColor.RED + "Invalid color! Use: RED, BLUE, GREEN, YELLOW, AQUA, GOLD, or RAINBOW");
                  return true;
               }
            } catch (IllegalArgumentException e) {
               sender.sendMessage(ChatColor.RED + "Invalid color! Use: RED, BLUE, GREEN, YELLOW, AQUA, GOLD, or RAINBOW");
               return true;
            }
         }
      }

      boolean success = this.citizensIntegration.setNPCGlowing(npc.getId(), color, animated, global);
      if (success) {
         String mode = animated ? ChatColor.LIGHT_PURPLE + "RAINBOW" : color.toString() + color.name();
         sender.sendMessage(ChatColor.GREEN + "NPC '" + ChatColor.YELLOW + npc.getName() + ChatColor.GREEN + "' (ID: " + npc.getId() + ") is now glowing with " + mode + ChatColor.GREEN + " color!");
         if (animated) {
            sender.sendMessage(ChatColor.GRAY + "Rainbow animation enabled - colors change every second!");
         }
      } else {
         sender.sendMessage(ChatColor.RED + "Failed to make NPC glow!");
      }

      return true;
   }

   private boolean handleUnglowNPC(CommandSender sender, String[] args) {
      if (!this.citizensIntegration.isEnabled()) {
         sender.sendMessage(ChatColor.RED + "Citizens plugin is not installed or enabled!");
         return true;
      }
      if (!sender.hasPermission("glowingblocks.unglownpc")) {
         sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
         return true;
      }
      if (args.length < 1) {
         sender.sendMessage(ChatColor.RED + "Usage: /unglownpc <npc_id|name>");
         return true;
      }
      NPC npc = this.citizensIntegration.getNPC(args[0]);
      if (npc == null) {
         sender.sendMessage(ChatColor.RED + "NPC not found: " + args[0]);
         return true;
      }

      boolean success = this.citizensIntegration.removeNPCGlowing(npc.getId());
      if (success) {
         sender.sendMessage(ChatColor.GREEN + "NPC '" + ChatColor.YELLOW + npc.getName() + ChatColor.GREEN + "' (ID: " + npc.getId() + ") is no longer glowing!");
      } else {
         sender.sendMessage(ChatColor.YELLOW + "NPC was not glowing or couldn't be found!");
      }

      return true;
   }
}
