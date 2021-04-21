package com.atstrack.ats.ats_vhf_receiver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.vectordrawable.graphics.drawable.Animatable2Compat;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;
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
import android.graphics.drawable.Animatable;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
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
import com.atstrack.ats.ats_vhf_receiver.Utils.Converters;

import java.util.UUID;

public class MainMenuActivity extends AppCompatActivity {

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
    @BindView(R.id.menu_linearLayout)
    LinearLayout menu_linearLayout;
    @BindView(R.id.vhf_constraintLayout)
    ConstraintLayout vhf_linearLayout;
    @BindView(R.id.state_textView)
    TextView state_textView;
    @BindView(R.id.name_textView)
    TextView name_textView;
    @BindView(R.id.disconnect_button)
    TextView disconnect_button;
    @BindView(R.id.connecting_device_mainMenu)
    LinearLayout connecting_device_linearLayout;
    @BindView(R.id.disconnect_linearLayout)
    LinearLayout disconnect_constraintLayout;
    @BindView(R.id.check_avd_anim)
    ImageView check_avd_anim;

    private final static String TAG = MainMenuActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String EXTRAS_BATTERY = "DEVICE_BATTERY";
    private static final long MESSAGE_PERIOD = 1000;
    private static final long CONNECT_PERIOD = 3000;

    private String mDeviceName;
    private String mDeviceAddress;
    private String mPercentBattery;
    private BluetoothLeService mBluetoothLeService;
    private Handler mHandler;
    private Handler mHandlerMenu;
    private boolean scanning;

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
    private String parameter;

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
                    invalidateOptionsMenu();
                } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                    if (parameter.equals("scanning")) // Checks if the BLE device is scanning
                        onClickScanning();
                } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                    byte[] packet = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                    if (parameter.equals("scanning")) // Checks if the BLE device is scanning
                        download(packet);
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
     * Requests a read for check if the BLE device is scanning.
     * Service name: Diagnostic.
     * Characteristic name: BoardStatus.
     */
    private void onClickScanning() {
        UUID uservice=UUID.fromString("fab2d796-3364-4b54-b9a1-7735545814ad");
        UUID uservicechar=UUID.fromString("cae6ad69-2a38-4285-b6f8-6a2f8517d1fd");
        mBluetoothLeService.readCharacteristicDiagnostic(uservice, uservicechar);
    }

    @OnClick(R.id.connecting_device_mainMenu)
    public void onClickConnectingDevice(View v) {
        menu_linearLayout.setVisibility(View.GONE);
        connecting_device_linearLayout.setVisibility(View.GONE);
        vhf_linearLayout.setVisibility(View.GONE);
        getSupportActionBar().hide();
        state_view.setVisibility(View.GONE);

        disconnect_constraintLayout.setVisibility(View.VISIBLE);
    }

    @OnClick(R.id.disconnect_exit_image)
    public void onClickConnectExit(View v) {
        disconnect_constraintLayout.setVisibility(View.GONE);

        getSupportActionBar().show();
        state_view.setVisibility(View.VISIBLE);
        menu_linearLayout.setVisibility(View.VISIBLE);
        connecting_device_linearLayout.setVisibility(View.VISIBLE);
    }

    @OnClick(R.id.disconnect_button)
    public void onClickDisconnect(View v) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        mBluetoothLeService.disconnect();
    }

    @OnClick(R.id.start_scanning_button)
    public void onClickStartScanning(View v) {
        Intent intent = new Intent(this, StartScanningActivity.class);
        intent.putExtra(StartScanningActivity.EXTRAS_DEVICE_NAME, mDeviceName);
        intent.putExtra(StartScanningActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
        intent.putExtra(StartScanningActivity.EXTRAS_BATTERY, mPercentBattery);
        startActivity(intent);
        mBluetoothLeService.disconnect();
    }

    @OnClick(R.id.receiver_configuration_button)
    public void onClickReceiverConfiguration(View v) {
        Intent intent = new Intent(this, ReceiverConfigurationActivity.class);
        intent.putExtra(ReceiverConfigurationActivity.EXTRAS_DEVICE_NAME, mDeviceName);
        intent.putExtra(ReceiverConfigurationActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
        intent.putExtra(ReceiverConfigurationActivity.EXTRAS_BATTERY, mPercentBattery);
        startActivity(intent);
        mBluetoothLeService.disconnect();
    }

    @OnClick(R.id.manage_receiver_data_button)
    public void onClickManageReceiverData(View v) {
        Intent intent = new Intent(this, GetDataActivity.class);
        intent.putExtra(GetDataActivity.EXTRAS_DEVICE_NAME, mDeviceName);
        intent.putExtra(GetDataActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
        intent.putExtra(GetDataActivity.EXTRAS_BATTERY, mPercentBattery);
        startActivity(intent);
        mBluetoothLeService.disconnect();
    }

    @OnClick(R.id.test_receiver_button)
    public void onClickTestReceiver(View v) {
        Intent intent = new Intent(this, TestReceiverActivity.class);
        intent.putExtra(TestReceiverActivity.EXTRAS_DEVICE_NAME, mDeviceName);
        intent.putExtra(TestReceiverActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
        intent.putExtra(TestReceiverActivity.EXTRAS_BATTERY, mPercentBattery);
        startActivity(intent);
        mBluetoothLeService.disconnect();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        ButterKnife.bind(this);

        // Customize the activity menu
        setSupportActionBar(toolbar);
        title_toolbar.setText("Home");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back);
        getSupportActionBar().hide();
        state_view.setVisibility(View.GONE);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Get device data from previous activity
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        mPercentBattery = intent.getStringExtra(EXTRAS_BATTERY);

        device_name_textView.setText(mDeviceName);
        device_address_textView.setText(mDeviceAddress);
        percent_battery_textView.setText(mPercentBattery);

        name_textView.setText(mDeviceName);
        scanning = false;

        boolean isMenu = intent.getBooleanExtra("menu", false);

        if (!isMenu) { // Connecting to the selected BLE device
            // Checks if the BLE device is scanning
            parameter = "scanning";

            // Initializes the spinner to connect to BLE device
            check_avd_anim.setImageDrawable(getResources().getDrawable(R.drawable.avd_anim_spinner_48));
            final AnimatedVectorDrawable animated = (AnimatedVectorDrawable) check_avd_anim.getDrawable();
            animated.start();

            mHandlerMenu = new Handler();
            mHandler = new Handler();
            connectingToDevice();
        } else { // Only displays the main menu
            state_view.setVisibility(View.VISIBLE);
            getSupportActionBar().show();
            vhf_linearLayout.setVisibility(View.GONE);
            menu_linearLayout.setVisibility(View.VISIBLE);
            state_textView.setVisibility(View.GONE);
            check_avd_anim.setVisibility(View.GONE);
        }

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
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
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: // Go back to the previous activity
                finish();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mConnected) {
            connecting_device_linearLayout.setVisibility(View.VISIBLE);
        } else if (menu_linearLayout.getVisibility() == View.VISIBLE) {
            showDisconnectionMessage("Receiver Disconnected");
        }
        return true;
    }

    /**
     * Shows an alert dialog because the connection with the BLE device was lost or the client disconnected it.
     *
     * @param message The message that will be displayed on the screen.
     */
    private void showDisconnectionMessage(String message) {
        LayoutInflater inflater = LayoutInflater.from(this);

        View view =inflater.inflate(R.layout.disconnect_message, null);
        final AlertDialog dialog = new AlertDialog.Builder(this).create();

        TextView disconnect_message = view.findViewById(R.id.disconnect_message);
        disconnect_message.setText(message);

        dialog.setView(view);
        dialog.show();

        // The message disappears after a pre-defined period and will search for other available BLE devices again
        mHandlerMenu.postDelayed(() -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }, CONNECT_PERIOD);
    }

    /**
     * Tries to connect to the selected BLE device or shows a connection error.
     */
    private void connectingToDevice() {
        mHandler.postDelayed(() -> {
            if (mConnected){
                state_textView.setText(R.string.lb_connected);

                // Initializes the check animation when connected successfully
                check_avd_anim.setImageDrawable((AnimatedVectorDrawable) getResources().getDrawable(R.drawable.check_avd_anim));
                Drawable drawable = check_avd_anim.getDrawable();
                Animatable animatable = (Animatable) drawable;
                AnimatedVectorDrawableCompat.registerAnimationCallback(drawable, new Animatable2Compat.AnimationCallback() {
                    @Override
                    public void onAnimationEnd(Drawable drawable) {
                        new Handler().postDelayed(() -> animatable.start(), 1000);
                    }
                });
                animatable.start();

                mHandlerMenu.postDelayed(() -> {
                    if (!scanning) { // After connecting displays the main menu
                        state_view.setVisibility(View.VISIBLE);
                        getSupportActionBar().show();
                        vhf_linearLayout.setVisibility(View.GONE);
                        menu_linearLayout.setVisibility(View.VISIBLE);
                        state_textView.setVisibility(View.GONE);
                        check_avd_anim.setVisibility(View.GONE);
                    }
                }, MESSAGE_PERIOD);
            } else {
                showDisconnectionMessage("Failed to connect to receiver");
            }
        }, CONNECT_PERIOD);
    }

    /**
     * With the received packet, check if the BLE device is in scanning.
     *
     * @param data The received packet.
     */
    public void download(byte[] data) {
        switch (Converters.getHexValue(data[0])) {
            case "41": // The BLE device is not in scanning
                scanning = false;
                break;
            case "82": // The BLE device in in aerial scanning
                scanning = true;
                Intent intentA = new Intent(this, AerialScanActivity.class);
                intentA.putExtra(AerialScanActivity.EXTRAS_DEVICE_NAME, mDeviceName);
                intentA.putExtra(AerialScanActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
                intentA.putExtra(AerialScanActivity.EXTRAS_BATTERY, mPercentBattery);
                intentA.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intentA.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intentA.putExtra("scanning", true);
                intentA.putExtra("year", Converters.getDecimalValue(data[6]));
                intentA.putExtra("month", Converters.getDecimalValue(data[7]));
                startActivity(intentA);
                mBluetoothLeService.disconnect();
                break;
            case "83": // The BLE device is in stationary scanning
                scanning = true;
                Intent intentS = new Intent(this, StationaryScanActivity.class);
                intentS.putExtra(StationaryScanActivity.EXTRAS_DEVICE_NAME, mDeviceName);
                intentS.putExtra(StationaryScanActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
                intentS.putExtra(StationaryScanActivity.EXTRAS_BATTERY, mPercentBattery);
                intentS.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intentS.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intentS.putExtra("scanning", true);
                intentS.putExtra("year", Converters.getDecimalValue(data[6]));
                intentS.putExtra("month", Converters.getDecimalValue(data[7]));
                startActivity(intentS);
                mBluetoothLeService.disconnect();
                break;
        }
    }
}
