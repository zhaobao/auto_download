package com.zsmo.autodownload.utils;

import android.content.Context;
import android.os.Environment;

import java.io.File;

/**
 * File Utils
 */
public class FileUtils {

    public static final int FILE_TYPE_APK = 1;

    public static String initStorageLocation(Context context) {
        File file = Environment.getExternalStorageDirectory();
        File dirPath = new File(file, context.getApplicationContext().getPackageName());
        if (!dirPath.exists()) {
            dirPath.mkdirs();
        }
        return dirPath.getAbsolutePath();
    }

    public static String getFileDirPath(Context context) {
        File file = Environment.getExternalStorageDirectory();
        File dirPath = new File(file, context.getApplicationContext().getPackageName());
        if (!dirPath.exists()) {
            dirPath.mkdirs();
        }
        return dirPath.getAbsolutePath();
    }
}
