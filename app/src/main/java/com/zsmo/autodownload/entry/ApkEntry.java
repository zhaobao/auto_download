package com.zsmo.autodownload.entry;

import android.graphics.drawable.Drawable;

/**
 * 存储APK信息
 */
public class ApkEntry implements Comparable<ApkEntry> {

    private String mName;
    private String mPackageName;
    private String mVersionName;
    private String mFileName;
    private String mFilePath;
    private float mSize;
    private Drawable mIcon;

    public String getFilePath() {
        return mFilePath;
    }

    public void setFilePath(String mFilePath) {
        this.mFilePath = mFilePath;
    }

    public String getFileName() {
        return mFileName;
    }

    public void setFileName(String mFileName) {
        this.mFileName = mFileName;
    }

    public String getName() {
        return mName;
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public void setPackageName(String mPackageName) {
        this.mPackageName = mPackageName;
    }

    public float getSize() {
        return mSize;
    }

    public void setSize(float mSize) {
        this.mSize = mSize;
    }

    public Drawable getIcon() {
        return mIcon;
    }

    public void setIcon(Drawable mIcon) {
        this.mIcon = mIcon;
    }

    public String getVersionName() {
        return mVersionName;
    }

    public void setVersionName(String mVersionName) {
        this.mVersionName = mVersionName;
    }

    @Override
    public int compareTo(ApkEntry another) {
        return this.getFileName().compareToIgnoreCase(another.getFileName());
    }
}
