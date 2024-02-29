package me.fisch37.betterresourcepack;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.kohsuke.github.GHAsset;
import org.kohsuke.github.GHRelease;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.net.URL;

public class GithubUpdateTask extends BukkitRunnable {
    private final BetterServerResourcepack plugin;
    private final PackInfo packInfo;

    GithubUpdateTask(BetterServerResourcepack plugin, PackInfo packInfo) {
        super();
        this.plugin = plugin;
        this.packInfo = packInfo;
    }

    private String getLatestRelease() {
        try {
            GitHub gitHub = GitHub.connectAnonymously();

            GHRepository repository = gitHub.getRepository("%s/%s".formatted(this.plugin.getConfig().getString("github.username"), this.plugin.getConfig().getString("github.repository")));
            GHRelease release = repository.getLatestRelease();

            GHAsset latestAsset = release.listAssets().toList().get(0);

            if (latestAsset != null)
                return latestAsset.getBrowserDownloadUrl();

            return null;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Runs this operation.
     */
    @Override
    public void run() {
        try {
            final String downloadLink = getLatestRelease();
            if (downloadLink == null)
                return;

            final URL url = new URL(downloadLink);

            this.packInfo.setUrl(url);
            this.packInfo.saveURL();
        } catch (java.net.MalformedURLException e){
            return;
        }

        if (this.packInfo.getUrl() == null)
            return;

        new ReloadPackTask(
            this.plugin,
            Bukkit.getConsoleSender(),
            this.packInfo,
            false,
            true
        ).start();
    }
}
