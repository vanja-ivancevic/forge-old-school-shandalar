package forge.download;

/**
 * Holds the result of an update check.
 */
public record UpdateInfo(
    boolean checkSuccessful,
    boolean isUpdateAvailable,
    String currentVersion,
    String latestVersion
) {
    /**
     * Creates an UpdateInfo instance indicating a failed check.
     * @param currentVersion The current version detected.
     * @return UpdateInfo instance for a failed check.
     */
    public static UpdateInfo failed(String currentVersion) {
        return new UpdateInfo(false, false, currentVersion, null);
    }

    /**
     * Creates an UpdateInfo instance indicating no update is available.
     * @param currentVersion The current version detected.
     * @return UpdateInfo instance for no update available.
     */
    public static UpdateInfo upToDate(String currentVersion) {
        return new UpdateInfo(true, false, currentVersion, currentVersion);
    }

    /**
     * Creates an UpdateInfo instance indicating an update is available.
     * @param currentVersion The current version detected.
     * @param latestVersion The latest version found.
     * @return UpdateInfo instance for an available update.
     */
    public static UpdateInfo available(String currentVersion, String latestVersion) {
        return new UpdateInfo(true, true, currentVersion, latestVersion);
    }
}