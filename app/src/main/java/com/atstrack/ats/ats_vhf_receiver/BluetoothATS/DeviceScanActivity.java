package com.atstrack.ats.ats_vhf_receiver.BluetoothATS;


import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.atstrack.ats.ats_vhf_receiver.R;

import java.util.ArrayList;

public class DeviceScanActivity extends ListActivity {
    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    //private  String parameter1;
    private boolean theme;
    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(() -> {
                        mLeDeviceListAdapter.addDevice(device);
                        mLeDeviceListAdapter.notifyDataSetChanged();
                    });
                }
            };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setSubtitle("Please select a product to continue");
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeAsUpIndicator(R.drawable.ic_ab_back);
        getActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorPrimary)));

        theme = getIntent().getExtras().getBoolean("theme");
        if (theme)
            super.getListView().setBackgroundColor(getResources().getColor(R.color.dark));
        else
            super.getListView().setBackgroundColor(getResources().getColor(R.color.light));

        /*parameter1 = getIntent().getExtras().getString("parameter1");
        if(parameter1.equals("setup")) getActionBar().setTitle("Set Up: Device Scan");
        {
            getActionBar().setTitle("Program: Device Scan");
            intervalProgram= Integer.valueOf(getIntent().getExtras().getString("interval"));
            timeProgram= Integer.valueOf(getIntent().getExtras().getString("time"));
        }
        else if (parameter1.equals("getdata")) getActionBar().setTitle("Get Data: Device Scan");
        else if (parameter1.equals("freqtables")) getActionBar().setTitle("Freq Tables: Device Scan");
        else if (parameter1.equals("createtable")) getActionBar().setTitle("Create Table: Device Scan");*/

        mHandler = new Handler();
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: //hago un case por si en un futuro agrego mas opciones
                finish();
                return true;
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                Log.i("DeviceScanActivity", "DeviceScanActivity-onResume");
            }
        }
        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        //setListAdapter(mLeDeviceListAdapter);
        scanLeDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        if (device == null) return;
        /*if (parameter1.equals("getdata")){
            final Intent intent = new Intent(this, GetDataActivity.class);
            intent.putExtra(GetDataActivity.EXTRAS_DEVICE_NAME, device.getName());
            intent.putExtra(GetDataActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
            intent.putExtra("parameter1", parameter1);
            startActivity(intent);
        } else if (parameter1.equals("setup")){
            final Intent intent = new Intent(this, SetUpActivity.class);
            intent.putExtra(SetUpActivity.EXTRAS_DEVICE_NAME, device.getName());
            intent.putExtra(SetUpActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
            intent.putExtra("parameter1", parameter1);
            startActivity(intent);
        } else if (parameter1.equals("freqtables")){
            final Intent intent = new Intent(this, FreqTablesActivity.class);
            //intent.putExtra(FreqTablesActivity.EXTRAS_DEVICE_NAME, device.getName());//Change FreqTablesActivity
            //intent.putExtra(FreqTablesActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
            intent.putExtra("parameter1", parameter1);
            startActivity(intent);
        } else if (parameter1.equals("createtable")){//Example
            final Intent intent = new Intent(this, EditTablesActivity.class);
            intent.putExtra(EditTablesActivity.EXTRAS_DEVICE_NAME, device.getName());
            intent.putExtra(EditTablesActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
            intent.putExtra("type", parameter1);
            intent.putExtra("count", 1);
            startActivity(intent);
        }*/
        Log.i("DeviceScanActivity", "mS: "+ mScanning);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(() -> {
                mScanning = false;
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                setListAdapter(mLeDeviceListAdapter);
                invalidateOptionsMenu();
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            Log.i("DeviceScanActivity", "DeviceScanActivity-scanLeDevice");
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        View line;
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = DeviceScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                final String deviceName = device.getName();
                if(deviceName != null)//add new for filter only Tinkler device
                    if(deviceName.equals("Thermometer RTOS"))//add new for filter only Tinkler device: Thermometer RTOS, ATS Vhf Receiver
                        mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = view.findViewById(R.id.device_address);
                viewHolder.deviceName = view.findViewById(R.id.device_name);
                if (theme){
                    viewHolder.deviceAddress.setTextColor(getResources().getColor(R.color.light));
                    viewHolder.deviceAddress.setBackgroundColor(getResources().getColor(R.color.dark));
                    viewHolder.deviceName.setTextColor(getResources().getColor(R.color.light));
                    viewHolder.deviceName.setBackgroundColor(getResources().getColor(R.color.dark));
                    viewHolder.line.setBackgroundColor(getResources().getColor(R.color.light));
                }/*else{
                    viewHolder.deviceAddress.setTextColor(getResources().getColor(R.color.colortext));
                    viewHolder.deviceAddress.setBackgroundColor(getResources().getColor(R.color.light));
                }*/
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }
}