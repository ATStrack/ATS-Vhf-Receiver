package com.atstrack.ats.ats_vhf_receiver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.atstrack.ats.ats_vhf_receiver.BluetoothATS.BluetoothLeService;
import com.bumptech.glide.Glide;

public class MainMenuActivity extends AppCompatActivity {

    @BindView(R.id.device_name_mainMenu)
    TextView device_name_mainMenu;
    @BindView(R.id.device_address_mainMenu)
    TextView device_address_mainMenu;
    @BindView(R.id.menu_linearLayout)
    LinearLayout menu_linearLayout;
    @BindView(R.id.vhf_linearLayout)
    LinearLayout vhf_linearLayout;
    @BindView(R.id.state_view)
    View state_view;
    @BindView(R.id.state_textView)
    TextView state_textView;
    @BindView(R.id.name_textView)
    TextView name_textView;
    @BindView(R.id.address_textView)
    TextView address_textView;
    @BindView(R.id.connecting_device_mainMenu)
    LinearLayout connecting_device_linearLayout;
    @BindView(R.id.disconnect_constraintLayout)
    ConstraintLayout disconnect_constraintLayout;
    @BindView(R.id.check_avd_anim)
    ImageView check_avd_anim;

    private final static String TAG = MainMenuActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private static final long MESSAGE_PERIOD = 3000;
    private static final long CONNECT_PERIOD = 5000;
    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private Handler mHandler;
    private Handler mHandlerMenu;
    private int heightPixels;
    private int widthPixels;

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

    @OnClick(R.id.connecting_device_mainMenu)
    public void onClickConnectingDevice(View v){
        menu_linearLayout.setVisibility(View.GONE);
        connecting_device_linearLayout.setVisibility(View.GONE);
        vhf_linearLayout.setVisibility(View.GONE);
        getSupportActionBar().hide();

        name_textView.setText(mDeviceName);
        address_textView.setText(mDeviceAddress);
        disconnect_constraintLayout.setVisibility(View.VISIBLE);
    }

    @OnClick(R.id.disconnect_exit_image)
    public void onClickConnectExit(View v){
        disconnect_constraintLayout.setVisibility(View.GONE);

        getSupportActionBar().show();
        menu_linearLayout.setVisibility(View.VISIBLE);
        connecting_device_linearLayout.setVisibility(View.VISIBLE);
    }

    @OnClick(R.id.disconnect_button)
    public void onClickDisconnect(View v){
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        mBluetoothLeService.disconnect();
    }

    @OnClick(R.id.start_scanning_button)
    public void onClickStartScanning(View v){
        Intent intent = new Intent(this, StartScanningActivity.class);
        intent.putExtra(StartScanningActivity.EXTRAS_DEVICE_NAME, mDeviceName);
        intent.putExtra(StartScanningActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
        startActivity(intent);
        mBluetoothLeService.disconnect();
    }

    @OnClick(R.id.receiver_configuration_button)
    public void onClickReceiverConfiguration(View v){
        Intent intent = new Intent(this, ReceiverConfigurationActivity.class);
        intent.putExtra(ReceiverConfigurationActivity.EXTRAS_DEVICE_NAME, mDeviceName);
        intent.putExtra(ReceiverConfigurationActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
        startActivity(intent);
        mBluetoothLeService.disconnect();
    }

    @OnClick(R.id.manage_receiver_data_button)
    public void onClickManageReceiverData(View v){
        Intent intent = new Intent(this, GetDataActivity.class);
        intent.putExtra(GetDataActivity.EXTRAS_DEVICE_NAME, mDeviceName);
        intent.putExtra(GetDataActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
        startActivity(intent);
        mBluetoothLeService.disconnect();
    }

    @OnClick(R.id.test_receiver_button)
    public void onClickTestReceiver(View v){
        Intent intent = new Intent(this, TestReceiverActivity.class);
        intent.putExtra(TestReceiverActivity.EXTRAS_DEVICE_NAME, mDeviceName);
        intent.putExtra(TestReceiverActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
        startActivity(intent);
        mBluetoothLeService.disconnect();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        ButterKnife.bind(this);

        getSupportActionBar().setElevation(0);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_chevron_back_icon_opt);
        getSupportActionBar().setTitle("HOME");
        getSupportActionBar().hide();

        heightPixels = getResources().getDisplayMetrics().heightPixels;
        widthPixels = getResources().getDisplayMetrics().widthPixels;

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        device_name_mainMenu.setText(mDeviceName);
        device_address_mainMenu.setText(mDeviceAddress);

        mHandlerMenu = new Handler();
        mHandler = new Handler();
        connectingToReceiver();

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
            case android.R.id.home:
                unbindService(mServiceConnection);
                mBluetoothLeService = null;
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mConnected) {
            connecting_device_linearLayout.setVisibility(View.VISIBLE);
        } else if (menu_linearLayout.getVisibility() == View.VISIBLE) {
            showMessageDisconnect();
        }
        return true;
    }

    private void showMessageDisconnect() {
        LayoutInflater inflater = LayoutInflater.from(this);

        View view =inflater.inflate(R.layout.disconnect_message, null);
        final androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this).create();

        dialog.setView(view);
        dialog.show();
        dialog.getWindow().setLayout(widthPixels * 29 / 30, heightPixels * 1 / 2);

        mHandler.postDelayed(() -> {
            dialog.dismiss();
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }, MESSAGE_PERIOD);
    }

    private void connectingToReceiver() {
        mHandler.postDelayed(() -> {
            if (mConnected){
                state_textView.setText(R.string.lb_connected);
                check_avd_anim.setImageDrawable(getResources().getDrawable(R.drawable.check_avd_anim));
                state_textView.setTextColor(ContextCompat.getColor(this, R.color.colorbutton));
                state_view.setBackgroundResource(R.color.colorbutton);
                mHandlerMenu.postDelayed(() -> {
                    getSupportActionBar().show();
                    vhf_linearLayout.setVisibility(View.GONE);
                    menu_linearLayout.setVisibility(View.VISIBLE);
                    state_textView.setVisibility(View.GONE);
                    check_avd_anim.setVisibility(View.GONE);
                }, MESSAGE_PERIOD);
            } else {
                SharedPreferences preferences = getSharedPreferences("Connection", 0);
                SharedPreferences.Editor connectState = preferences.edit();
                connectState.putBoolean("retry", true);
                connectState.apply();
                unbindService(mServiceConnection);
                mBluetoothLeService = null;
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
            }
        }, CONNECT_PERIOD);
    }
}
