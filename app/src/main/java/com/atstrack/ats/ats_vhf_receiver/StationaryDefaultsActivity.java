package com.atstrack.ats.ats_vhf_receiver;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.atstrack.ats.ats_vhf_receiver.BluetoothATS.BluetoothLeService;
import com.atstrack.ats.ats_vhf_receiver.Utils.Converters;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StationaryDefaultsActivity extends AppCompatActivity {

    @BindView(R.id.device_name_stationaryDefaults)
    TextView device_name_textView;
    @BindView(R.id.device_address_stationaryDefaults)
    TextView device_address_textView;
    @BindView(R.id.percent_battery_stationaryDefaults)
    TextView percent_battery_textView;
    @BindView(R.id.stationary_tables_spinner)
    Spinner stationary_tables_spinner;
    @BindView(R.id.stationary_antennas_spinner)
    Spinner stationary_antennas_spinner;
    @BindView(R.id.stationary_gps_switch)
    SwitchCompat stationary_gps_switch;
    @BindView(R.id.stationary_auto_record_switch)
    SwitchCompat stationary_auto_record_switch;
    @BindView(R.id.scan_timeout_stationary_defaults_editText)
    EditText scan_timeout_stationary_defaults_editText;
    @BindView(R.id.scan_rate_stationary_defaults_editText)
    EditText scan_rate_stationary_defaults_editText;

    private final static String TAG = StationaryDefaultsActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String EXTRAS_BATTERY = "DEVICE_BATTERY";
    private final int MESSAGE_PERIOD = 3000;

    private String mDeviceName;
    private String mDeviceAddress;
    private String mPercentBattery;
    private BluetoothLeService mBluetoothLeService;
    private boolean state = true;

    private Handler mHandler;
    private int heightPixels;
    private int widthPixels;

    private List<String> tables;
    private int positionFrequencyTableNumber;
    private int numberOfAntennas;

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
                    if (parameter.equals("save")) {
                        onClickSave();
                    } else if (parameter.equals("stationary")){
                        onClickStationaryDefaults();
                    }
                } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                    byte[] packet = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                    if (parameter.equals("stationary"))
                        downloadData(packet);
                    else if (parameter.equals("save"))
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

    private void onClickStationaryDefaults(){
        UUID uservice = UUID.fromString("8d60a8bb-1f60-4703-92ff-411103c493e6");
        UUID uservicechar = UUID.fromString("6dd91f4d-b30b-46c4-b111-dd49cd1f952e");
        mBluetoothLeService.readCharacteristicDiagnostic(uservice, uservicechar);
    }

    private void onClickSave() {
        positionFrequencyTableNumber = stationary_tables_spinner.getSelectedItemPosition();
        numberOfAntennas = stationary_antennas_spinner.getSelectedItemPosition() + 1;
        int info = (stationary_gps_switch.isChecked() ? 1 : 0) << 7;
        info = info | ((stationary_auto_record_switch.isChecked() ? 1 : 0) << 6);
        info  = info | numberOfAntennas;
        byte[] b = new byte[]{(byte) 0x7D, (byte) Integer.parseInt(tables.get(positionFrequencyTableNumber).replace("Table ", "")),
                (byte) info, (byte) Integer.parseInt(scan_rate_stationary_defaults_editText.getText().toString()),
                (byte) Integer.parseInt(scan_timeout_stationary_defaults_editText.getText().toString()), (byte) 0x0, (byte) 0x0, (byte) 0x0};

        UUID uservice = UUID.fromString("8d60a8bb-1f60-4703-92ff-411103c493e6");
        UUID uservicechar = UUID.fromString("6dd91f4d-b30b-46c4-b111-dd49cd1f952e");
        mBluetoothLeService.writeCharacteristic( uservice,uservicechar,b);

        finish();
        mBluetoothLeService.disconnect();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stationary_defaults);
        ButterKnife.bind(this);

        getSupportActionBar().setElevation(0);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_chevron_back_icon_opt);
        getSupportActionBar().setTitle("STATIONARY DEFAULTS");

        heightPixels = getResources().getDisplayMetrics().heightPixels;
        widthPixels = getResources().getDisplayMetrics().widthPixels;

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        mPercentBattery = intent.getStringExtra(EXTRAS_BATTERY);
        parameter = "stationary";
        tables = new ArrayList<>();

        ArrayAdapter<CharSequence> antennasAdapter = ArrayAdapter.createFromResource(this, R.array.antennas, android.R.layout.simple_spinner_item);
        antennasAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        stationary_antennas_spinner.setAdapter(antennasAdapter);

        device_name_textView.setText(mDeviceName);
        device_address_textView.setText(mDeviceAddress);
        percent_battery_textView.setText(mPercentBattery);

        mHandler = new Handler();

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: //hago un case por si en un futuro agrego mas opciones
                parameter = "save";
                mBluetoothLeService.connect(mDeviceAddress);
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
        if (!mConnected && !state)
            showMessageDisconnect();
        return true;
    }

    private void showMessageDisconnect() {
        LayoutInflater inflater = LayoutInflater.from(this);

        View view =inflater.inflate(R.layout.disconnect_message, null);
        final androidx.appcompat.app.AlertDialog dialog = new AlertDialog.Builder(this).create();

        dialog.setView(view);
        dialog.show();
        //dialog.getWindow().setLayout(widthPixels * 29 / 30, heightPixels * 2 / 3);

        new Handler().postDelayed(() -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }, MESSAGE_PERIOD);
    }

    private void downloadData(byte[] data) {
        mBluetoothLeService.disconnect();
        setTables(data);
        stationary_tables_spinner.setSelection(positionFrequencyTableNumber);
        int gps = Integer.parseInt(Converters.getDecimalValue(data[2])) >> 7 & 1;
        stationary_gps_switch.setChecked(gps == 1);
        int autoRecord = Integer.parseInt(Converters.getDecimalValue(data[2])) >> 6 & 1;
        stationary_auto_record_switch.setChecked(autoRecord == 1);
        int antennaNumber = Integer.parseInt(Converters.getDecimalValue(data[2])) & 15;
        stationary_antennas_spinner.setSelection(antennaNumber - 1);
        scan_rate_stationary_defaults_editText.setText(Converters.getDecimalValue(data[3]));
        scan_timeout_stationary_defaults_editText.setText(Converters.getDecimalValue(data[4]));
    }

    private void setTables(byte[] data) {
        positionFrequencyTableNumber = 0;
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
        stationary_tables_spinner.setAdapter(tablesAdapter);
    }

    private void showMessage(byte[] data) {
        int status = Integer.parseInt(Converters.getDecimalValue(data[0]));

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Success!");
        if (status == 0)
            builder.setMessage("Completed.");
        builder.setPositiveButton("OK", (dialog, which) -> {
            Intent intent = new Intent(this, EditReceiverDefaultsActivity.class);
            intent.putExtra(EditReceiverDefaultsActivity.EXTRAS_DEVICE_NAME, mDeviceName);
            intent.putExtra(EditReceiverDefaultsActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
            intent.putExtra(EditReceiverDefaultsActivity.EXTRAS_BATTERY, mPercentBattery);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
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
