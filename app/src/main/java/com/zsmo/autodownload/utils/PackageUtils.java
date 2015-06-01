package com.zsmo.autodownload.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * PackageUtils
 */
public class PackageUtils {

    public static final String MIME_APK = "application/vnd.android.package-archive";

    public static boolean checkInsInstalled(Context context, String packageName) {
        boolean isInstalled = false;
        PackageManager manager = context.getPackageManager();
        try {
            manager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            isInstalled = true;
        } catch (PackageManager.NameNotFoundException e) {
            // ignore
        }
        return isInstalled;
    }

    public static void unInstallPackage(Context context, String packageName) {
        Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
        intent.setData(Uri.parse("package:" + packageName));
        context.startActivity(intent);
    }

    public static void installPackage(Context context, String filePath) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(new File(filePath)), PackageUtils.MIME_APK);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static PackageInfo getPackageInfo(Context context, String packageName) {
        PackageManager packageManager = context.getPackageManager();
        PackageInfo info = null;
        try {
            info = packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return info;
    }

    public static Map<String, Object> parseApkFile(Context context, String filePath) {
        Map<String, Object> result = new HashMap<>();
        if (null != context) {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo info = packageManager.getPackageArchiveInfo(filePath, PackageManager.GET_ACTIVITIES);
            if (null != info) {
                ApplicationInfo applicationInfo = info.applicationInfo;
                applicationInfo.sourceDir = filePath;
                applicationInfo.publicSourceDir = filePath;
                result.put("packageName", info.packageName);
                result.put("versionName", info.versionName);
                result.put("icon", applicationInfo.loadIcon(packageManager));
                result.put("label", applicationInfo.loadLabel(packageManager));
            }
        }
        return result;
    }
}
