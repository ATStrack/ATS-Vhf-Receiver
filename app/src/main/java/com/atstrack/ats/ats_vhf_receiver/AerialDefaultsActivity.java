package com.atstrack.ats.ats_vhf_receiver;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
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
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.atstrack.ats.ats_vhf_receiver.BluetoothATS.BluetoothLeService;
import com.atstrack.ats.ats_vhf_receiver.Utils.Converters;

import java.util.UUID;

public class AerialDefaultsActivity extends AppCompatActivity {

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
    @BindView(R.id.frequency_table_number_aerial_textView)
    TextView frequency_table_number_aerial_textView;
    @BindView(R.id.scan_rate_seconds_aerial_textView)
    TextView scan_rate_seconds_aerial_textView;
    @BindView(R.id.number_of_antennas_aerial_textView)
    TextView number_of_antennas_aerial_textView;
    @BindView(R.id.aerial_gps_switch)
    SwitchCompat aerial_gps_switch;
    @BindView(R.id.aerial_auto_record_switch)
    SwitchCompat aerial_auto_record_switch;

    private final static String TAG = AerialDefaultsActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String EXTRAS_BATTERY = "DEVICE_BATTERY";
    private static final long MESSAGE_PERIOD = 3000;

    private String mDeviceName;
    private String mDeviceAddress;
    private String mPercentBattery;
    private BluetoothLeService mBluetoothLeService;
    private boolean state = true;

    // Code to manage Service lifecycle.
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

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read or notification operations.
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
                    if (parameter.equals("save")) { // Save aerial defaults data
                        onClickSave();
                    } else if (parameter.equals("aerial")) { // Gets aerial defaults data
                        onClickAerialDefaults();
                    }
                } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                    byte[] packet = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                    if (parameter.equals("aerial")) // Gets aerial defaults data
                        downloadData(packet);
                    if (parameter.equals("save")) // Save aerial defaults data
                        showMessage(packet);
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

    /**
     * Requests a read for get aerial defaults data.
     * Service name: Scan.
     * Characteristic name: Aerial.
     */
    private void onClickAerialDefaults() {
        UUID uservice = UUID.fromString("8d60a8bb-1f60-4703-92ff-411103c493e6");
        UUID uservicechar = UUID.fromString("111584dd-b374-417c-a51d-9314eba66d6c");
        mBluetoothLeService.readCharacteristicDiagnostic(uservice, uservicechar);
    }

    /**
     * Writes the modified aerial defaults data by the user.
     * Service name: Scan.
     * Characteristic name: Aerial.
     */
    private void onClickSave() {
        int info = (aerial_gps_switch.isChecked() ? 1 : 0) << 7;
        info = info | ((aerial_auto_record_switch.isChecked() ? 1 : 0) << 6);
        info = info | Integer.parseInt(number_of_antennas_aerial_textView.getText().toString());
        float scanRate = Float.parseFloat(scan_rate_seconds_aerial_textView.getText().toString());
        int frequencyTableNumber = Integer.parseInt(frequency_table_number_aerial_textView.getText().toString());
        byte[] b = new byte[]{(byte) 0x7D, (byte) frequencyTableNumber, (byte) info, (byte) (scanRate * 10), 0, 0, 0, 0};

        UUID uservice = UUID.fromString("8d60a8bb-1f60-4703-92ff-411103c493e6");
        UUID uservicechar = UUID.fromString("111584dd-b374-417c-a51d-9314eba66d6c");
        mBluetoothLeService.writeCharacteristic(uservice, uservicechar, b, false);
        finish();
        mBluetoothLeService.disconnect();
    }

    @OnClick(R.id.frequency_table_number_aerial_imageView)
    public void onClickFrequencyTableNumber(View v) {
        Intent intent = new Intent(this, InputValueActivity.class);
        intent.putExtra(AerialDefaultsActivity.EXTRAS_DEVICE_NAME, mDeviceName);
        intent.putExtra(AerialDefaultsActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
        intent.putExtra(AerialDefaultsActivity.EXTRAS_BATTERY, mPercentBattery);
        intent.putExtra("type", "aerial");
        intent.putExtra("value", InputValueActivity.FREQUENCY_TABLE_NUMBER);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivityForResult(intent, InputValueActivity.FREQUENCY_TABLE_NUMBER);
        mBluetoothLeService.disconnect();
    }

    @OnClick(R.id.scan_rate_seconds_aerial_imageView)
    public void onClickScanRateSeconds(View v) {
        Intent intent = new Intent(this, InputValueActivity.class);
        intent.putExtra(AerialDefaultsActivity.EXTRAS_DEVICE_NAME, mDeviceName);
        intent.putExtra(AerialDefaultsActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
        intent.putExtra(AerialDefaultsActivity.EXTRAS_BATTERY, mPercentBattery);
        intent.putExtra("type", "aerial");
        intent.putExtra("value", InputValueActivity.SCAN_RATE_SECONDS);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivityForResult(intent, InputValueActivity.SCAN_RATE_SECONDS);
        mBluetoothLeService.disconnect();
    }

    @OnClick(R.id.number_of_antennas_aerial_imageView)
    public void onClickNumberOfAntennas(View v) {
        Intent intent = new Intent(this, InputValueActivity.class);
        intent.putExtra(AerialDefaultsActivity.EXTRAS_DEVICE_NAME, mDeviceName);
        intent.putExtra(AerialDefaultsActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
        intent.putExtra(AerialDefaultsActivity.EXTRAS_BATTERY, mPercentBattery);
        intent.putExtra("type", "aerial");
        intent.putExtra("value", InputValueActivity.NUMBER_OF_ANTENNAS);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivityForResult(intent, InputValueActivity.NUMBER_OF_ANTENNAS);
        mBluetoothLeService.disconnect();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aerial_defaults);
        ButterKnife.bind(this);

        // Customize the activity menu
        setSupportActionBar(toolbar);
        title_toolbar.setText("Mobile Defaults");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Get device data from previous activity
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        mPercentBattery = intent.getStringExtra(EXTRAS_BATTERY);
        parameter = "aerial";

        device_name_textView.setText(mDeviceName);
        device_address_textView.setText(mDeviceAddress);
        percent_battery_textView.setText(mPercentBattery);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == InputValueActivity.FREQUENCY_TABLE_NUMBER) { // Gets the modified frequency table number
            frequency_table_number_aerial_textView.setText(String.valueOf(resultCode));
        }
        if (requestCode == InputValueActivity.SCAN_RATE_SECONDS) { // Gets the modified scan rate
            scan_rate_seconds_aerial_textView.setText(String.valueOf(resultCode * 0.1));
        }
        if (requestCode == InputValueActivity.NUMBER_OF_ANTENNAS) { // Gets the modified number of antennas
            number_of_antennas_aerial_textView.setText(String.valueOf(resultCode));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: //Go back to the previous activity
                parameter = "save";
                onRestartConnection();
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
            showDisconnectionMessage();
        }
        return true;
    }

    /**
     * Shows an alert dialog because the connection with the BLE device was lost or the client disconnected it.
     */
    private void showDisconnectionMessage() {
        LayoutInflater inflater = LayoutInflater.from(this);

        View view =inflater.inflate(R.layout.disconnect_message, null);
        final androidx.appcompat.app.AlertDialog dialog = new AlertDialog.Builder(this).create();

        dialog.setView(view);
        dialog.show();

        // The message disappears after a pre-defined period and will search for other available BLE devices again
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }, MESSAGE_PERIOD);
    }

    /**
     * With the received packet, gets aerial defaults data.
     *
     * @param data The received packet.
     */
    private void downloadData(byte[] data) {
        mBluetoothLeService.disconnect();
        frequency_table_number_aerial_textView.setText(Converters.getDecimalValue(data[1]));
        int gps = Integer.parseInt(Converters.getDecimalValue(data[2])) >> 7 & 1;
        aerial_gps_switch.setChecked(gps == 1);
        int autoRecord = Integer.parseInt(Converters.getDecimalValue(data[2])) >> 6 & 1;
        aerial_auto_record_switch.setChecked(autoRecord == 1);
        int antennaNumber = Integer.parseInt(Converters.getDecimalValue(data[2])) & 15;
        number_of_antennas_aerial_textView.setText(String.valueOf(antennaNumber));
        float scanRate = (float) (Integer.parseInt(Converters.getDecimalValue(data[3])) * 0.1);
        scan_rate_seconds_aerial_textView.setText(String.valueOf(scanRate));
    }

    /**
     * Displays a message indicating whether the writing was successful.
     *
     * @param data This packet indicates the writing status.
     */
    private void showMessage(byte[] data) {
        int status = Integer.parseInt(Converters.getDecimalValue(data[0]));

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Success!");
        if (status == 0)
            builder.setMessage("Completed.");
        builder.setPositiveButton("OK", (dialog, which) -> {
            finish();
            mBluetoothLeService.disconnect();
        });
        builder.show();
    }

    public void onRestartConnection() {
        mBluetoothLeService.disconnect();
        SystemClock.sleep(1000);
        mBluetoothLeService.connect(mDeviceAddress);
    }
}
