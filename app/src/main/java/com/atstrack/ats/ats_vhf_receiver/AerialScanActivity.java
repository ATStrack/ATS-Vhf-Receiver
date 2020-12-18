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
import android.os.SystemClock;
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

import java.util.Calendar;
import java.util.TimeZone;
import java.util.UUID;

public class AerialScanActivity extends AppCompatActivity {

    @BindView(R.id.device_name_aerialScan)
    TextView device_name_textView;
    @BindView(R.id.device_address_aerialScan)
    TextView device_address_textView;
    @BindView(R.id.ready_aerial_scan_LinearLayout)
    LinearLayout ready_aerial_scan_LinearLayout;
    @BindView(R.id.scan_rate_aerial_textView)
    TextView scan_rate_aerial_textView;
    @BindView(R.id.selected_frequency_aerial_textView)
    TextView selected_frequency_aerial_textView;
    @BindView(R.id.number_antennas_aerial_textView)
    TextView number_antennas_aerial_textView;
    @BindView(R.id.timeout_aerial_textView)
    TextView timeout_aerial_textView;
    @BindView(R.id.aerial_result_constraintLayout)
    ConstraintLayout aerial_result_constraintLayout;

    private final static String TAG = MainMenuActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private final int MESSAGE_PERIOD = 3000;

    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private boolean state = true;
    private boolean response = true;

    private Handler mHandler;
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
                    if (parameter.equals("aerial"))
                        onClickAerial();
                    else if (parameter.equals("startAerial"))
                        onClickStart();
                    else if (parameter.equals("wait"))
                        onClickWait();
                    else if (parameter.equals("stopAerial"))
                        onClickStop();
                } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                    byte[] packet = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                    if (parameter.equals("aerial"))
                        downloadData(packet);
                    else if (parameter.equals("stopAerial"))
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

    private void onClickAerial(){
        UUID uservice = UUID.fromString("8d60a8bb-1f60-4703-92ff-411103c493e6");
        UUID uservicechar = UUID.fromString("111584dd-b374-417c-a51d-9314eba66d6c");
        mBluetoothLeService.readCharacteristicDiagnostic(uservice, uservicechar); //aerial
    }

    private void onClickStart(){
        parameter = "wait";

        Calendar currentDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        int YY = currentDate.get(Calendar.YEAR);
        int MM = currentDate.get(Calendar.MONTH);
        int DD = currentDate.get(Calendar.DAY_OF_MONTH);
        int hh = currentDate.get(Calendar.HOUR_OF_DAY);
        int mm =  currentDate.get(Calendar.MINUTE);
        int ss = currentDate.get(Calendar.SECOND);
        int ms = currentDate.get(Calendar.MILLISECOND);

        byte[] b = new byte[]{
                (byte) 0x82, (byte) 1, (byte) 0, (byte) 10, (byte) 2, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x7f,
                (byte) (YY % 100), (byte) MM, (byte) DD, (byte) hh, (byte) mm, (byte) ss, (byte) (ms / 100), (byte) (ms % 100)};

        UUID uservice = UUID.fromString("8d60a8bb-1f60-4703-92ff-411103c493e6");
        UUID uservicechar = UUID.fromString("111584dd-b374-417c-a51d-9314eba66d6c");
        mBluetoothLeService.writeCharacteristic( uservice,uservicechar,b, true);
    }

    private void onClickStop(){
        parameter = "aerial";
        byte[] b = new byte[]{(byte)0x87};

        UUID uservice = UUID.fromString("8d60a8bb-1f60-4703-92ff-411103c493e6");
        UUID uservicechar = UUID.fromString("111584dd-b374-417c-a51d-9314eba66d6c");
        mBluetoothLeService.writeCharacteristic( uservice,uservicechar,b, true);

        getSupportActionBar().show();
        aerial_result_constraintLayout.setVisibility(View.GONE);
        ready_aerial_scan_LinearLayout.setVisibility(View.VISIBLE);
    }

    private void onClickWait(){
        if (response) {
            showMessage(new byte[]{0});
            response = false;
        }
        mBluetoothLeService.waiting();
    }

    @OnClick(R.id.edit_aerial_defaults_textView)
    public void onClickEditDefaults(View v){
        Intent intent = new Intent(this, AerialDefaultsActivity.class);
        intent.putExtra(AerialDefaultsActivity.EXTRAS_DEVICE_NAME, mDeviceName);
        intent.putExtra(AerialDefaultsActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
        startActivity(intent);
        mBluetoothLeService.disconnect();
    }

    @OnClick(R.id.start_aerial_button)
    public void onClickStartAerial(View v){
        parameter = "startAerial";
        mBluetoothLeService.connect(mDeviceAddress);
        //onRestartConnection();
        getSupportActionBar().hide();
        ready_aerial_scan_LinearLayout.setVisibility(View.GONE);
        aerial_result_constraintLayout.setVisibility(View.VISIBLE);
    }

    @OnClick(R.id.aerial_exit_image)
    public void onClickExit(View  v){
        parameter = "stopAerial";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aerial_scan);
        ButterKnife.bind(this);

        getSupportActionBar().setElevation(0);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_chevron_back_icon_opt);
        getSupportActionBar().setTitle("AERIAL SCANNING");

        heightPixels = getResources().getDisplayMetrics().heightPixels;
        widthPixels = getResources().getDisplayMetrics().widthPixels;

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        parameter = "aerial";

        device_name_textView.setText(mDeviceName);
        device_address_textView.setText(mDeviceAddress);

        mHandler = new Handler();

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

    private void downloadData(byte[] data) {
        mBluetoothLeService.disconnect();
        if (!response) {
            showMessage(new byte[]{0});
            response = true;
        }
        selected_frequency_aerial_textView.setText(Converters.getDecimalValue(data[1]));
        byte numberAntennas = (data[2] >= (byte) 0x80) ? (byte) (data[2] - (byte) 0x80) : data[2];
        numberAntennas = (numberAntennas >= (byte) 0x40) ? (byte) (numberAntennas - (byte) 0x40) : numberAntennas;
        number_antennas_aerial_textView.setText(Converters.getDecimalValue(numberAntennas));
        scan_rate_aerial_textView.setText(Converters.getDecimalValue(data[3]));
        timeout_aerial_textView.setText(Converters.getDecimalValue(data[4]));
    }

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
