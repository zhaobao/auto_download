package com.zsmo.autodownload.ui;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.zsmo.autodownload.Const;
import com.zsmo.autodownload.DownloadAsyncTask;
import com.zsmo.autodownload.R;
import com.zsmo.autodownload.utils.NetWorkUtils;
import com.zsmo.autodownload.utils.PackageUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MainActivity extends BaseActivity {

    private Button mBtnUninstall;
    private Button mBtnInstall;
    private Spinner mSpinnerProduct;
    private Spinner mSpinnerType;
    private Spinner mSpinnerVersion;

    private List<String> mProducts;
    private Map<String, List<String>> mTypesMap;
    private List<String> mTypes;
    private Map<String, Map<String, List<String>>> mVersionsMap;
    private List<String> mVersions;
    private ConcurrentMap<String, Boolean> mDownloadSigs;

    private ArrayAdapter<String> mTypesAdapter;

    private BroadcastReceiver mWifiEnableReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (NetWorkUtils.isWifiConnected(context)) {
                getServerData();
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mProducts = new ArrayList<>();
        mTypes = new ArrayList<>();
        mVersions = new ArrayList<>();

        mTypesMap = new HashMap<>();
        mVersionsMap = new HashMap<>();

        mDownloadSigs = new ConcurrentHashMap<>();

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
        mSpinnerType = (Spinner) findViewById(R.id.spinner_type);
        mSpinnerVersion = (Spinner) findViewById(R.id.spinner_version);
        setButtonClickListener();
    }

    private void setButtonClickListener() {
        mBtnUninstall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String packageName = mProducts.get(mSpinnerProduct.getSelectedItemPosition());
                if (PackageUtils.checkInsInstalled(MainActivity.this, packageName)) {
                    PackageUtils.unInstallPackage(MainActivity.this, packageName);
                } else {
                    Toast.makeText(MainActivity.this, R.string.toast_not_installed, Toast.LENGTH_LONG).show();
                }
            }
        });
        mBtnInstall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String packageName = String.valueOf(mSpinnerProduct.getSelectedItem());
                if (mDownloadSigs.get(packageName)) {
                    Toast.makeText(MainActivity.this, R.string.toast_downloading, Toast.LENGTH_LONG).show();
                } else {
                    // set flag
                    String selectedVersion = String.valueOf(mSpinnerVersion.getSelectedItem());
                    mDownloadSigs.replace(packageName, true);
                    PackageInfo info = PackageUtils.getPackageInfo(MainActivity.this, packageName);
                    boolean download = true;
                    if (null != info) {
                        String versionName = info.versionName;
                        if (0 > versionName.compareToIgnoreCase(selectedVersion)) {
                            download = false;
                            Toast.makeText(MainActivity.this, R.string.toast_no_update, Toast.LENGTH_LONG).show();
                        }
                    }
                    // download
                    if (download) {
                        String requestUrl = Const.DOWNLOAD_URL;
                        final String p = mSpinnerProduct.getSelectedItem().toString();
                        final String t = mSpinnerType.getSelectedItem().toString();
                        final String version = mSpinnerVersion.getSelectedItem().toString();
                        requestUrl += "?p=" + p;
                        requestUrl += "&t=" + t;
                        requestUrl += "&v=" + version;
                        requestUrl += "&k=love";
                        SimpleDateFormat format = new SimpleDateFormat("MM-dd", Locale.getDefault());
                        String date = format.format(new Date());
                        final String fullName = p + "_" + t + "_" + version + "_" + date + ".apk";
                        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
                        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                                Request.Method.GET,
                                requestUrl,
                                null,
                                new Response.Listener<JSONObject>() {
                                    @Override
                                    public void onResponse(JSONObject response) {
                                        try {
                                            int code = response.getInt("code");
                                            if (0 == code) {
                                                String downloadUrl = response.getString("download_url");
                                                new DownloadAsyncTask(MainActivity.this, fullName, p).execute(downloadUrl);
                                            }
                                        } catch (JSONException e) {
                                            // ignore
                                        }
                                    }
                                },
                                new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        mDownloadSigs.replace(p, false);
                                        Toast.makeText(MainActivity.this, R.string.toast_request_error, Toast.LENGTH_LONG).show();
                                    }
                                });
                        queue.add(jsonObjectRequest);
                    }
                }
            }
        });
    }

    private void populateSpanner() {

        final ArrayAdapter<String> adapterVersions = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, mVersions);
        adapterVersions.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerVersion.setAdapter(adapterVersions);

        mTypesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, mTypes);
        mTypesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerType.setAdapter(mTypesAdapter);
        mSpinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedProducts = (String) mSpinnerProduct.getSelectedItem();
                String selectedTypes = String.valueOf(parent.getSelectedItem());
                if (null != mVersionsMap.get(selectedProducts).get(selectedTypes)) {
                    adapterVersions.clear();
                    adapterVersions.addAll(mVersionsMap.get(selectedProducts).get(selectedTypes));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // ignore
            }
        });

        final ArrayAdapter<String> productsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, mProducts);
        productsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerProduct.setAdapter(productsAdapter);
        mSpinnerProduct.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedProducts = String.valueOf(parent.getSelectedItem());
                if (null != mTypesMap.get(selectedProducts)) {
                    mTypesAdapter.clear();
                    mTypesAdapter.addAll(mTypesMap.get(selectedProducts));
                }
                String selectedTypes = (String) mSpinnerType.getSelectedItem();
                if (null != mVersionsMap.get(selectedProducts).get(selectedTypes)) {
                    adapterVersions.clear();
                    adapterVersions.addAll(mVersionsMap.get(selectedProducts).get(selectedTypes));
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

            final ProgressDialog progressDialog = ProgressDialog.show(this, null, getString(R.string.dialog_loading));
            progressDialog.setCancelable(false);
            RequestQueue queue = Volley.newRequestQueue(this);
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, Const.REQUEST_URL, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                JSONArray products = response.getJSONArray("p");
                                mProducts = MainActivity.transferJsonArrayToList(products);
                                for (String p : mProducts) {
                                    mDownloadSigs.put(p, false);
                                }

                                JSONObject types = response.getJSONObject("t");
                                Iterator<String> tKeys = types.keys();
                                while (tKeys.hasNext()) {
                                    String key = tKeys.next();
                                    JSONArray values = types.getJSONArray(key);
                                    List<String> list = MainActivity.transferJsonArrayToList(values);
                                    mTypesMap.put(key, list);
                                }

                                JSONObject versions = response.getJSONObject("v");
                                Iterator<String> vKeys = versions.keys();
                                while (vKeys.hasNext()) {
                                    String key = vKeys.next();
                                    JSONObject values = versions.getJSONObject(key);
                                    Iterator<String> subKeys = values.keys();
                                    Map<String, List<String>> vs = new HashMap<>();
                                    while (subKeys.hasNext()) {
                                        String tkey = subKeys.next();
                                        JSONArray tvalues = values.getJSONArray(tkey);
                                        List<String> tvlist = MainActivity.transferJsonArrayToList(tvalues);
                                        vs.put(tkey, tvlist);
                                    }
                                    mVersionsMap.put(key, vs);
                                }

                                populateSpanner();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            if (progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            error.printStackTrace();
                            if (progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            Toast.makeText(MainActivity.this, R.string.toast_request_error, Toast.LENGTH_LONG).show();
                        }
                    });
            queue.add(jsonObjectRequest);
        }
    }

    private static List<String> transferJsonArrayToList(JSONArray array) {
        List<String> list = new ArrayList<>();
        try {
            for (int i = 0; i < array.length(); i++) {
                list.add(array.getString(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void initDownloadingSig(String packageName) {
        mDownloadSigs.replace(packageName, false);
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
