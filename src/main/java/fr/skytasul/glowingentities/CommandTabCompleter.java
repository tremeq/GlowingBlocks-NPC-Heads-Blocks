package fr.skytasul.glowingentities;

import fr.skytasul.glowingentities.citizens.CitizensIntegration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CommandTabCompleter implements TabCompleter {
   private final CitizensIntegration citizensIntegration;
   private static final List<String> COLORS = Arrays.asList("RED", "BLUE", "GREEN", "YELLOW", "AQUA", "GOLD", "DARK_RED", "DARK_BLUE", "DARK_GREEN", "DARK_AQUA", "DARK_PURPLE", "LIGHT_PURPLE", "BLACK", "DARK_GRAY", "GRAY", "WHITE", "RAINBOW");
   private static final List<String> VISIBILITY_OPTIONS = Arrays.asList("global", "private");
   private static final List<String> MAIN_SUBCOMMANDS = Arrays.asList("help", "version", "reload", "save", "list");

   public CommandTabCompleter(CitizensIntegration citizensIntegration) {
      this.citizensIntegration = citizensIntegration;
   }

   @Nullable
   public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
      String cmdName = command.getName().toLowerCase();
      switch (cmdName) {
         case "glowblock":
            return this.handleGlowBlockTab(args);
         case "unglowblock":
            return new ArrayList<>();
         case "glownpc":
            return this.handleGlowNPCTab(args);
         case "unglownpc":
            return this.handleUnglowNPCTab(args);
         case "glowingentities":
            return this.handleMainCommandTab(args);
         default:
            return null;
      }
   }

   private List<String> handleGlowBlockTab(String[] args) {
      if (args.length == 1) {
         return this.filterMatches(COLORS, args[0]);
      }
      if (args.length == 2) {
         return this.filterMatches(VISIBILITY_OPTIONS, args[1]);
      }
      return new ArrayList<>();
   }

   private List<String> handleMainCommandTab(String[] args) {
      if (args.length == 1) {
         return this.filterMatches(MAIN_SUBCOMMANDS, args[0]);
      }
      return new ArrayList<>();
   }

   private List<String> handleGlowNPCTab(String[] args) {
      if (!this.citizensIntegration.isEnabled()) {
         return new ArrayList<>();
      }
      if (args.length == 1) {
         List<String> npcNames = new ArrayList<>();
         for (NPC npc : CitizensAPI.getNPCRegistry()) {
            npcNames.add(npc.getName());
            npcNames.add(String.valueOf(npc.getId()));
         }
         return this.filterMatches(npcNames, args[0]);
      }
      if (args.length == 2) {
         return this.filterMatches(COLORS, args[1]);
      }
      return new ArrayList<>();
   }

   private List<String> handleUnglowNPCTab(String[] args) {
      if (!this.citizensIntegration.isEnabled()) {
         return new ArrayList<>();
      }
      if (args.length == 1) {
         List<String> glowingNPCs = new ArrayList<>();
         for (NPC npc : CitizensAPI.getNPCRegistry()) {
            if (this.citizensIntegration.isNPCGlowing(npc.getId())) {
               glowingNPCs.add(npc.getName());
               glowingNPCs.add(String.valueOf(npc.getId()));
            }
         }
         return this.filterMatches(glowingNPCs, args[0]);
      }
      return new ArrayList<>();
   }

   private List<String> filterMatches(List<String> options, String input) {
      String inputLower = input.toLowerCase();
      return options.stream()
              .filter(option -> option.toLowerCase().startsWith(inputLower))
              .sorted()
              .collect(Collectors.toList());
   }
}
