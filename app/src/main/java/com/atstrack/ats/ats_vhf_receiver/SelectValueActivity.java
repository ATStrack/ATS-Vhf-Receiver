package com.atstrack.ats.ats_vhf_receiver;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.atstrack.ats.ats_vhf_receiver.BluetoothATS.BluetoothLeService;

import java.security.PublicKey;

public class SelectValueActivity extends AppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.title_toolbar)
    TextView title_toolbar;
    @BindView(R.id.state_view)
    View state_view;
    @BindView(R.id.device_name)
    TextView device_name_textView;
    @BindView(R.id.device_address)
    TextView device_address_textView;
    @BindView(R.id.percent_battery)
    TextView percent_battery_textView;
    @BindView(R.id.select_pulse_rate_linearLayout)
    LinearLayout select_pulse_rate_linearLayout;
    @BindView(R.id.fixed_filter_type_linearLayout)
    LinearLayout fixed_filter_type_linearLayout;
    @BindView(R.id.variable_filter_type_linearLayout)
    LinearLayout variable_filter_type_linearLayout;
    @BindView(R.id.fixed_pulse_rate_imageView)
    ImageView fixed_pulse_rate_imageView;
    @BindView(R.id.variable_pulse_rate_imageView)
    ImageView variable_pulse_rate_imageView;
    @BindView(R.id.pattern_matching_imageView)
    ImageView pattern_matching_imageView;
    @BindView(R.id.pulses_per_scan_time_imageView)
    ImageView pulses_per_scan_time_imageView;
    @BindView(R.id.temperature_imageView)
    ImageView temperature_imageView;
    @BindView(R.id.period_imageView)
    ImageView period_imageView;
    @BindView(R.id.altitude_imageView)
    ImageView altitude_imageView;
    @BindView(R.id.depth_imageView)
    ImageView depth_imageView;

    private final static String TAG = SelectValueActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String EXTRAS_BATTERY = "DEVICE_BATTERY";
    private static final long MESSAGE_PERIOD = 3000;

    public static final int PULSE_RATE = 1001;
    public static final int FILTER = 1002;
    public static final int FIXED_PULSE_RATE = 1003;
    public static final int VARIABLE_PULSE_RATE = 1004;
    public static final int PATTERN_MATCHING = 1005;
    public static final int PULSES_PER_SCAN_TIME = 1006;
    public static final int TEMPERATURE = 1007;
    public static final int PERIOD = 1008;
    public static final int ALTITUDE = 1009;
    public static final int DEPTH = 1010;

    private String mDeviceName;
    private String mDeviceAddress;
    private String mPercentBattery;
    private BluetoothLeService mBluetoothLeService;
    private boolean state = true;

    private int type;
    private int value;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG,"Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private boolean mConnected = false;
    private String parameter = "";

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                final String action = intent.getAction();
                if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                    mConnected = true;
                    invalidateOptionsMenu();
                } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                    mConnected = false;
//                    state = false;
                    invalidateOptionsMenu();
                } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {

                } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                    byte[] packet = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);

                }
            }
            catch (Exception e) {
                Timber.tag("DCA:BR 198").e(e, "Unexpected error.");
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    @OnClick(R.id.fixed_pulse_rate_linearLayout)
    public void onClickFixedPulseRate(View v) {
        fixed_pulse_rate_imageView.setVisibility(View.VISIBLE);
        variable_pulse_rate_imageView.setVisibility(View.GONE);
        value = FIXED_PULSE_RATE;
    }

    @OnClick(R.id.variable_pulse_rate_linearLayout)
    public void onClickVariablePulseRate(View v) {
        variable_pulse_rate_imageView.setVisibility(View.VISIBLE);
        fixed_pulse_rate_imageView.setVisibility(View.GONE);
        value = VARIABLE_PULSE_RATE;
    }

    @OnClick(R.id.pattern_matching_linearLayout)
    public void onClickPatternMatching(View v) {
        pattern_matching_imageView.setVisibility(View.VISIBLE);
        pulses_per_scan_time_imageView.setVisibility(View.GONE);
        value = PATTERN_MATCHING;
    }

    @OnClick(R.id.pulses_per_scan_time_linearLayout)
    public void onClickPulsesPerScanTime(View v) {
        pulses_per_scan_time_imageView.setVisibility(View.VISIBLE);
        pattern_matching_imageView.setVisibility(View.GONE);
        value = PULSES_PER_SCAN_TIME;
    }

    @OnClick(R.id.temperature_linearLayout)
    public void onClickTemperature(View v) {
        temperature_imageView.setVisibility(View.VISIBLE);
        period_imageView.setVisibility(View.GONE);
        altitude_imageView.setVisibility(View.GONE);
        depth_imageView.setVisibility(View.GONE);
        value = TEMPERATURE;
    }

    @OnClick(R.id.period_linearLayout)
    public void onClickPeriod(View v) {
        period_imageView.setVisibility(View.VISIBLE);
        temperature_imageView.setVisibility(View.GONE);
        altitude_imageView.setVisibility(View.GONE);
        depth_imageView.setVisibility(View.GONE);
        value = PERIOD;
    }

    @OnClick(R.id.altitude_linearLayout)
    public void onClickAltitude(View v) {
        altitude_imageView.setVisibility(View.VISIBLE);
        temperature_imageView.setVisibility(View.GONE);
        period_imageView.setVisibility(View.GONE);
        depth_imageView.setVisibility(View.GONE);
        value = ALTITUDE;
    }

    @OnClick(R.id.depth_linearLayout)
    public void onClickDepth(View v) {
        depth_imageView.setVisibility(View.VISIBLE);
        temperature_imageView.setVisibility(View.GONE);
        period_imageView.setVisibility(View.GONE);
        altitude_imageView.setVisibility(View.GONE);
        value = DEPTH;
    }

    @OnClick(R.id.save_changes_select_value_button)
    public void onClickSaveChanges(View v) {
        setResult(value);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_value);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        title_toolbar.setText("Edit Receiver Defaults");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        mPercentBattery = intent.getStringExtra(EXTRAS_BATTERY);

        type = intent.getIntExtra("type", 0);
        if (type == PULSE_RATE) {
            select_pulse_rate_linearLayout.setVisibility(View.VISIBLE);
            value = FIXED_PULSE_RATE;
        }
        if (type == FIXED_PULSE_RATE) {
            fixed_filter_type_linearLayout.setVisibility(View.VISIBLE);
            value = PATTERN_MATCHING;
        }
        if (type == VARIABLE_PULSE_RATE) {
            variable_filter_type_linearLayout.setVisibility(View.VISIBLE);
            value = TEMPERATURE;
        }

        device_name_textView.setText(mDeviceName);
        device_address_textView.setText(mDeviceAddress);
        percent_battery_textView.setText(mPercentBattery);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: //hago un case por si en un futuro agrego mas opciones
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG,"Connect request result= " + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mConnected && !state) {
            showMessageDisconnect();
        }
        return true;
    }

    private void showMessageDisconnect() {
        LayoutInflater inflater = LayoutInflater.from(this);

        View view =inflater.inflate(R.layout.disconnect_message, null);
        final androidx.appcompat.app.AlertDialog dialog = new AlertDialog.Builder(this).create();

        dialog.setView(view);
        dialog.show();

        new Handler().postDelayed(() -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }, MESSAGE_PERIOD);
    }
}