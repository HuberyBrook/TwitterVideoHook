package com.hook.twitter;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * LSPosed module for "Download Twitter Videos"
 *
 * Strategy: hook SharedPreferences and billing state directly,
 * avoiding any interference with pairip native code.
 */
@SuppressWarnings("unused")
public class MainHook implements IXposedHookLoadPackage {

    private static final String PACKAGE_ORIG = "tweeter.gif.twittervideodownloader";
    private static final String PACKAGE_MOD  = "tweeter.gif.twittervideodownloader.mod";
    private static final String TAG = "TwitterVideoHook";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(PACKAGE_ORIG) && !lpparam.packageName.equals(PACKAGE_MOD)) {
            return;
        }
        log("Module loaded for " + lpparam.packageName);

        // Delay hooks until app is fully initialized to avoid startup crashes
        hookAppOnCreate(lpparam);
    }

    /**
     * Hook Application.onCreate() - safe entry point after pairip has done its work.
     * All other hooks are installed here, after the app is stable.
     */
    private void hookAppOnCreate(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            String appClass = lpparam.packageName.equals(PACKAGE_MOD)
                ? PACKAGE_MOD + ".App"
                : PACKAGE_ORIG + ".App";

            XposedHelpers.findAndHookMethod(
                appClass,
                lpparam.classLoader,
                "onCreate",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        log("App.onCreate() completed, installing hooks...");
                        installSafeHooks(lpparam);
                    }
                }
            );
            log("Installed App.onCreate hook for " + appClass);
        } catch (Throwable t) {
            log("FAILED to hook App.onCreate: " + t.getMessage());
            // Fallback: try with Application superclass
            try {
                XposedHelpers.findAndHookMethod(
                    "android.app.Application",
                    lpparam.classLoader,
                    "onCreate",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            installSafeHooks(lpparam);
                        }
                    }
                );
                log("Fallback: hooked Application.onCreate");
            } catch (Throwable t2) {
                log("FAILED fallback: " + t2.getMessage());
            }
        }
    }

    /**
     * Install hooks that are safe to run after app initialization.
     */
    private void installSafeHooks(XC_LoadPackage.LoadPackageParam lpparam) {
        hookDownloadLimit(lpparam);
        hookBillingPremium(lpparam);
        hookAdBlocker(lpparam);
    }

    /**
     * Remove download limit by hooking Pref.g() -> always return 9999.
     */
    private void hookDownloadLimit(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            String pkg = lpparam.packageName;
            XposedHelpers.findAndHookMethod(
                pkg + ".pref.Pref", lpparam.classLoader, "g",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(9999);
                    }
                }
            );
            log("Hooked: Pref.g() -> unlimited downloads");
        } catch (Throwable t) {
            log("FAILED Pref.g: " + t.getMessage());
        }
    }

    /**
     * Force billing/purchase state to PURCHASED so premium is unlocked.
     * og.k.d() returns the purchase state MutableStateFlow value.
     * og.b enum: SKU_STATE_PURCHASED = ordinal 6.
     */
    private void hookBillingPremium(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> stateEnum = XposedHelpers.findClass("og.b", lpparam.classLoader);
            Object purchased = stateEnum.getEnumConstants()[6]; // SKU_STATE_PURCHASED

            XposedHelpers.findAndHookMethod(
                "og.k", lpparam.classLoader, "d",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(purchased);
                    }
                }
            );
            log("Hooked: og.k.d() -> SKU_STATE_PURCHASED");
        } catch (Throwable t) {
            log("FAILED billing: " + t.getMessage());
        }
    }

    /**
     * Block ads by hooking the ad enable/disable flags directly.
     * qg.b has fields: d (boolean, lifecycle), e (boolean, suppress)
     * Setting e=true suppresses all ads.
     */
    private void hookAdBlocker(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> qgb = XposedHelpers.findClass("qg.b", lpparam.classLoader);
            XposedHelpers.setStaticBooleanField(qgb, "e", true);
            log("Hooked: qg.b.e = true (ads suppressed)");
        } catch (Throwable t) {
            log("FAILED ad blocker: " + t.getMessage());
        }

        // Also try to set ad provider to "none"
        try {
            Class<?> qgc = XposedHelpers.findClass("qg.c", lpparam.classLoader);
            java.lang.reflect.Field field = qgc.getDeclaredField("a");
            field.setAccessible(true);
            field.set(null, "none");
            log("Hooked: qg.c.a = 'none' (ad provider disabled)");
        } catch (Throwable t) {
            log("FAILED ad provider: " + t.getMessage());
        }
    }

    private void log(String msg) {
        XposedBridge.log("[" + TAG + "] " + msg);
    }
}
