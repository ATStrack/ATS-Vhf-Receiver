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
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.atstrack.ats.ats_vhf_receiver.BluetoothATS.BluetoothLeService;
import com.atstrack.ats.ats_vhf_receiver.Utils.Converters;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InputValueActivity extends AppCompatActivity {

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
    @BindView(R.id.input_value_linearLayout)
    LinearLayout input_value_linearLayout;
    @BindView(R.id.spinner_value_linearLayout)
    LinearLayout spinner_value_linearLayout;
    @BindView(R.id.value_editText)
    EditText value_editText;
    @BindView(R.id.value_spinner)
    Spinner value_spinner;
    @BindView(R.id.message_error_textView)
    TextView message_error_textView;

    private final static String TAG = InputValueActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String EXTRAS_BATTERY = "DEVICE_BATTERY";
    private static final long MESSAGE_PERIOD = 3000;

    public static final int FREQUENCY_TABLE_NUMBER = 1001;
    public static final int SCAN_RATE_SECONDS = 1002;
    public static final int NUMBER_OF_ANTENNAS = 1003;
    public static final int SCAN_TIMEOUT_SECONDS = 1004;

    private String mDeviceName;
    private String mDeviceAddress;
    private String mPercentBattery;
    private BluetoothLeService mBluetoothLeService;
    private boolean state = true;

    private int value;

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
                    if (parameter.equals("aerial")) { // Gets aerial defaults data
                        onClickAerialDefaults();
                    } else if (parameter.equals("stationary")) { // Gets stationary defaults data
                        onClickStationaryDefaults();
                    }
                } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                    byte[] packet = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                    if (value == FREQUENCY_TABLE_NUMBER)
                        downloadTable(packet);
                    if (value == SCAN_RATE_SECONDS)
                        downloadScanRate(packet);
                    if (value == NUMBER_OF_ANTENNAS)
                        downloadAntennas(packet);
                    if (value == SCAN_TIMEOUT_SECONDS)
                        downloadTimeout(packet);
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
     * Requests a read for get stationary defaults data.
     * Service name: Scan.
     * Characteristic name: Stationary.
     */
    private void onClickStationaryDefaults() {
        UUID uservice = UUID.fromString("8d60a8bb-1f60-4703-92ff-411103c493e6");
        UUID uservicechar = UUID.fromString("6dd91f4d-b30b-46c4-b111-dd49cd1f952e");
        mBluetoothLeService.readCharacteristicDiagnostic(uservice, uservicechar);
    }

    @OnClick(R.id.save_changes_input_value_button)
    public void onClickSaveChanges(View v) {
        if (parameter.equals("aerial") && value == SCAN_RATE_SECONDS) { // Sends the scan rate value for aerial
            float scanRate = Float.parseFloat(value_spinner.getSelectedItem().toString());
            setResult((int) (scanRate * 10));
        }
        if (parameter.equals("stationary") && value == SCAN_RATE_SECONDS) { // Sends the scan rate value for stationary
            int scanRate = Integer.parseInt(value_editText.getText().toString());
            setResult(scanRate);
        }
        if (value == FREQUENCY_TABLE_NUMBER) { // Sends the frequency table number
            int frequencyTableNumber = Integer.parseInt(value_spinner.getSelectedItem().toString().replace("Table ", ""));
            setResult(frequencyTableNumber);
        }
        if (value == NUMBER_OF_ANTENNAS) { // Sends the number of antennas
            int numberAntennas = Integer.parseInt(value_spinner.getSelectedItem().toString());
            setResult(numberAntennas);
        }
        if (value == SCAN_TIMEOUT_SECONDS) { // Sends scan timeout value
            int timeout = Integer.parseInt(value_editText.getText().toString());
            setResult(timeout);
        }
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_value);
        ButterKnife.bind(this);

        // Customize the activity menu
        setSupportActionBar(toolbar);
        title_toolbar.setText("Edit Receiver Defaults");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Get device data from previous activity
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        mPercentBattery = intent.getStringExtra(EXTRAS_BATTERY);

        parameter = getIntent().getStringExtra("type");
        value = getIntent().getIntExtra("value", 0);

        if (parameter.equals("aerial") && value == SCAN_RATE_SECONDS) {
            spinner_value_linearLayout.setVisibility(View.VISIBLE);
        }
        if (parameter.equals("stationary") && value == SCAN_RATE_SECONDS) {
            input_value_linearLayout.setVisibility(View.VISIBLE);
        }
        if (value == FREQUENCY_TABLE_NUMBER || value == NUMBER_OF_ANTENNAS) {
            spinner_value_linearLayout.setVisibility(View.VISIBLE);
        }
        if (value == SCAN_TIMEOUT_SECONDS) {
            input_value_linearLayout.setVisibility(View.VISIBLE);
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
            case android.R.id.home: //Go back to the previous activity
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
     * With the received packet, gets frequency table number and display on the screen.
     *
     * @param data The received packet.
     */
    public void downloadTable(byte[] data) {
        List<String> tables = new ArrayList<>();
        int positionFrequencyTableNumber = 0;
        byte b = data[6];
        for (int i = 1; i <= 8; i++) {
            if ((b & 1) == 1) {
                tables.add("Table " + i);
                positionFrequencyTableNumber = (i == Integer.parseInt(Converters.getDecimalValue(data[1]))) ? tables.size() - 1 : 0;
            }
            b = (byte) (b >> 1);
        }
        b = data[7];
        for (int i = 9; i <= 12; i++) {
            if ((b & 1) == 1) {
                tables.add("Table " + i);
                positionFrequencyTableNumber = (i == Integer.parseInt(Converters.getDecimalValue(data[1]))) ? tables.size() - 1 : 0;
            }
            b = (byte) (b >> 1);
        }
        if (tables.isEmpty()) {
            tables.add("None");
        }
        ArrayAdapter<String> tablesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tables);
        tablesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        value_spinner.setAdapter(tablesAdapter);
        value_spinner.setSelection(positionFrequencyTableNumber);
    }

    /**
     * With the received packet, gets scan rate value and display on the screen.
     *
     * @param data The received packet.
     */
    public void downloadScanRate(byte[] data) {
        if (parameter.equals("aerial")) {
            ArrayAdapter<CharSequence> scanRateAdapter = ArrayAdapter.createFromResource(this, R.array.scanRate, android.R.layout.simple_spinner_item);
            scanRateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            value_spinner.setAdapter(scanRateAdapter);

            int index = 0;
            for (int i = 0; i < 49; i++) {
                String item = value_spinner.getItemAtPosition(i).toString().replace(".", "");
                if (item.equals(Converters.getDecimalValue(data[3]))) {
                    index = i;
                    break;
                }
            }
            value_spinner.setSelection(index);
        } else {
            float scanRate = (float) (Integer.parseInt(Converters.getDecimalValue(data[3])) * 0.1);
            value_editText.setText(String.valueOf(scanRate));
        }
    }

    /**
     * With the received packet, gets number of antennas and display on the screen.
     *
     * @param data The received packet.
     */
    public void downloadAntennas(byte[] data) {
        ArrayAdapter<CharSequence> antennasAdapter = ArrayAdapter.createFromResource(this, R.array.antennas, android.R.layout.simple_spinner_item);
        antennasAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        value_spinner.setAdapter(antennasAdapter);

        int antennaNumber = Integer.parseInt(Converters.getDecimalValue(data[2])) & 15;
        value_spinner.setSelection(antennaNumber - 1);
    }

    /**
     * With the received packet, gets scan timeout value and display on the screen.
     *
     * @param data The received packet.
     */
    public void downloadTimeout(byte[] data) {
        value_editText.setText(Converters.getDecimalValue(data[4]));
    }
}