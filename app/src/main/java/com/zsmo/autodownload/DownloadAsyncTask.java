package com.zsmo.autodownload;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.widget.Toast;

import com.zsmo.autodownload.ui.MainActivity;
import com.zsmo.autodownload.utils.FileUtils;
import com.zsmo.autodownload.utils.PackageUtils;
import com.zsmo.autodownload.utils.SmbUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

import jcifs.smb.SmbFile;

/**
 * 使用async task下载文件
 */
public class DownloadAsyncTask extends AsyncTask<Void, Integer, String> {

    private ProgressDialog mDialog;
    private PowerManager.WakeLock mWakeLock;
    private WeakReference<MainActivity> mContext;
    private String mPath;

    public DownloadAsyncTask(MainActivity context, String path) {
        mContext = new WeakReference<>(context);
        mPath = path;
    }

    @Override
    protected String doInBackground(Void... params) {

        String result = null;
        InputStream in = null;
        FileOutputStream out = null;
        try {
            // 活取最新的文件
            SmbFile file = SmbUtils.readNeweastSmbFile(mPath);
            if (null != file) {
                in = file.getInputStream();
                long fileLength = file.length();
                String path = FileUtils.getFileDirPath(mContext.get());
                File outFile = new File(path, file.getName());
                out = new FileOutputStream(outFile);

                byte[] data = new byte[4096];
                int count;
                long total = 0;
                while (-1 != (count = in.read(data))) {
                    total += count;
                    if (0 < fileLength) {
                        publishProgress((int) (total * 100 / fileLength));
                    }
                    out.write(data, 0, count);
                }
                result = outFile.getAbsolutePath();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != out) {
                    out.close();
                }
                if (null != in) {
                    in.close();
                }
            } catch (IOException ignored) {
                // ignore
            }
        }
        return result;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        mDialog.setIndeterminate(false);
        mDialog.setMax(100);
        mDialog.setProgress(values[0]);
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if (null != mWakeLock && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        mDialog.dismiss();
        if (null != mContext.get()) {
            if (null == result) {
                Toast.makeText(mContext.get(), R.string.toast_connect_error, Toast.LENGTH_LONG).show();
            } else if (new File(result).exists()) {
                PackageUtils.installPackage(mContext.get(), result);
            }
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (null != mContext.get()) {
            PowerManager pm = (PowerManager) mContext.get().getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
            mDialog = new ProgressDialog(mContext.get());
            mDialog.setProgressNumberFormat(null);
            mDialog.setCancelable(false);
            mDialog.setIndeterminate(true);
            mDialog.setMax(100);
            mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mDialog.setMessage(mContext.get().getString(R.string.dialog_downloading));
            mDialog.show();
        }
    }
}
