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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.atstrack.ats.ats_vhf_receiver.BluetoothATS.BluetoothLeService;
import com.atstrack.ats.ats_vhf_receiver.Utils.Converters;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.UUID;

import static com.atstrack.ats.ats_vhf_receiver.R.color.catskill_white;
import static com.atstrack.ats.ats_vhf_receiver.R.color.light_blue;

public class EditTablesActivity extends AppCompatActivity {

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
    @BindView(R.id.number_table_textView)
    TextView number_table_textView;
    @BindView(R.id.load_from_file_editTable)
    TextView load_from_file_editTable;
    @BindView(R.id.discard_changes_editTable)
    TextView discard_changes_editTable;
    @BindView(R.id.frequency_table)
    TableLayout frequency_table;
    @BindView(R.id.edit_table_message_textView)
    TextView edit_table_message_textView;
    @BindView(R.id.save_changes_button)
    Button save_changes_button;

    final private String TAG = EditTablesActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String EXTRAS_BATTERY = "DEVICE_BATTERY";
    private final int MESSAGE_PERIOD = 3000;

    private int[] table;
    private int number;
    private int totalFrequencies;
    private int baseFrequency;
    private int range;
    private boolean isFile;

    private TextView textCell;
    private TableRow tableRow;
    private int widthPixels;
    private int heightPixels;
    private int tableLimitEdit;
    private final static char LF  = (char) 0x0A;
    private boolean isChanged;

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
                        case "table": // Ask for frequencies from a table
                            onClickTable();
                            break;
                        case "receive": // Gets the frequencies from a table
                            onReceiveTable();
                            break;
                        case "sendTable": // Sends the modified frequencies
                            onClickSendTable();
                            break;
                    }
                } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                    byte[] packet = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                    if (parameter1.equals("receive")) // Gets the frequencies from a table
                        downloadData(packet);
                    else if (parameter1.equals("sendTable")) // Sends the modified frequencies
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

    /**
     * Writes the table number to get its frequencies.
     * Service name: StoredData.
     * Characteristic name: FreqTable.
     */
    public void onClickTable() {
        parameter1 = "receive";
        byte[] b = new byte[]{(byte) 0x7E, (byte) 0x7E, (byte) 0x7E, (byte) 0x7E, (byte) 0x7E, (byte) 0x7E, (byte) 0x7E, (byte)number};
        UUID uservice = UUID.fromString("609d10ad-d22d-48f3-9e6e-d035398c3606");
        UUID uservicechar = UUID.fromString("ad0ea6e5-d93a-47a5-a6fc-a930552520dd");
        mBluetoothLeService.writeCharacteristic(uservice, uservicechar, b, true);
    }

    /**
     * Enables notification for receive the frequencies from that table.
     * Service name: StoredData.
     * Characteristic name: FreqTable.
     */
    public void onReceiveTable() {
        UUID uservice = UUID.fromString("609d10ad-d22d-48f3-9e6e-d035398c3606");
        UUID uservicechar = UUID.fromString("ad0ea6e5-d93a-47a5-a6fc-a930552520dd");
        mBluetoothLeService.setCharacteristicNotificationRead(uservice, uservicechar, true);
    }

    /**
     * Writes the modified frequencies by the user.
     * Service name: StoredData.
     * Characteristic name: FreqTable.
     */
    private void onClickSendTable() {
        parameter1 = "";
        Calendar currentDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        int YY = currentDate.get(Calendar.YEAR);
        int MM = currentDate.get(Calendar.MONTH);
        int DD = currentDate.get(Calendar.DAY_OF_MONTH);
        int hh = currentDate.get(Calendar.HOUR_OF_DAY);
        int mm =  currentDate.get(Calendar.MINUTE);
        int ss = currentDate.get(Calendar.SECOND);

        byte[] b = new byte[244];
        b[0] = (byte) 0x7E;
        b[1] = (byte) YY;
        b[2] = (byte) MM;
        b[3] = (byte) DD;
        b[4] = (byte) hh;
        b[5] = (byte) mm;
        b[6] = (byte) ss;
        b[7] = (byte) number;//frequency number table
        b[8] = (byte) table.length;//Number of frequencies in the table
        b[9] = (byte) baseFrequency;//base frequency

        int index = 10;
        int i = 0;
        while (i < table.length) {
            table[i] = (table[i] % (Integer.parseInt(Converters.getDecimalValue(b[9])) * 1000));
            Log.i(TAG, ""+table[i]);
            b[index] = (byte) (table[i] / 256);
            b[index + 1] = (byte) (table[i] % 256);
            i++;
            index += 2;
        }

        UUID uservice = UUID.fromString("609d10ad-d22d-48f3-9e6e-d035398c3606");
        UUID uservicechar = UUID.fromString("ad0ea6e5-d93a-47a5-a6fc-a930552520dd");
        mBluetoothLeService.writeCharacteristic(uservice, uservicechar, b, false);

        finish();
        mBluetoothLeService.disconnect();
    }

    /**
     * Displays a alert dialog for the user to choose to edit the table or delete all frequencies.
     */
    public void createDialogSelect() {
        LayoutInflater inflater = LayoutInflater.from(this);

        View view = inflater.inflate(R.layout.layout_dialog, null);
        final AlertDialog dialog = new AlertDialog.Builder(this).create();

        Button editTable = view.findViewById(R.id.edit_table_button);
        Button clearTable = view.findViewById(R.id.clear_table_button);

        editTable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                createDialogEdit();
            }
        });
        clearTable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setTitle("Discard Changes");
                builder.setMessage("Are you sure you want to delete all the frequencies in this table?");
                builder.setPositiveButton("Delete Frequencies", (dialog, which) -> {
                    table = new int[]{};
                    isChanged = true;
                    frequency_table.removeAllViews();
                    showTable();
                });
                builder.setNegativeButton("Cancel", null);
                AlertDialog dialogI = builder.create();
                dialogI.show();
                dialogI.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.catskill_white)));
                dialog.dismiss();
            }
        });
        dialog.getWindow().setLayout(widthPixels * 1 / 3, heightPixels * 1 / 5);
        dialog.setView(view);
        dialog.show();
    }

    /**
     * Displays a alert dialog where the user can edit the frequencies of that table, also check that each frequency is correct.
     */
    public void createDialogEdit() {
        LayoutInflater inflater = LayoutInflater.from(this);

        View view = inflater.inflate(R.layout.layout_edit_table, null);
        final AlertDialog builder = new AlertDialog.Builder(this).create();
        // Does not allow the alert dialog to disappear when touch outside of it
        builder.setCanceledOnTouchOutside(false);
        // Asks if you want to lose the data already modified when touching back while the alert dialog is displayed
        builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogI, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP){
                    AlertDialog.Builder builder = new AlertDialog.Builder(getBaseContext());
                    builder.setTitle("Discard Changes");
                    builder.setMessage("If you leave without saving you will lose your changes. Do you wish to continue?");
                    builder.setNegativeButton("Cancel", null);
                    builder.setPositiveButton("Discard", (dialog, which) -> {
                        dialogI.dismiss();
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.catskill_white)));
                    return true;
                }
                return false;
            }
        });

        // Defines the size of the alert dialog
        ScrollView editScrollView = view.findViewById(R.id.edit_table_scrollView);
        ViewGroup.LayoutParams params = editScrollView.getLayoutParams();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = heightPixels / 3;
        editScrollView.setLayoutParams(params);

        // Customizes the style of the alert dialog
        TextView title = new TextView(this);
        title.setText("Edit Table " + number);
        title.setPadding(100, 80, 10, 10);
        title.setTextSize(20);
        title.setTypeface(null, Typeface.BOLD);
        title.setBackgroundColor(ContextCompat.getColor(this, catskill_white));
        title.setTextColor(ContextCompat.getColor(this, light_blue));
        builder.setCustomTitle(title);
        EditText editTable = view.findViewById(R.id.edit_table_editText);
        String frequencies = "";

        for (int i = 0; i < table.length; i++) {
            if (i > 0)
                frequencies += "" + LF;
            frequencies = frequencies + table[i];
        }
        // Set the frequencies to edit
        editTable.setText(frequencies);
        /*editTable.setEnabled(true);
        editTable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                builder.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }
        });*/

        // Keeps keys visible
        editTable.requestFocus();
        editTable.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus){
                    builder.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });

        Button undoChanges = view.findViewById(R.id.undo_changes_button);
        Button done = view.findViewById(R.id.done_button);

        // Copy the initial frequencies
        String initFrequencies = frequencies;
        undoChanges.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setTitle("Warning");
                builder.setMessage("Are you sure you want to discard your changes?");
                builder.setNegativeButton("No", null);
                builder.setPositiveButton("Yes", (dialog, which) -> {
                    editTable.getText().clear();
                    editTable.setText(initFrequencies);
                });
                AlertDialog dialogI = builder.create();
                dialogI.show();
                dialogI.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(catskill_white)));
            }
        });
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String frequencies = String.valueOf(editTable.getText());
                if (checkFormat(frequencies)) {
                    if (checkTableLimit()) {
                        saveChanges(frequencies);
                        // Removes all frequencies
                        frequency_table.removeAllViews();
                        // Set the modified frequencies
                        showTable();
                        if (isChanged) { // If changes were made, enables the discard changes button and save changes button
                            save_changes_button.setBackgroundResource(R.color.mountain_meadow);
                            save_changes_button.setTextColor(ContextCompat.getColor(v.getContext(), R.color.white));
                            save_changes_button.setEnabled(true);
                            edit_table_message_textView.setVisibility(View.GONE);
                            load_from_file_editTable.setVisibility(View.GONE);
                            discard_changes_editTable.setVisibility(View.VISIBLE);
                        }
                        builder.dismiss();
                    } else {
                        showWarningDialog("Exceeded Table Limit", "Please enter no more than 100 frequencies.");
                    }
                } else {
                    showWarningDialog("Invalid Format or Values", "Please enter valid frequency values, each on a separate line.");
                }
            }
        });
        builder.setView(view);
        builder.show();
        // Enables the keys when the alert dialog appears
        builder.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        builder.getWindow().setLayout(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
    }

    /**
     * Checks that all edited frequencies are in the correct format.
     *
     * @param frequencies The edited frequencies.
     *
     * @return Returns true, if the frequencies are correct.
     */
    public boolean checkFormat(String frequencies) {
        tableLimitEdit = 0;
        int beginIndex = 0;
        int endIndex;
        boolean correct = true;
        while (beginIndex < frequencies.length() && correct) {
            endIndex = frequencies.indexOf(LF, beginIndex);
            if (endIndex == -1)
                endIndex = frequencies.length();
            // Gets a frequency from the string
            String frequency = frequencies.substring(beginIndex, endIndex);
            beginIndex = endIndex + 1;
            tableLimitEdit++;
            // Checks that the frequency size is 6
            if (frequency.length() != 6)
                correct = false;
            // Checks that the frequency is within the range
            if ((Integer.parseInt(frequency) < (baseFrequency * 1000))
                    || (Integer.parseInt(frequency) > (range * 1000) + (baseFrequency * 1000))) //>=
                correct = false;
        }
        return correct;
    }

    /**
     * Checks that the number of frequencies is not greater than 100.
     *
     * @return Returns true, if the number of frequencies is less than or equal to 100.
     */
    public boolean checkTableLimit(){
        return tableLimitEdit <= 100;
    }

    /**
     * Displays a specific error message.
     *
     * @param error The error name.
     * @param message The message to be displayed.
     */
    public void showWarningDialog(String error, String message){
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(error);
        builder.setMessage(message);
        builder.setPositiveButton("Ok", null);
        android.app.AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(catskill_white)));
    }

    /**
     * Saves the edited frequencies and updates the vector containing all the previous frequencies.
     *
     * @param frequencies The edited frequencies.
     */
    public void saveChanges(String frequencies) {
        int[] newTable = new int[tableLimitEdit];
        int beginIndex = 0;
        int endIndex;
        int column = 0;
        // If the number of edited frequencies is greater than the previous frequencies then it means that there were changes
        if (tableLimitEdit != table.length)
            isChanged = true;
        while (beginIndex < frequencies.length()) {
            endIndex = frequencies.indexOf(LF, beginIndex);
            if (endIndex == -1)
                endIndex = frequencies.length();
            // Gets a frequency from the string
            String frequency = frequencies.substring(beginIndex, endIndex);
            beginIndex = endIndex + 1;
            //Add to table
            newTable[column] = Integer.parseInt(frequency);
            if (!isChanged) {
                // If that frequency is different from the previous one then it means that there were changes
                if (newTable[column] != table[column])
                    isChanged = true;
            }
            column++;
        }
        // The table is updated
        table = newTable;
    }

    @OnClick(R.id.edit_aerial_defaults_textView)
    public void onClickEditDefaults(View v) {
        createDialogSelect();
    }

    @OnClick(R.id.discard_changes_editTable)
    public void onClickDiscardChanges(View v) {
        parameter1 = "table";
        mBluetoothLeService.connect(mDeviceAddress);
    }

    @OnClick(R.id.save_changes_button)
    public void onClickSaveChanges(View v) {
        parameter1 = "sendTable";
        mBluetoothLeService.connect(mDeviceAddress);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_tables);
        ButterKnife.bind(this);

        // Customize the activity menu
        setSupportActionBar(toolbar);
        title_toolbar.setText("Edit Frequency Tables");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Gets the size of the cell phone screen in pixels
        widthPixels = getResources().getDisplayMetrics().widthPixels;
        heightPixels = getResources().getDisplayMetrics().heightPixels;

        isChanged = false;

        // Get device data from previous activity
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        mPercentBattery = intent.getStringExtra(EXTRAS_BATTERY);

        device_name_textView.setText(mDeviceName);
        device_address_textView.setText(mDeviceAddress);
        percent_battery_textView.setText(mPercentBattery);

        // Gets the table number to ask its frequencies
        number = getIntent().getExtras().getInt("number");
        // Gets the number of frequencies from that table
        totalFrequencies = getIntent().getExtras().getInt("total");
        number_table_textView.setText("Table " + number);

        isFile = getIntent().getExtras().getBoolean("isFile");
        if (isFile) { // Asks for the frequencies obtained from a file
            int[] fileTable = getIntent().getExtras().getIntArray("frequencies");
            table = new int[totalFrequencies];
            for (int i = 0; i < table.length; i++)
                table[i] = fileTable[i];

            showTable();
        } else { // Asks for the frequencies from that table, the frequency base and range
            baseFrequency = getIntent().getExtras().getInt("baseFrequency");
            range = getIntent().getExtras().getInt("range");
            if (totalFrequencies > 0) { // Asks for the frequencies from the table
                parameter1 = "table";
            } else { // Initializes empty
                downloadData(new byte[]{});
            }
        }

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: //Go back to the previous activity
                if (isChanged) { // Asks if you want to lose changes
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Discard Changes");
                    builder.setMessage("If you leave without saving you will lose your changes. Do you wish to continue?");
                    builder.setPositiveButton("Discard", (dialog, which) -> {
                        Intent intent = new Intent(this, TableOverviewActivity.class);
                        intent.putExtra(TableOverviewActivity.EXTRAS_DEVICE_NAME, mDeviceName);
                        intent.putExtra(TableOverviewActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
                        intent.putExtra(TableOverviewActivity.EXTRAS_BATTERY, mPercentBattery);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        mBluetoothLeService.disconnect();
                    });
                    builder.setNegativeButton("Cancel", null);
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(catskill_white)));
                } else {
                    finish();
                }
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
        if (!isFile) {
            unbindService(mServiceConnection);
            mBluetoothLeService = null;
        }
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
     * Displays the frequencies on the screen.
     */
    private void showTable() {
        for (int i = 0; i < table.length - 1; i++) {
            newCell();
            textCell.setText("" + table[i]);
            tableRow.addView(textCell, newTableRowParams(0, 0, 0, 16));
            frequency_table.addView(tableRow);
        }
        if (table.length > 0) {
            newCell();
            textCell.setText("" + table[table.length - 1]);
            tableRow.addView(textCell, newTableRowParams(0, 0, 0, 0));
            frequency_table.addView(tableRow);
        }
    }

    /**
     * Initializes the customize TextView.
     */
    private void newCell() {
        tableRow = new TableRow(this);
        textCell = new TextView(this);
        textCell.setTextSize(16);
        textCell.setTextColor(ContextCompat.getColor(this, light_blue));
    }

    /**
     * Set the margins for the TableRow.
     *
     * @param left The size left margin.
     * @param top The size top margin.
     * @param right The size right margin.
     * @param bottom The size bottom margin.
     *
     * @return Returns a LayoutParams with the customize margins.
     */
    public TableRow.LayoutParams newTableRowParams(int left, int top, int right, int bottom) {
        TableRow.LayoutParams params = new TableRow.LayoutParams();
        params.setMargins(left, top, right, bottom);
        params.weight = 1;
        return params;
    }

    /**
     * With the received packet, gets the frequencies from the table and display on the screen.
     *
     * @param data The received packet.
     */
    public void downloadData(byte[] data) {
        mBluetoothLeService.disconnect();
        isChanged = false;
        discard_changes_editTable.setVisibility(View.GONE);
        load_from_file_editTable.setVisibility(View.VISIBLE);
        edit_table_message_textView.setVisibility(View.VISIBLE);
        save_changes_button.setBackgroundResource(R.color.slate_gray);
        save_changes_button.setTextColor(ContextCompat.getColor(this, R.color.ghost));
        save_changes_button.setEnabled(false);
        table = new int[totalFrequencies];

        int index = 10;
        int i = 0;
        while (i < table.length) {
            int frequency = (Integer.parseInt(Converters.getDecimalValue(data[index])) * 256) +
                    Integer.parseInt(Converters.getDecimalValue(data[index + 1]));
            table[i] = (baseFrequency * 1000) + frequency;
            i++;
            index += 2;
        }

        frequency_table.removeAllViews();
        showTable();
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
}
