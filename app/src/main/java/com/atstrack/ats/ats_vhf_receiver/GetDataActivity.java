package com.atstrack.ats.ats_vhf_receiver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.atstrack.ats.ats_vhf_receiver.BluetoothATS.BluetoothLeService;
import com.atstrack.ats.ats_vhf_receiver.BluetoothATS.Snapshots;
import com.atstrack.ats.ats_vhf_receiver.Utils.Converters;
import com.atstrack.ats.ats_vhf_receiver.Utils.DriveServiceHelper;
import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

public class GetDataActivity extends AppCompatActivity {

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
    @BindView(R.id.memory_used_percent_textView)
    TextView memory_used;
    @BindView(R.id.memory_used_progressBar)
    ProgressBar memory_used_progressBar;
    @BindView(R.id.bytes_stored)
    TextView bytes_stored;
    @BindView(R.id.iv_ProgressGIF)
    ImageView progressGIF;
    @BindView(R.id.percentage)
    TextView percentage;
    @BindView(R.id.menu_manage_receiver_linearLayout)
    LinearLayout subMenu;

    private final static String TAG = GetDataActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String EXTRAS_BATTERY = "DEVICE_BATTERY";

    public final static char CR  = (char) 0x0D;
    public final static char LF  = (char) 0x0A;
    private static final int REQUEST_CODE_SIGN_IN = 1;
    private final int MESSAGE_PERIOD = 3000;

    FileOutputStream stream;
    File newFile;
    File root;
    String fName = "";

    private ArrayList<Snapshots> snapshotArray;
    private Snapshots rawDataCollector;
    private Snapshots processDataCollector;
    private int finalPageNumber;
    private int pageNumber;
    private int percent;
    private boolean begin;
    private boolean error;

    private DriveServiceHelper driveServiceHelper;

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
    private String parameter1;

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
                    switch (parameter1) {
                        case "test": // Gets memory used and byte stored
                            onClickTest();
                            break;
                        case "downloadData": //
                            onClickDownloadData();
                            break;
                        case "eraseData": // Delete data
                            onClickEraseData();
                            break;
                    }
                } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                    byte[] packet = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                    if (parameter1.equals("downloadData")) {
                        // Gets raw data in pages, each page contains 2048 bytes.
                        // 8 packets of 244 bytes and one of 96 bytes
                        if (begin) { // Start download with an 8-byte packet
                            // Only the first packet received contains 8 bytes
                            // The first package indicates the total number of pages and the current page
                            finalPageNumber = findPageNumber(new byte[]{packet[0], packet[1], packet[2], packet[3]});
                            pageNumber = findPageNumber(new byte[]{packet[4], packet[5], packet[6], packet[7]});
                            error = false;
                            percent = 0;
                            begin = false;
                            percentage.setText(percent + "%");
                            // The list that stores the raw and processed data
                            snapshotArray = new ArrayList<>();
                            // size is defined
                            rawDataCollector = new Snapshots(finalPageNumber * Snapshots.BYTES_PER_PAGE);
                        } else {
                            downloadData(packet);
                        }
                    }
                    if (parameter1.equals("eraseData")) // Delete data
                        showMessage(packet);
                    if (parameter1.equals("test")) // Gets memory used and byte stored
                        downloadTest(packet);
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
     * Requests a read for get BLE device data.
     * Service name: Diagnostic.
     * Characteristic name: DiagInfo.
     */
    private void onClickTest() {
        UUID uservice=UUID.fromString("fab2d796-3364-4b54-b9a1-7735545814ad");
        UUID uservicechar=UUID.fromString("42d03a17-ebe1-4072-97a5-393f4a0515d7");
        mBluetoothLeService.readCharacteristicDiagnostic(uservice,uservicechar);
    }

    public void onClickDownloadData() {
        UUID uservice=UUID.fromString("609d10ad-d22d-48f3-9e6e-d035398c3606");
        UUID uservicechar=UUID.fromString("91dced42-a8ee-4d9d-aecf-dbd22d390568");
        mBluetoothLeService.setCharacteristicNotificationRead(uservice, uservicechar, true);
        subMenu.setVisibility(View.GONE);
        progressGIF.setVisibility(View.VISIBLE);
        percentage.setVisibility(View.VISIBLE);
        Glide.with(this).asGif().load(R.raw.barra_puntos).into(progressGIF);
    }

    /**
     * Writes delete data.
     * Service name: StoredData.
     * Characteristic name: StudyData.
     */
    public void onClickEraseData() {
        byte[] b = new byte[]{(byte) 0x93};

        UUID uservice=UUID.fromString("609d10ad-d22d-48f3-9e6e-d035398c3606");
        UUID uservicechar=UUID.fromString("91dced42-a8ee-4d9d-aecf-dbd22d390568");
        mBluetoothLeService.writeCharacteristic(uservice, uservicechar, b, false);
    }

    @OnClick(R.id.download_data_button)
    public void onClickDownloadData(View v){
        parameter1 = "downloadData";
        begin = true;

        onRestartConnection();
    }

    @OnClick(R.id.erase_data_button)
    public void onClickEraseData(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Warning");
        builder.setMessage("Are you sure you want to delete data?");
        builder.setNegativeButton("Cancel", null);
        builder.setPositiveButton("Delete", (dialog, which) -> {
            parameter1 = "eraseData";
            onRestartConnection();
        });
        builder.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_data);
        ButterKnife.bind(this);

        // Customize the activity menu
        setSupportActionBar(toolbar);
        title_toolbar.setText("Manage Receiver Data");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Get device data from previous activity
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        mPercentBattery = intent.getStringExtra(EXTRAS_BATTERY);
        parameter1 = "test";

        device_name_textView.setText(mDeviceName);
        device_address_textView.setText(mDeviceAddress);
        percent_battery_textView.setText(mPercentBattery);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                if (resultCode == RESULT_OK)
                    handleSignInIntent(data);
                break;
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
        final androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this).create();

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home: //Go back to the previous activity
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * With the received packet, Gets memory used and byte stored and display on screen.
     *
     * @param data The received packet.
     */
    public void downloadTest(byte[] data) {
        int numberPage = findPageNumber(new byte[]{data[18], data[17], data[16], data[15]});
        int lastPage = findPageNumber(new byte[]{data[22], data[21], data[20], data[19]});
        memory_used.setText((numberPage * 100 / lastPage) + "%");
        memory_used_progressBar.setProgress(numberPage * 100 / lastPage);
        bytes_stored.setText("Memory Used " + "(" + (numberPage * 2048) + " bytes stored)");
    }

    /**
     * Finds the page number of a 4-byte packet.
     *
     * @param packet The received packet.
     *
     * @return Returns the page number.
     */
    private int findPageNumber(byte[] packet) {
        int pageNumber = Integer.parseInt(Converters.getDecimalValue(packet[0]));
        pageNumber = (Integer.parseInt(Converters.getDecimalValue(packet[1])) << 8) | pageNumber;
        pageNumber = (Integer.parseInt(Converters.getDecimalValue(packet[2])) << 16) | pageNumber;
        pageNumber = (Integer.parseInt(Converters.getDecimalValue(packet[3])) << 24) | pageNumber;
        return pageNumber;
    }

    /**
     * Processes the data when the download is complete.
     *
     * @param packet The raw data.
     *
     * @return Returns the processed data.
     */
    private String readPacket(byte[] packet) {
        String data = "";
        int index = 0;
        Log.i(TAG, "PACKET[0]: "+Converters.getHexValue(packet[0]));
        if (Converters.getHexValue(packet[0]).equals("83")){//130
            data += "Year, Month, Day, Hour, Min, Sec, Ant, Freq, SS, Code, Det, Mort, Lat, Long, GpsAge" + CR + LF;
            data += Converters.getDecimalValue(packet[6]) + ", " + Converters.getDecimalValue(packet[7]) + ", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0" + CR + LF;
            index += 8;
            while (index < packet.length) {
                if (Converters.getHexValue(packet[index]).equals("F0")) {//52
                    Log.i(TAG, Converters.getHexValue(packet[index]));
                    data += "0, " + Converters.getDecimalValue(packet[index + 3]) + ", " + Converters.getDecimalValue(packet[index + 4]) + ", " +
                            Converters.getDecimalValue(packet[index + 5]) + ", " + Converters.getDecimalValue(packet[index + 6]) + ", " +
                            Converters.getDecimalValue(packet[index + 7]) + ", 0, " +
                            ((Integer.parseInt(Converters.getDecimalValue(packet[index + 1])) * 256) + Integer.parseInt(Converters.getDecimalValue(packet[index + 2]))) +
                            ", 0, 0, 0, 0, 0, 0, 0" + CR + LF;
                } else if (Converters.getHexValue(packet[index]).equals("F1")) {//52
                    Log.i(TAG, Converters.getHexValue(packet[index]));
                    data += "0, 0, 0, 0, 0, 0, " + Converters.getDecimalValue(packet[index + 2]) + ", 0," +
                            (Integer.parseInt(Converters.getDecimalValue(packet[index + 4])) + 200) + ", " +
                            Converters.getDecimalValue(packet[index + 3]) + ", " +
                            (Integer.parseInt(Converters.getDecimalValue(packet[index + 5])) - 100) + ", " +
                            ((Integer.parseInt(Converters.getDecimalValue(packet[index + 5])) - 100) / 100) + ", 0, 0, 0" + CR + LF;
                }
                index += 8;
            }
        } else {
            error = true;
        }
        return data;
    }

    /**
     * With the received packet, gets the raw data.
     *
     * @param packet The received packet.
     */
    private void downloadData(byte[] packet) {
        if(snapshotArray.size() == 0 && mConnected)
            Timber.tag("DCA:dD 344").d("Collection begins");
        if (mConnected) {
            if (packet.length == 4) { // A 4-byte packet contains the current page number
                if (finalPageNumber == 0) { // No data to download
                    progressGIF.setVisibility(View.GONE);
                    percentage.setVisibility(View.GONE);
                    showPrintDialog(this, "Message", "No data to download.", 1);
                    parameter1 = "";
                    return;
                }
                if ((pageNumber + 1) == findPageNumber(packet) && (pageNumber + 1) < finalPageNumber) {
                    // The current page number must be one more than the previous one and less than the total number of pages
                    pageNumber = findPageNumber(packet);
                    // Download percentage is updated
                    percent = (pageNumber / finalPageNumber) * 100;
                    percentage.setText(percent + "%");
                } else { // Shows an error and stops downloading
                    progressGIF.setVisibility(View.GONE);
                    percentage.setVisibility(View.GONE);
                    showPrintDialog(this, "Error", "Download error.", 1);
                    parameter1 = "";
                }
            } else if (packet.length == 5) { //Shows an error when the packet contains 5 bytes and stops downloading
                Log.i(TAG, Converters.getHexValue(packet));
                progressGIF.setVisibility(View.GONE);
                percentage.setVisibility(View.GONE);
                showPrintDialog(this, "Error", "Download error.", 1);
                parameter1 = "";
            } else { // Copy the downloaded package
                Log.i(TAG, "SIZE: "+packet.length+" PAGE NUMBER: "+pageNumber);
                rawDataCollector.processSnapshotRaw(packet);
                if (rawDataCollector.isFilled()) {
                    // Completed the download and have to process the data
                    String processData = readPacket(rawDataCollector.getSnapshot());
                    if (!error) { // Adds the data to the list if you didn't find any errors during processing
                        byte[] data = Converters.convertToUTF8(processData);
                        processDataCollector = new Snapshots(data.length);
                        processDataCollector.processSnapshot(data);
                        snapshotArray.add(processDataCollector);
                    }
                    percentage.setText("100%");
                    printSnapshotFiles();
                }
            }
        } else {
            if (rawDataCollector.getFileName().contains("error"))
                Timber.tag("DCA:dD 356").wtf("Data collection fail by disconnection. %s", rawDataCollector.getFileName());
            else
                Timber.tag("DCA:dD 360").d("All files collected: %s", snapshotArray.size());
        }
    }

    private void printSnapshotFiles() {
        int i = 0; boolean outcome; String msg;
        try {
            //set the directory path
            root = new File(Environment.getExternalStorageDirectory(), "atstrack");
            if (!root.exists()) {
                outcome = root.mkdirs();
                if (!outcome)
                    throw new Exception("Folder 'atstrack' can't be created.");
                root.setReadable(true);
                root.setWritable(true);
            }
            while(i < snapshotArray.size()) {
                //get the fileName and create the file path
                fName = snapshotArray.get(i).getFileName();
                newFile = new File(root.getAbsolutePath(),fName);
                //see if there's a possible copy
                int copy = 1;
                while (!(newFile.createNewFile())) {
                    newFile = new File(root.getAbsolutePath(), fName.substring(0, fName.length() - 4) + " (" + copy + ").txt");
                    copy++;
                }
                newFile.setReadable(true);
                newFile.setWritable(true);
                //write in the file created
                stream = new FileOutputStream(newFile);
                stream.write(snapshotArray.get(i).getSnapshot());
                //save the file
                stream.flush();
                stream.close();
                //go for the next
                i++;
            }

            if (i == snapshotArray.size()) {
                progressGIF.setVisibility(View.GONE);
                percentage.setVisibility(View.GONE);
                Timber.tag("DCA:psF 543").d("%s byte(s) downloaded successfully. No fails!", (Snapshots.BYTES_PER_PAGE * finalPageNumber));
                msg = "Download finished: " + (Snapshots.BYTES_PER_PAGE * finalPageNumber) + " byte(s) downloaded successfully.";
                if (error) {
                    msg += " No data found in bytes downloaded. No file was generated.";
                    showPrintDialog(this,"Finished", msg, 1);
                } else {
                    showPrintDialog(this,"Finished", msg, 3);
                }
            }
        }
        catch (Exception e) {
            progressGIF.setVisibility(View.GONE);
            percentage.setVisibility(View.GONE);
            Timber.tag("DCA:psF 552").e(e, "%s fail(s), %ss byte(s) downloaded in total.", 0, (Snapshots.BYTES_PER_PAGE * finalPageNumber));
            msg = "Download finished: "+ (Snapshots.BYTES_PER_PAGE * finalPageNumber) + " byte(s) downloaded.";
            if (error) {
                msg += " No data found in bytes downloaded. No file was generated.";
                showPrintDialog(this,"Finished", msg, 1);
            } else {
                showPrintDialog(this,"Finished", msg, 3);
            }
        }
    }

    public void showPrintDialog(Activity activity, String title, CharSequence message, int buttonNum) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        if (title != null) builder.setTitle(title);
        builder.setMessage(message);
        if (buttonNum == 1) // No data found in bytes downloaded
            builder.setPositiveButton("OK", (dialog, which) -> {
                subMenu.setVisibility(View.VISIBLE);
            });
        if (buttonNum == 2) { // Save to the cloud
            builder.setPositiveButton("OK", (dialog, which) -> {
                requestSignIn();
            });
            builder.setNegativeButton("Cancel", null);
        }
        if (buttonNum == 3) // Ask if you want to save file to the cloud
            builder.setPositiveButton("OK", (dialog, which) -> {
                subMenu.setVisibility(View.VISIBLE);
                showPrintDialog(this, "Finished", "Do you want to send the file to the cloud?", 2);
            });
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.catskill_white)));
    }

    public void onRestartConnection() {
        mBluetoothLeService.disconnect();
        SystemClock.sleep(1000);
        mBluetoothLeService.connect(mDeviceAddress);
    }

    private void requestSignIn() {
        GoogleSignInOptions signInOptions = new
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().requestScopes(new Scope(DriveScopes.DRIVE_FILE)).build();
        GoogleSignInClient client = GoogleSignIn.getClient(this, signInOptions);
        startActivityForResult(client.getSignInIntent(), REQUEST_CODE_SIGN_IN);
    }

    private void handleSignInIntent(Intent data) {
        GoogleSignIn.getSignedInAccountFromIntent(data).addOnSuccessListener(new OnSuccessListener<GoogleSignInAccount>() {
            @Override
            public void onSuccess(GoogleSignInAccount googleSignInAccount) {
                GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                        GetDataActivity.this, Collections.singleton(DriveScopes.DRIVE_FILE));

                credential.setSelectedAccount(googleSignInAccount.getAccount());

                Drive googleDriveService = new Drive.Builder(
                        AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential).setApplicationName("ATS VHF Receiver").build();

                driveServiceHelper = new DriveServiceHelper(googleDriveService);

                uploadFile();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }

    public void uploadFile() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Uploading to Google Drive.");
        progressDialog.setMessage("Please wait...");
        progressDialog.show();

        driveServiceHelper.createFile(root.getAbsolutePath(), fName).addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String s) {
                progressDialog.dismiss();
                Toast.makeText(getApplicationContext(), "Uploaded successfully.", Toast.LENGTH_LONG).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();
                Toast.makeText(getApplicationContext(), "Check your Google Drive Api key", Toast.LENGTH_LONG).show();
            }
        });
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
        });
        builder.show();
    }
}
