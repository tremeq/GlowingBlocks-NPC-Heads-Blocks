package fr.skytasul.glowingentities.citizens;

import org.bukkit.ChatColor;

public class NPCGlowData {
   private final int npcId;
   private final String npcName;
   private ChatColor color;
   private final boolean animated;
   private final boolean global;

   public NPCGlowData(int npcId, String npcName, ChatColor color, boolean animated, boolean global) {
      this.npcId = npcId;
      this.npcName = npcName;
      this.color = color;
      this.animated = animated;
      this.global = global;
   }

   public int getNpcId() {
      return this.npcId;
   }

   public String getNpcName() {
      return this.npcName;
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

   public boolean isGlobal() {
      return this.global;
   }
}
