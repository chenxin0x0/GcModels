package com.cxapp.gcmodel;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class xpModel implements IXposedHookLoadPackage {
    private static String Tag = "cxGCash";


    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!loadPackageParam.processName.equals("com.globe.gcash.android")) {
            return;
        }
        g.hhh();
        XposedBridge.log(Tag+  "进入GCash：" + loadPackageParam.processName);
        helper.init(loadPackageParam);

    }
}
