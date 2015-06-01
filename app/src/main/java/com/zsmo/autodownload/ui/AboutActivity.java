package com.zsmo.autodownload.ui;

import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.widget.TextView;

import com.zsmo.autodownload.R;
import com.zsmo.autodownload.utils.PackageUtils;

/**
 * About
 */
public class AboutActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        if (null != getActionBar()) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        String versionName = "";
        PackageInfo info = PackageUtils.getPackageInfo(this, getPackageName());
        if (null != info) {
            versionName = info.versionName;
        }

        String versionPattern = getResources().getString(R.string.about_version);
        String versionFullName = String.format(versionPattern, versionName);

        TextView text = (TextView) findViewById(R.id.tv_version_full_name);
        text.setText(versionFullName);

    }
}
