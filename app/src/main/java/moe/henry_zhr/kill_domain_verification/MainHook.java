package moe.henry_zhr.kill_domain_verification;

import android.os.Build;

import java.lang.reflect.Field;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

@SuppressWarnings("unused")
public class MainHook implements IXposedHookLoadPackage {
  private final static String TAG = "KillDomainVerification";

  @Override
  public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
    if (!lpparam.packageName.equals("android")) {
      return;
    }

    XposedBridge.log(String.format("[%s] Starting hook", TAG));

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      XposedBridge.log(String.format(
          "[%s] Android 12 or above detected, maybe not supported",
          TAG
      ));
    }

    // https://cs.android.com/android/platform/superproject/+/android-11.0.0_r48:frameworks/base/services/core/java/com/android/server/pm/PackageSettingBase.java
    XposedHelpers.findAndHookMethod(
        "com.android.server.pm.PackageSettingBase",
        lpparam.classLoader,
        "readUserState",
        int.class,
        new XC_MethodHook() {
          @Override
          protected void afterHookedMethod(MethodHookParam param) {
            try {
              final Field field = XposedHelpers.findField(
                  param.getResult().getClass(),
                  "domainVerificationStatus"
              );
              if (field.get(param.getResult()).equals(2)) {
                field.set(param.getResult(), 1);
              }
            } catch (Throwable t) {
              XposedBridge.log(String.format(
                  "[%s] Cannot set domainVerificationStatus (%s)",
                  TAG,
                  t.toString()
              ));
            }
          }
        }
    );

    XposedBridge.log(String.format("[%s] Hook finished", TAG));
  }
}