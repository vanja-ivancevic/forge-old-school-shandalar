/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package forge.util;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * Provides access to information about the current version and build ID.
 */
public class BuildInfo {
    private static Date timestamp = null;
    private static String modVersion = null; // Cache the loaded version

    // disable instantiation
    private BuildInfo() {
    }

    /**
     * Get the current version of Forge.
     *
     * @return a String representing the version specifier, or "GIT" if unknown.
     */
    public static String getVersionString() {
        String version = BuildInfo.class.getPackage().getImplementationVersion();
        if (StringUtils.isEmpty(version)) {
            return "GIT";
        }
        return version;
    }

    /**
     * Get the specific version of the Old-School Shandalar mod from properties.
     *
     * @return a String representing the mod version, or "UNKNOWN" if not found/loaded.
     */
    public static String getModVersionString() {
        if (modVersion == null) {
            loadModProperties();
        }
        return modVersion != null ? modVersion : "UNKNOWN";
    }

    private static synchronized void loadModProperties() {
        if (modVersion != null) return; // Already loaded

        Properties props = new Properties();
        try (InputStream input = BuildInfo.class.getResourceAsStream("/mod.properties")) {
            if (input == null) {
                System.err.println("BuildInfo: Unable to find mod.properties file.");
                modVersion = "ERROR"; // Indicate loading failure
                return;
            }
            props.load(input);
            modVersion = props.getProperty("mod.version", "UNKNOWN");
            // Handle case where property might still be the placeholder if filtering failed
            if (modVersion.contains("${")) {
                System.err.println("BuildInfo: mod.version property was not filtered by Maven.");
                modVersion = "FILTER_FAIL";
            }
        } catch (IOException ex) {
            System.err.println("BuildInfo: Error loading mod.properties file: " + ex.getMessage());
            modVersion = "IO_ERROR"; // Indicate loading failure
        }
    }

    public static boolean isDevelopmentVersion() {
        String forgeVersion = getVersionString();
        return StringUtils.containsIgnoreCase(forgeVersion, "git") ||
                StringUtils.containsIgnoreCase(forgeVersion, "snapshot");
    }

    public static Date getTimestamp() {
        if (timestamp != null)
            return timestamp;
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            InputStream inputStream = BuildInfo.class.getResourceAsStream("/build.txt");
            String data = readFromInputStream(inputStream);
            timestamp = simpleDateFormat.parse(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return timestamp;
    }

    public static boolean verifyTimestamp(Date updateTimestamp) {
        if (updateTimestamp == null)
            return false;
        if (getTimestamp() == null)
            return false;
        // System.err.println("Update Timestamp: " + updateTimestamp + "\nBuild Timestamp: " + getTimestamp());
        // if morethan 23 hours the difference, then allow to update.
        return DateUtil.getElapsedHours(getTimestamp(), updateTimestamp) > 23;
    }

    public static String getUserAgent() {
        return "Forge/" + getVersionString();
    }

    private static String readFromInputStream(InputStream inputStream) throws IOException {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }
}
