# Forge: Old-School Shandalar - Release Process

This document outlines the steps required to compile the project, package it, and create a new release on GitHub for the Old-School Shandalar fork.

## Prerequisites

1.  **Maven:** Ensure Apache Maven is installed and configured correctly. You need the `mvn` command available in your terminal. (Check with `mvn -v`)
2.  **Git:** Ensure Git is installed. (Check with `git --version`)
3.  **GitHub CLI:** Ensure the GitHub CLI (`gh`) is installed and authenticated. (Check with `gh --version` and `gh auth status`)
4.  **Repository Setup:**
    *   The local repository should be cloned.
    *   A remote named `github` pointing to the `vanja-ivancevic/forge-old-school-shandalar` repository should exist (`git remote -v`).
    *   The default repository for `gh` should be set to `vanja-ivancevic/forge-old-school-shandalar` (`gh repo view`). If not, run `gh repo set-default vanja-ivancevic/forge-old-school-shandalar`.

## Release Steps

1.  **Update Version (If Necessary):**
    *   Before starting the release process, you might want to update the version number in the relevant `pom.xml` files (primarily `pom.xml` in the root and `forge-gui-mobile-dev/pom.xml`). This is a standard Maven practice but optional for this specific setup if you just want to rebuild the *current* snapshot version. The current build process generates a ZIP named `forge-gui-mobile-dev-*-SNAPSHOT.zip`.

2.  **Compile and Package:**
    *   Open your terminal in the root directory of the project (`forge/`).
    *   Run the Maven package command. This cleans previous builds, compiles the code, runs tests, and executes the assembly plugin to create both the JAR-with-dependencies and the final distribution ZIP file.
    ```bash
    /Applications/apache-maven-3.9.9/bin/mvn clean package
    ```
    *   *Note:* Replace `/Applications/apache-maven-3.9.9/bin/mvn` with your actual path to `mvn` if it's different or if `mvn` is already in your system's PATH.
    *   Verify that the ZIP file `forge-gui-mobile-dev/target/forge-gui-mobile-dev-*-SNAPSHOT.zip` has been created/updated.

3.  **Commit Changes (Recommended):**
    *   If you made any code changes or updated POM versions, commit them to Git.
    ```bash
    git add .
    git commit -m "Prepare release v<version>" # Replace <version> (e.g., v2.0.05)
    ```

4.  **Push Changes:**
    *   Push your local commits to the `master` branch on the `github` remote.
    ```bash
    git push github master
    ```

5.  **Create GitHub Release:**
    *   Use the GitHub CLI to create a new release. Choose a new tag name (e.g., `v2.0.05`).
    *   You need to specify the path to the generated ZIP file. **Make sure the version number in the filename matches the output from the `mvn package` step.**
    *   Update the `--title` and `--notes` flags as needed for the new release.
    ```bash
    gh release create <tag_name> \
      forge-gui-mobile-dev/target/forge-gui-mobile-dev-2.0.04-SNAPSHOT.zip \
      --title "Old-School Shandalar <version_title>" \
      --notes "Release notes for <version_title>. Download the attached ZIP file, unzip, and run the appropriate launcher for your OS."
    ```
    *   **Example:**
        ```bash
        # Make sure the ZIP filename is correct from the target directory!
        gh release create v2.0.05 \
          forge-gui-mobile-dev/target/forge-gui-mobile-dev-2.0.04-SNAPSHOT.zip \
          --title "Old-School Shandalar v2.0.05" \
          --notes "Bug fixes and deck updates. Download the attached ZIP file, unzip, and run the appropriate launcher for your OS."
        ```
    *   *Important:* Replace `<tag_name>` (e.g., `v2.0.05`) and `<version_title>` (e.g., `v2.0.05`) with the actual new version details. Double-check the exact name of the ZIP file in `forge-gui-mobile-dev/target/` before running the command.

---

This process ensures that your code is built correctly and a new release with the distributable package is made available on GitHub.