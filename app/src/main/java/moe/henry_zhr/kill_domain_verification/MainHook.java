package moe.henry_zhr.kill_domain_verification;

import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.Field;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {
  private final static String TAG = "KillDomainVerification";

  @Override
  public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
    if (!lpparam.packageName.equals("android")) {
      return;
    }

    Log.i(TAG, "Starting hook");

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      Log.i(TAG, "Android 13 or above detected");

      // https://cs.android.com/android/platform/superproject/+/android-13.0.0_r3:frameworks/base/services/core/java/com/android/server/pm/ComputerEngine.java;l=1058
      XposedHelpers.findAndHookMethod(
          "com.android.server.pm.verify.domain.DomainVerificationUtils",
          lpparam.classLoader,
          "isDomainVerificationIntent",
          Intent.class,
          long.class,
          XC_MethodReplacement.returnConstant(false)
      );
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      Log.i(TAG, "Android 12 detected");

      // https://cs.android.com/android/platform/superproject/+/android-12.0.0_r34:frameworks/base/services/core/java/com/android/server/pm/PackageManagerService.java;l=2788
      XposedHelpers.findAndHookMethod(
          "com.android.server.pm.verify.domain.DomainVerificationUtils",
          lpparam.classLoader,
          "isDomainVerificationIntent",
          Intent.class,
          int.class,
          XC_MethodReplacement.returnConstant(false)
      );
    } else {
      Log.i(TAG, "Android 11 or below detected");

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
                final Object result = param.getResult();
                final Field field = XposedHelpers.findField(
                    result.getClass(),
                    "domainVerificationStatus"
                );
                if (field.get(result).equals(2)) {
                  field.set(result, 1);
                }
              } catch (Throwable t) {
                Log.e(TAG, "Cannot set domainVerificationStatus", t);
                XposedBridge.log(String.format(
                    "[%s] Cannot set domainVerificationStatus (%s)",
                    TAG,
                    t
                ));
              }
            }
          }
      );
    }

    Log.i(TAG, "Hook finished");
  }
}