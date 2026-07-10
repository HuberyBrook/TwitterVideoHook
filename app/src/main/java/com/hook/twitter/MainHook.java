package com.hook.twitter;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * LSPosed module for "Download Twitter Videos" (tweeter.gif.twittervideodownloader)
 *
 * Features:
 *   1. Remove all ads (AdMob, Facebook, ironSource, Unity)
 *   2. Unlock unlimited downloads (remove limitDl restriction)
 *   3. Unlock premium features (force purchase state)
 *
 * Note: pairip license check works fine on unmodified APK with original signature.
 */

@SuppressWarnings("unused")
public class MainHook implements IXposedHookLoadPackage {

    private static final String PACKAGE_ORIG = "tweeter.gif.twittervideodownloader";
    private static final String PACKAGE_MOD  = "tweeter.gif.twittervideodownloader.mod";
    private static final String TAG = "TwitterVideoHook";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // Only hook our target packages
        if (!lpparam.packageName.equals(PACKAGE_ORIG) && !lpparam.packageName.equals(PACKAGE_MOD)) {
            return;
        }

        log("Module loaded for " + lpparam.packageName);

        // === 1. Force ad provider to "none" (disables all ad loading) ===
        hookAdProvider(lpparam);

        // === 2. Permanently suppress ad flags ===
        hookAdFlags(lpparam);

        // === 3. Remove download limit ===
        hookDownloadLimit(lpparam);

        // === 4. Force premium/purchased state ===
        hookPremiumState(lpparam);
    }

    /**
     * Force the ad network selector to "none".
     * Class: qg.c (static field 'a' holds the ad network name)
     * Values: "admob", "meta", or "none"
     */
    private void hookAdProvider(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook class initializer to replace the field after it's set
            XposedHelpers.findAndHookMethod(
                "qg.c",
                lpparam.classLoader,
                "a",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        // Always return "none" so ads never load
                        param.setResult("none");
                    }
                }
            );
            log("Hooked: qg.c.a (ad provider) -> forced 'none'");
        } catch (Throwable t) {
            // The field might not have a getter method, try direct field hook
            try {
                Class<?> clz = XposedHelpers.findClass("qg.c", lpparam.classLoader);
                // We'll hook the static initializer to override
                // Actually let's hook where it's used
                log("qg.c hook failed, trying alternative...");
            } catch (Throwable ignored) {}
        }

        // Alternative: hook the ad loading entry points to return immediately
        try {
            XposedHelpers.findAndHookMethod(
                "sg.b",  // Interstitial ad manager
                lpparam.classLoader,
                "a",
                boolean.class,  // showAd method
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(false); // Never show interstitial
                    }
                }
            );
            log("Hooked: sg.b (interstitial) -> never show");
        } catch (Throwable t) {
            log("FAILED to hook sg.b: " + t.getMessage());
        }

        try {
            XposedHelpers.findAndHookMethod(
                "sg.i",  // Native ad loader
                lpparam.classLoader,
                "a",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(null); // Never load native ads
                    }
                }
            );
            log("Hooked: sg.i (native ad loader) -> never load");
        } catch (Throwable t) {
            log("FAILED to hook sg.i: " + t.getMessage());
        }
    }

    /**
     * Hook the ad enable/disable flags.
     * qg.b.d (boolean) - ads enabled lifecycle flag (false=disable)
     * qg.b.e (boolean) - settings suppress flag (true=suppress)
     */
    private void hookAdFlags(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Override the lifecycle flag setter
            // ng.b.C() sets qg.b.d = true (on start)
            // ng.b.u() sets qg.b.d = false (on stop)
            // We intercept and keep it false
            XposedHelpers.findAndHookMethod(
                "ng.b",
                lpparam.classLoader,
                "C",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        // After lifecycle sets d=true, force it back to false
                        try {
                            XposedHelpers.setStaticBooleanField(
                                XposedHelpers.findClass("qg.b", lpparam.classLoader),
                                "d", false
                            );
                        } catch (Throwable ignored) {}
                    }
                }
            );
            log("Hooked: ng.b.C() -> force qg.b.d = false after lifecycle");
        } catch (Throwable t) {
            log("FAILED to hook ng.b.C: " + t.getMessage());
        }

        try {
            // Force static field e to true (suppress ads permanently)
            XposedHelpers.findAndHookMethod(
                "qg.b",
                lpparam.classLoader,
                "a",  // static getter for 'e'
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(true);
                    }
                }
            );
            log("Hooked: qg.b.a() -> always suppress ads");
        } catch (Throwable t) {
            log("FAILED to hook qg.b: " + t.getMessage());
        }
    }

    /**
     * Remove the download limit by overriding Pref.g() which returns the limit.
     * The default is 4, we force it to Integer.MAX_VALUE.
     */
    private void hookDownloadLimit(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                PACKAGE_ORIG + ".pref.Pref",
                lpparam.classLoader,
                "g",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(9999); // Effectively unlimited
                    }
                }
            );
            log("Hooked: Pref.g() (limitDl) -> always return 9999");
        } catch (Throwable t) {
            // Try with .mod package name
            try {
                XposedHelpers.findAndHookMethod(
                    PACKAGE_MOD + ".pref.Pref",
                    lpparam.classLoader,
                    "g",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            param.setResult(9999);
                        }
                    }
                );
                log("Hooked: Pref.g() (.mod) -> always return 9999");
            } catch (Throwable t2) {
                log("FAILED to hook Pref.g: " + t2.getMessage());
            }
        }
    }

    /**
     * Force premium/purchased state so the app thinks the user bought
     * the "remove ads" IAP. This prevents purchase gating in settings
     * and unlocks any premium-locked features.
     *
     * The purchase state enum og.b has SKU_STATE_PURCHASED = 6.
     * The billing repo og.k stores state in field 'd' (MutableStateFlow).
     */
    private void hookPremiumState(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook the billing repo state getter to always return PURCHASED
            // og.k.d returns a CoroutineStateFlow wrapping og.b enum
            Class<?> ogk = XposedHelpers.findClass("og.k", lpparam.classLoader);
            Class<?> ogb = XposedHelpers.findClass("og.b", lpparam.classLoader);

            // Get the SKU_STATE_PURCHASED enum value (ordinal 6)
            Object purchasedState = ogb.getEnumConstants()[6];

            // Hook the state flow getter
            XposedHelpers.findAndHookMethod(
                "og.k",
                lpparam.classLoader,
                "d",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(purchasedState);
                    }
                }
            );
            log("Hooked: og.k.d() -> always return SKU_STATE_PURCHASED");
        } catch (Throwable t) {
            log("FAILED to hook premium state: " + t.getMessage());
        }
    }

    private void log(String msg) {
        XposedBridge.log("[" + TAG + "] " + msg);
    }
}
