package fr.skytasul.glowingentities.data;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

public class GlowingBlockData {
   private final Location location;
   private final Material blockType;
   private ChatColor color;
   private boolean animated;
   private boolean global;
   private String ownerUUID;
   private long createdTime;

   public GlowingBlockData(@NotNull Location location, @NotNull Material blockType, @NotNull ChatColor color, boolean animated, boolean global, @NotNull String ownerUUID) {
      this.location = location;
      this.blockType = blockType;
      this.color = color;
      this.animated = animated;
      this.global = global;
      this.ownerUUID = ownerUUID;
      this.createdTime = System.currentTimeMillis();
   }

   public GlowingBlockData(@NotNull Location location, @NotNull Material blockType, @NotNull ChatColor color, boolean animated, boolean global, @NotNull String ownerUUID, long createdTime) {
      this.location = location;
      this.blockType = blockType;
      this.color = color;
      this.animated = animated;
      this.global = global;
      this.ownerUUID = ownerUUID;
      this.createdTime = createdTime;
   }

   public Location getLocation() {
      return this.location;
   }

   public Material getBlockType() {
      return this.blockType;
   }

   public ChatColor getColor() {
      return this.color;
   }

   public void setColor(ChatColor color) {
      this.color = color;
   }

   public boolean isAnimated() {
      return this.animated;
   }

   public void setAnimated(boolean animated) {
      this.animated = animated;
   }

   public boolean isGlobal() {
      return this.global;
   }

   public void setGlobal(boolean global) {
      this.global = global;
   }

   public String getOwnerUUID() {
      return this.ownerUUID;
   }

   public long getCreatedTime() {
      return this.createdTime;
   }

   public String getLocationString() {
      String var10000 = this.location.getWorld().getName();
      return var10000 + "," + this.location.getBlockX() + "," + this.location.getBlockY() + "," + this.location.getBlockZ();
   }
}
