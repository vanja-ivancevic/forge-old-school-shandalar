package forge.download;

import com.google.common.collect.ImmutableList;
import forge.gui.GuiBase;
import forge.gui.download.GuiDownloadZipService;
import forge.gui.util.SOptionPane;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.util.*;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static forge.localinstance.properties.ForgeConstants.GITHUB_SNAPSHOT_URL;
import static forge.localinstance.properties.ForgeConstants.RELEASE_URL;

public class AutoUpdater {
    private static final boolean VERSION_FROM_METADATA = true;
    private static final Localizer localizer = Localizer.getInstance();

    public static String[] updateChannels = new String[]{ "none", "snapshot", "release", "mod_release"}; // Added mod_release conceptually

    // GitHub repo details for the mod
    private static final String GITHUB_MOD_REPO_OWNER = "vanja-ivancevic"; // Replaced placeholder
    private static final String GITHUB_MOD_REPO_NAME = "forge-old-school-shandalar"; // Replaced placeholder
    private static final String GITHUB_MOD_API_LATEST_RELEASE_URL = "https://api.github.com/repos/" + GITHUB_MOD_REPO_OWNER + "/" + GITHUB_MOD_REPO_NAME + "/releases/latest";

    private final boolean isLoading;
    private String updateChannel;
    private String version;
    private final String buildVersion;
    private String versionUrlString;
    private String packageUrl;
    private String packagePath;
    private String buildDate = "";
    private String snapsBuildDate = "";

    public AutoUpdater(boolean loading) {
        // What do I need? Preferences? Splashscreen? UI? Skins?
        isLoading = loading;
        // Read preference initially (though we will override it)
        updateChannel = FModel.getPreferences().getPref(ForgePreferences.FPref.AUTO_UPDATE);
        // Force the update channel to always check the mod's GitHub releases for this build
        this.updateChannel = "mod_release";
        System.out.println("AutoUpdater: Forcing update channel to 'mod_release'"); // Log for debugging
        buildVersion = BuildInfo.getVersionString();
    }

    public boolean updateAvailable() {
        // TODO Check if an update is available, and add a UI element to notify the user.
        return verifyUpdateable();
    }

    public boolean attemptToUpdate(CompletableFuture<String> cf) {
        if (!verifyUpdateable()) {
            return false;
        }
        try {
            if (downloadUpdate(cf)) {
                extractAndRestart();
            }
        } catch(IOException | URISyntaxException | ExecutionException | InterruptedException e) {
            return false;
        }
        return true;
    }

    private void extractAndRestart() {
        extractUpdate();
        restartForge();
    }

    private boolean verifyUpdateable() {
        // START: Force disable auto-updater
        System.out.println("DEBUG: Auto-updater explicitly disabled in code. Skipping check.");
        return false;
        // END: Force disable auto-updater

        /* Original logic below is now unreachable due to the return statement above */
        /*
        if (buildVersion.contains("GIT")) {
            //return false;
        }

        if (isLoading) {
        */
            // TODO This doesn't work yet, because FSkin isn't loaded at the time.
            return false;
        } else if (updateChannel.equals("none")) {
            String message = localizer.getMessage("lblYouHaventSetUpdateChannel");
            List<String> options = ImmutableList.of(localizer.getMessageorUseDefault("lblCancel", "Cancel"), localizer.getMessageorUseDefault("lblRelease", "Release"), localizer.getMessageorUseDefault("lblSnapshot", "Snapshot"));
            int option = SOptionPane.showOptionDialog(message, localizer.getMessage("lblManualCheck"), null, options, 0);
            if (option < 1) {
                return false;
            }
            updateChannel = options.get(option);
        }

        // Determine version source based on channel
        if (updateChannel.equalsIgnoreCase("mod_release")) {
            // Version and package URL will be fetched directly from GitHub API later
            versionUrlString = null; // Not used for mod_release
        } else if (buildVersion.contains("SNAPSHOT")) {
            if (!updateChannel.equalsIgnoreCase(localizer.getMessageorUseDefault("lblSnapshot", "Snapshot"))) {
                System.out.println("Snapshot build versions must use snapshot update channel to work");
                return false;
            }
            versionUrlString = GITHUB_SNAPSHOT_URL + "version.txt";
        } else { // Default to release channel behavior
            if (!updateChannel.equalsIgnoreCase(localizer.getMessageorUseDefault("lblRelease", "Release"))) {
                 // Allow release channel check even if not explicitly selected, maybe remove this check?
                 // System.out.println("Release build versions must use release update channel to work");
                 // return false;
            }
             // Use official release URL if channel is 'release' or unrecognized (besides mod_release/snapshot)
            versionUrlString = RELEASE_URL + "forge/forge-gui-desktop/version.txt";
        }


        // Check the internet connection
        if (!testNetConnection()) {
            return false;
        }

        // Download appropriate version file
        return compareBuildWithLatestChannelVersion();
    }

    private boolean testNetConnection() {
        try (Socket socket = new Socket()) {
            // Check connection to GitHub API endpoint instead of cardforge.org for mod updates
            String hostToCheck = updateChannel.equalsIgnoreCase("mod_release") ? "api.github.com" : "releases.cardforge.org";
            InetSocketAddress address = new InetSocketAddress(hostToCheck, 443);
            socket.connect(address, 1000);
            return true;
        } catch (IOException e) {
            return false; // Either timeout or unreachable or failed DNS lookup.
        }
    }

    private boolean compareBuildWithLatestChannelVersion() {
        try {
            if (updateChannel.equalsIgnoreCase("mod_release")) {
                System.out.println("DEBUG: Attempting to retrieve mod version from GitHub...");
                if (!retrieveModVersionFromGitHub()) {
                    System.err.println("DEBUG: retrieveModVersionFromGitHub() returned false. Update check failed.");
                    // Explicitly show an error here? Or let it potentially fall back?
                    // For now, let's prevent fallback to avoid confusion.
                     SOptionPane.showErrorDialog("Failed to retrieve update information from the mod repository.", "Update Check Error");
                    return false; // Failed to get version from GitHub
                }
                 System.out.println("DEBUG: retrieveModVersionFromGitHub() succeeded.");
                // Version and packageUrl should now be set by retrieveModVersionFromGitHub
            } else {
                 System.out.println("DEBUG: Handling non-mod_release channel: " + updateChannel);
                // Handle snapshot and official release channels
                retrieveVersion(); // Sets version and packageUrl for snapshot/release
                if (buildVersion.contains("SNAPSHOT")) {
                    URL url = new URL(GITHUB_SNAPSHOT_URL + "build.txt");
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date snapsTimestamp = simpleDateFormat.parse(FileUtil.readFileToString(url));
                    snapsBuildDate = snapsTimestamp.toString();
                    buildDate = BuildInfo.getTimestamp().toString();
                    return BuildInfo.verifyTimestamp(snapsTimestamp);
                }
            }

            // Add logging before comparison
            System.out.println("DEBUG: Before comparison:");
            System.out.println("DEBUG:   updateChannel = " + updateChannel);
            System.out.println("DEBUG:   this.version (fetched) = " + this.version);
            System.out.println("DEBUG:   this.packageUrl = " + this.packageUrl);
            System.out.println("DEBUG:   buildVersion (local) = " + buildVersion);


            // Common version comparison logic (for mod_release and non-snapshot official release)
            if (StringUtils.isEmpty(version) ) {
                System.err.println("Could not retrieve latest version string.");
                return false;
            }

            System.out.println("Current build version: " + buildVersion);
            System.out.println("Latest available version (" + updateChannel + "): " + version);

            // Strip leading 'v' from fetched version (tag name) for comparison
            String comparableVersion = version.startsWith("v") ? version.substring(1) : version;
            if (buildVersion.equals(comparableVersion)) {
                System.out.println("Build version (" + buildVersion + ") matches latest available version (" + version + ").");
                return false;
            }
        }
        catch (Exception e) {
            System.err.println("Error comparing versions: " + e.getMessage());
            e.printStackTrace(); // Keep for debugging
            SOptionPane.showOptionDialog("Error checking for updates: " + e.getMessage(), localizer.getMessage("lblError"), null, ImmutableList.of("Ok"));
            return false;
        }
        // If version doesn't match, it's assumably newer.
        System.out.println("Newer version available.");
        return true;
    }

    // New method to retrieve version and package URL from GitHub Releases API
    private boolean retrieveModVersionFromGitHub() {
        // Use the constants defined earlier which should now have the correct repo details
        System.out.println("Checking for mod updates via GitHub API: " + GITHUB_MOD_API_LATEST_RELEASE_URL);
        try {
            URL url = new URL(GITHUB_MOD_API_LATEST_RELEASE_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            // Add Authorization header if needed for private repos, but gh auth should handle public repos
            // String token = System.getenv("GITHUB_TOKEN"); // Example: Get token from env var
            // if (token != null && !token.isEmpty()) {
            //    conn.setRequestProperty("Authorization", "token " + token);
            // }


            if (conn.getResponseCode() != 200) {
                System.err.println("Failed : HTTP error code : " + conn.getResponseCode() + " " + conn.getResponseMessage());
                // Consider showing an error to the user here
                System.err.println("Failed : HTTP error code : " + conn.getResponseCode() + " " + conn.getResponseMessage());
                conn.disconnect(); // Disconnect even on error
                // Consider showing an error to the user here
                return false;
            }
            conn.disconnect(); // Disconnect after checking response code

            // Use the existing FileUtil method to read directly from the URL
            String jsonResponse = FileUtil.readFileToString(url);
            if (jsonResponse == null || jsonResponse.isEmpty()) {
                 System.err.println("Failed to read response content from GitHub API URL.");
                 return false;
            }

            // Basic JSON parsing (replace with a proper library like Gson/Jackson for robustness)
            Pattern tagPattern = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
            Matcher tagMatcher = tagPattern.matcher(jsonResponse);
            if (tagMatcher.find()) {
                this.version = tagMatcher.group(1);
            } else {
                System.err.println("Could not find 'tag_name' in GitHub API response.");
                return false;
            }

            // Find the download URL - assumes the first asset is the one we want, or look for a specific extension
            // This part needs refinement based on your release asset naming convention
            Pattern assetUrlPattern = Pattern.compile("\"browser_download_url\"\\s*:\\s*\"([^\"]+?\\.(?:jar|zip|tar\\.bz2))\""); // Example: look for .jar, .zip, or .tar.bz2
            Matcher assetUrlMatcher = assetUrlPattern.matcher(jsonResponse);
            if (assetUrlMatcher.find()) {
                this.packageUrl = assetUrlMatcher.group(1);
            } else {
                System.err.println("Could not find suitable 'browser_download_url' in GitHub API response (looking for .jar, .zip, .tar.bz2).");
                // Fallback or error? Maybe try finding *any* asset URL?
                Pattern anyAssetUrlPattern = Pattern.compile("\"browser_download_url\"\\s*:\\s*\"([^\"]+)\"");
                Matcher anyAssetUrlMatcher = anyAssetUrlPattern.matcher(jsonResponse);
                 if (anyAssetUrlMatcher.find()) {
                    this.packageUrl = anyAssetUrlMatcher.group(1);
                     System.out.println("Warning: Found a download URL, but it might not be the correct asset type: " + this.packageUrl);
                 } else {
                    System.err.println("Could not find any 'browser_download_url' in GitHub API response.");
                    return false;
                 }
            }

            System.out.println("Latest mod version from GitHub: " + this.version);
            System.out.println("Mod package URL from GitHub: " + this.packageUrl);
            return true;

        } catch (IOException e) {
            System.err.println("IOException while contacting GitHub API: " + e.getMessage());
            // Consider showing an error to the user
            return false;
        } catch (Exception e) {
            System.err.println("Unexpected error retrieving mod version from GitHub: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    private void retrieveVersion() throws MalformedURLException {
        if (updateChannel.equalsIgnoreCase("mod_release")) {
             // This case should now be handled by retrieveModVersionFromGitHub
             System.err.println("Error: retrieveVersion called for mod_release channel.");
             throw new IllegalStateException("retrieveVersion should not be called for mod_release channel.");
        }

        // Original logic for snapshot/release
        if (VERSION_FROM_METADATA && updateChannel.equalsIgnoreCase(localizer.getMessageorUseDefault("lblRelease", "Release"))) {
            extractVersionFromMavenRelease();
        } else if (versionUrlString != null) { // Check if versionUrlString is set (it won't be for mod_release initial check)
            URL versionUrl = new URL(versionUrlString);
            version = FileUtil.readFileToString(versionUrl);
        } else {
             System.err.println("Warning: retrieveVersion called with null versionUrlString and not release channel.");
             // Avoid setting version to null if it was already fetched (e.g., by mod check)
             if (this.version == null) version = ""; // Or handle as error
        }

        // Determine packageUrl based on channel (original logic)
        if (updateChannel.equalsIgnoreCase(localizer.getMessageorUseDefault("lblRelease", "Release"))) {
            packageUrl = RELEASE_URL + "forge/forge-gui-desktop/" + version + "/forge-gui-desktop-" + version + ".tar.bz2";
        } else if (updateChannel.equalsIgnoreCase(localizer.getMessageorUseDefault("lblSnapshot", "Snapshot"))) {
            packageUrl = GITHUB_SNAPSHOT_URL + "forge-installer-" + version + ".jar";
        }
        // No else needed, packageUrl for mod_release is set in retrieveModVersionFromGitHub
    }

    private void extractVersionFromMavenRelease() throws MalformedURLException {
        String RELEASE_MAVEN_METADATA = RELEASE_URL + "forge/forge-gui-desktop/maven-metadata.xml";
        URL metadataUrl = new URL(RELEASE_MAVEN_METADATA);
        String xml = FileUtil.readFileToString(metadataUrl);

        Pattern p = Pattern.compile("<release>(.*)</release>");
        Matcher m = p.matcher(xml);
        while (m.find()) {
            version = m.group(1);
        }
    }

    private boolean downloadUpdate(CompletableFuture<String> cf) throws URISyntaxException, IOException, ExecutionException, InterruptedException {
        // TODO Change the "auto" to be more auto.
        if (isLoading) {
            // We need to preload enough of a Skins to show a dialog and a button if we're in loading
            // splashScreen.prepareForDialogs();
            return downloadFromBrowser();
        }
        String logs = snapsBuildDate.isEmpty() ? "" : cf.get();
        // Use the potentially modified 'version' and 'buildVersion' for display
        String v = snapsBuildDate.isEmpty() ? version : version + TextUtil.enclosedParen(snapsBuildDate); // 'version' should hold the fetched version (e.g., v0.34.1 or 2.0.03 if failed)
        String b = buildDate.isEmpty() ? buildVersion : buildVersion + TextUtil.enclosedParen(buildDate); // 'buildVersion' is the local version (e.g., 0.34.1)

        // Add logging just before showing the dialog
        System.out.println("DEBUG: Showing update dialog:");
        System.out.println("DEBUG:   Available Version (v) = " + v);
        System.out.println("DEBUG:   Current Version (b) = " + b);
        System.out.println("DEBUG:   Message Key = lblNewVersionForgeAvailableUpdateConfirm");


        String message = localizer.getMessage("lblNewVersionForgeAvailableUpdateConfirm", v, b) + logs;
        final List<String> options = ImmutableList.of(localizer.getMessage("lblUpdateNow"), localizer.getMessage("lblUpdateLater"));
        if (SOptionPane.showOptionDialog(message, localizer.getMessage("lblNewVersionAvailable"), null, options, 0) == 0) {
            return downloadFromForge();
        }

        return false;
    }

    private boolean downloadFromBrowser() throws URISyntaxException, IOException {
        final Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            // Linking directly there will auto download, but won't auto-update
            desktop.browse(new URI(packageUrl));
            return true;
        } else {
            System.out.println("Download latest version: " + packageUrl);
            return false;
        }
    }

    private boolean downloadFromForge() {
        System.out.println("Downloading update from " + packageUrl + " to Downloads folder");
        WaitCallback<Boolean> callback = new WaitCallback<Boolean>() {
            @Override
            public void run() {
                GuiBase.getInterface().download(new GuiDownloadZipService("Auto Updater", localizer.getMessage("lblNewVersionDownloading"), packageUrl, System.getProperty("user.home") + "/Downloads/", null, null) {
                    @Override
                    public void downloadAndUnzip() {
                        // Determine filename based on packageUrl
                        String filename = packageUrl.substring(packageUrl.lastIndexOf('/') + 1);
                        // Append "-upgrade" before the extension if possible, otherwise just use filename
                        String baseName = filename;
                        String extension = "";
                        int dotIndex = filename.lastIndexOf('.');
                        if (dotIndex > 0) {
                            baseName = filename.substring(0, dotIndex);
                            extension = filename.substring(dotIndex);
                        }
                        String downloadFilename = baseName + "-upgrade" + extension;

                        packagePath = download(downloadFilename); // Use dynamic filename
                        if (packagePath != null) {
                            restartAndUpdate(packagePath);
                        }
                    }
                }, this);
            }
        };

        SwingUtilities.invokeLater(callback);

        return false; // download happens asynchronously
    }
    private void restartAndUpdate(String packagePath) {
        if (SOptionPane.showOptionDialog(localizer.getMessage("lblForgeUpdateMessage", packagePath), localizer.getMessage("lblRestart"), null, ImmutableList.of(localizer.getMessage("lblOK")), 0) == 0) {
            final Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            if (desktop != null) {
                try {
                    File installer = new File(packagePath);
                    if (installer.exists()) {
                        if (packagePath.endsWith(".jar")) {
                            installer.setExecutable(true, false);
                            desktop.open(installer);
                        } else {
                            // If it's not a JAR, just open the containing folder
                            desktop.open(installer.getParentFile());
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Update downloaded to: " + packagePath);
                System.out.println("Please manually extract/install the update.");
            }
            System.exit(0); // Exit Forge after attempting to launch installer/open folder
        }
    }
    private void extractUpdate() {
        // TODO: Implement proper extraction for .zip or .tar.bz2 if needed
        // This method is currently not called if the downloaded file is a .jar
        // If your mod release is an archive, this needs implementation.
        System.out.println("Extraction logic needed for non-JAR updates (packagePath: " + packagePath + ")");
        final Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null) {
            try {
                // Default: open the containing folder for manual extraction
                desktop.open(new File(packagePath).getParentFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Please manually extract the update from: " + packagePath);
        }
    }

    private void restartForge() {
        // This is called after downloadUpdate -> extractAndRestart
        // The actual restart happens in restartAndUpdate via System.exit(0)
        // This method might be redundant now, but keep it for structure unless refactoring further.
        if (isLoading || SOptionPane.showConfirmDialog(localizer.getMessage("lblForgeHasBeenUpdateRestartForgeToUseNewVersion"), localizer.getMessage("lblExitNowConfirm"))) {
             // System.exit(0) is already called in restartAndUpdate, so this might not be reached
             // unless restartAndUpdate fails to open the file/folder.
             System.out.println("Attempting exit after update process.");
             System.exit(0);
        }
    }
}
