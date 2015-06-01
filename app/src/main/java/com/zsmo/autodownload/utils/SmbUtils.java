package com.zsmo.autodownload.utils;

import android.widget.ListView;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileFilter;

/**
 * SmbFile 处理
 */
public class SmbUtils {

    public static List<String> readSmbDir(String path, SmbFileFilter filter) {
        List<String> list = new ArrayList<>();
        try {
            SmbFile smbFile = new SmbFile(path);
            if (smbFile.isDirectory()) {
                SmbFile[] files = null != filter ? smbFile.listFiles(filter) : smbFile.listFiles();
                for (SmbFile file : files) {
                    String dirName = file.getName();
                    String name = dirName.substring(0, dirName.length() - 1);
                    list.add(name);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static SmbFile readNeweastSmbFile(String path) {

        SmbFile result = null;
        try {
            SmbFile smbFile = new SmbFile(path);
            if (smbFile.isDirectory()) {
                // 遍历,查找最新的
                SmbFile[] files = smbFile.listFiles(new SmbFileFilter() {
                    @Override
                    public boolean accept(SmbFile smbFile) throws SmbException {
                        return smbFile.getName().endsWith(".apk");
                    }
                });
                if (0 < files.length) {
                    SmbFile neweastFile = null;
                    for (SmbFile file : files) {
                        if (null == neweastFile || neweastFile.getLastModified() < file.getLastModified()) {
                            neweastFile = file;
                        }
                    }
                    if (null != neweastFile) {
                        result = neweastFile;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
