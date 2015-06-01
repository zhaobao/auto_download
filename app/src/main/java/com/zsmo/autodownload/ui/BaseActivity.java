package com.zsmo.autodownload.ui;

import android.app.Activity;
import android.view.MenuItem;

/**
 * Base Activity
 */
public class BaseActivity extends Activity {

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (android.R.id.home == item.getItemId()) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
