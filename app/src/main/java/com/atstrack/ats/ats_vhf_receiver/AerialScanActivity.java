package com.atstrack.ats.ats_vhf_receiver;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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

public class AerialScanActivity extends AppCompatActivity {

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
    @BindView(R.id.ready_aerial_scan_LinearLayout)
    LinearLayout ready_aerial_scan_LinearLayout;
    @BindView(R.id.ready_aerial_textView)
    TextView ready_aerial_textView;
    @BindView(R.id.scan_rate_aerial_textView)
    TextView scan_rate_aerial_textView;
    @BindView(R.id.selected_frequency_aerial_textView)
    TextView selected_frequency_aerial_textView;
    @BindView(R.id.number_antennas_aerial_textView)
    TextView number_antennas_aerial_textView;
    @BindView(R.id.gps_aerial_textView)
    TextView gps_aerial_textView;
    @BindView(R.id.auto_record_aerial_textView)
    TextView auto_record_aerial_textView;
    @BindView(R.id.edit_aerial_defaults_textView)
    TextView edit_aerial_defaults_textView;
    @BindView(R.id.frequency_empty_textView)
    TextView frequency_empty_textView;
    @BindView(R.id.start_aerial_button)
    Button start_aerial_button;
    @BindView(R.id.aerial_result_linearLayout)
    LinearLayout aerial_result_linearLayout;
    @BindView(R.id.table_aerial_textView)
    TextView table_aerial_textView;
    @BindView(R.id.table_index_aerial_textView)
    TextView table_index_aerial_textView;
    @BindView(R.id.scan_rateD_aerial_textView)
    TextView scan_rateD_aerial_textView;
    @BindView(R.id.frequency_aerial_textView)
    TextView frequency_aerial_textView;
    @BindView(R.id.first_result_aerial_textView)
    TextView firstResultTextView;
    @BindView(R.id.second_result_aerial_textView)
    TextView secondResultTextView;
    @BindView(R.id.third_result_aerial_textView)
    TextView thirdResultTextView;
    @BindView(R.id.forth_result_aerial_textView)
    TextView forthResultTextView;
    @BindView(R.id.fifth_result_aerial_textView)
    TextView fifthResultTextView;
    @BindView(R.id.sixth_result_aerial_textView)
    TextView sixthResultTextView;

    private final static String TAG = AerialScanActivity.class.getSimpleName();

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
    private float scanRate;
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
                        case "aerial": // Gets aerial defaults data
                            onClickAerial();
                            break;
                        case "startAerial": // Starts to scan
                            onClickStart();
                            break;
                        case "sendLog": // Receives the data
                            onClickLog();
                            break;
                        case "stopAerial": // Stops scan
                            onClickStop();
                            break;
                    }
                } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                    byte[] packet = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                    switch (parameter) {
                        case "aerial": // Gets aerial defaults data
                            downloadData(packet);
                            break;
                        case "sendLog": // Receives the data
                            setCurrentLog(packet);
                            break;
                        case "stopAerial": // Stops scan
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
     * Requests a read for get aerial defaults data.
     * Service name: Scan.
     * Characteristic name: Aerial.
     */
    private void onClickAerial() {
        UUID uservice = UUID.fromString("8d60a8bb-1f60-4703-92ff-411103c493e6");
        UUID uservicechar = UUID.fromString("111584dd-b374-417c-a51d-9314eba66d6c");
        mBluetoothLeService.readCharacteristicDiagnostic(uservice, uservicechar);
    }

    /**
     * Writes the aerial scan data for start to scan.
     * Service name: Scan.
     * Characteristic name: Aerial.
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
                (byte) 0x82, (byte) selectedFrequency, (byte) numberAntennas, (byte) (scanRate * 10), 0, (byte) gps, (byte) autoRecord,
                (byte) 0xff, (byte) 0x7f, (byte) (YY % 100), (byte) MM, (byte) DD, (byte) hh, (byte) mm, (byte) ss, (byte) (ms / 100), (byte) (ms % 100)};

        UUID uservice = UUID.fromString("8d60a8bb-1f60-4703-92ff-411103c493e6");
        UUID uservicechar = UUID.fromString("111584dd-b374-417c-a51d-9314eba66d6c");
        mBluetoothLeService.writeCharacteristic(uservice, uservicechar, b, true);

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
     * Characteristic name: Aerial.
     */
    private void onClickStop() {
        parameter = "aerial";
        byte[] b = new byte[]{(byte) 0x87};

        UUID uservice = UUID.fromString("8d60a8bb-1f60-4703-92ff-411103c493e6");
        UUID uservicechar = UUID.fromString("111584dd-b374-417c-a51d-9314eba66d6c");
        mBluetoothLeService.writeCharacteristic(uservice, uservicechar, b, false);

        scanning = false;
        getSupportActionBar().show();
        state_view.setVisibility(View.VISIBLE);
        clear();
        aerial_result_linearLayout.setVisibility(View.GONE);
        ready_aerial_scan_LinearLayout.setVisibility(View.VISIBLE);

        new Handler().postDelayed(() -> {
            onRestartConnection();
        }, 500);
    }

    @OnClick(R.id.edit_aerial_defaults_textView)
    public void onClickEditDefaults(View v) {
        Intent intent = new Intent(this, AerialDefaultsActivity.class);
        intent.putExtra(AerialDefaultsActivity.EXTRAS_DEVICE_NAME, mDeviceName);
        intent.putExtra(AerialDefaultsActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
        intent.putExtra(AerialDefaultsActivity.EXTRAS_BATTERY, mPercentBattery);
        startActivity(intent);
        mBluetoothLeService.disconnect();
    }

    @OnClick(R.id.start_aerial_button)
    public void onClickStartAerial(View v) {
        parameter = "startAerial";
        mBluetoothLeService.connect(mDeviceAddress);
        getSupportActionBar().hide();
        state_view.setVisibility(View.GONE);
        ready_aerial_scan_LinearLayout.setVisibility(View.GONE);
        aerial_result_linearLayout.setVisibility(View.VISIBLE);
    }

    @OnClick(R.id.aerial_exit_image)
    public void onClickExit(View  v) {
        parameter = "stopAerial";
        onRestartConnection();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aerial_scan);
        ButterKnife.bind(this);

        // Customize the activity menu
        setSupportActionBar(toolbar);
        title_toolbar.setText("Mobile Scanning");
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
            ready_aerial_scan_LinearLayout.setVisibility(View.GONE);
            aerial_result_linearLayout.setVisibility(View.VISIBLE);
        } else { // Gets aerial defaults data
            parameter = "aerial";
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
            builder.setTitle("Stop Aerial");
            builder.setMessage("Are you sure you want to stop scanning?");
            builder.setPositiveButton("OK", (dialog, which) -> {
                parameter = "stopAerial";
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
     * With the received packet, gets aerial defaults data.
     *
     * @param data The received packet.
     */
    private void downloadData(byte[] data) {
        mBluetoothLeService.disconnect();
        if (!response) {
            showMessage(new byte[]{0});
            response = true;
        }
        parameter = "aerial";
        if (Converters.getHexValue(data[0]).equals("7D")) {
            if (Integer.parseInt(Converters.getDecimalValue(data[1])) == 0) { // There are no tables with frequencies to scan
                selected_frequency_aerial_textView.setText("None");
                ready_aerial_textView.setText("Not Ready to Scan");
                frequency_empty_textView.setVisibility(View.VISIBLE);
                start_aerial_button.setEnabled(false);
                start_aerial_button.setBackgroundResource(R.color.slate_gray);
                start_aerial_button.setTextColor(ContextCompat.getColor(this, R.color.ghost));
            } else { // Shows the table to be scanned
                selected_frequency_aerial_textView.setText(Converters.getDecimalValue(data[1]));
                ready_aerial_textView.setText("Ready to Scan");
                frequency_empty_textView.setVisibility(View.GONE);
                start_aerial_button.setEnabled(true);
                start_aerial_button.setBackgroundResource(R.color.mountain_meadow);
                start_aerial_button.setTextColor(ContextCompat.getColor(this, R.color.white));
            }
            selectedFrequency = Integer.parseInt(Converters.getDecimalValue(data[1]));
            numberAntennas = Integer.parseInt(Converters.getDecimalValue(data[2])) & 15;
            number_antennas_aerial_textView.setText("" + numberAntennas);
            scanRate = (float) (Integer.parseInt(Converters.getDecimalValue(data[3])) * 0.1);
            scan_rate_aerial_textView.setText(String.valueOf(scanRate));
            gps = Integer.parseInt(Converters.getDecimalValue(data[2])) >> 7 & 1;
            gps_aerial_textView.setText((gps == 1) ? "ON" : "OFF");
            autoRecord = Integer.parseInt(Converters.getDecimalValue(data[2])) >> 6 & 1;
            auto_record_aerial_textView.setText((autoRecord == 1) ? "ON" : "OFF");
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
            case "82":
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
        table_aerial_textView.setText(String.valueOf(selectedFrequency));
        table_index_aerial_textView.setText(Converters.getDecimalValue(data[3]));
        scan_rateD_aerial_textView.setText(String.valueOf(scanRate));
        frequency_aerial_textView.setText(String.valueOf(freqOffset).substring(0, 3) + "." + String.valueOf(freqOffset).substring(3));
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
        table_aerial_textView.setText("");
        table_index_aerial_textView.setText("");
        scan_rateD_aerial_textView.setText("");
        frequency_aerial_textView.setText("");
        firstResultTextView.setText("");
        secondResultTextView.setText("");
        thirdResultTextView.setText("");
        forthResultTextView.setText("");
        fifthResultTextView.setText("");
        sixthResultTextView.setText("");
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
