package com.hook.twitter;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Minimal test module - does nothing but log.
 * If this also crashes, pairip has anti-Xposed protection.
 */
@SuppressWarnings("unused")
public class MainHook implements IXposedHookLoadPackage {

    private static final String PACKAGE_ORIG = "tweeter.gif.twittervideodownloader";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(PACKAGE_ORIG)) {
            return;
        }
        XposedBridge.log("[TwitterVideoHook] Minimal module loaded - no hooks installed");
    }
}
