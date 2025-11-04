package fr.skytasul.glowingentities;

import fr.skytasul.glowingentities.animation.RainbowAnimator;
import fr.skytasul.glowingentities.citizens.CitizensIntegration;
import fr.skytasul.glowingentities.data.DataManager;
import fr.skytasul.glowingentities.listeners.PlayerJoinListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class GlowingEntitiesPlugin extends JavaPlugin {
   private GlowingBlocks glowingBlocks;
   private CommandHandler commandHandler;
   private DataManager dataManager;
   private RainbowAnimator rainbowAnimator;
   private CitizensIntegration citizensIntegration;
   private CommandTabCompleter tabCompleter;

   public void onEnable() {
      try {
         this.saveDefaultConfig();
         this.dataManager = new DataManager(this);
         this.glowingBlocks = new GlowingBlocks(this);
         this.citizensIntegration = new CitizensIntegration(this);
         this.citizensIntegration.setGlowingEntities(this.glowingBlocks.getGlowingEntities());
         this.citizensIntegration.initialize();
         this.rainbowAnimator = new RainbowAnimator(this, this.citizensIntegration);
         long rainbowInterval = this.getConfig().getLong("rainbow.interval-ticks", 20L);
         if (this.getConfig().getBoolean("rainbow.enabled", true)) {
            this.rainbowAnimator.start(rainbowInterval);
         }
         this.commandHandler = new CommandHandler(this, this.glowingBlocks, this.dataManager, this.rainbowAnimator, this.citizensIntegration);
         this.tabCompleter = new CommandTabCompleter(this.citizensIntegration);
         this.getCommand("glowblock").setTabCompleter(this.tabCompleter);
         this.getCommand("unglowblock").setTabCompleter(this.tabCompleter);
         this.getCommand("glownpc").setTabCompleter(this.tabCompleter);
         this.getCommand("unglownpc").setTabCompleter(this.tabCompleter);
         this.getCommand("glowingentities").setTabCompleter(this.tabCompleter);
         PlayerJoinListener joinListener = new PlayerJoinListener(this, this.glowingBlocks, this.dataManager, this.citizensIntegration);
         this.getServer().getPluginManager().registerEvents(joinListener, this);
         this.commandHandler.loadAllSavedBlocks();
         this.getLogger().info("GlowingEntities has been enabled!");
         this.getLogger().info("Version: " + this.getDescription().getVersion());
         this.getLogger().info("Loaded " + this.dataManager.getAllBlocks().size() + " saved glowing blocks");
         if (this.isDebugEnabled()) {
            this.getLogger().warning("Debug mode is ENABLED - logs will be verbose!");
         }
      } catch (Exception var2) {
         this.getLogger().severe("Failed to initialize GlowingEntities!");
         var2.printStackTrace();
         this.getServer().getPluginManager().disablePlugin(this);
      }

   }

   public void onDisable() {
      if (this.rainbowAnimator != null) {
         this.rainbowAnimator.stop();
      }

      if (this.citizensIntegration != null) {
         this.citizensIntegration.clearAll();
      }

      if (this.dataManager != null) {
         this.dataManager.save();
      }

      if (this.glowingBlocks != null) {
         this.glowingBlocks.disable();
      }

      this.getLogger().info("GlowingEntities has been disabled!");
   }

   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      return this.commandHandler.handleCommand(sender, command, label, args);
   }

   public GlowingBlocks getGlowingBlocks() {
      return this.glowingBlocks;
   }

   public DataManager getDataManager() {
      return this.dataManager;
   }

   public RainbowAnimator getRainbowAnimator() {
      return this.rainbowAnimator;
   }

   public CitizensIntegration getCitizensIntegration() {
      return this.citizensIntegration;
   }

   public boolean isDebugEnabled() {
      return this.getConfig().getBoolean("debug.enabled", false);
   }
}
