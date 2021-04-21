package com.atstrack.ats.ats_vhf_receiver;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
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
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.atstrack.ats.ats_vhf_receiver.BluetoothATS.BluetoothLeService;
import com.atstrack.ats.ats_vhf_receiver.Utils.Converters;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.UUID;

import static com.atstrack.ats.ats_vhf_receiver.R.color.light_blue;
import static com.atstrack.ats.ats_vhf_receiver.R.color.tall_poppy;

public class StationaryScanActivity extends AppCompatActivity {

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
    @BindView(R.id.ready_stationary_scan_LinearLayout)
    LinearLayout ready_stationary_scan_LinearLayout;
    @BindView(R.id.ready_stationary_textView)
    TextView ready_stationary_textView;
    @BindView(R.id.scan_rate_stationary_textView)
    TextView scan_rate_stationary_textView;
    @BindView(R.id.selected_frequency_stationary_textView)
    TextView selected_frequency_stationary_textView;
    @BindView(R.id.number_antennas_stationary_textView)
    TextView number_antennas_stationary_textView;
    @BindView(R.id.timeout_stationary_textView)
    TextView timeout_stationary_textView;
    @BindView(R.id.gps_stationary_textView)
    TextView gps_stationary_textView;
    @BindView(R.id.auto_record_stationary_textView)
    TextView auto_record_stationary_textView;
    @BindView(R.id.edit_stationary_defaults_textView)
    TextView edit_stationary_defaults_textView;
    @BindView(R.id.frequency_empty_textView)
    TextView frequency_empty_textView;
    @BindView(R.id.start_stationary_button)
    Button start_stationary_button;
    @BindView(R.id.stationary_result_constraintLayout)
    ConstraintLayout stationary_result_constraintLayout;
    @BindView(R.id.table_freq_stationary)
    TextView table_freq;
    @BindView(R.id.first_result_stationary_textView)
    TextView firstResultTextView;
    @BindView(R.id.second_result_stationary_textView)
    TextView secondResultTextView;
    @BindView(R.id.third_result_stationary_textView)
    TextView thirdResultTextView;
    @BindView(R.id.forth_result_stationary_textView)
    TextView forthResultTextView;
    @BindView(R.id.fifth_result_stationary_textView)
    TextView fifthResultTextView;
    @BindView(R.id.sixth_result_stationary_textView)
    TextView sixthResultTextView;
    @BindView(R.id.seventh_result_stationary_textView)
    TextView seventhResultTextView;
    @BindView(R.id.eighth_result_stationary_textView)
    TextView eighthResultTextView;
    @BindView(R.id.ninth_result_stationary_textView)
    TextView ninthResultTextView;
    @BindView(R.id.tenth_result_stationary_textView)
    TextView tenthResultTextView;

    private final static String TAG = StationaryScanActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String EXTRAS_BATTERY = "DEVICE_BATTERY";
    private final int MESSAGE_PERIOD = 3000;

    private String mDeviceName;
    private String mDeviceAddress;
    private String mPercentBattery;
    private BluetoothLeService mBluetoothLeService;
    private boolean state = true;
    private boolean response = true;

    private boolean scanning;
    private String currentData;
    private int selectedFrequency;
    private int numberAntennas;
    private int scanRate;
    private int timeout;
    private int gps;
    private int autoRecord;
    private String year;
    private String month;
    private boolean mortality;

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
                    switch (parameter) {
                        case "stationary": // Gets stationary defaults data
                            onClickStationary();
                            break;
                        case "startStationary": // Starts to scan
                            onClickStart();
                            break;
                        case "sendLog": // Receives the data
                            onClickLog();
                            break;
                        case "stopStationary": // Stops scan
                            onClickStop();
                            break;
                    }
                } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                    byte[] packet = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                    switch (parameter) {
                        case "stationary": // Gets stationary defaults data
                            downloadData(packet);
                            break;
                        case "sendLog": // Receives the data
                            setCurrentLog(packet);
                            break;
                        case "stopStationary": // Stops scan
                            showMessage(packet);
                            break;
                    }
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
     * Requests a read for get stationary defaults data.
     * Service name: Scan.
     * Characteristic name: Stationary.
     */
    private void onClickStationary() {
        UUID uservice = UUID.fromString("8d60a8bb-1f60-4703-92ff-411103c493e6");
        UUID uservicechar = UUID.fromString("6dd91f4d-b30b-46c4-b111-dd49cd1f952e");
        mBluetoothLeService.readCharacteristicDiagnostic(uservice, uservicechar);
    }

    /**
     * Writes the stationary scan data for start to scan.
     * Service name: Scan.
     * Characteristic name: Stationary.
     */
    private void onClickStart() {
        parameter = "sendLog";

        Calendar currentDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        int YY = currentDate.get(Calendar.YEAR);
        int MM = currentDate.get(Calendar.MONTH);
        int DD = currentDate.get(Calendar.DAY_OF_MONTH);
        int hh = currentDate.get(Calendar.HOUR_OF_DAY);
        int mm =  currentDate.get(Calendar.MINUTE);
        int ss = currentDate.get(Calendar.SECOND);
        int ms = currentDate.get(Calendar.MILLISECOND);

        byte[] b = new byte[]{
                (byte) 0x83, (byte) selectedFrequency, (byte) numberAntennas, (byte) scanRate, (byte) timeout, (byte) gps, (byte) autoRecord,
                (byte) 0xff, (byte) 0x7f, (byte) (YY % 100), (byte) MM, (byte) DD, (byte) hh, (byte) mm, (byte) ss, (byte) (ms / 100), (byte) (ms % 100)};

        UUID uservice = UUID.fromString("8d60a8bb-1f60-4703-92ff-411103c493e6");
        UUID uservicechar = UUID.fromString("6dd91f4d-b30b-46c4-b111-dd49cd1f952e");
        mBluetoothLeService.writeCharacteristic( uservice,uservicechar,b, true);

        scanning = true;
    }

    /**
     * Enables notification for receive the data.
     * Service name: Screen.
     * Characteristic name: SendLog.
     */
    private void onClickLog() {
        UUID uservice=UUID.fromString("26da3d0d-9119-48bb-af48-b0b96c665a66");
        UUID uservicechar=UUID.fromString("7052b8df-95f9-4ba3-8324-0d8ff9232435");
        mBluetoothLeService.setCharacteristicNotificationRead(uservice, uservicechar, true);
    }

    /**
     * Writes a value for stop scan.
     * Service name: Scan.
     * Characteristic name: Stationary.
     */
    private void onClickStop() {
        parameter = "stationary";
        byte[] b = new byte[]{(byte)0x87};

        UUID uservice = UUID.fromString("8d60a8bb-1f60-4703-92ff-411103c493e6");
        UUID uservicechar = UUID.fromString("6dd91f4d-b30b-46c4-b111-dd49cd1f952e");
        mBluetoothLeService.writeCharacteristic(uservice, uservicechar, b, false);

        scanning =false;
        getSupportActionBar().show();
        state_view.setVisibility(View.VISIBLE);
        clear();
        stationary_result_constraintLayout.setVisibility(View.GONE);
        ready_stationary_scan_LinearLayout.setVisibility(View.VISIBLE);

        new Handler().postDelayed(() -> {
            onRestartConnection();
        }, 500);
    }

    @OnClick(R.id.edit_stationary_defaults_textView)
    public void onClickEditDefaults(View v) {
        Intent intent = new Intent(this, StationaryDefaultsActivity.class);
        intent.putExtra(StationaryDefaultsActivity.EXTRAS_DEVICE_NAME, mDeviceName);
        intent.putExtra(StationaryDefaultsActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
        intent.putExtra(StationaryDefaultsActivity.EXTRAS_BATTERY, mPercentBattery);
        startActivity(intent);
        mBluetoothLeService.disconnect();
    }

    @OnClick(R.id.start_stationary_button)
    public void onClickStartStationary(View v) {
        parameter = "startStationary";
        mBluetoothLeService.connect(mDeviceAddress);
        getSupportActionBar().hide();
        state_view.setVisibility(View.GONE);
        ready_stationary_scan_LinearLayout.setVisibility(View.GONE);
        stationary_result_constraintLayout.setVisibility(View.VISIBLE);
    }

    @OnClick(R.id.stationary_exit_image)
    public void onClickExit(View  v) {
        parameter = "stopStationary";
        onRestartConnection();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stationary_scan);
        ButterKnife.bind(this);

        // Customize the activity menu
        setSupportActionBar(toolbar);
        title_toolbar.setText("Stationary Scanning");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Get device data from previous activity
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        mPercentBattery = intent.getStringExtra(EXTRAS_BATTERY);
        scanning = intent.getExtras().getBoolean("scanning");

        mortality = false;

        if (scanning) { // The device is already scanning
            parameter = "sendLog";
            year = intent.getExtras().getString("year");
            month = intent.getExtras().getString("month");
            getSupportActionBar().hide();
            ready_stationary_scan_LinearLayout.setVisibility(View.GONE);
            stationary_result_constraintLayout.setVisibility(View.VISIBLE);
        } else { // Gets aerial defaults data
            parameter = "stationary";
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
                Intent intent = new Intent(this, StartScanningActivity.class);
                intent.putExtra(AerialScanActivity.EXTRAS_DEVICE_NAME, mDeviceName);
                intent.putExtra(AerialScanActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
                intent.putExtra(AerialScanActivity.EXTRAS_BATTERY, mPercentBattery);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                mBluetoothLeService.disconnect();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (scanning) { // Asks if you want to stop the scan
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Stop Stationary");
            builder.setMessage("Are you sure you want to stop scanning?");
            builder.setPositiveButton("OK", (dialog, which) -> {
                parameter = "stopStationary";
                onRestartConnection();
            });
            builder.setNegativeButton("Cancel", null);
            AlertDialog dialog = builder.create();
            dialog.show();
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.catskill_white)));
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
            showDisconnectionMessage();
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
     * With the received packet, gets stationary defaults data.
     *
     * @param data The received packet.
     */
    private void downloadData(byte[] data) {
        mBluetoothLeService.disconnect();
        if (!response) {
            showMessage(new byte[]{0});
            response = true;
        }
        parameter = "stationary";
        if (Converters.getHexValue(data[0]).equals("7C")) {
            if (Integer.parseInt(Converters.getDecimalValue(data[1])) == 0) { // There are no tables with frequencies to scan
                selected_frequency_stationary_textView.setText("None");
                ready_stationary_textView.setText("Not Ready to Scan");
                frequency_empty_textView.setVisibility(View.VISIBLE);
                start_stationary_button.setEnabled(false);
                start_stationary_button.setBackgroundResource(R.color.slate_gray);
                start_stationary_button.setTextColor(ContextCompat.getColor(this, R.color.ghost));
            } else { // Shows the table to be scanned
                selected_frequency_stationary_textView.setText(Converters.getDecimalValue(data[1]));
                ready_stationary_textView.setText("Ready to Scan");
                frequency_empty_textView.setVisibility(View.GONE);
                start_stationary_button.setEnabled(true);
                start_stationary_button.setBackgroundResource(R.color.mountain_meadow);
                start_stationary_button.setTextColor(ContextCompat.getColor(this, R.color.white));
            }
            selected_frequency_stationary_textView.setText(Converters.getDecimalValue(data[1]));
            selectedFrequency = Integer.parseInt(Converters.getDecimalValue(data[1]));
            numberAntennas = Integer.parseInt(Converters.getDecimalValue(data[2])) & 15;
            number_antennas_stationary_textView.setText("" + numberAntennas);
            scan_rate_stationary_textView.setText(Converters.getDecimalValue(data[3]));
            scanRate = Integer.parseInt(Converters.getDecimalValue(data[3]));
            timeout_stationary_textView.setText(Converters.getDecimalValue(data[4]));
            timeout = Integer.parseInt(Converters.getDecimalValue(data[4]));
            gps = Integer.parseInt(Converters.getDecimalValue(data[2])) >> 7 & 1;
            gps_stationary_textView.setText((gps == 1) ? "ON" : "OFF");
            autoRecord = Integer.parseInt(Converters.getDecimalValue(data[2])) >> 6 & 1;
            auto_record_stationary_textView.setText((autoRecord == 1) ? "ON" : "OFF");
        }
    }

    /**
     * With the received packet, gets the data of scanning.
     *
     * @param data The received packet.
     */
    public void setCurrentLog(byte[] data) {
        currentData = "";
        String format = Converters.getHexValue(data[0]);
        switch (format) {
            case "83":
                year = Converters.getDecimalValue(data[6]);
                month = Converters.getDecimalValue(data[7]);
                logFreq(new byte[]{data[8], data[9], data[10], data[11], data[12], data[13], data[14], data[15]});//byte 0 = F0
                break;
            case "F0":
                logFreq(data);
                break;
            case "F1":
                logCode(data);
                break;
        }
        refresh();
    }

    /**
     * With the received packet, processes the data to display.
     *
     * @param data The received packet.
     */
    public void logFreq(byte[] data){
        mortality = false;
        int freqOffset = 0;
        for (int i = 1;i < data.length; i++) {
            byte b = data[i];
            switch (i) {
                case 1:
                    freqOffset = Integer.parseInt(Converters.getDecimalValue(b)) * 256;
                    break;
                case 2:
                    freqOffset = (150 * 1000) + (Integer.parseInt(Converters.getDecimalValue(b)) + freqOffset);
                    break;
                case 4:
                    currentData += month + "/" + Converters.getDecimalValue(b) + "/" + year + "     ";
                    break;
                case 5:
                    currentData += Converters.getDecimalValue(b) + ":";
                    break;
                case 6:
                    currentData += Converters.getDecimalValue(b) + ":";
                    break;
                case 7:
                    currentData += Converters.getDecimalValue(b);
                    break;
            }
        }
        table_freq.setText("Table: " + selectedFrequency + " [" + Converters.getDecimalValue(data[3]) + "] Freq: " +
                String.valueOf(freqOffset).substring(0, 3) + "." + String.valueOf(freqOffset).substring(3));
    }

    /**
     * With the received packet, processes the data to display.
     *
     * @param data The received packet.
     */
    public void logCode(byte[] data){
        mortality = (Integer.parseInt(Converters.getDecimalValue(data[5])) >= 100);
        for (int i = 1;i < data.length; i++) {
            byte b = data[i];
            switch (i) {
                case 1:
                    currentData += "Sec:" + Converters.getDecimalValue(b) + " ";
                    break;
                case 2:
                    currentData += "A:" + (Integer.parseInt(Converters.getDecimalValue(b)) > 128 ?
                            Integer.parseInt(Converters.getDecimalValue(b)) - 128 : Converters.getDecimalValue(b)) + " ";//Si es mayor que x80 se resta x80
                    break;
                case 3:
                    currentData += "C:" +
                            ((Integer.valueOf(Converters.getDecimalValue(b)) < 10)? "0" + Converters.getDecimalValue(b): Converters.getDecimalValue(b))
                            + (mortality ? "M " : " ");
                    break;
                case 4:
                    currentData += "SS:" + (Integer.valueOf(Converters.getDecimalValue(b)) + 200) + " ";
                    break;
                case 5:
                    currentData += "#:" + (mortality ? Integer.parseInt(Converters.getDecimalValue(b)) - 100 : Converters.getDecimalValue(b));
                    break;
            }
        }
    }

    /**
     * Updates the data displayed on the screen.
     */
    public void refresh() {
        tenthResultTextView.setText(ninthResultTextView.getText());
        tenthResultTextView.setTextColor(ninthResultTextView.getText().toString().contains("M") ?
                ContextCompat.getColor(this, tall_poppy) : ContextCompat.getColor(this, light_blue));

        ninthResultTextView.setText(eighthResultTextView.getText());
        ninthResultTextView.setTextColor(eighthResultTextView.getText().toString().contains("M") ?
                ContextCompat.getColor(this, tall_poppy) : ContextCompat.getColor(this, light_blue));

        eighthResultTextView.setText(seventhResultTextView.getText());
        eighthResultTextView.setTextColor(seventhResultTextView.getText().toString().contains("M") ?
                ContextCompat.getColor(this, tall_poppy) : ContextCompat.getColor(this, light_blue));

        seventhResultTextView.setText(sixthResultTextView.getText());
        seventhResultTextView.setTextColor(sixthResultTextView.getText().toString().contains("M") ?
                ContextCompat.getColor(this, tall_poppy) : ContextCompat.getColor(this, light_blue));

        sixthResultTextView.setText(fifthResultTextView.getText());
        sixthResultTextView.setTextColor(fifthResultTextView.getText().toString().contains("M") ?
                ContextCompat.getColor(this, tall_poppy) : ContextCompat.getColor(this, light_blue));

        fifthResultTextView.setText(forthResultTextView.getText());
        fifthResultTextView.setTextColor(forthResultTextView.getText().toString().contains("M") ?
                ContextCompat.getColor(this, tall_poppy) : ContextCompat.getColor(this, light_blue));

        forthResultTextView.setText(thirdResultTextView.getText());
        forthResultTextView.setTextColor(thirdResultTextView.getText().toString().contains("M") ?
                ContextCompat.getColor(this, tall_poppy) : ContextCompat.getColor(this, light_blue));

        thirdResultTextView.setText(secondResultTextView.getText());
        thirdResultTextView.setTextColor(secondResultTextView.getText().toString().contains("M") ?
                ContextCompat.getColor(this, tall_poppy) : ContextCompat.getColor(this, light_blue));

        secondResultTextView.setText(firstResultTextView.getText());
        secondResultTextView.setTextColor(firstResultTextView.getText().toString().contains("M") ?
                ContextCompat.getColor(this, tall_poppy) : ContextCompat.getColor(this, light_blue));

        firstResultTextView.setText(currentData);
        firstResultTextView.setTextColor(mortality ?
                ContextCompat.getColor(this, tall_poppy) : ContextCompat.getColor(this, light_blue));
    }

    /**
     * Clears the screen to start displaying the data.
     */
    public void clear() {
        table_freq.setText("");
        firstResultTextView.setText("");
        secondResultTextView.setText("");
        thirdResultTextView.setText("");
        forthResultTextView.setText("");
        fifthResultTextView.setText("");
        sixthResultTextView.setText("");
        seventhResultTextView.setText("");
        eighthResultTextView.setText("");
        ninthResultTextView.setText("");
        tenthResultTextView.setText("");
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
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    public void onRestartConnection() {
        mBluetoothLeService.disconnect();
        SystemClock.sleep(1000);
        mBluetoothLeService.connect(mDeviceAddress);
    }
}
