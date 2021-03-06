package com.pyler.betterbatterysaver.activities;


import android.app.ActionBar;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import com.pyler.betterbatterysaver.R;
import com.pyler.betterbatterysaver.util.DeviceController;
import com.pyler.betterbatterysaver.util.Utils;

public class AppReceiverSettingsActivity extends PreferenceActivity {
    public static Context mContext;
    public static SharedPreferences mPrefs;
    public static Utils mUtils;
    private static String packageName;
    private static String appName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mUtils = new Utils(this);
        super.onCreate(savedInstanceState);
        mContext = getApplicationContext();


        Intent intent = getIntent();
        if (intent == null) return;
        packageName = intent.getStringExtra("package");
        appName = intent.getStringExtra("app");

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setTitle(appName);
        }
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new Settings()).commit();
    }

    @SuppressWarnings("deprecation")
    public static class Settings extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (Build.VERSION.SDK_INT < 24) {
                getPreferenceManager()
                        .setSharedPreferencesMode(MODE_WORLD_READABLE);
            }
            addPreferencesFromResource(R.xml.app_services);
            mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);

            final PreferenceScreen appServices = (PreferenceScreen) findPreference("app_services");
            PackageManager pm = mContext.getPackageManager();
            PackageInfo pi = null;
            try {
                pi = pm.getPackageInfo(packageName, PackageManager.GET_RECEIVERS);
            } catch (PackageManager.NameNotFoundException e) {
                getActivity().finish();
            }
            ActivityInfo[] receiverList = pi.receivers;

            if (receiverList != null) {
                for (ActivityInfo receiver : receiverList) {
                    CheckBoxPreference serviceSetting = new CheckBoxPreference(mContext);
                    String longName = receiver.name;
                    String shortName = longName.substring(longName.lastIndexOf(".") + 1, longName.length());
                    ComponentName cn = new ComponentName(packageName, longName);
                    final boolean isReceiverEnabled = (pm.getComponentEnabledSetting(cn) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
                    final String serviceComponentName = cn.flattenToShortString();
                    serviceSetting.setTitle(shortName);
                    serviceSetting.setSummary(longName);
                    serviceSetting.setChecked(isReceiverEnabled);
                    serviceSetting.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(
                                Preference preference, Object newValue) {
                            DeviceController device = new DeviceController(mContext);
                            boolean set = (boolean) newValue;
                            if (set) {
                                if (!isReceiverEnabled) {
                                    device.setServiceMode(serviceComponentName, true);
                                }

                            } else {
                                if (isReceiverEnabled) {
                                    device.setServiceMode(serviceComponentName, false);
                                }
                            }
                            return true;
                        }
                    });
                    appServices.addPreference(serviceSetting);
                }
            }
        }


        @Override
        public void onPause() {
            super.onPause();

            mUtils.setPrefsFileWorldReadable();
        }
    }
}
