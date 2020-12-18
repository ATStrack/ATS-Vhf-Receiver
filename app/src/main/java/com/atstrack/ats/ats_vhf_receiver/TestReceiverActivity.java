package com.atstrack.ats.ats_vhf_receiver;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.atstrack.ats.ats_vhf_receiver.BluetoothATS.BluetoothLeService;
import com.atstrack.ats.ats_vhf_receiver.Utils.Converters;
import com.bumptech.glide.Glide;

import java.util.UUID;

public class TestReceiverActivity extends AppCompatActivity {

    @BindView(R.id.device_name_testReceiver)
    TextView device_name_textView;
    @BindView(R.id.device_address_testReceiver)
    TextView device_address_textView;
    @BindView(R.id.running_test_constraintLayout)
    ConstraintLayout running_test_constraintLayout;
    @BindView(R.id.test_exit_image)
    ImageView exit_image;
    @BindView(R.id.testing_linearLayout)
    LinearLayout testing_linearLayout;
    @BindView(R.id.iv_ProgressGIF_testing)
    ImageView iv_ProgressGIF_testing;
    @BindView(R.id.test_complete_linearLayout)
    LinearLayout test_complete_linearLayout;
    @BindView(R.id.battery_textView)
    TextView battery_textView;
    @BindView(R.id.bytes_stored_textView)
    TextView bytes_stored_textView;
    @BindView(R.id.memory_used_textView)
    TextView memory_used_textView;
    @BindView(R.id.frequency_tables_textView)
    TextView frequency_tables_textView;
    @BindView(R.id.first_table_textView)
    TextView first_table_textView;
    @BindView(R.id.second_table_textView)
    TextView second_table_textView;
    @BindView(R.id.third_table_textView)
    TextView third_table_textView;
    @BindView(R.id.fourth_table_textView)
    TextView fourth_table_textView;
    @BindView(R.id.fifth_table_textView)
    TextView fifth_table_textView;
    @BindView(R.id.sixth_table_textView)
    TextView sixth_table_textView;
    @BindView(R.id.seventh_table_textView)
    TextView seventh_table_textView;
    @BindView(R.id.eighth_table_textView)
    TextView eighth_table_textView;
    @BindView(R.id.ninth_table_textView)
    TextView ninth_table_textView;
    @BindView(R.id.tenth_table_textView)
    TextView tenth_table_textView;
    @BindView(R.id.eleventh_table_textView)
    TextView eleventh_table_textView;
    @BindView(R.id.twelfth_table_textView)
    TextView twelfth_table_textView;

    private final static String TAG = TestReceiverActivity.class.getSimpleName();
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private final int MESSAGE_PERIOD = 3000;

    private final int TEST_PERIOD = 5000;
    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private boolean state = true;

    private Handler mHandler;
    private int heightPixels;
    private int widthPixels;

    private Handler mHandlerTest;

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
                    state = false;
                    invalidateOptionsMenu();
                } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                    if (parameter.equals("test"))
                        onClickTest();
                } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                    byte[] packet = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                    if (parameter.equals("test"))
                        downloadData(packet);
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

    private void onClickTest(){
        UUID uservice=UUID.fromString("fab2d796-3364-4b54-b9a1-7735545814ad");
        UUID uservicechar=UUID.fromString("42d03a17-ebe1-4072-97a5-393f4a0515d7");
        mBluetoothLeService.readCharacteristicDiagnostic(uservice,uservicechar);
    }

    @OnClick(R.id.test_exit_image)
    public void onClickExit(View v){
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_receiver);
        ButterKnife.bind(this);

        Glide.with(this).asGif().load(R.raw.searching).into(iv_ProgressGIF_testing);

        heightPixels = getResources().getDisplayMetrics().heightPixels;
        widthPixels = getResources().getDisplayMetrics().widthPixels;

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        device_name_textView.setText(mDeviceName);
        device_address_textView.setText(mDeviceAddress);

        mHandler = new Handler();

        mHandlerTest = new Handler();
        parameter = "test";

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        runningTest();
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
        dialog.getWindow().setLayout(widthPixels * 29 / 30, heightPixels * 1 / 2);

        mHandler.postDelayed(() -> {
            dialog.dismiss();
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }, MESSAGE_PERIOD);
    }

    private int findPageNumber(byte[] packet) {
        int pageNumber = Integer.valueOf(Converters.getDecimalValue(packet[0]));
        pageNumber = (Integer.valueOf(Converters.getDecimalValue(packet[1])) << 8) | pageNumber;
        pageNumber = (Integer.valueOf(Converters.getDecimalValue(packet[2])) << 16) | pageNumber;
        pageNumber = (Integer.valueOf(Converters.getDecimalValue(packet[3])) << 24) | pageNumber;
        return pageNumber;
    }

    private void downloadData(byte[] data) {
        battery_textView.setText(Converters.getDecimalValue(data[1]));
        int numberPage = findPageNumber(new byte[]{data[18], data[17], data[16], data[15]});
        int lastPage = findPageNumber(new byte[]{data[22], data[21], data[20], data[19]});
        bytes_stored_textView.setText(String.valueOf(numberPage * 2048));
        memory_used_textView.setText(String.valueOf(numberPage * 100 / lastPage));
        frequency_tables_textView.setText(Converters.getDecimalValue(data[2]));
        first_table_textView.setText(Converters.getDecimalValue(data[3]));
        second_table_textView.setText(Converters.getDecimalValue(data[4]));
        third_table_textView.setText(Converters.getDecimalValue(data[5]));
        fourth_table_textView.setText(Converters.getDecimalValue(data[6]));
        fifth_table_textView.setText(Converters.getDecimalValue(data[7]));
        sixth_table_textView.setText(Converters.getDecimalValue(data[8]));
        seventh_table_textView.setText(Converters.getDecimalValue(data[9]));
        eighth_table_textView.setText(Converters.getDecimalValue(data[10]));
        ninth_table_textView.setText(Converters.getDecimalValue(data[11]));
        tenth_table_textView.setText(Converters.getDecimalValue(data[12]));
        eleventh_table_textView.setText(Converters.getDecimalValue(data[13]));
        twelfth_table_textView.setText(Converters.getDecimalValue(data[14]));
    }

    private void runningTest(){
        mHandlerTest.postDelayed(() -> {
            running_test_constraintLayout.setVisibility(View.GONE);
            test_complete_linearLayout.setVisibility(View.VISIBLE);

            getSupportActionBar().setTitle("Screen Title");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_ab_back);
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorPrimary)));
        }, TEST_PERIOD);
    }
}
