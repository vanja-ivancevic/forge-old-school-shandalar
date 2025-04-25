package forge.assets;

import forge.gui.GuiBase;

import forge.Forge;
import forge.gui.FThreads;


public class AssetsDownloader {


    public static void checkForUpdates(boolean exited, Runnable runnable) {
        // START: Force disable update check in AssetsDownloader
        System.out.println("DEBUG: AssetsDownloader update check explicitly disabled. Running original runnable directly.");
        run(runnable); // Directly execute the original continuation logic
        return;
        // END: Force disable update check

        // Removed unreachable original code block to prevent compilation errors.
    }

    private static void run(Runnable toRun) {
        if (toRun != null) {
            if (!GuiBase.isAndroid()) {
                Forge.getSplashScreen().getProgressBar().setDescription("Loading game resources...");
            }
            FThreads.invokeInBackgroundThread(toRun);
            return;
        }
        if (!GuiBase.isAndroid()) {
            Forge.isMobileAdventureMode = Forge.advStartup;
            Forge.exitAnimation(false);
        }
    }
}
