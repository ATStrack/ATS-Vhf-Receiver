package com.atstrack.ats.ats_vhf_receiver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.vectordrawable.graphics.drawable.Animatable2Compat;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.atstrack.ats.ats_vhf_receiver.Utils.DriveServiceHelper;
import com.atstrack.ats.ats_vhf_receiver.Utils.FileLoggingTree;
import com.atstrack.ats.ats_vhf_receiver.Utils.LeDeviceListAdapter;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.locEnablerLL)
    LinearLayout locEnablerLL;
    @BindView(R.id.locationmsgTV)
    TextView locationmsgTV;
    @BindView(R.id.locationONbtn)
    Button locationONbtn;
    @BindView(R.id.btEnablerLL)
    LinearLayout btEnablerLL;
    @BindView(R.id.bluetoothmsgTV)
    TextView bluetoothmsgTV;
    @BindView(R.id.bluetoothONbtn)
    Button bluetoothONbtn;
    @BindView(R.id.version)
    TextView version;
    @BindView(R.id.switch_dark_mode)
    Switch switchdarkmode;

    @BindView(R.id.search_linearLayout)
    LinearLayout search_linearLayout;
    @BindView(R.id.device_recyclerView)
    RecyclerView device_recyclerView;
    @BindView(R.id.retry_linearLayout)
    LinearLayout retry_linearLayout;
    @BindView(R.id.update_textView)
    TextView update_textView;
    @BindView(R.id.state_connect_textView)
    TextView state_connect_textView;
    @BindView(R.id.devices_scrollview)
    ScrollView devices_scrollview;
    @BindView(R.id.anim_spinner)
    ImageView anim_spinner;

    final private String TAG = MainActivity.class.getSimpleName();

    boolean isNightModeOn;
    SharedPreferences.Editor sharedPrefsEdit;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 8 seconds.
    private static final long SCAN_PERIOD = 8000;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(() -> {
                        mLeDeviceListAdapter.addDevice(device, scanRecord);
                        mLeDeviceListAdapter.notifyDataSetChanged();
                    });
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        Timber.plant(new FileLoggingTree(getApplicationContext()));

        getSupportActionBar().hide();

        String v = "version: " + BuildConfig.VERSION_NAME;
        version.setText(v);

        initDarkMode();

        anim_spinner.setImageDrawable(getResources().getDrawable(R.drawable.avd_anim_spinner_48));
        final Animatable animated = (Animatable) anim_spinner.getDrawable();
        ((Animatable) animated).start();

        /*SharedPreferences preferences = getSharedPreferences("Connection", 0);
        connectState = preferences.edit();
        retry = preferences.getBoolean("retry", false);
        if (retry){
            connectState.clear();
            connectState.apply();
            devices_scrollview.setVisibility(View.GONE);
            search_linearLayout.setVisibility(View.GONE);
            state_connect_textView.setText(R.string.lb_unable_connect);
            state_connect_textView.setTextColor(ContextCompat.getColor(this, R.color.colordeletebutton));
            retry_linearLayout.setVisibility(View.VISIBLE);
        }*/

        mHandler = new Handler();
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initDarkMode() {
        SharedPreferences appSettingPrefs = getSharedPreferences("AppSettingPrefs", 0);
        sharedPrefsEdit = appSettingPrefs.edit();
        isNightModeOn = appSettingPrefs.getBoolean("NightMode", false);

        if (isNightModeOn){
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            switchdarkmode.setChecked(true);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            switchdarkmode.setChecked(false);
        }
        checkPermissions();
        if(!BluetoothAdapter.getDefaultAdapter().isEnabled()){
            btEnablerLL.setVisibility(View.VISIBLE);
        }
        if(!isLocationEnable()){
            locEnablerLL.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                Log.i("DeviceScanActivity", "DeviceScanActivity-onResume");
            }
        }
        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter(this);
        //if (!retry)
            scanLeDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        Log.i("EROOOR", "RQC: "+requestCode + " RSC: "+resultCode);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(() -> {
                mScanning = false;
                mBluetoothAdapter.stopLeScan(mLeScanCallback);

                search_linearLayout.setVisibility(View.GONE);

                if (!mLeDeviceListAdapter.mLeDevices.isEmpty()) {
                    devices_scrollview.setVisibility(View.VISIBLE);
                    device_recyclerView.setAdapter(mLeDeviceListAdapter);
                    device_recyclerView.setLayoutManager(new LinearLayoutManager(this));
                } else {
                    devices_scrollview.setVisibility(View.GONE);
                    state_connect_textView.setText(R.string.lb_unable_find);
                    state_connect_textView.setTextColor(ContextCompat.getColor(this, R.color.colortext));
                    retry_linearLayout.setVisibility(View.VISIBLE);
                }

                invalidateOptionsMenu();
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            Log.i("DeviceScanActivity", "DeviceScanActivity-scanLeDevice");
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    @Override
    protected void onDestroy() {
        Timber.uprootAll();
        super.onDestroy();
    }

    @OnClick(R.id.locationONbtn)
    public void enableLocation(View v){
        locEnablerLL.setVisibility(View.GONE);
        Intent enableLocIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        int REQUEST_ENABLE_LOC = 1;
        startActivityForResult(enableLocIntent, REQUEST_ENABLE_LOC);
    }
    @OnClick (R.id.bluetoothONbtn)
    public void enableBluetooth(View v){
        btEnablerLL.setVisibility(View.GONE);
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        int REQUEST_ENABLE_BT = 1;
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    /**
     * Checks permissions to be able to use Bluetooth (meaning, Location Permissions if API 23+) and Storage.
     * If Location Permissions are needed, it's capable to ask the user for them.
     */
    private void checkPermissions() {
        if(Build.VERSION.SDK_INT >= 23) {
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.WRITE_EXTERNAL_STORAGE");
            if (permissionCheck != 0) {
                this.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        }
    }

    private Boolean isLocationEnable(){
        int locationMode;
        try {
            locationMode = Settings.Secure.getInt(getApplicationContext().getContentResolver(), Settings.Secure.LOCATION_MODE);

        } catch (Settings.SettingNotFoundException e) {
            return false;
        }

        return locationMode != Settings.Secure.LOCATION_MODE_OFF;
    }

    @OnClick(R.id.retry_button)
    public void onClickRetry(View v){
        retry_linearLayout.setVisibility(View.GONE);
        search_linearLayout.setVisibility(View.VISIBLE);
        scanLeDevice(true);
    }

    @OnClick(R.id.switch_dark_mode)
    public void onDarkModeToggleClick(View v)
    {
        if (isNightModeOn){
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            sharedPrefsEdit.putBoolean("NightMode", false);
            sharedPrefsEdit.apply();
        }else{
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            sharedPrefsEdit.putBoolean("NightMode", true);
            sharedPrefsEdit.apply();
        }
    }
}
