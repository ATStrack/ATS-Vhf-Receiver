package com.atstrack.ats.ats_vhf_receiver;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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
import android.os.SystemClock;
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

import static com.atstrack.ats.ats_vhf_receiver.R.color.colorbackground;
import static com.atstrack.ats.ats_vhf_receiver.R.color.colorbody;
import static com.atstrack.ats.ats_vhf_receiver.R.color.colorbodypair;
import static com.atstrack.ats.ats_vhf_receiver.R.color.colorheader;
import static com.atstrack.ats.ats_vhf_receiver.R.color.colortext;
import static com.atstrack.ats.ats_vhf_receiver.R.color.colortextbutton;
import static com.atstrack.ats.ats_vhf_receiver.R.color.colortexttable;
import static com.atstrack.ats.ats_vhf_receiver.R.color.dark;

public class EditTablesActivity extends AppCompatActivity {

    @BindView(R.id.device_name_editTables)
    TextView device_name_textView;
    @BindView(R.id.device_address_editTables)
    TextView device_address_textView;
    @BindView(R.id.frequency_table)
    TableLayout frequency_table;

    private int[] table;

    final private String TAG = EditTablesActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private final int MESSAGE_PERIOD = 3000;

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
    public final static char LF  = (char) 0x0A;
    private boolean isChanged;

    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private boolean state = true;

    private Handler mHandler;

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
                    if (parameter1.equals("table"))
                        onClickTable();
                    else if (parameter1.equals("receive"))
                        onReceiveTable();
                    else if (parameter1.equals("sendTable"))
                        onClickSendTable();
                } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                    byte[] packet = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                    if (parameter1.equals("receive"))
                        downloadData(packet);
                    else if (parameter1.equals("sendTable"))
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

    public void onClickTable(){
        parameter1 = "receive";
        byte[] b = new byte[]{(byte) 0x7E, (byte) 0x7E, (byte) 0x7E, (byte) 0x7E, (byte) 0x7E, (byte) 0x7E, (byte) 0x7E, (byte)number};
        UUID uservice = UUID.fromString("609d10ad-d22d-48f3-9e6e-d035398c3606");
        UUID uservicechar = UUID.fromString("ad0ea6e5-d93a-47a5-a6fc-a930552520dd");
        mBluetoothLeService.writeCharacteristic(uservice, uservicechar, b, true);
    }

    public void onReceiveTable(){
        UUID uservice = UUID.fromString("609d10ad-d22d-48f3-9e6e-d035398c3606");
        UUID uservicechar = UUID.fromString("ad0ea6e5-d93a-47a5-a6fc-a930552520dd");
        mBluetoothLeService.setCharacteristicNotificationRead(uservice, uservicechar, true);
    }

    private void onClickSendTable(){
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
        Log.i(TAG, Converters.getDecimalValue(b));

        UUID uservice = UUID.fromString("609d10ad-d22d-48f3-9e6e-d035398c3606");
        UUID uservicechar = UUID.fromString("ad0ea6e5-d93a-47a5-a6fc-a930552520dd");
        mBluetoothLeService.writeCharacteristic( uservice,uservicechar,b);

        Intent intent = new Intent(this, TableOverviewActivity.class);
        intent.putExtra(EditReceiverDefaultsActivity.EXTRAS_DEVICE_NAME, mDeviceName);
        intent.putExtra(EditReceiverDefaultsActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
        startActivity(intent);
        mBluetoothLeService.disconnect();
    }

    public View.OnLongClickListener listenerTable = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            createDialogSelect();
            return false;
        }
    };

    public void createDialogSelect(){
        mBluetoothLeService.disconnect();
        LayoutInflater inflater = LayoutInflater.from(this);

        View view =inflater.inflate(R.layout.layout_dialog, null);
        final AlertDialog dialog = new AlertDialog.Builder(this).create();

        Button editTable = view.findViewById(R.id.btt_editTable);
        Button clearTable = view.findViewById(R.id.btt_clearTable);

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
                dialogI.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorbackground)));
                dialog.dismiss();
            }
        });
        dialog.getWindow().setLayout(widthPixels * 1 / 3, heightPixels * 1 / 5);
        dialog.setView(view);
        dialog.show();
    }

    public void createDialogEdit() {
        LayoutInflater inflater = LayoutInflater.from(this);

        View view =inflater.inflate(R.layout.layout_edit_table, null);
        final AlertDialog builder = new AlertDialog.Builder(this).create();
        builder.setCanceledOnTouchOutside(false);
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
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorbackground)));
                    return true;
                }
                return false;
            }
        });

        ScrollView editScrollView = view.findViewById(R.id.editScrollView);
        ViewGroup.LayoutParams params = editScrollView.getLayoutParams();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = heightPixels * 1 / 3;
        editScrollView.setLayoutParams(params);

        TextView title = new TextView(this);
        title.setText("Edit Table " + number);
        title.setPadding(100, 80, 10, 10);
        title.setTextSize(20);
        title.setTypeface(null, Typeface.BOLD);
        title.setBackgroundColor(ContextCompat.getColor(this, colorbackground));
        title.setTextColor(ContextCompat.getColor(this, colortext));
        builder.setCustomTitle(title);
        EditText editTable = view.findViewById(R.id.editTable);
        String frequencies = "";

        for (int i = 0; i < table.length; i++) {
            if (i > 0)
                frequencies += "" + LF;
            frequencies = frequencies + table[i];
        }
        editTable.setText(frequencies);
        /*editTable.setEnabled(true);
        editTable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                builder.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }
        });*/
        editTable.requestFocus();
        editTable.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus){
                    builder.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });

        Button undoChanges = view.findViewById(R.id.btt_UndoChanges);
        Button done = view.findViewById(R.id.btt_Done);

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
                dialogI.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorbackground)));
            }
        });
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = String.valueOf(editTable.getText());
                if (checkFormat(text)) {
                    if (checkTableLimit()) {
                        saveChanges(text);
                        frequency_table.removeAllViews();
                        showTable();
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
        builder.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        builder.getWindow().setLayout(RelativeLayout.LayoutParams.WRAP_CONTENT, heightPixels * 3 / 5);
    }

    public boolean checkFormat(String text) {
        tableLimitEdit = 0;
        int beginIndex = 0;
        int endIndex;
        boolean correct = true;
        while (beginIndex < text.length() && correct) {
            endIndex = text.indexOf(LF, beginIndex);
            if (endIndex == -1)
                endIndex = text.length();
            String frequency = text.substring(beginIndex, endIndex);
            beginIndex = endIndex + 1;
            tableLimitEdit++;
            if (frequency.length() != 6)
                correct = false;
            if ((Integer.parseInt(frequency) < (baseFrequency * 1000))
                    || (Integer.parseInt(frequency) > (range * 1000) + (baseFrequency * 1000))) //>=
                correct = false;
        }
        return correct;
    }

    public boolean checkTableLimit(){
        return tableLimitEdit <= 100;
    }

    public void showWarningDialog(String error, String message){
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(error);
        builder.setMessage(message);
        builder.setPositiveButton("Ok", null);
        android.app.AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorbackground)));
    }

    public void saveChanges(String text){
        int[] newTable = new int[tableLimitEdit];
        int beginIndex = 0;
        int endIndex;
        int column = 0;
        if (tableLimitEdit != table.length)
            isChanged = true;
        while (beginIndex < text.length()) {
            endIndex = text.indexOf(LF, beginIndex);
            if (endIndex == -1)
                endIndex = text.length();
            String frequency = text.substring(beginIndex, endIndex);
            beginIndex = endIndex + 1;//Add to matrix
            newTable[column] = Integer.valueOf(frequency);
            if (!isChanged) {
                if (newTable[column] != table[column])
                    isChanged = true;
            }
            column++;
        }
        table = newTable;
    }

    @OnClick(R.id.save_changes_button)
    public void onClickSaveChanges(View v) {
        mBluetoothLeService.connect(mDeviceAddress);
        parameter1 = "sendTable";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_tables);
        ButterKnife.bind(this);

        widthPixels = getResources().getDisplayMetrics().widthPixels;
        heightPixels = getResources().getDisplayMetrics().heightPixels;

        isChanged = false;

        final Intent intent = getIntent();
        number = getIntent().getExtras().getInt("number");
        totalFrequencies = getIntent().getExtras().getInt("total");
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        device_name_textView.setText(mDeviceName);
        device_address_textView.setText(mDeviceAddress);

        mHandler = new Handler();

        getSupportActionBar().setElevation(0);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_chevron_back_icon_opt);
        getSupportActionBar().setTitle("EDIT TABLE " + number);

        isFile = getIntent().getExtras().getBoolean("isFile");
        if (isFile) {
            int[] fileTable = getIntent().getExtras().getIntArray("frequencies");
            table = new int[totalFrequencies];
            for (int i = 0; i < table.length; i++)
                table[i] = fileTable[i];

            showTable();
        } else {
            baseFrequency = getIntent().getExtras().getInt("baseFrequency");
            range = getIntent().getExtras().getInt("range");
            if (totalFrequencies > 0) {
                parameter1 = "table";
            } else {
                downloadData(new byte[]{});
            }
        }

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        /*getSupportActionBar().setDisplayHomeAsUpEnabled(true);//Connect to device

        int heightPixels = getResources().getDisplayMetrics().heightPixels;
        ViewGroup.LayoutParams params = editTables.getLayoutParams();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = heightPixels * 3 / 4;
        editTables.setLayoutParams(params);
        longTable = 0;

        final Intent intent = getIntent();
        type = intent.getExtras().getString("type");

        //Init connect to device
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        //Finish connect to device

        count = intent.getExtras().getInt("count");
        tables = new int[count][];
        if (!type.equals("createtable")) {
            for (int i = 0; i < count; i++)
                tables[i] = intent.getExtras().getIntArray("table" + i);

            countTablesSelected = intent.getExtras().getInt("countTablesSelected");
            idTablesSelected = intent.getExtras().getIntArray("idTablesSelected");
            tablesSelected = new int[countTablesSelected][100];
        } else {
            countTablesSelected = 1;
            idTablesSelected = new int[]{idInit};
            tablesSelected = new int[countTablesSelected][100];
        }
        insertTable();

        createHeader();
        tableDynamic = new TableDynamic(tableLayout, this);
        tableDynamic.addType(type);
        tableDynamic.addLongTable(longTable);
        tableDynamic.addHeader(header);
        tableDynamic.addTable(tablesSelected);*/
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home://hago un case por si en un futuro agrego mas opciones
                if (isChanged) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Discard Changes");
                    builder.setMessage("If you leave without saving you will lose your changes. Do you wish to continue?");
                    builder.setPositiveButton("Discard", (dialog, which) -> {
                        Intent intent = new Intent(this, TableOverviewActivity.class);
                        intent.putExtra(EditReceiverDefaultsActivity.EXTRAS_DEVICE_NAME, mDeviceName);
                        intent.putExtra(EditReceiverDefaultsActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    });
                    builder.setNegativeButton("Cancel", null);
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorbackground)));
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

    private void showTable() {
        createHeader();
        createBody();
    }

    private void newCell() {
        tableRow = new TableRow(this);
        textCell = new TextView(this);
        textCell.setTextSize(16);
        textCell.setTextColor(ContextCompat.getColor(this, dark));
    }

    public TableRow.LayoutParams newTableRowParams(int left, int top, int right, int bottom) {
        TableRow.LayoutParams params = new TableRow.LayoutParams();
        params.setMargins(left, top, right, bottom);
        params.weight = 1;
        return params;
    }

    private void createHeader() {
        newCell();
        textCell.setTextSize(18);
        textCell.setTextColor(ContextCompat.getColor(this, colortextbutton));
        textCell.setText("Table " + number + " (tap table to edit)");
        textCell.setBackgroundColor(ContextCompat.getColor(this, colortext));
        textCell.setOnLongClickListener(listenerTable);
        tableRow.addView(textCell, newTableRowParams(10, 10, 10, 10));
        frequency_table.addView(tableRow);
    }

    private void createBody() {
        if (table.length > 1) {
            newCell();
            textCell.setText("" + table[0]);
            textCell.setOnLongClickListener(listenerTable);
            tableRow.addView(textCell, newTableRowParams(8, 10, 8, 6));
            frequency_table.addView(tableRow);
        }
        for (int i = 1; i < table.length - 1; i++) {
            newCell();
            textCell.setText("" + table[i]);
            textCell.setOnLongClickListener(listenerTable);
            tableRow.addView(textCell, newTableRowParams(8, 6, 8, 6));
            frequency_table.addView(tableRow);
        }
        if (table.length > 0) {
            newCell();
            textCell.setText("" + table[table.length - 1]);
            textCell.setOnLongClickListener(listenerTable);
            tableRow.addView(textCell, newTableRowParams(8, 6, 8, 10));
            frequency_table.addView(tableRow);
        }
    }

    public void downloadData(byte[] data) {
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

        showTable();
    }

    private void showMessage(byte[] data) {
        int status = Integer.parseInt(Converters.getDecimalValue(data[0]));

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Success!");
        if (status == 0)
            builder.setMessage("Completed.");
        builder.setPositiveButton("OK", (dialog, which) -> {
            Intent intent = new Intent(this, TableOverviewActivity.class);
            intent.putExtra(EditReceiverDefaultsActivity.EXTRAS_DEVICE_NAME, mDeviceName);
            intent.putExtra(EditReceiverDefaultsActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            mBluetoothLeService.disconnect();
        });
        builder.show();
    }

    /*@Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Warning");
        builder.setMessage("Are you sure you want to discard your changes?");
        builder.setNegativeButton("No", null);
        builder.setPositiveButton("Yes", (dialog, which) -> {
            super.onBackPressed();
        });
        builder.show();
    }

    public void createHeader(){
        header = new String[countTablesSelected];
        for (int i=0;i<countTablesSelected;i++){
            header[i] = "Table " + ((idTablesSelected[i] % 1000) + 1);
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
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                Intent i=new Intent(this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void insertTable(){
        if (!type.equals("addnewtable") && !type.equals("createtable")) {
            for (int i = 0; i < countTablesSelected; i++) {
                int row = idTablesSelected[i] - idInit;
                for (int j = 0; j < tables[row].length; j++)
                    tablesSelected[i][j] = tables[row][j];
                if (tables[row].length > longTable)
                    longTable = tables[row].length;
            }
        }
    }

    public int findCount(int index){
        int count = 0;
        while (count < 100 && tablesSelected[index][count] > 0){
            count++;
        }
        return count;
    }

    public void setTableChanged(int count, int index, int id){
        int[] table = new int[count];
        for (int i = 0; i < count; i++){
            table[i] = tableDynamic.table[index][i];
        }
        if (type.equals("addnewtable"))
            addNewTable(table);
        else
            tables[id - idInit] = table;
    }

    public void addNewTable(int[] table){
        int[][] newTablesReceived = new int[tables.length + 1][];
        for (int i = 0; i < tables.length; i++)
            newTablesReceived[i] = tables[i];
        newTablesReceived[tables.length] = table;
        tables = newTablesReceived;
    }

    public void onRestartConnection() {
        mBluetoothLeService.disconnect();
        SystemClock.sleep(1000);
        mBluetoothLeService.connect(mDeviceAddress);
    }*/
}
