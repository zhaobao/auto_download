package com.zsmo.autodownload.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.zsmo.autodownload.R;
import com.zsmo.autodownload.entry.ApkEntry;
import com.zsmo.autodownload.utils.FileUtils;
import com.zsmo.autodownload.utils.PackageUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * List apk
 */
public class ApkListActivity extends BaseActivity {

    private static boolean sSigDeleting;
    private ListView mLvApkList;
    private ApkListAdapter mListAdapter;
    private ProgressDialog mDialog;
    private List<ApkEntry> mData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        mLvApkList = (ListView) findViewById(R.id.lv_apks);
        mData = new ArrayList<>();
        showProgressDialog();
        new ScanFileAsyncTask(this).execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_lsit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (R.id.menu_clear_all == item.getItemId()) {
            if (!ApkListActivity.sSigDeleting && null != mListAdapter && 0 < mListAdapter.getCount()) {
                new DeleteFileAsyncTask(this).execute();
            }
            return true;
        } else if (R.id.menu_refresh == item.getItemId() && !mDialog.isShowing()) {
            showProgressDialog();
            if (null != mListAdapter) {
                mListAdapter.clear();
            }
            new ScanFileAsyncTask(this).execute();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        ApkListActivity.sSigDeleting = false;
    }

    private void updateAdapter() {
        if (null == mListAdapter) {
            mListAdapter = new ApkListAdapter(this, R.layout.item_apk_list, mData);
            mLvApkList.setAdapter(mListAdapter);
        } else {
            mListAdapter.notifyDataSetChanged();
        }
        hideProgressDialog();
    }

    private void showProgressDialog() {
        if (null == mDialog) {
            mDialog = new ProgressDialog(this, ProgressDialog.STYLE_SPINNER);
            mDialog.setCancelable(false);
            mDialog.setIndeterminate(true);
            mDialog.setMessage(getString(R.string.toast_reading_dir));
        }

        if (!mDialog.isShowing()) {
            mDialog.show();
        }
    }

    private void hideProgressDialog() {
        if (null != mDialog && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    private class DeleteFileAsyncTask extends AsyncTask<Void, Integer, Integer> {

        private WeakReference<ApkListActivity> mContext;

        public DeleteFileAsyncTask(ApkListActivity context) {
            mContext = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            if (null != mContext.get()) {
                ApkListActivity.sSigDeleting = true;
            }
        }

        @Override
        protected Integer doInBackground(Void... params) {
            // delete files
            int result = 1;
            if (null != mContext.get()) {
                int length = mContext.get().mListAdapter.getCount();
                for (int i = 0; i < length; i++) {
                    ApkEntry entry = mContext.get().mListAdapter.getItem(i);
                    String filePath = entry.getFilePath();
                    File file = new File(filePath);
                    if (file.exists()) {
                        boolean deleteResult = file.delete();
                        if (deleteResult) {
                            publishProgress();
                        }
                    }
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            ApkListActivity.sSigDeleting = false;
            if (null != mContext.get()) {
                Toast.makeText(mContext.get(), R.string.toast_delete_successfully, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (null != mContext.get()) {
                mContext.get().mListAdapter.remove(mContext.get().mData.get(0));
            }
        }
    }

    private class ScanFileAsyncTask extends AsyncTask<Void, Void, Integer> {

        private WeakReference<ApkListActivity> mContext;
        private String mStoragePath;

        public ScanFileAsyncTask(ApkListActivity context) {
            mContext = new WeakReference<>(context);
            mStoragePath = FileUtils.getFileDirPath(context);
        }

        @Override
        protected Integer doInBackground(Void... params) {
            int result = 1;
            File storagePath = new File(mStoragePath);
            File[] files = storagePath.listFiles();
            if (0 == files.length) {
                return result;
            }
            ApkEntry entry = null;
            for (File file : files) {
                entry = new ApkEntry();
                Map<String, Object> parseInfo = PackageUtils.parseApkFile(mContext.get(), file.getAbsolutePath());
                entry.setIcon((Drawable) parseInfo.get("icon"));
                entry.setName((String) parseInfo.get("label"));
                entry.setPackageName((String) parseInfo.get("packageName"));
                entry.setVersionName((String) parseInfo.get("versionName"));
                entry.setSize(file.length());
                entry.setFileName(file.getName());
                entry.setFilePath(file.getAbsolutePath());
                if (null != mContext.get()) {
                    mContext.get().mData.add(entry);
                }
            }
            if (null != mContext.get()) {
                Collections.sort(mContext.get().mData);
            }
            result = 0;
            return result;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            if (null != mContext.get()) {
                if (0 == integer) {
                    ApkListActivity activity = mContext.get();
                    activity.updateAdapter();
                } else {
                    hideProgressDialog();
                    Toast.makeText(mContext.get(), R.string.toast_no_files, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private class ApkListAdapter extends ArrayAdapter<ApkEntry> {

        private int mResource;
        private LayoutInflater mInflater;

        public ApkListAdapter(Context context, int resource, List<ApkEntry> objects) {
            super(context, resource, objects);
            mResource = resource;
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder holder;
            if (null == convertView) {
                holder = new ViewHolder();
                convertView = mInflater.inflate(mResource, parent, false);
                holder.mImgvIcon = (ImageView) convertView.findViewById(R.id.iv_item_image);
                holder.mTvTitle = (TextView) convertView.findViewById(R.id.tv_item_name);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            ApkEntry entry = getItem(position);
            holder.mImgvIcon.setImageDrawable(entry.getIcon());
            holder.mTvTitle.setText(entry.getFileName());

            return convertView;
        }
    }

    private static class ViewHolder {
        ImageView mImgvIcon;
        TextView mTvTitle;
    }

}
