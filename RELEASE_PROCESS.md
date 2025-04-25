# Forge: Shandalar - "The First 10 Years" Edition - Release Process

This document outlines the steps required to compile the project, package it, and create a new release on GitHub for this specific fork.

## Prerequisites

1.  **Maven:** Ensure Apache Maven is installed and configured correctly. You need the `mvn` command available in your terminal. (Check with `mvn -v`)
2.  **Git:** Ensure Git is installed. (Check with `git --version`)
3.  **GitHub CLI:** Ensure the GitHub CLI (`gh`) is installed and authenticated. (Check with `gh --version` and `gh auth status`)
4.  **Repository Setup:**
    *   The local repository should be cloned.
    *   A remote named `github` pointing to the `vanja-ivancevic/forge-old-school-shandalar` repository should exist (`git remote -v`).
    *   The default repository for `gh` should be set to `vanja-ivancevic/forge-old-school-shandalar` (`gh repo view`). If not, run `gh repo set-default vanja-ivancevic/forge-old-school-shandalar`.

## Release Steps

1.  **Update Version in `pom.xml`:**
    *   Open the root `pom.xml` file.
    *   Locate the `<properties>` section.
    *   Update the `<versionCode>` tag to the new release version (e.g., `0.34.1`).
    *   Ensure the `<snapshotName>` tag is empty (i.e., `<snapshotName></snapshotName>`). This defines the final version used in the build.
    *   Example for version `0.34.1`:
        ```xml
        <properties>
            ...
            <versionCode>0.34.1</versionCode>
            <!-- set snapshotName to blank for release -->
            <snapshotName></snapshotName>
            <!-- revision will be the global version string -->
            <revision>${versionCode}${snapshotName}</revision>
            ...
        </properties>
        ```
    *   Save the `pom.xml` file.

2.  **Compile and Package:**
    *   Open your terminal in the root directory of the project (`forge/`).
    *   Run the Maven package command, skipping tests to ensure a smooth release build. This cleans previous builds, compiles the code, and creates the distribution ZIP file.
    ```bash
    /Applications/apache-maven-3.9.9/bin/mvn clean package -DskipTests
    ```
    *   *Note:* Replace `/Applications/apache-maven-3.9.9/bin/mvn` with your actual path to `mvn` if it's different or if `mvn` is already in your system's PATH.
    *   Verify that the ZIP file `forge-gui-mobile-dev/target/forge-gui-mobile-dev-<version>.zip` (e.g., `forge-gui-mobile-dev-0.34.1.zip`) has been created/updated, matching the `<versionCode>` you set.

3.  **Commit Changes:**
    *   Commit the changes made (primarily `pom.xml` and any code changes included in this release).
    *   Use a descriptive commit message.
    ```bash
    # Stage the changed pom.xml and any other relevant files (e.g., code fixes)
    git add pom.xml [path/to/other/changed/files...] 

    # Commit the changes
    git commit -m "Prepare release v<version>" # Or "Fix: [brief description]" etc.
    ```
    *   **Example:**
        ```bash
        git add pom.xml forge-gui/src/main/java/forge/download/AutoUpdater.java
        git commit -m "Prepare release v0.34.1 (Fix auto-updater comparison)"
        ```

4.  **Push Changes:**
    *   Push your local commits to the `master` branch on the `github` remote.
    ```bash
    git push github master
    ```

5.  **Create GitHub Release:**
    *   Use the GitHub CLI to create a new release.
    *   Use a tag name matching the version, prefixed with `v` (e.g., `v0.34.1`).
    *   Specify the path to the generated ZIP file from step 2.
    *   Set the title using the project name and version.
    *   Provide release notes. You can use the last commit message or write custom notes.
    ```bash
    # Get the last commit message (optional, for release notes)
    LAST_COMMIT_MSG=$(git log -1 --pretty=%B)

    # Create the release (replace <version> with the actual version number, e.g., 0.34.1)
    gh release create v<version> \
      forge-gui-mobile-dev/target/forge-gui-mobile-dev-<version>.zip \
      --title "Forge: Shandalar - \"The First 10 Years\" Edition v<version>" \
      --notes "$LAST_COMMIT_MSG. Download the attached ZIP file, unzip, and run the appropriate launcher for your OS." 
      # Or use --notes "Your custom release notes here."
    ```
    *   **Example for v0.34.1:**
        ```bash
        # Make sure the ZIP filename and version match!
        gh release create v0.34.1 \
          forge-gui-mobile-dev/target/forge-gui-mobile-dev-0.34.1.zip \
          --title "Forge: Shandalar - \"The First 10 Years\" Edition v0.34.1" \
          --notes "Fix: Auto-updater version comparison for mod releases. Download the attached ZIP file, unzip, and run the appropriate launcher for your OS."
        ```
    *   *Important:* Double-check the version number in the tag, ZIP filename, and title before running the command.

---

This process ensures that your code is built correctly with the intended version, and a new release with the distributable package and appropriate metadata is made available on GitHub.