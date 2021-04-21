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
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.atstrack.ats.ats_vhf_receiver.BluetoothATS.BluetoothLeService;
import com.atstrack.ats.ats_vhf_receiver.Utils.Converters;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.UUID;

public class TableOverviewActivity extends AppCompatActivity {

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
    @BindView(R.id.table1_frequency)
    TextView table1_frequency;
    @BindView(R.id.table2_frequency)
    TextView table2_frequency;
    @BindView(R.id.table3_frequency)
    TextView table3_frequency;
    @BindView(R.id.table4_frequency)
    TextView table4_frequency;
    @BindView(R.id.table5_frequency)
    TextView table5_frequency;
    @BindView(R.id.table6_frequency)
    TextView table6_frequency;
    @BindView(R.id.table7_frequency)
    TextView table7_frequency;
    @BindView(R.id.table8_frequency)
    TextView table8_frequency;
    @BindView(R.id.table9_frequency)
    TextView table9_frequency;
    @BindView(R.id.table10_frequency)
    TextView table10_frequency;
    @BindView(R.id.table11_frequency)
    TextView table11_frequency;
    @BindView(R.id.table12_frequency)
    TextView table12_frequency;
    @BindView(R.id.google_drive_webView)
    WebView google_drive_webView;
    @BindView(R.id.google_drive_linearLayout)
    LinearLayout google_drive_linearLayout;
    @BindView(R.id.table_overview_linearLayout)
    LinearLayout table_overview_linearLayout;

    final private String TAG = TableOverviewActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String EXTRAS_BATTERY = "DEVICE_BATTERY";
    private final int MESSAGE_PERIOD = 3000;
    //private static final int REQUEST_CODE_SIGN_IN = 1;
    private static final int REQUEST_CODE_OPEN_STORAGE = 3;

    private int[] data;
    private int[][] tables;
    private int baseFrequency;
    private int range;
    private boolean isFile = false;

    /*private DriveServiceHelper driveServiceHelper;
    private String fileUrl;
    private String fileId;*/

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
                Log.e(TAG, "Unable to initialize Bluetooth");
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
                    state = false;
                    invalidateOptionsMenu();
                } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                    if (parameter.equals("frequencies")) // Gets the number of frequencies from each table
                        onClickFrequencies();
                } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                    byte[] packet = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                    if (parameter.equals("frequencies")) // Gets the number of frequencies from each table
                        downloadData(packet);
                }
            } catch (Exception e) {
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
     * Requests a read for get the number of frequencies from each table and display it.
     * Service name: StoredData.
     * Characteristic name: FreqTable.
     */
    public void onClickFrequencies() {
        UUID uservice = UUID.fromString("609d10ad-d22d-48f3-9e6e-d035398c3606");
        UUID uservicechar = UUID.fromString("ad0ea6e5-d93a-47a5-a6fc-a930552520dd");
        mBluetoothLeService.readCharacteristicDiagnostic(uservice, uservicechar);
    }

    /*@OnClick(R.id.ok_drive_button)
    public void onClickOK(View v) {
        fileId = findFileId();
        requestSignIn();
    }

    public String findFileId() {
        String[] word = fileUrl.split("/");
        return word[5];
    }*/

    @OnClick(R.id.load_from_file_overview)
    public void onClickLoadTablesFromFile(View v) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);

        intent.setDataAndType(Uri.parse(Environment.getExternalStorageDirectory().getPath()), "*/*");
        startActivityForResult(intent, REQUEST_CODE_OPEN_STORAGE);
        /*requestSignIn();

        google_drive_webView.loadUrl("https://drive.google.com/drive/my-drive");
        table_overview_linearLayout.setVisibility(View.GONE);
        google_drive_linearLayout.setVisibility(View.VISIBLE);*/
    }

    /*private class Callback extends WebViewClient {
        @Override
        public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
            return true;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.i(TAG, "LOAD: "+url);
            fileUrl = url;
            return super.shouldOverrideUrlLoading(view, url);
        }
    }

    private void requestSignIn() {
        GoogleSignInOptions signInOptions = new
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().requestScopes(new Scope(DriveScopes.DRIVE_FILE)).build();
        GoogleSignInClient client = GoogleSignIn.getClient(this, signInOptions);
        startActivityForResult(client.getSignInIntent(), REQUEST_CODE_SIGN_IN);
    }*/

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_OPEN_STORAGE:
                if (resultCode == RESULT_OK) { // Gets the Uri of the selected file
                    Uri uri = data.getData();
                    String uriString = uri.toString();
                    File myFile = new File(uriString);
                    String path = myFile.getAbsolutePath();
                    if (uriString.startsWith("content://")) {
                        Cursor cursor = null;
                        try {
                            cursor = getBaseContext().getContentResolver().query(uri, null, null, null, null);
                            if (cursor != null && cursor.moveToFirst()) {
                                String fileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                                Log.i(TAG, "NAME FILE: " + fileName);
                                Log.i(TAG, "PATH FILE: " + myFile.getAbsolutePath());
                                mBluetoothLeService.disconnect();
                                readFile(path);
                            }
                        } finally {
                            cursor.close();
                        }
                    } else if (uriString.startsWith("file://")) {
                        String fileName = myFile.getName();
                        mBluetoothLeService.disconnect();
                        readFile(path);
                    }
                }
                break;
            /*case REQUEST_CODE_SIGN_IN:
                if (resultCode == RESULT_OK)
                    handleSignInIntent(data);
                break;*/
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /*private void handleSignInIntent(Intent data) {
        GoogleSignIn.getSignedInAccountFromIntent(data).addOnSuccessListener(new OnSuccessListener<GoogleSignInAccount>() {
            @Override
            public void onSuccess(GoogleSignInAccount googleSignInAccount) {
                GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                        TableOverviewActivity.this, Collections.singleton(DriveScopes.DRIVE_FILE));

                credential.setSelectedAccount(googleSignInAccount.getAccount());

                Drive googleDriveService = new Drive.Builder(
                        AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential).setApplicationName("ATS VHF Receiver").build();

                driveServiceHelper = new DriveServiceHelper(googleDriveService);

                downloadFile();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }

    public void readFile() {
        if (driveServiceHelper != null) {
            Log.d(TAG, "Reading file " + fileId);

            driveServiceHelper.readFile(fileId)
                    .addOnSuccessListener(nameAndContent -> {
                        String name = nameAndContent.first;
                        String content = nameAndContent.second;

                        Log.i(TAG, name);
                        Log.i(TAG, content);

                        google_drive_linearLayout.setVisibility(View.GONE);
                        table_overview_linearLayout.setVisibility(View.VISIBLE);
                    })
                    .addOnFailureListener(exception ->
                            Log.e(TAG, "Couldn't read file.", exception));
        }
    }

    public void downloadFile() {
        if (driveServiceHelper != null) {
            Log.d(TAG, "Downloading file " + fileId);

            driveServiceHelper.downloadFile(fileId)
                    .addOnSuccessListener(outputStream -> {
                        Log.i(TAG, "Downloading...");
                    })
                    .addOnFailureListener(exception -> Log.e(TAG, "Couldn't download file.", exception));
        }
    }*/

    /**
     * Reads a file from the local storage and get the frequencies from each table.
     *
     * @param path The directory path where is the file.
     */
    private void readFile(String path) {
        if (isExternalStorageReadable()) {
            StringBuilder stringBuilder = new StringBuilder();
            try {
                // Gets the selected file
                File file = new File(Environment.getExternalStorageDirectory(), findPath(path));
                FileInputStream fileInputStream = new FileInputStream(file);

                if (fileInputStream != null) {
                    InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
                    BufferedReader bufferedReader =  new BufferedReader(inputStreamReader);

                    String line;
                    this.data = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                    tables = new int[12][];
                    LinkedList<String> tableList = new LinkedList<>();
                    while ((line = bufferedReader.readLine()) != null) { // Reads each line of the file and add it to the list
                        stringBuilder.append(line + "\n");
                        tableList.add(line);
                    }
                    setData(tableList);
                    fileInputStream.close();
                }
                Log.i(TAG, stringBuilder.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "Cannot read from external storage");
        }
    }

    /**
     * Finds the directory path of the selected file.
     *
     * @param path The directory path where is the file.
     *
     * @return Returns the directory path separated by /.
     */
    private String findPath(String path) {
        String[] splitPath = path.split("%");
        String newPath = "";
        for (int i = 1; i < splitPath.length; i++) {
            newPath += "/" + splitPath[i].substring(2);
        }
        return newPath;
    }

    /**
     * Checks if external storage is readable.
     *
     * @return Returns true, if external storage is readable.
     */
    private boolean isExternalStorageReadable() {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || Environment.MEDIA_MOUNTED_READ_ONLY.equals(Environment.getExternalStorageState())) {
            Log.i("State", "Yes, it is readable!");
            return true;
        }
        return false;
    }

    /**
     * Processes the frequencies from each table that are in the list.
     *
     * @param tableList This list contains the lines read from the file.
     */
    private void setData(LinkedList<String> tableList) {
        isFile = true;
        String line = tableList.removeFirst();
        int[] table = new int[100];
        if (line.equals("TABLE1") || line.equals("TABLE1 ")) {
            line = tableList.removeFirst();
            int index = 0;
            while (!line.equals("TABLE2") && !line.equals("TABLE2 ")) {
                data[1]++;
                table[index] = Integer.parseInt(line.replaceAll(" ", ""));
                line = tableList.removeFirst();
                index++;
            }
            tables[0] = table;
        }
        table = new int[100];
        if (line.equals("TABLE2") || line.equals("TABLE2 ")) {
            line = tableList.removeFirst();
            int index = 0;
            while (!line.equals("TABLE3") && !line.equals("TABLE3 ")) {
                data[2]++;
                table[index] = Integer.parseInt(line.replaceAll(" ", ""));
                line = tableList.removeFirst();
                index++;
            }
            tables[1] = table;
        }
        table = new int[100];
        if (line.equals("TABLE3") || line.equals("TABLE3 ")) {
            line = tableList.removeFirst();
            int index = 0;
            while (!line.equals("TABLE4") && !line.equals("TABLE4 ")) {
                data[3]++;
                table[index] = Integer.parseInt(line.replaceAll(" ", ""));
                line = tableList.removeFirst();
                index++;
            }
            tables[2] = table;
        }
        table = new int[100];
        if (line.equals("TABLE4") || line.equals("TABLE4 ")) {
            line = tableList.removeFirst();
            int index = 0;
            while (!line.equals("TABLE5") && !line.equals("TABLE5 ")) {
                data[4]++;
                table[index] = Integer.parseInt(line.replaceAll(" ", ""));
                line = tableList.removeFirst();
                index++;
            }
            tables[3] = table;
        }
        table = new int[100];
        if (line.equals("TABLE5") || line.equals("TABLE5 ")) {
            line = tableList.removeFirst();
            int index = 0;
            while (!line.equals("TABLE6") && !line.equals("TABLE6 ")) {
                data[5]++;
                table[index] = Integer.parseInt(line.replaceAll(" ", ""));
                line = tableList.removeFirst();
                index++;
            }
            tables[4] = table;
        }
        table = new int[100];
        if (line.equals("TABLE6") || line.equals("TABLE6 ")) {
            line = tableList.removeFirst();
            int index = 0;
            while (!line.equals("TABLE7") && !line.equals("TABLE7 ")) {
                data[6]++;
                table[index] = Integer.parseInt(line.replaceAll(" ", ""));
                line = tableList.removeFirst();
                index++;
            }
            tables[5] = table;
        }
        table = new int[100];
        if (line.equals("TABLE7") || line.equals("TABLE7 ")) {
            line = tableList.removeFirst();
            int index = 0;
            while (!line.equals("TABLE8") && !line.equals("TABLE8 ")) {
                data[7]++;
                table[index] = Integer.parseInt(line.replaceAll(" ", ""));
                line = tableList.removeFirst();
                index++;
            }
            tables[6] = table;
        }
        table = new int[100];
        if (line.equals("TABLE8") || line.equals("TABLE8 ")) {
            line = tableList.removeFirst();
            int index = 0;
            while (!line.equals("TABLE9") && !line.equals("TABLE9 ")) {
                data[8]++;
                table[index] = Integer.parseInt(line.replaceAll(" ", ""));
                line = tableList.removeFirst();
                index++;
            }
            tables[7] = table;
        }
        table = new int[100];
        if (line.equals("TABLE9") || line.equals("TABLE9 ")) {
            line = tableList.removeFirst();
            int index = 0;
            while (!line.equals("TABLE10") && !line.equals("TABLE10 ")) {
                data[9]++;
                table[index] = Integer.parseInt(line.replaceAll(" ", ""));
                line = tableList.removeFirst();
                index++;
            }
            tables[8] = table;
        }
        table = new int[100];
        if (line.equals("TABLE10") || line.equals("TABLE10 ")) {
            line = tableList.removeFirst();
            int index = 0;
            while (!line.equals("TABLE11") && !line.equals("TABLE11 ")) {
                data[10]++;
                table[index] = Integer.parseInt(line.replaceAll(" ", ""));
                line = tableList.removeFirst();
                index++;
            }
            tables[9] = table;
        }
        table = new int[100];
        if (line.equals("TABLE11") || line.equals("TABLE11 ")) {
            line = tableList.removeFirst();
            int index = 0;
            while (!line.equals("TABLE12") && !line.equals("TABLE12 ")) {
                data[11]++;
                table[index] = Integer.parseInt(line.replaceAll(" ", ""));
                line = tableList.removeFirst();
                index++;
            }
            tables[10] = table;
        }
        table = new int[100];
        int index = 0;
        while (!tableList.isEmpty()) {
            line = tableList.removeFirst();
            table[index] = Integer.parseInt(line.replaceAll(" ", ""));
            data[12]++;
            index++;
        }
        tables[11] = table;


        table1_frequency.setText(data[1] + " frequencies");
        table2_frequency.setText(data[2] + " frequencies");
        table3_frequency.setText(data[3] + " frequencies");
        table4_frequency.setText(data[4] + " frequencies");
        table5_frequency.setText(data[5] + " frequencies");
        table6_frequency.setText(data[6] + " frequencies");
        table7_frequency.setText(data[7] + " frequencies");
        table8_frequency.setText(data[8] + " frequencies");
        table9_frequency.setText(data[9] + " frequencies");
        table10_frequency.setText(data[10] + " frequencies");
        table11_frequency.setText(data[11] + " frequencies");
        table12_frequency.setText(data[12] + " frequencies");
    }

    @OnClick(R.id.table1)
    public void onClickTable1(View v) {
        Intent intent = new Intent(this, EditTablesActivity.class);
        intent.putExtra(EditTablesActivity.EXTRAS_DEVICE_NAME, mDeviceName);
        intent.putExtra(EditTablesActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
        intent.putExtra(EditTablesActivity.EXTRAS_BATTERY, mPercentBattery);
        intent.putExtra("number", 1);
        intent.putExtra("total", (int)data[1]);
        intent.putExtra("isFile", isFile);
        if (isFile) {
            intent.putExtra("frequencies", tables[0]);
        } else {
            intent.putExtra("baseFrequency", baseFrequency);
            intent.putExtra("range", range);
        }
        startActivity(intent);
        mBluetoothLeService.disconnect();
    }

    @OnClick(R.id.table2)
    public void onClickTable2(View v) {
        Intent intent = new Intent(this, EditTablesActivity.class);
        intent.putExtra(EditTablesActivity.EXTRAS_DEVICE_NAME, mDeviceName);
        intent.putExtra(EditTablesActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
        intent.putExtra(EditTablesActivity.EXTRAS_BATTERY, mPercentBattery);
        intent.putExtra("number", 2);
        intent.putExtra("total", (int)data[2]);
        intent.putExtra("isFile", isFile);
        if (isFile) {
            intent.putExtra("frequencies", tables[1]);
        } else {
            intent.putExtra("baseFrequency", baseFrequency);
            intent.putExtra("range", range);
        }
        startActivity(intent);
        mBluetoothLeService.disconnect();
    }

    @OnClick(R.id.table3)
    public void onClickTable3(View v) {
        Intent intent = new Intent(this, EditTablesActivity.class);
        intent.putExtra(EditTablesActivity.EXTRAS_DEVICE_NAME, mDeviceName);
        intent.putExtra(EditTablesActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
        intent.putExtra(EditTablesActivity.EXTRAS_BATTERY, mPercentBattery);
        intent.putExtra("number", 3);
        intent.putExtra("total", (int)data[3]);
        if (isFile) {
            intent.putExtra("frequencies", tables[2]);
        } else {
            intent.putExtra("baseFrequency", baseFrequency);
            intent.putExtra("range", range);
        }
        startActivity(intent);
        mBluetoothLeService.disconnect();
    }

    @OnClick(R.id.table4)
    public void onClickTable4(View v) {
        Intent intent = new Intent(this, EditTablesActivity.class);
        intent.putExtra(EditTablesActivity.EXTRAS_DEVICE_NAME, mDeviceName);
        intent.putExtra(EditTablesActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
        intent.putExtra(EditTablesActivity.EXTRAS_BATTERY, mPercentBattery);
        intent.putExtra("number", 4);
        intent.putExtra("total", (int)data[4]);
        if (isFile) {
            intent.putExtra("frequencies", tables[3]);
        } else {
            intent.putExtra("baseFrequency", baseFrequency);
            intent.putExtra("range", range);
        }
        startActivity(intent);
        mBluetoothLeService.disconnect();
    }

    @OnClick(R.id.table5)
    public void onClickTable5(View v) {
        Intent intent = new Intent(this, EditTablesActivity.class);
        intent.putExtra(EditTablesActivity.EXTRAS_DEVICE_NAME, mDeviceName);
        intent.putExtra(EditTablesActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
        intent.putExtra(EditTablesActivity.EXTRAS_BATTERY, mPercentBattery);
        intent.putExtra("number", 5);
        intent.putExtra("total", (int)data[5]);
        if (isFile) {
            intent.putExtra("frequencies", tables[4]);
        } else {
            intent.putExtra("baseFrequency", baseFrequency);
            intent.putExtra("range", range);
        }
        startActivity(intent);
        mBluetoothLeService.disconnect();
    }

    @OnClick(R.id.table6)
    public void onClickTable6(View v) {
        Intent intent = new Intent(this, EditTablesActivity.class);
        intent.putExtra(EditTablesActivity.EXTRAS_DEVICE_NAME, mDeviceName);
        intent.putExtra(EditTablesActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
        intent.putExtra(EditTablesActivity.EXTRAS_BATTERY, mPercentBattery);
        intent.putExtra("number", 6);
        intent.putExtra("total", (int)data[6]);
        if (isFile) {
            intent.putExtra("frequencies", tables[5]);
        } else {
            intent.putExtra("baseFrequency", baseFrequency);
            intent.putExtra("range", range);
        }
        startActivity(intent);
        mBluetoothLeService.disconnect();
    }

    @OnClick(R.id.table7)
    public void onClickTable7(View v) {
        Intent intent = new Intent(this, EditTablesActivity.class);
        intent.putExtra(EditTablesActivity.EXTRAS_DEVICE_NAME, mDeviceName);
        intent.putExtra(EditTablesActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
        intent.putExtra(EditTablesActivity.EXTRAS_BATTERY, mPercentBattery);
        intent.putExtra("number", 7);
        intent.putExtra("total", (int)data[7]);
        if (isFile) {
            intent.putExtra("frequencies", tables[6]);
        } else {
            intent.putExtra("baseFrequency", baseFrequency);
            intent.putExtra("range", range);
        }
        startActivity(intent);
        mBluetoothLeService.disconnect();
    }

    @OnClick(R.id.table8)
    public void onClickTable8(View v) {
        Intent intent = new Intent(this, EditTablesActivity.class);
        intent.putExtra(EditTablesActivity.EXTRAS_DEVICE_NAME, mDeviceName);
        intent.putExtra(EditTablesActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
        intent.putExtra(EditTablesActivity.EXTRAS_BATTERY, mPercentBattery);
        intent.putExtra("number", 8);
        intent.putExtra("total", (int)data[8]);
        if (isFile) {
            intent.putExtra("frequencies", tables[7]);
        } else {
            intent.putExtra("baseFrequency", baseFrequency);
            intent.putExtra("range", range);
        }
        startActivity(intent);
        mBluetoothLeService.disconnect();
    }

    @OnClick(R.id.table9)
    public void onClickTable9(View v) {
        Intent intent = new Intent(this, EditTablesActivity.class);
        intent.putExtra(EditTablesActivity.EXTRAS_DEVICE_NAME, mDeviceName);
        intent.putExtra(EditTablesActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
        intent.putExtra(EditTablesActivity.EXTRAS_BATTERY, mPercentBattery);
        intent.putExtra("number", 9);
        intent.putExtra("total", (int)data[9]);
        if (isFile) {
            intent.putExtra("frequencies", tables[8]);
        } else {
            intent.putExtra("baseFrequency", baseFrequency);
            intent.putExtra("range", range);
        }
        startActivity(intent);
        mBluetoothLeService.disconnect();
    }

    @OnClick(R.id.table10)
    public void onClickTable10(View v) {
        Intent intent = new Intent(this, EditTablesActivity.class);
        intent.putExtra(EditTablesActivity.EXTRAS_DEVICE_NAME, mDeviceName);
        intent.putExtra(EditTablesActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
        intent.putExtra(EditTablesActivity.EXTRAS_BATTERY, mPercentBattery);
        intent.putExtra("number", 10);
        intent.putExtra("total", (int)data[10]);
        if (isFile) {
            intent.putExtra("frequencies", tables[9]);
        } else {
            intent.putExtra("baseFrequency", baseFrequency);
            intent.putExtra("range", range);
        }
        startActivity(intent);
        mBluetoothLeService.disconnect();
    }

    @OnClick(R.id.table11)
    public void onClickTable11(View v) {
        Intent intent = new Intent(this, EditTablesActivity.class);
        intent.putExtra(EditTablesActivity.EXTRAS_DEVICE_NAME, mDeviceName);
        intent.putExtra(EditTablesActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
        intent.putExtra(EditTablesActivity.EXTRAS_BATTERY, mPercentBattery);
        intent.putExtra("number", 11);
        intent.putExtra("total", (int)data[11]);
        if (isFile) {
            intent.putExtra("frequencies", tables[10]);
        } else {
            intent.putExtra("baseFrequency", baseFrequency);
            intent.putExtra("range", range);
        }
        startActivity(intent);
        mBluetoothLeService.disconnect();
    }

    @OnClick(R.id.table12)
    public void onClickTable12(View v) {
        Intent intent = new Intent(this, EditTablesActivity.class);
        intent.putExtra(EditTablesActivity.EXTRAS_DEVICE_NAME, mDeviceName);
        intent.putExtra(EditTablesActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
        intent.putExtra(EditTablesActivity.EXTRAS_BATTERY, mPercentBattery);
        intent.putExtra("number", 12);
        intent.putExtra("total", (int)data[12]);
        if (isFile) {
            intent.putExtra("frequencies", tables[11]);
        } else {
            intent.putExtra("baseFrequency", baseFrequency);
            intent.putExtra("range", range);
        }
        startActivity(intent);
        mBluetoothLeService.disconnect();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table_overview);
        ButterKnife.bind(this);

        // Customize the activity menu
        setSupportActionBar(toolbar);
        title_toolbar.setText("Edit Frequency Tables");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back);

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

        // Gets the number of frequencies from each table
        parameter = "frequencies";

        //google_drive_webView.getSettings().setJavaScriptEnabled(true);
        //google_drive_webView.setWebViewClient(new Callback());

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
            Log.d(TAG, "Connect request result= " + result);
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
     * With the received packet, gets the number of frequencies from each table and display on the screen.
     *
     * @param data The received packet.
     */
    private void downloadData(byte[] data) {
        table1_frequency.setText(Converters.getDecimalValue(data[1]) + " frequencies");
        table2_frequency.setText(Converters.getDecimalValue(data[2]) + " frequencies");
        table3_frequency.setText(Converters.getDecimalValue(data[3]) + " frequencies");
        table4_frequency.setText(Converters.getDecimalValue(data[4]) + " frequencies");
        table5_frequency.setText(Converters.getDecimalValue(data[5]) + " frequencies");
        table6_frequency.setText(Converters.getDecimalValue(data[6]) + " frequencies");
        table7_frequency.setText(Converters.getDecimalValue(data[7]) + " frequencies");
        table8_frequency.setText(Converters.getDecimalValue(data[8]) + " frequencies");
        table9_frequency.setText(Converters.getDecimalValue(data[9]) + " frequencies");
        table10_frequency.setText(Converters.getDecimalValue(data[10]) + " frequencies");
        table11_frequency.setText(Converters.getDecimalValue(data[11]) + " frequencies");
        table12_frequency.setText(Converters.getDecimalValue(data[12]) + " frequencies");

        baseFrequency = Integer.parseInt(Converters.getDecimalValue(data[13]));
        range = Integer.parseInt(Converters.getDecimalValue(data[14]));

        this.data = new int[data.length];
        for (int i = 0; i < data.length; i++) {
            this.data[i] = data[i];
        }
    }
}

