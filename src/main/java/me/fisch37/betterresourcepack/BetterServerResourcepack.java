package me.fisch37.betterresourcepack;

import me.fisch37.betterresourcepack.commands.PackCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.*;
import java.net.MalformedURLException;
import java.util.HexFormat;

public final class BetterServerResourcepack extends JavaPlugin {
    private final static String hashCacheName = "pack.sha1";
    private PackInfo packInfo;
    private BukkitTask githubTask = null;

    private final static java.util.logging.Logger logger = Bukkit.getLogger();

    @Override
    public void onEnable() {
        _CompatRenameConfig();
        // Plugin startup logic
        saveDefaultConfig();
        boolean forceHashUpdate = this.getConfig().getBoolean("force-hash-update-on-start");
        File hashCache = new File(getDataFolder(), hashCacheName);
        boolean shouldUpdateHash = forceHashUpdate || !hashCache.exists();

        boolean loadSuccessful = this.loadPackInfo(shouldUpdateHash,hashCache);
        // Cache Save/Loading
        if (loadSuccessful && this.packInfo.isConfigured()) {
            if (shouldUpdateHash) {
                try {
                    this.packInfo.saveHash();
                } catch (IOException e) {
                    logger.warning("[BSP] Could not update cached hash!");
                }
            } else this.readFromCache(hashCache);
        }

        getServer().getPluginManager().registerEvents(new JoinHandler(this.packInfo),this);
        PluginCommand command = getCommand("pack");
        if (command == null){
            logger.severe("[BSP] Could not find command /pack. Cancelling activation");
            return;
        }
        TabExecutor executor = new PackCommand(this.packInfo,this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);

        enableUpdateTask();
    }

    private void readFromCache(File cache){
        try (BufferedReader hashReader = new BufferedReader(new FileReader(cache))){
            String hexHash = hashReader.readLine();
            this.packInfo.setSha1(HexFormat.of().parseHex(hexHash));
        } catch (java.io.FileNotFoundException e){
            // I have done stuff like this in other parts of this plugin as well.
            // Basically: We don't ever want this error to occur.
            // => If it occurs, it's real bad and should appear as a full-blown error.
            throw new RuntimeException(e);
        } catch (IOException e){
            logger.severe("Could not load cached hash! The plugin will not work unless you reload it.");
        }
    }

    private boolean loadPackInfo(boolean forceUpdate, File cache){
        try{
            this.packInfo = new PackInfo(this,forceUpdate,cache);
            return true;
        } catch (MalformedURLException e){
            logger.warning("[BSP] Resourcepack URL has an invalid format");
        }
        return false;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        disableUpdateTask();
    }

    private void enableUpdateTask() {
        final boolean shouldRun = getConfig().getBoolean("github.enabled", false);
        if (!shouldRun)
            return;

        if (githubTask != null && githubTask.isCancelled())
            return;

        final long interval = getConfig().getLong("github.interval", 600L);
        final long delay = 2400L; // Wait 2 minutes after startup before running first
        final long period = 20L * interval;

        githubTask = new GithubUpdateTask(this, packInfo).runTaskTimer(this, delay, period);
    }

    private void disableUpdateTask() {
        if (githubTask == null)
            return;

        if (githubTask.isCancelled())
            return;

        githubTask.cancel();
    }

    /*
     * Compatibility method.
     * Renames an existing config.yaml to config.yml if config.yml doesn't exist.
     */
    private void _CompatRenameConfig(){
        File old_config_loc = new File(getDataFolder(), "config.yaml");
        File new_config_loc = new File(getDataFolder(), "config.yml");
        boolean renameSuccess = false;
        logger.info("[BSP] Compat 0.1->0.2: Attempting to rename config.");
        try {
            if (old_config_loc.exists() && !new_config_loc.exists())
                renameSuccess = old_config_loc.renameTo(new_config_loc);
        } catch (SecurityException e){}
        if (!renameSuccess)
            logger.warning("[BSP] Compat: Could not rename config. ALl your configuration has been reset!");
    }
}
