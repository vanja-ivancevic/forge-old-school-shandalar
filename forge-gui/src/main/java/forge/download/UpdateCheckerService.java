package forge.download;

import forge.util.BuildInfo;
import forge.util.FileUtil;
import forge.util.Localizer;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service to check for available updates for the mod from GitHub releases.
 */
public class UpdateCheckerService {

    private static final Localizer localizer = Localizer.getInstance();

    // GitHub repo details for the mod (copied from AutoUpdater)
    private static final String GITHUB_MOD_REPO_OWNER = "vanja-ivancevic";
    private static final String GITHUB_MOD_REPO_NAME = "forge-old-school-shandalar";
    private static final String GITHUB_MOD_API_LATEST_RELEASE_URL = "https://api.github.com/repos/" + GITHUB_MOD_REPO_OWNER + "/" + GITHUB_MOD_REPO_NAME + "/releases/latest";

    private String latestFetchedVersion = null;
    private boolean checkAttempted = false;
    private boolean checkSucceeded = false;

    /**
     * Asynchronously checks for updates against the GitHub latest release.
     *
     * @return A CompletableFuture containing the UpdateInfo result.
     */
    public CompletableFuture<UpdateInfo> checkUpdateAsync() {
        return CompletableFuture.supplyAsync(() -> {
            checkAttempted = true;
            String currentVersion = BuildInfo.getVersionString();
            System.out.println("UpdateCheckerService: Starting update check. Current version: " + currentVersion);

            try {
                String fetchedVersionTag = retrieveLatestModVersionTagFromGitHub();

                if (fetchedVersionTag == null) {
                    System.err.println("UpdateCheckerService: Failed to retrieve latest version tag from GitHub.");
                    checkSucceeded = false;
                    return UpdateInfo.failed(currentVersion);
                }

                latestFetchedVersion = fetchedVersionTag; // Store the fetched tag
                checkSucceeded = true;
                System.out.println("UpdateCheckerService: Latest version tag from GitHub: " + latestFetchedVersion);

                // Compare versions
                String comparableLatestVersion = latestFetchedVersion.startsWith("v") ? latestFetchedVersion.substring(1) : latestFetchedVersion;
                String comparableCurrentVersion = currentVersion.startsWith("v") ? currentVersion.substring(1) : currentVersion; // Ensure current version is also comparable

                if (comparableCurrentVersion.equals(comparableLatestVersion)) {
                    System.out.println("UpdateCheckerService: Current version matches the latest version.");
                    return UpdateInfo.upToDate(currentVersion);
                } else {
                    // Basic check: if they are not equal, assume fetched is newer.
                    // A more robust semantic version comparison could be added here if needed.
                    System.out.println("UpdateCheckerService: Newer version available (" + latestFetchedVersion + ").");
                    return UpdateInfo.available(currentVersion, latestFetchedVersion);
                }

            } catch (Exception e) {
                System.err.println("UpdateCheckerService: Error during update check: " + e.getMessage());
                e.printStackTrace(); // Keep for debugging
                checkSucceeded = false;
                return UpdateInfo.failed(currentVersion);
            }
        });
    }

    /**
     * Retrieves the latest release tag name from the mod's GitHub repository.
     * Adapted from AutoUpdater.retrieveModVersionFromGitHub.
     *
     * @return The latest tag name (e.g., "v0.34.1") or null if an error occurs.
     */
    private String retrieveLatestModVersionTagFromGitHub() {
        System.out.println("UpdateCheckerService: Fetching from GitHub API: " + GITHUB_MOD_API_LATEST_RELEASE_URL);
        HttpURLConnection conn = null;
        try {
            URL url = new URL(GITHUB_MOD_API_LATEST_RELEASE_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            conn.setConnectTimeout(5000); // 5 second timeout
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                System.err.println("UpdateCheckerService: GitHub API request failed. HTTP error code: " + responseCode + " " + conn.getResponseMessage());
                return null;
            }

            // Use FileUtil to read the response
            String jsonResponse = FileUtil.readFileToString(url); // Re-uses the connection implicitly? Let's test. If not, read from conn.getInputStream()
            if (StringUtils.isBlank(jsonResponse)) {
                 System.err.println("UpdateCheckerService: Failed to read response content from GitHub API URL or response was empty.");
                 return null;
            }

            // Basic JSON parsing for tag_name
            Pattern tagPattern = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
            Matcher tagMatcher = tagPattern.matcher(jsonResponse);
            if (tagMatcher.find()) {
                return tagMatcher.group(1);
            } else {
                System.err.println("UpdateCheckerService: Could not find 'tag_name' in GitHub API response.");
                return null;
            }

        } catch (IOException e) {
            System.err.println("UpdateCheckerService: IOException while contacting GitHub API: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("UpdateCheckerService: Unexpected error retrieving mod version from GitHub: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    // Optional: Methods to get cached status if needed later
    public boolean hasCheckAttempted() {
        return checkAttempted;
    }

    public boolean wasCheckSuccessful() {
        return checkSucceeded;
    }

    public String getLatestFetchedVersion() {
        return latestFetchedVersion;
    }
}