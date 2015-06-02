package com.zsmo.autodownload.ui;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.zsmo.autodownload.Const;
import com.zsmo.autodownload.DownloadAsyncTask;
import com.zsmo.autodownload.R;
import com.zsmo.autodownload.utils.NetWorkUtils;
import com.zsmo.autodownload.utils.PackageUtils;
import com.zsmo.autodownload.utils.SmbUtils;

import org.w3c.dom.Text;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileFilter;

public class MainActivity extends BaseActivity {

    private static Map<String, String> sPackageNameMapping;
    static {
        MainActivity.sPackageNameMapping = new HashMap<>();
        MainActivity.sPackageNameMapping.put("connectme", "cn.m15.connectme");
        MainActivity.sPackageNameMapping.put("zeroshare", "cn.m15.zeroshare");
        MainActivity.sPackageNameMapping.put("kittyplay", "cn.m15.kittyplay");
    }
    private static List<String> sAvailableProductNames;
    static {
        MainActivity.sAvailableProductNames = new ArrayList<>();
        MainActivity.sAvailableProductNames.add("connectme");
        MainActivity.sAvailableProductNames.add("zeroshare");
        MainActivity.sAvailableProductNames.add("kittyplay");
    }

    private Button mBtnUninstall;
    private Button mBtnInstall;
    private Spinner mSpinnerProduct;
    private Spinner mSpinnerVersion;

    private List<String> mProducts;
    private Map<String, List<String>> mVersionsMap;

    private BroadcastReceiver mWifiEnableReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (NetWorkUtils.isWifiConnected(context)) {
                getServerData();
            }
        }
    };

    private class ProductsSmbFilter implements SmbFileFilter {
        @Override
        public boolean accept(SmbFile smbFile) throws SmbException {
            String dirName = smbFile.getName().toLowerCase();
            String name = dirName.substring(0, dirName.length() - 1);
            return MainActivity.sAvailableProductNames.contains(name);
        }
    }

    private class DirSmbFilter implements SmbFileFilter {

        @Override
        public boolean accept(SmbFile smbFile) throws SmbException {
            return null != smbFile && smbFile.isDirectory();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mProducts = new ArrayList<>();
        mVersionsMap = new HashMap<>();

        getServerData();
        initViewsAndEvents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mWifiEnableReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mWifiEnableReceiver);
    }

    private void initViewsAndEvents() {
        mBtnInstall = (Button) findViewById(R.id.btn_install);
        mBtnUninstall = (Button) findViewById(R.id.btn_uninstall);
        mSpinnerProduct = (Spinner) findViewById(R.id.spinner_products);
        mSpinnerVersion = (Spinner) findViewById(R.id.spinner_version);
        setButtonClickListener();
    }

    private void setButtonClickListener() {
        // 点击卸载按钮
        mBtnUninstall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String productName = mProducts.get(mSpinnerProduct.getSelectedItemPosition());
                String packageName = MainActivity.sPackageNameMapping.get(productName.toLowerCase());
                if (null != packageName && PackageUtils.checkInsInstalled(MainActivity.this, packageName)) {
                    PackageUtils.unInstallPackage(MainActivity.this, packageName);
                } else {
                    Toast.makeText(MainActivity.this, R.string.toast_not_installed, Toast.LENGTH_LONG).show();
                }
            }
        });
        // 点击安装按钮
        mBtnInstall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!NetWorkUtils.isWifiConnected(MainActivity.this)) {
                    Toast.makeText(MainActivity.this, R.string.toast_no_network, Toast.LENGTH_SHORT).show();
                } else {
                    String productName = (String) mSpinnerProduct.getSelectedItem();
                    String versionName = (String) mSpinnerVersion.getSelectedItem();
                    if (!TextUtils.isEmpty(productName) && !TextUtils.isEmpty(versionName)) {
                        String filePath = Const.SMB_SERVER + '/' + productName + '/' + versionName + '/';
                        new DownloadAsyncTask(MainActivity.this, filePath).execute();
                    }
                }

            }
        });
    }

    private void populateSpanner() {

        final ArrayAdapter<String> adapterVersions = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<String>());
        adapterVersions.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerVersion.setAdapter(adapterVersions);

        final ArrayAdapter<String> productsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, mProducts);
        productsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerProduct.setAdapter(productsAdapter);
        mSpinnerProduct.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedProduct = (String) parent.getSelectedItem();
                adapterVersions.clear();
                if (null != mVersionsMap && null != mVersionsMap.get(selectedProduct)) {
                    adapterVersions.addAll(mVersionsMap.get(selectedProduct));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // ignore
            }
        });
    }

    private void getServerData() {

        if (!NetWorkUtils.isWifiConnected(this)) {
            Toast.makeText(this, R.string.toast_no_network, Toast.LENGTH_SHORT).show();
        } else {
            new GetSmbDirAsyncTask(this).execute(Const.SMB_SERVER);
        }
    }

    private class GetSmbDirAsyncTask extends AsyncTask<String, Void, Integer> {

        private WeakReference<MainActivity> mContext;
        private ProgressDialog mProgressDialog;

        public GetSmbDirAsyncTask(MainActivity context) {
            mContext = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            if (null != mContext.get()) {
                mProgressDialog = ProgressDialog.show(mContext.get(), null, getString(R.string.dialog_loading));
                mProgressDialog.setCancelable(false);
            }
        }

        @Override
        protected Integer doInBackground(String... params) {
            int productsResult = 1;
            int versionsResult = 1;
            String smbServer = params[0];
            // 读取顶级目录信息
            List<String> smbFiles = SmbUtils.readSmbDir(smbServer, new ProductsSmbFilter());
            if (!smbFiles.isEmpty()) {
                mProducts = smbFiles;
                productsResult = 0;
            }
            // 读取版本信息
            for (String dirName : mProducts) {
                String smbPath = smbServer + dirName + '/';
                List<String> paths = SmbUtils.readSmbDir(smbPath, new DirSmbFilter());
                if (!paths.isEmpty()) {
                    mVersionsMap.put(dirName, paths);
                }
                if (1 == versionsResult) {
                    versionsResult = 0;
                }
            }
            return productsResult & versionsResult;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            if (null != mContext.get()) {
                MainActivity context = mContext.get();
                if (0 == integer) {
                    if (null != mProgressDialog && mProgressDialog.isShowing()) {
                        mProgressDialog.dismiss();
                    }
                    context.populateSpanner();
                } else {
                    Toast.makeText(context, R.string.toast_request_error, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_about:
                startActivity(new Intent(this, AboutActivity.class));
                return true;
            case R.id.menu_clear:
                startActivity(new Intent(this, ApkListActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


}
