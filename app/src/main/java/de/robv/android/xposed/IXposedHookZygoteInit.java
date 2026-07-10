package de.robv.android.xposed;

public interface IXposedHookZygoteInit {
    interface StartupParam {}
    void initZygote(StartupParam startupParam) throws Throwable;
}
