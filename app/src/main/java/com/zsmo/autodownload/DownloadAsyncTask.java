package com.zsmo.autodownload;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.widget.Toast;

import com.zsmo.autodownload.ui.MainActivity;
import com.zsmo.autodownload.utils.FileUtils;
import com.zsmo.autodownload.utils.PackageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 使用async task下载文件
 */
public class DownloadAsyncTask extends AsyncTask<String, Integer, String> {

    private ProgressDialog mDialog;
    private PowerManager.WakeLock mWakeLock;
    private WeakReference<MainActivity> mContext;
    private String mFullName;
    private String mPackageName;

    public DownloadAsyncTask(MainActivity context, String fullName, String packageName) {
        mContext = new WeakReference<>(context);
        mFullName = fullName;
        mPackageName = packageName;
    }

    @Override
    protected String doInBackground(String... params) {

        String result = null;
        InputStream in = null;
        HttpURLConnection connection = null;
        FileOutputStream out = null;
        try {
            URL url = new URL(params[0]);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            if (HttpURLConnection.HTTP_OK != connection.getResponseCode()) {
                return null;
            }

            int fileLength = connection.getContentLength();
            in = connection.getInputStream();
            String path = FileUtils.getFileDirPath(mContext.get());
            File file = new File(path, mFullName);
            out = new FileOutputStream(file);

            byte[] data = new byte[4096];
            int count;
            long total = 0;
            while (-1 != (count = in.read(data))) {
                total += count;
                if (0 < fileLength) {
                    publishProgress((int)(total * 100 / fileLength));
                }
                out.write(data, 0, count);
            }
            result = file.getAbsolutePath();
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
                if (null != connection) {
                    connection.disconnect();
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
                mContext.get().initDownloadingSig(mPackageName);
                PackageUtils.installPackage(mContext.get(), result);
            }
        }
    }

    @Override
    protected void onCancelled(String s) {
        super.onCancelled(s);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
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
