package fr.skytasul.glowingentities;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import fr.skytasul.glowingentities.libs.reflection.MappedReflectionAccessor;
import fr.skytasul.glowingentities.libs.reflection.ReflectionAccessor;
import fr.skytasul.glowingentities.libs.reflection.TransparentReflectionAccessor;
import fr.skytasul.glowingentities.libs.reflection.Version;
import fr.skytasul.glowingentities.libs.reflection.mappings.Mappings;
import fr.skytasul.glowingentities.libs.reflection.mappings.files.MappingFileReader;
import fr.skytasul.glowingentities.libs.reflection.mappings.files.ProguardMapping;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class GlowingEntities implements Listener {
   @NotNull
   protected final Plugin plugin;
   private Map<Player, GlowingEntities.PlayerData> glowing;
   boolean enabled = false;
   private int uid;

   public GlowingEntities(@NotNull Plugin plugin) {
      GlowingEntities.Packets.ensureInitialized();
      this.plugin = Objects.requireNonNull(plugin);
      this.enable();
   }

   public void enable() {
      if (this.enabled) {
         throw new IllegalStateException("The Glowing Entities API has already been enabled.");
      } else {
         this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
         this.glowing = new HashMap<>();
         this.uid = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
         this.enabled = true;
      }
   }

   public void disable() {
      if (this.enabled) {
         HandlerList.unregisterAll(this);
         this.glowing.values().forEach((playerData) -> {
            try {
               GlowingEntities.Packets.removePacketsHandler(playerData);
            } catch (ReflectiveOperationException var2) {
               var2.printStackTrace();
            }

         });
         this.glowing = null;
         this.uid = 0;
         this.enabled = false;
      }
   }

   private void ensureEnabled() {
      if (!this.enabled) {
         throw new IllegalStateException("The Glowing Entities API is not enabled.");
      }
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      this.glowing.remove(event.getPlayer());
   }

   public void setGlowing(Entity entity, Player receiver) throws ReflectiveOperationException {
      this.setGlowing(entity, receiver, null);
   }

   public void setGlowing(Entity entity, Player receiver, ChatColor color) throws ReflectiveOperationException {
      String teamID = entity instanceof Player ? entity.getName() : entity.getUniqueId().toString();
      this.setGlowing(entity.getEntityId(), teamID, receiver, color, GlowingEntities.Packets.getEntityFlags(entity));
   }

   public void setGlowing(int entityID, String teamID, Player receiver) throws ReflectiveOperationException {
      this.setGlowing(entityID, teamID, receiver, null, (byte)0);
   }

   public void setGlowing(int entityID, String teamID, Player receiver, ChatColor color) throws ReflectiveOperationException {
      this.setGlowing(entityID, teamID, receiver, color, (byte)0);
   }

   public void setGlowing(int entityID, String teamID, Player receiver, ChatColor color, byte otherFlags) throws ReflectiveOperationException {
      this.ensureEnabled();
      if (color != null && !color.isColor()) {
         throw new IllegalArgumentException("ChatColor must be a color format");
      } else {
         GlowingEntities.PlayerData playerData = this.glowing.computeIfAbsent(receiver, (p) -> {
            try {
               GlowingEntities.PlayerData data = new GlowingEntities.PlayerData(this, p);
               GlowingEntities.Packets.addPacketsHandler(data);
               return data;
            } catch (ReflectiveOperationException e) {
               throw new RuntimeException(e);
            }
         });

         GlowingEntities.GlowingData glowingData = playerData.glowingDatas.get(entityID);
         if (glowingData == null) {
            glowingData = new GlowingEntities.GlowingData(playerData, entityID, teamID, color, otherFlags);
            playerData.glowingDatas.put(entityID, glowingData);
            GlowingEntities.Packets.createGlowing(glowingData);
            if (color != null) {
               GlowingEntities.Packets.setGlowingColor(glowingData);
            }
         } else {
            if (Objects.equals(glowingData.color, color)) {
               return;
            }

            if (color == null) {
               GlowingEntities.Packets.removeGlowingColor(glowingData);
               glowingData.color = null;
            } else {
               glowingData.color = color;
               GlowingEntities.Packets.setGlowingColor(glowingData);
            }
         }

      }
   }

   public void setGlowing(int entityID, String teamID, Player receiver, ChatColor color, byte otherFlags, boolean hideCustomName) throws ReflectiveOperationException {
      this.ensureEnabled();
      if (color != null && !color.isColor()) {
         throw new IllegalArgumentException("ChatColor must be a color format");
      } else {
         GlowingEntities.PlayerData playerData = this.glowing.computeIfAbsent(receiver, (p) -> {
            try {
               GlowingEntities.PlayerData data = new GlowingEntities.PlayerData(this, p);
               GlowingEntities.Packets.addPacketsHandler(data);
               return data;
            }
            catch (ReflectiveOperationException e) {
               throw new RuntimeException(e);
            }
         });

         GlowingEntities.GlowingData glowingData = playerData.glowingDatas.get(entityID);
         if (glowingData == null) {
            glowingData = new GlowingEntities.GlowingData(playerData, entityID, teamID, color, otherFlags, hideCustomName);
            playerData.glowingDatas.put(entityID, glowingData);
            GlowingEntities.Packets.createGlowing(glowingData);
            if (color != null) {
               GlowingEntities.Packets.setGlowingColor(glowingData);
            }
         } else {
            if (!Objects.equals(glowingData.color, color)) {
               if (color == null) {
                  GlowingEntities.Packets.removeGlowingColor(glowingData);
                  glowingData.color = null;
               } else {
                  glowingData.color = color;
                  GlowingEntities.Packets.setGlowingColor(glowingData);
               }
            }

            glowingData.hideCustomName = hideCustomName;
         }

      }
   }

   public void unsetGlowing(Entity entity, Player receiver) throws ReflectiveOperationException {
      this.unsetGlowing(entity.getEntityId(), receiver);
   }

   public byte getEntityFlags(Entity entity) throws ReflectiveOperationException {
      return GlowingEntities.Packets.getEntityFlags(entity);
   }

   public void unsetGlowing(int entityID, Player receiver) throws ReflectiveOperationException {
      this.ensureEnabled();
      GlowingEntities.PlayerData playerData = this.glowing.get(receiver);
      if (playerData != null) {
         GlowingEntities.GlowingData glowingData = playerData.glowingDatas.remove(entityID);
         if (glowingData != null) {
            GlowingEntities.Packets.removeGlowing(glowingData);
            if (glowingData.color != null) {
               GlowingEntities.Packets.removeGlowingColor(glowingData);
            }

         }
      }
   }

   protected static class Packets {
      private static final byte GLOWING_FLAG = 64;
      private static Cache<Object, Object> packets;
      private static Object dummy;
      private static Logger logger;
      private static String cpack;
      private static Version version;
      private static boolean isEnabled;
      private static boolean hasInitialized;
      private static Throwable initializationError;
      private static Method getHandle;
      private static Method getDataWatcher;
      private static Object watcherObjectFlags;
      private static Object watcherDummy;
      private static Method watcherGet;
      private static Constructor<?> watcherItemConstructor;
      private static Method watcherItemObject;
      private static Method watcherItemDataGet;
      private static Method watcherBCreator;
      private static Method watcherBId;
      private static Method watcherBSerializer;
      private static Method watcherSerializerObject;
      private static Field playerConnection;
      private static Method sendPacket;
      private static Field networkManager;
      private static Field channelField;
      private static ReflectionAccessor.ClassAccessor packetBundle;
      private static Method packetBundlePackets;
      private static ReflectionAccessor.ClassAccessor packetMetadata;
      private static Constructor<?> packetMetadataConstructor;
      private static Field packetMetadataEntity;
      private static Field packetMetadataItems;
      private static EnumMap<ChatColor, GlowingEntities.Packets.TeamData> teams;
      private static Constructor<?> createTeamPacket;
      private static Constructor<?> createTeamPacketData;
      private static Constructor<?> createTeam;
      private static Object scoreboardDummy;
      private static Object pushNever;
      private static Method setTeamPush;
      private static Method setTeamColor;
      private static Method getColorConstant;
      private static Object visibilityNever;
      private static Method setTeamNameTagVisibility;
      protected static Object shulkerEntityType;
      protected static Object slimeEntityType;
      protected static Object itemDisplayEntityType;
      private static Constructor<?> packetAddEntity;
      private static Constructor<?> packetRemove;
      private static Constructor<?> packetTeleport;
      private static Constructor<?> positionMoveRotationConstructor;
      private static Object vec3dZero;

      protected static void ensureInitialized() {
         if (!hasInitialized) {
            initialize();
         }

         if (!isEnabled) {
            throw new IllegalStateException("The Glowing Entities API is disabled. An error has occured during first initialization.", initializationError);
         }
      }

      private static void initialize() {
         hasInitialized = true;

         try {
            logger = new Logger("GlowingEntities", null) {
               public void log(LogRecord logRecord) {
                  logRecord.setMessage("[GlowingEntities] " + logRecord.getMessage());
                  super.log(logRecord);
               }
            };
            logger.setParent(Bukkit.getServer().getLogger());
            logger.setLevel(Level.ALL);
            String versionString = Bukkit.getBukkitVersion().split("-R")[0];
            Version serverVersion = Version.parse(versionString);
            logger.info("Found server version " + serverVersion);
            cpack = Bukkit.getServer().getClass().getPackage().getName();
            boolean remapped = cpack.split("\\.").length == 3;
            ReflectionAccessor reflection;
            if (remapped) {
               version = serverVersion;
               reflection = new TransparentReflectionAccessor();
               logger.info("Loaded transparent mappings.");
            } else {
               String mappingsFile = new String(Objects.requireNonNull(GlowingEntities.class.getResourceAsStream("mappings/spigot.txt")).readAllBytes());
               MappingFileReader mappingsReader = new MappingFileReader(new ProguardMapping(false), mappingsFile.lines().toList());
               Optional<Version> foundVersion = mappingsReader.keepBestMatchedVersion(serverVersion);
               if (foundVersion.isEmpty()) {
                  throw new UnsupportedOperationException("Cannot find mappings to match server version");
               }

               if (!foundVersion.get().is(serverVersion)) {
                  logger.warning("Loaded not matching version of the mappings for your server version");
               }

               version = foundVersion.get();
               mappingsReader.parseMappings();
               Mappings mappings = mappingsReader.getParsedMappings(foundVersion.get());
               logger.info("Loaded mappings for " + version);
               reflection = new MappedReflectionAccessor(mappings);
            }

            loadReflection(reflection, version);
            isEnabled = true;
         } catch (Exception var8) {
            initializationError = var8;
            String errorMsg = "Glowing Entities reflection failed to initialize. The util is disabled. Please ensure your version (" + Bukkit.getBukkitVersion() + ") is supported.";
            if (logger == null) {
               var8.printStackTrace();
               System.err.println(errorMsg);
            } else {
               logger.log(Level.SEVERE, errorMsg, var8);
            }
         }

      }

      protected static void loadReflection(@NotNull ReflectionAccessor reflection, @NotNull Version version) throws ReflectiveOperationException {
         ReflectionAccessor.ClassAccessor entityClass = getNMSClass(reflection, "world.entity", "Entity");
         ReflectionAccessor.ClassAccessor entityTypesClass = getNMSClass(reflection, "world.entity", "EntityType");
         Object worldInstance = version.isAfter(1, 21, 3) && cpack != null ? getCraftClass("", "CraftWorld").getDeclaredMethod("getHandle").invoke(Bukkit.getWorlds().get(0)) : null;
         Object markerEntity = getNMSClass(reflection, "world.entity", "Marker").getConstructor(entityTypesClass, getNMSClass(reflection, "world.level", "Level")).newInstance(entityTypesClass.getField("MARKER").get(null), worldInstance);
         getHandle = cpack == null ? null : getCraftClass("entity", "CraftEntity").getDeclaredMethod("getHandle");
         getDataWatcher = entityClass.getMethodInstance("getEntityData");
         ReflectionAccessor.ClassAccessor dataWatcherClass = getNMSClass(reflection, "network.syncher", "SynchedEntityData");
         ReflectionAccessor.ClassAccessor entityDataAccessorClass;
         if (version.isAfter(1, 20, 5)) {
            entityDataAccessorClass = getNMSClass(reflection, "network.syncher", "SynchedEntityData$Builder");
            Object watcherBuilder = entityDataAccessorClass.getConstructor(getNMSClass(reflection, "network.syncher", "SyncedDataHolder")).newInstance(markerEntity);
            ReflectionAccessor.ClassAccessor.FieldAccessor watcherBuilderItems = entityDataAccessorClass.getField("itemsById");
            watcherBuilderItems.set(watcherBuilder, Array.newInstance(getNMSClass(reflection, "network.syncher", "SynchedEntityData$DataItem").getClassInstance(), 0));
            watcherDummy = entityDataAccessorClass.getMethod("build").invoke(watcherBuilder);
         } else {
            watcherDummy = dataWatcherClass.getConstructor(entityClass).newInstance(markerEntity);
         }

         entityDataAccessorClass = getNMSClass(reflection, "network.syncher", "EntityDataAccessor");
         watcherObjectFlags = entityClass.getField("DATA_SHARED_FLAGS_ID").get(null);
         watcherGet = dataWatcherClass.getMethodInstance("get", entityDataAccessorClass);
         ReflectionAccessor.ClassAccessor packetListenerClass;
         if (!version.isAfter(1, 19, 3)) {
            packetListenerClass = getNMSClass(reflection, "network.syncher", "SynchedEntityData$DataItem");
            watcherItemConstructor = packetListenerClass.getConstructorInstance(entityDataAccessorClass, Object.class);
            watcherItemObject = packetListenerClass.getMethodInstance("getAccessor");
            watcherItemDataGet = packetListenerClass.getMethodInstance("getValue");
         } else {
            packetListenerClass = getNMSClass(reflection, "network.syncher", "SynchedEntityData$DataValue");
            watcherBCreator = packetListenerClass.getMethodInstance("create", entityDataAccessorClass, Object.class);
            watcherBId = packetListenerClass.getMethodInstance("id");
            watcherBSerializer = packetListenerClass.getMethodInstance("serializer");
            watcherItemDataGet = packetListenerClass.getMethodInstance("value");
            watcherSerializerObject = getNMSClass(reflection, "network.syncher", "EntityDataSerializer").getMethodInstance("createAccessor", Integer.TYPE);
         }

         playerConnection = getNMSClass(reflection, "server.level", "ServerPlayer").getFieldInstance("connection");
         packetListenerClass = getNMSClass(reflection, "server.network", version.isAfter(1, 20, 2) ? "ServerCommonPacketListenerImpl" : "ServerGamePacketListenerImpl");
         sendPacket = packetListenerClass.getMethodInstance("send", getNMSClass(reflection, "network.protocol", "Packet"));
         networkManager = packetListenerClass.getFieldInstance("connection");
         channelField = getNMSClass(reflection, "network", "Connection").getFieldInstance("channel");
         if (version.isAfter(1, 19, 4)) {
            packetBundle = getNMSClass(reflection, "network.protocol", "BundlePacket");
            packetBundlePackets = packetBundle.getMethodInstance("subPackets");
         }

         packetMetadata = getNMSClass(reflection, "network.protocol.game", "ClientboundSetEntityDataPacket");
         packetMetadataEntity = packetMetadata.getFieldInstance("id");
         packetMetadataItems = packetMetadata.getFieldInstance("packedItems");
         if (version.isAfter(1, 19, 3)) {
            packetMetadataConstructor = packetMetadata.getConstructorInstance(Integer.TYPE, List.class);
         } else {
            packetMetadataConstructor = packetMetadata.getConstructorInstance(Integer.TYPE, dataWatcherClass, Boolean.TYPE);
         }

         ReflectionAccessor.ClassAccessor scoreboardClass = getNMSClass(reflection, "world.scores", "Scoreboard");
         ReflectionAccessor.ClassAccessor teamClass = getNMSClass(reflection, "world.scores", "PlayerTeam");
         ReflectionAccessor.ClassAccessor pushClass = getNMSClass(reflection, "world.scores", "Team$CollisionRule");
         ReflectionAccessor.ClassAccessor visibilityClass = getNMSClass(reflection, "world.scores", "Team$Visibility");
         ReflectionAccessor.ClassAccessor chatFormatClass = getNMSClass(reflection, "ChatFormatting");
         createTeamPacket = getNMSClass(reflection, "network.protocol.game", "ClientboundSetPlayerTeamPacket").getConstructorInstance(String.class, Integer.TYPE, Optional.class, Collection.class);
         createTeamPacketData = getNMSClass(reflection, "network.protocol.game", "ClientboundSetPlayerTeamPacket$Parameters").getConstructorInstance(teamClass);
         createTeam = teamClass.getConstructorInstance(scoreboardClass, String.class);
         scoreboardDummy = scoreboardClass.getConstructor().newInstance();
         pushNever = pushClass.getField("NEVER").get(null);
         visibilityNever = visibilityClass.getField("NEVER").get(null);
         setTeamPush = teamClass.getMethodInstance("setCollisionRule", pushClass);
         setTeamColor = teamClass.getMethodInstance("setColor", chatFormatClass);
         setTeamNameTagVisibility = teamClass.getMethodInstance("setNameTagVisibility", visibilityClass);
         getColorConstant = chatFormatClass.getMethodInstance("getByCode", Character.TYPE);
         shulkerEntityType = entityTypesClass.getField("SHULKER").get(null);
         slimeEntityType = entityTypesClass.getField("SLIME").get(null);

         try {
            itemDisplayEntityType = entityTypesClass.getField("ITEM_DISPLAY").get(null);
            logger.info("Item Display entity loaded - will use for glowing heads (no hitbox, looks like head!)");
         } catch (NoSuchFieldException var16) {
            itemDisplayEntityType = null;
            logger.warning("Item Display not available (requires 1.19.4+) - using Slime (will block interactions)");
         }

         ReflectionAccessor.ClassAccessor vec3dClass = getNMSClass(reflection, "world.phys", "Vec3");
         vec3dZero = vec3dClass.getConstructor(Double.TYPE, Double.TYPE, Double.TYPE).newInstance(0.0D, 0.0D, 0.0D);
         ReflectionAccessor.ClassAccessor addEntityPacketClass = getNMSClass(reflection, "network.protocol.game", "ClientboundAddEntityPacket");
         if (version.isAfter(1, 19, 0)) {
            packetAddEntity = addEntityPacketClass.getConstructorInstance(Integer.TYPE, UUID.class, Double.TYPE, Double.TYPE, Double.TYPE, Float.TYPE, Float.TYPE, entityTypesClass, Integer.TYPE, vec3dClass, Double.TYPE);
         } else {
            packetAddEntity = addEntityPacketClass.getConstructorInstance(Integer.TYPE, UUID.class, Double.TYPE, Double.TYPE, Double.TYPE, Float.TYPE, Float.TYPE, entityTypesClass, Integer.TYPE, vec3dClass);
         }

         packetRemove = version.is(1, 17, 0) ? getNMSClass(reflection, "network.protocol.game", "ClientboundRemoveEntityPacket").getConstructorInstance(Integer.TYPE) : getNMSClass(reflection, "network.protocol.game", "ClientboundRemoveEntitiesPacket").getConstructorInstance(int[].class);

         try {
            packetTeleport = getNMSClass(reflection, "network.protocol.game", "ClientboundTeleportEntityPacket").getConstructorInstance(Integer.TYPE, Double.TYPE, Double.TYPE, Double.TYPE, Float.TYPE, Float.TYPE, Boolean.TYPE);
         } catch (NoSuchMethodException e) {
            try {
               ReflectionAccessor.ClassAccessor positionMoveRotationClass = getNMSClass(reflection, "world.entity", "PositionMoveRotation");
               positionMoveRotationConstructor = positionMoveRotationClass.getConstructorInstance(vec3dClass, vec3dClass, Float.TYPE, Float.TYPE);
               packetTeleport = getNMSClass(reflection, "network.protocol.game", "ClientboundTeleportEntityPacket").getConstructorInstance(Integer.TYPE, positionMoveRotationClass);
            } catch (Exception ex) {
               logger.warning("Failed to load teleport packet constructor - teleportation may not work properly");
               logger.warning("This is not critical, glowing should still work");
            }
         }
      }

      public static void sendPackets(Player p, Object... packets) throws ReflectiveOperationException {
         Object connection = playerConnection.get(getHandle.invoke(p));
         for (Object packet : packets) {
            if (packet != null) {
               sendPacket.invoke(connection, packet);
            }
         }
      }

      public static byte getEntityFlags(Entity entity) throws ReflectiveOperationException {
         Object nmsEntity = getHandle.invoke(entity);
         Object dataWatcher = getDataWatcher.invoke(nmsEntity);
         return (Byte)watcherGet.invoke(dataWatcher, watcherObjectFlags);
      }

      public static void createGlowing(GlowingEntities.GlowingData glowingData) throws ReflectiveOperationException {
         byte flags = computeFlags(glowingData);
         setMetadata(glowingData.player.player, glowingData.entityID, flags, true);
      }

      private static byte computeFlags(GlowingEntities.GlowingData glowingData) {
         byte newFlags = glowingData.otherFlags;
         if (glowingData.enabled) {
            newFlags |= 64;
         }

         if (glowingData.hideCustomName) {
            newFlags &= ~8;
         }

         return newFlags;
      }

      public static Object createFlagWatcherItem(byte newFlags) throws ReflectiveOperationException {
         return watcherItemConstructor != null ? watcherItemConstructor.newInstance(watcherObjectFlags, newFlags) : watcherBCreator.invoke(null, watcherObjectFlags, newFlags);
      }

      public static void removeGlowing(GlowingEntities.GlowingData glowingData) throws ReflectiveOperationException {
         if (!glowingData.hideCustomName) {
            setMetadata(glowingData.player.player, glowingData.entityID, glowingData.otherFlags, true);
         }

      }

      public static void updateGlowingState(GlowingEntities.GlowingData glowingData) throws ReflectiveOperationException {
         if (glowingData.enabled) {
            createGlowing(glowingData);
         } else {
            removeGlowing(glowingData);
         }

      }

      public static void setMetadata(Player player, int entityId, byte flags, boolean ignore) throws ReflectiveOperationException {
         List<Object> dataItems = new ArrayList<>(1);
         dataItems.add(watcherItemConstructor != null ? watcherItemConstructor.newInstance(watcherObjectFlags, flags) : watcherBCreator.invoke(null, watcherObjectFlags, flags));
         Object packetMetadata;
         if (version.isBefore(1, 19, 3)) {
            packetMetadata = packetMetadataConstructor.newInstance(entityId, watcherDummy, false);
            packetMetadataItems.set(packetMetadata, dataItems);
         } else {
            packetMetadata = packetMetadataConstructor.newInstance(entityId, dataItems);
         }

         if (ignore) {
            packets.put(packetMetadata, dummy);
         }

         sendPackets(player, packetMetadata);
      }

      public static void setSlimeSize(Player player, int entityId, int size) throws ReflectiveOperationException {
         try {
            Object dataAccessor;
            if (watcherSerializerObject != null) {
               Object serializer = Class.forName("net.minecraft.network.syncher.EntityDataSerializers").getField("INT").get(null);
               dataAccessor = watcherSerializerObject.invoke(serializer, 16);
            } else {
               Class<?> serializerClass = Class.forName("net.minecraft.network.syncher.EntityDataSerializer");
               Class<?> accessorClass = Class.forName("net.minecraft.network.syncher.EntityDataAccessor");
               Object intSerializer = Class.forName("net.minecraft.network.syncher.EntityDataSerializers").getField("INT").get(null);
               Constructor<?> accessorConstructor = accessorClass.getDeclaredConstructor(Integer.TYPE, serializerClass);
               accessorConstructor.setAccessible(true);
               dataAccessor = accessorConstructor.newInstance(16, intSerializer);
            }

            List<Object> dataItems = new ArrayList<>(1);
            dataItems.add(watcherItemConstructor != null ? watcherItemConstructor.newInstance(dataAccessor, size) : watcherBCreator.invoke(null, dataAccessor, size));
            Object packetMetadata;
            if (version.isBefore(1, 19, 3)) {
               packetMetadata = packetMetadataConstructor.newInstance(entityId, watcherDummy, false);
               packetMetadataItems.set(packetMetadata, dataItems);
            } else {
               packetMetadata = packetMetadataConstructor.newInstance(entityId, dataItems);
            }

            packets.put(packetMetadata, dummy);
            sendPackets(player, packetMetadata);
         } catch (Exception var8) {
            logger.log(Level.WARNING, "Failed to set slime size", var8);
         }

      }

      public static void setItemDisplayData(Player player, int entityId) throws ReflectiveOperationException {
         try {
            Class<?> craftItemStackClass = getCraftClass("inventory", "CraftItemStack");
            ItemStack bukkitStack = new ItemStack(Material.AIR, 1);
            Method asNMSCopyMethod = craftItemStackClass.getDeclaredMethod("asNMSCopy", ItemStack.class);
            Object nmsItemStack = asNMSCopyMethod.invoke(null, bukkitStack);
            Object dataAccessor;
            if (watcherSerializerObject != null) {
               Object serializer = Class.forName("net.minecraft.network.syncher.EntityDataSerializers").getField("ITEM_STACK").get(null);
               dataAccessor = watcherSerializerObject.invoke(serializer, 23);
            } else {
               Class<?> serializerClass = Class.forName("net.minecraft.network.syncher.EntityDataSerializer");
               Class<?> accessorClass = Class.forName("net.minecraft.network.syncher.EntityDataAccessor");
               Object itemStackSerializer = Class.forName("net.minecraft.network.syncher.EntityDataSerializers").getField("ITEM_STACK").get(null);
               Constructor<?> accessorConstructor = accessorClass.getDeclaredConstructor(Integer.TYPE, serializerClass);
               accessorConstructor.setAccessible(true);
               dataAccessor = accessorConstructor.newInstance(23, itemStackSerializer);
            }

            List<Object> dataItems = new ArrayList<>(1);
            dataItems.add(watcherItemConstructor != null ? watcherItemConstructor.newInstance(watcherObjectFlags, (byte)96) : watcherBCreator.invoke(null, watcherObjectFlags, (byte)96));
            Object packetMetadata;
            if (version.isBefore(1, 19, 3)) {
               packetMetadata = packetMetadataConstructor.newInstance(entityId, watcherDummy, false);
               packetMetadataItems.set(packetMetadata, dataItems);
            } else {
               packetMetadata = packetMetadataConstructor.newInstance(entityId, dataItems);
            }

            packets.put(packetMetadata, dummy);
            sendPackets(player, packetMetadata);
         } catch (Exception var12) {
            logger.log(Level.WARNING, "Failed to set item display invisible+glowing", var12);
         }

      }

      public static void setGlowingColor(GlowingEntities.GlowingData glowingData) throws ReflectiveOperationException {
         boolean sendCreation = false;
         if (glowingData.player.sentColors == null) {
            glowingData.player.sentColors = EnumSet.of(glowingData.color);
            sendCreation = true;
         } else if (glowingData.player.sentColors.add(glowingData.color)) {
            sendCreation = true;
         }

         GlowingEntities.Packets.TeamData teamData = teams.computeIfAbsent(glowingData.color, (color) -> {
            try {
               return new GlowingEntities.Packets.TeamData(glowingData.player.instance.uid, color);
            } catch (ReflectiveOperationException e) {
               throw new RuntimeException(e);
            }
         });

         Object entityAddPacket = teamData.getEntityAddPacket(glowingData.teamID);
         if (sendCreation) {
            sendPackets(glowingData.player.player, teamData.creationPacket, entityAddPacket);
         } else {
            sendPackets(glowingData.player.player, entityAddPacket);
         }
      }

      public static void removeGlowingColor(GlowingEntities.GlowingData glowingData) throws ReflectiveOperationException {
         GlowingEntities.Packets.TeamData teamData = teams.get(glowingData.color);
         if (teamData != null) {
            sendPackets(glowingData.player.player, teamData.getEntityRemovePacket(glowingData.teamID));
         }
      }

      public static void createEntity(Player player, int entityId, UUID entityUuid, Object entityType, Location location) throws IllegalArgumentException, ReflectiveOperationException {
         Object packet;
         if (version.isAfter(1, 19, 0)) {
            packet = packetAddEntity.newInstance(entityId, entityUuid, location.getX(), location.getY(), location.getZ(), location.getPitch(), location.getYaw(), entityType, 0, vec3dZero, 0.0D);
         } else {
            packet = packetAddEntity.newInstance(entityId, entityUuid, location.getX(), location.getY(), location.getZ(), location.getPitch(), location.getYaw(), entityType, 0, vec3dZero);
         }

         sendPackets(player, packet);
      }

      public static void removeEntities(Player player, int... entitiesId) throws ReflectiveOperationException {
         Object[] packets;
         if (version.is(1, 17, 0)) {
            packets = new Object[entitiesId.length];

            for(int i = 0; i < entitiesId.length; ++i) {
               packets[i] = packetRemove.newInstance(entitiesId[i]);
            }
         } else {
            packets = new Object[]{packetRemove.newInstance(entitiesId)};
         }

         sendPackets(player, packets);
      }

      public static void teleportEntity(Player player, int entityId, Location location) throws ReflectiveOperationException {
         if (packetTeleport == null) {
            return;
         }

         Object packet;
         if (positionMoveRotationConstructor != null) {
            Object position = vec3dZero.getClass().getConstructor(Double.TYPE, Double.TYPE, Double.TYPE).newInstance(location.getX(), location.getY(), location.getZ());
            Object positionMoveRotation = positionMoveRotationConstructor.newInstance(position, vec3dZero, location.getYaw(), location.getPitch());
            packet = packetTeleport.newInstance(entityId, positionMoveRotation);
         } else {
            packet = packetTeleport.newInstance(entityId, location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch(), true);
         }
         sendPackets(player, packet);
      }

      private static Channel getChannel(Player player) throws ReflectiveOperationException {
         return (Channel)channelField.get(networkManager.get(playerConnection.get(getHandle.invoke(player))));
      }

      public static void addPacketsHandler(GlowingEntities.PlayerData playerData) throws ReflectiveOperationException {
         playerData.packetsHandler = new ChannelDuplexHandler() {
            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
               if (msg.getClass().equals(GlowingEntities.Packets.packetMetadata.getClassInstance()) && GlowingEntities.Packets.packets.asMap().remove(msg) == null) {
                  int entityID = GlowingEntities.Packets.packetMetadataEntity.getInt(msg);
                  GlowingEntities.GlowingData glowingData = playerData.glowingDatas.get(entityID);
                  if (glowingData != null) {
                     List<Object> items = (List<Object>) GlowingEntities.Packets.packetMetadataItems.get(msg);
                     if (items != null) {
                        boolean containsFlags = false;
                        boolean edited = false;

                        for(int i = 0; i < items.size(); ++i) {
                           Object item = items.get(i);
                           Object watcherObject;
                           if (GlowingEntities.Packets.watcherItemObject != null) {
                              watcherObject = GlowingEntities.Packets.watcherItemObject.invoke(item);
                           } else {
                              Object serializer = GlowingEntities.Packets.watcherBSerializer.invoke(item);
                              watcherObject = GlowingEntities.Packets.watcherSerializerObject.invoke(serializer, GlowingEntities.Packets.watcherBId.invoke(item));
                           }

                           if (watcherObject.equals(GlowingEntities.Packets.watcherObjectFlags)) {
                              containsFlags = true;
                              byte originalFlags = (Byte)GlowingEntities.Packets.watcherItemDataGet.invoke(item);
                              byte flags = originalFlags;
                              if (glowingData.hideCustomName) {
                                 flags &= ~8;
                              }

                              glowingData.otherFlags = flags;
                              byte newFlags = GlowingEntities.Packets.computeFlags(glowingData);
                              if (newFlags != originalFlags) {
                                 edited = true;
                                 items = new ArrayList<>(items);
                                 items.set(i, GlowingEntities.Packets.createFlagWatcherItem(newFlags));
                                 break;
                              }
                           }
                        }

                        if (!edited && !containsFlags) {
                           byte flags = GlowingEntities.Packets.computeFlags(glowingData);
                           if (flags != 0) {
                              edited = true;
                              items = new ArrayList<>(items);
                              items.add(GlowingEntities.Packets.createFlagWatcherItem(flags));
                           }
                        }

                        if (edited) {
                           Object newMsg;
                           if (GlowingEntities.Packets.version.isBefore(1, 19, 3)) {
                              newMsg = GlowingEntities.Packets.packetMetadataConstructor.newInstance(entityID, GlowingEntities.Packets.watcherDummy, false);
                              GlowingEntities.Packets.packetMetadataItems.set(newMsg, items);
                           } else {
                              newMsg = GlowingEntities.Packets.packetMetadataConstructor.newInstance(entityID, items);
                           }

                           GlowingEntities.Packets.packets.put(newMsg, GlowingEntities.Packets.dummy);
                           GlowingEntities.Packets.sendPackets(playerData.player, newMsg);
                           return;
                        }
                     }
                  }
               } else if (GlowingEntities.Packets.packetBundle != null && GlowingEntities.Packets.packetBundle.getClassInstance().isInstance(msg)) {
                  this.handlePacketBundle(msg);
               }

               super.write(ctx, msg, promise);
            }

            private void handlePacketBundle(Object bundle) throws ReflectiveOperationException {
               Iterable<?> subPackets = (Iterable<?>) GlowingEntities.Packets.packetBundlePackets.invoke(bundle);
               for (Object packet : subPackets) {
                  if (packet.getClass().equals(GlowingEntities.Packets.packetMetadata.getClassInstance())) {
                     int entityID = GlowingEntities.Packets.packetMetadataEntity.getInt(packet);
                     GlowingEntities.GlowingData glowingData = playerData.glowingDatas.get(entityID);
                     if (glowingData != null) {
                        Bukkit.getScheduler().runTaskLaterAsynchronously(playerData.instance.plugin, () -> {
                           try {
                              GlowingEntities.Packets.updateGlowingState(glowingData);
                           } catch (ReflectiveOperationException var2) {
                              var2.printStackTrace();
                           }

                        }, 1L);
                        return;
                     }
                  }
               }

            }
         };
         getChannel(playerData.player).pipeline().addBefore("packet_handler", null, playerData.packetsHandler);
      }

      public static void removePacketsHandler(GlowingEntities.PlayerData playerData) throws ReflectiveOperationException {
         if (playerData.packetsHandler != null) {
            getChannel(playerData.player).pipeline().remove(playerData.packetsHandler);
         }

      }

      private static Class<?> getCraftClass(String craftPackage, String className) throws ClassNotFoundException {
         return Class.forName(cpack + "." + (craftPackage.isBlank() ? className : craftPackage + "." + className));
      }

      @NotNull
      private static ReflectionAccessor.ClassAccessor getNMSClass(@NotNull ReflectionAccessor reflection, @NotNull String className) throws ClassNotFoundException {
         return reflection.getClass("net.minecraft." + className);
      }

      @NotNull
      private static ReflectionAccessor.ClassAccessor getNMSClass(@NotNull ReflectionAccessor reflection, @NotNull String nmPackage, @NotNull String className) throws ClassNotFoundException {
         return reflection.getClass("net.minecraft." + nmPackage + "." + className);
      }

      static {
         packets = CacheBuilder.newBuilder().expireAfterWrite(5L, TimeUnit.SECONDS).build();
         dummy = new Object();
         isEnabled = false;
         hasInitialized = false;
         initializationError = null;
         teams = new EnumMap<>(ChatColor.class);
      }

      private static class TeamData {
         private final String id;
         private final Object creationPacket;
         private final Cache<String, Object> addPackets;
         private final Cache<String, Object> removePackets;

         public TeamData(int uid, ChatColor color) throws ReflectiveOperationException {
            this.addPackets = CacheBuilder.newBuilder().expireAfterAccess(3L, TimeUnit.MINUTES).build();
            this.removePackets = CacheBuilder.newBuilder().expireAfterAccess(3L, TimeUnit.MINUTES).build();
            if (!color.isColor()) {
               throw new IllegalArgumentException();
            } else {
               this.id = "glow-" + uid + color.getChar();
               Object team = GlowingEntities.Packets.createTeam.newInstance(GlowingEntities.Packets.scoreboardDummy, this.id);
               GlowingEntities.Packets.setTeamPush.invoke(team, GlowingEntities.Packets.pushNever);
               GlowingEntities.Packets.setTeamColor.invoke(team, GlowingEntities.Packets.getColorConstant.invoke(null, color.getChar()));
               GlowingEntities.Packets.setTeamNameTagVisibility.invoke(team, GlowingEntities.Packets.visibilityNever);
               Object packetData = GlowingEntities.Packets.createTeamPacketData.newInstance(team);
               this.creationPacket = GlowingEntities.Packets.createTeamPacket.newInstance(this.id, 0, Optional.of(packetData), Collections.EMPTY_LIST);
            }
         }

         public Object getEntityAddPacket(String teamID) throws ReflectiveOperationException {
            try {
               return this.addPackets.get(teamID, () -> GlowingEntities.Packets.createTeamPacket.newInstance(this.id, 3, Optional.empty(), Arrays.asList(teamID)));
            } catch (ExecutionException e) {
               throw new ReflectiveOperationException(e);
            }
         }

         public Object getEntityRemovePacket(String teamID) throws ReflectiveOperationException {
            try {
               return this.removePackets.get(teamID, () -> GlowingEntities.Packets.createTeamPacket.newInstance(this.id, 4, Optional.empty(), Arrays.asList(teamID)));
            } catch (ExecutionException e) {
               throw new ReflectiveOperationException(e);
            }
         }
      }
   }

   private static class PlayerData {
      final GlowingEntities instance;
      final Player player;
      final Map<Integer, GlowingEntities.GlowingData> glowingDatas;
      ChannelHandler packetsHandler;
      EnumSet<ChatColor> sentColors;

      PlayerData(GlowingEntities instance, Player player) {
         this.instance = instance;
         this.player = player;
         this.glowingDatas = new HashMap<>();
      }
   }

   private static class GlowingData {
      final GlowingEntities.PlayerData player;
      final int entityID;
      final String teamID;
      ChatColor color;
      byte otherFlags;
      boolean enabled;
      boolean hideCustomName;

      GlowingData(GlowingEntities.PlayerData player, int entityID, String teamID, ChatColor color, byte otherFlags) {
         this(player, entityID, teamID, color, otherFlags, false);
      }

      GlowingData(GlowingEntities.PlayerData player, int entityID, String teamID, ChatColor color, byte otherFlags, boolean hideCustomName) {
         this.player = player;
         this.entityID = entityID;
         this.teamID = teamID;
         this.color = color;
         this.otherFlags = otherFlags;
         this.enabled = true;
         this.hideCustomName = hideCustomName;
      }
   }
}
