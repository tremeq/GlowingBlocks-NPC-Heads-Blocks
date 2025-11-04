package fr.skytasul.glowingentities;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class GlowingBlocks implements Listener {
   @NotNull
   private final GlowingEntities entities;
   private Map<Player, GlowingBlocks.PlayerData> glowing;
   private Map<Player, ChannelDuplexHandler> packetHandlers;
   private boolean enabled = false;
   private final Map<UUID, GlowingBlocks.LastClickData> lastClickCache = new HashMap<>();
   private static Class<?> packetUseEntityClass;
   private static Field packetUseEntityIdField;
   private static Field packetUseEntityActionField;
   private static Method getHandleMethod;
   private static Field playerConnectionField;
   private static Field networkManagerField;
   private static Field channelField;

   public GlowingBlocks(@NotNull Plugin plugin) {
      this.testForPaper();
      this.entities = new GlowingEntities(plugin);
      this.initializePacketInterception();
      this.enable();
   }

   private void initializePacketInterception() {
      try {
         String nmsPackage = "net.minecraft.";
         String[] packageParts = Bukkit.getServer().getClass().getPackage().getName().split("\\.");
         Class<?> craftPlayerClass;
         if (packageParts.length <= 3) {
            craftPlayerClass = Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer");
         } else {
            String version = packageParts[3];
            craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
         }

         getHandleMethod = craftPlayerClass.getDeclaredMethod("getHandle");
         getHandleMethod.setAccessible(true);
         Class<?> serverPlayerClass = Class.forName(nmsPackage + "server.level.ServerPlayer");
         Class<?> serverCommonPacketListenerClass = Class.forName(nmsPackage + "server.network.ServerCommonPacketListenerImpl");
         Class<?> connectionClass = Class.forName(nmsPackage + "network.Connection");
         playerConnectionField = serverPlayerClass.getDeclaredField("connection");
         playerConnectionField.setAccessible(true);
         networkManagerField = serverCommonPacketListenerClass.getDeclaredField("connection");
         networkManagerField.setAccessible(true);
         channelField = connectionClass.getDeclaredField("channel");
         channelField.setAccessible(true);
         packetUseEntityClass = Class.forName(nmsPackage + "network.protocol.game.ServerboundInteractPacket");
         packetUseEntityIdField = packetUseEntityClass.getDeclaredField("entityId");
         packetUseEntityIdField.setAccessible(true);
         packetUseEntityActionField = packetUseEntityClass.getDeclaredField("action");
         packetUseEntityActionField.setAccessible(true);
         this.entities.plugin.getLogger().info("Packet interception initialized - Slime hitbox will be bypassed!");
      } catch (Exception var7) {
         this.entities.plugin.getLogger().warning("Could not initialize packet interception: " + var7.getMessage());
         this.entities.plugin.getLogger().warning("Slime will block interactions - consider updating to 1.19.4+");
      }

   }

   public void enable() {
      if (this.enabled) {
         throw new IllegalStateException("The Glowing Blocks API has already been enabled.");
      } else {
         this.entities.plugin.getServer().getPluginManager().registerEvents(this, this.entities.plugin);
         if (!this.entities.enabled) {
            this.entities.enable();
         }

         this.glowing = new HashMap<>();
         this.packetHandlers = new HashMap<>();
         this.enabled = true;
      }
   }

   public void disable() {
      if (this.enabled) {
         HandlerList.unregisterAll(this);
         this.glowing.values().forEach((playerData) -> {
            playerData.datas.values().forEach((glowingData) -> {
               try {
                  glowingData.remove();
               } catch (ReflectiveOperationException var2) {
                  var2.printStackTrace();
               }

            });
         });
         this.packetHandlers.forEach((player, handler) -> {
            try {
               this.removePacketHandler(player);
            } catch (Exception var4) {
               var4.printStackTrace();
            }

         });
         this.entities.disable();
         this.glowing = null;
         this.packetHandlers = null;
         this.enabled = false;
      }
   }

   private void ensureEnabled() {
      if (!this.enabled) {
         throw new IllegalStateException("The Glowing Blocks API is not enabled.");
      }
   }

   @NotNull
   public GlowingEntities getGlowingEntities() {
      return this.entities;
   }

   public Location getGlowingBlockLocation(int entityId) {
      this.ensureEnabled();
      for (GlowingBlocks.PlayerData playerData : this.glowing.values()) {
         for (Map.Entry<Location, GlowingBlocks.GlowingBlockData> entry : playerData.datas.entrySet()) {
            if (entry.getValue().entityId == entityId) {
               return entry.getKey();
            }
         }
      }
      return null;
   }

   private void testForPaper() {
      try {
         Class.forName("io.papermc.paper.event.packet.PlayerChunkLoadEvent");
      } catch (ClassNotFoundException var2) {
         throw new UnsupportedOperationException("The GlowingBlocks util can only be used on a Paper server.");
      }
   }

   public void setGlowing(@NotNull Block block, @NotNull Player receiver, @NotNull ChatColor color) throws ReflectiveOperationException {
      this.setGlowing(block.getLocation(), receiver, color, block.getType());
   }

   public void setGlowing(@NotNull Location block, @NotNull Player receiver, @NotNull ChatColor color) throws ReflectiveOperationException {
      this.setGlowing(block, receiver, color, Material.AIR);
   }

   private void setGlowing(@NotNull Location block, @NotNull Player receiver, @NotNull ChatColor color, @NotNull Material blockType) throws ReflectiveOperationException {
      this.ensureEnabled();
      block = this.normalizeLocation(block);
      if (!color.isColor()) {
         throw new IllegalArgumentException("ChatColor must be a color format");
      } else {
         GlowingBlocks.PlayerData playerData = this.glowing.computeIfAbsent(Objects.requireNonNull(receiver), GlowingBlocks.PlayerData::new);
         this.addPacketHandler(receiver);
         GlowingBlocks.GlowingBlockData blockData = playerData.datas.get(block);
         if (blockData == null) {
            blockData = new GlowingBlocks.GlowingBlockData(receiver, block, color, blockType);
            playerData.datas.put(block, blockData);
            if (this.canSee(receiver, block)) {
               blockData.spawn();
            }
         } else {
            blockData.setColor(color);
         }

      }
   }

   public void unsetGlowing(@NotNull Block block, @NotNull Player receiver) throws ReflectiveOperationException {
      this.unsetGlowing(block.getLocation(), receiver);
   }

   public void unsetGlowing(@NotNull Location block, @NotNull Player receiver) throws ReflectiveOperationException {
      this.ensureEnabled();
      block = this.normalizeLocation(block);
      GlowingBlocks.PlayerData playerData = this.glowing.get(receiver);
      if (playerData != null) {
         GlowingBlocks.GlowingBlockData blockData = playerData.datas.remove(block);
         if (blockData != null) {
            blockData.remove();
            if (playerData.datas.isEmpty()) {
               this.glowing.remove(receiver);
            }

         }
      }
   }

   @NotNull
   private Location normalizeLocation(@NotNull Location location) {
      location.checkFinite();
      return new Location(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
   }

   private boolean canSee(Player player, Location location) {
      int viewDistance = Math.min(player.getViewDistance(), Bukkit.getViewDistance());
      int deltaChunkX = (player.getLocation().getBlockX() >> 4) - (location.getBlockX() >> 4);
      int deltaChunkZ = (player.getLocation().getBlockZ() >> 4) - (location.getBlockZ() >> 4);
      int chunkDistanceSquared = deltaChunkX * deltaChunkX + deltaChunkZ * deltaChunkZ;
      return chunkDistanceSquared <= viewDistance * viewDistance;
   }

   @EventHandler
   public void onPlayerChunkLoad(PlayerChunkLoadEvent event) {
      GlowingBlocks.PlayerData playerData = this.glowing.get(event.getPlayer());
      if (playerData != null) {
         playerData.datas.forEach((location, blockData) -> {
            if (Objects.equals(location.getWorld(), event.getWorld()) && location.getBlockX() >> 4 == event.getChunk().getX() && location.getBlockZ() >> 4 == event.getChunk().getZ()) {
               try {
                  blockData.spawn();
               } catch (ReflectiveOperationException var4) {
                  var4.printStackTrace();
               }
            }

         });
      }
   }

   @EventHandler
   public void onPlayerQuit(PlayerQuitEvent event) {
      try {
         this.removePacketHandler(event.getPlayer());
      } catch (Exception var3) {
         var3.printStackTrace();
      }

      this.lastClickCache.remove(event.getPlayer().getUniqueId());
   }

   private void addPacketHandler(final Player player) {
      if (!this.packetHandlers.containsKey(player) && packetUseEntityClass != null) {
         try {
            Object nmsPlayer = getHandleMethod.invoke(player);
            Object playerConnection = playerConnectionField.get(nmsPlayer);
            Object networkManager = networkManagerField.get(playerConnection);
            Channel channel = (Channel)channelField.get(networkManager);
            ChannelDuplexHandler handler = new ChannelDuplexHandler() {
               public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                  if (GlowingBlocks.packetUseEntityClass.isInstance(msg)) {
                     int entityId = GlowingBlocks.packetUseEntityIdField.getInt(msg);
                     Location blockLocation = GlowingBlocks.this.getGlowingBlockLocation(entityId);
                     if (blockLocation != null) {
                        Block block = blockLocation.getBlock();
                        Object packetAction = GlowingBlocks.packetUseEntityActionField.get(msg);
                        String actionClassName = packetAction.getClass().getName();
                        boolean isLeftClick = actionClassName.endsWith("$1");
                        long currentTime = System.currentTimeMillis();
                        UUID playerId = player.getUniqueId();
                        GlowingBlocks.LastClickData lastClick = GlowingBlocks.this.lastClickCache.get(playerId);
                        if (lastClick != null && lastClick.isDuplicate(entityId, isLeftClick, currentTime)) {
                           return;
                        }

                        GlowingBlocks.this.lastClickCache.put(playerId, new GlowingBlocks.LastClickData(currentTime, entityId, isLeftClick));
                        Logger logger = GlowingBlocks.this.entities.plugin.getLogger();
                        logger.info("[DEBUG] Player " + player.getName() + " - Action class: " + actionClassName);
                        Bukkit.getScheduler().runTask(GlowingBlocks.this.entities.plugin, () -> {
                           Action bukkitAction;
                           if (actionClassName.endsWith("$1")) {
                              bukkitAction = Action.LEFT_CLICK_BLOCK;
                              logger.info("[DEBUG] Mapped to LEFT_CLICK_BLOCK");
                           } else {
                              bukkitAction = Action.RIGHT_CLICK_BLOCK;
                              logger.info("[DEBUG] Mapped to RIGHT_CLICK_BLOCK");
                           }

                           PlayerInteractEvent event = new PlayerInteractEvent(player, bukkitAction, player.getInventory().getItemInMainHand(), block, BlockFace.UP, EquipmentSlot.HAND);
                           Bukkit.getPluginManager().callEvent(event);
                        });
                        return;
                     }
                  }

                  super.channelRead(ctx, msg);
               }
            };
            channel.pipeline().addBefore("packet_handler", "glowing_blocks_handler", handler);
            this.packetHandlers.put(player, handler);
         } catch (Exception var7) {
            Logger logger = this.entities.plugin.getLogger();
            logger.warning("Error adding packet handler for " + player.getName() + ": " + var7.getMessage());
         }

      }
   }

   private void removePacketHandler(Player player) throws Exception {
      ChannelDuplexHandler handler = this.packetHandlers.remove(player);
      if (handler != null) {
         Object nmsPlayer = getHandleMethod.invoke(player);
         Object playerConnection = playerConnectionField.get(nmsPlayer);
         Object networkManager = networkManagerField.get(playerConnection);
         Channel channel = (Channel)channelField.get(networkManager);
         if (channel.pipeline().get("glowing_blocks_handler") != null) {
            channel.pipeline().remove("glowing_blocks_handler");
         }
      }

   }

   private record PlayerData(@NotNull Player player, @NotNull Map<Location, GlowingBlocks.GlowingBlockData> datas) {
      public PlayerData(@NotNull Player player) {
         this(player, new HashMap<>());
      }
   }

   private class GlowingBlockData {
      private static final byte FLAGS = 32;
      private static final AtomicInteger ENTITY_ID_COUNTER = new AtomicInteger(ThreadLocalRandom.current().nextInt(1000000, 2000000000));
      @NotNull
      private final Player player;
      @NotNull
      private final Location location;
      @NotNull
      private final Material blockType;
      @NotNull
      private ChatColor color;
      private int entityId;
      private UUID entityUuid;
      private BukkitRunnable runnable;

      public GlowingBlockData(@NotNull Player player, @NotNull Location location, @NotNull ChatColor color, @NotNull Material blockType) {
         this.player = player;
         this.location = location;
         this.color = color;
         this.blockType = blockType;
      }

      public void setColor(@NotNull ChatColor color) throws ReflectiveOperationException {
         this.color = color;
         if (this.entityUuid != null) {
            if (isPlayerHead(this.blockType) && ((GlowingEntitiesPlugin)GlowingBlocks.this.entities.plugin).isDebugEnabled()) {
               GlowingBlocks.this.entities.plugin.getLogger().info("[DEBUG HEAD] setColor() called - entityID=" + this.entityId + ", color=" + color.name() + ", FLAGS=" + FLAGS);
            }
            GlowingBlocks.this.entities.setGlowing(this.entityId, this.entityUuid.toString(), this.player, color, FLAGS);
            if (isPlayerHead(this.blockType) && ((GlowingEntitiesPlugin)GlowingBlocks.this.entities.plugin).isDebugEnabled()) {
               GlowingBlocks.this.entities.plugin.getLogger().info("[DEBUG HEAD] setGlowing() completed successfully");
            }
         }

      }

      public void spawn() throws ReflectiveOperationException {
         if (this.entityUuid == null) {
            this.entityId = ENTITY_ID_COUNTER.getAndIncrement();
            this.entityUuid = UUID.randomUUID();
            this.setColor(this.color);
         }

         boolean isHead = isPlayerHead(this.blockType);
         Object entityType = isHead ? GlowingEntities.Packets.slimeEntityType : GlowingEntities.Packets.shulkerEntityType;

         Location spawnLoc = this.location.clone();
         if (isHead) {
            spawnLoc.add(0.5, 0.0, 0.5);
         } else {
            spawnLoc.add(0.5, 0.0, 0.5);
         }

         GlowingEntities.Packets.createEntity(this.player, this.entityId, this.entityUuid, entityType, spawnLoc);

         GlowingEntities.Packets.setMetadata(this.player, this.entityId, (byte)32, false);

         if (isHead) {
            GlowingEntities.Packets.setSlimeSize(this.player, this.entityId, 1);
         }

         if (this.runnable != null) {
            this.runnable.cancel();
         }
         this.runnable = new BukkitRunnable() {
            @Override
            public void run() {
               if (!player.isOnline()) {
                  cancel();
                  return;
               }
               try {
                  GlowingEntities.Packets.teleportEntity(player, entityId, spawnLoc);
               } catch (ReflectiveOperationException e) {
                  e.printStackTrace();
                  cancel();
               }
            }
         };
         this.runnable.runTaskTimer(entities.plugin, 20L, 100L);
      }

      private boolean isPlayerHead(Material material) {
         return material == Material.PLAYER_HEAD || material == Material.PLAYER_WALL_HEAD;
      }

      public void remove() throws ReflectiveOperationException {
         if (this.entityUuid != null) {
            if (this.runnable != null) {
               this.runnable.cancel();
            }
            GlowingEntities.Packets.removeEntities(this.player, this.entityId);
            GlowingBlocks.this.entities.unsetGlowing(this.entityId, this.player);
         }
      }

   }

   private static class LastClickData {
      long timestamp;
      int entityId;
      boolean isLeftClick;

      LastClickData(long timestamp, int entityId, boolean isLeftClick) {
         this.timestamp = timestamp;
         this.entityId = entityId;
         this.isLeftClick = isLeftClick;
      }

      boolean isDuplicate(int entityId, boolean isLeftClick, long currentTime) {
         return this.entityId == entityId && this.isLeftClick == isLeftClick && currentTime - this.timestamp < 50L;
      }
   }
}
