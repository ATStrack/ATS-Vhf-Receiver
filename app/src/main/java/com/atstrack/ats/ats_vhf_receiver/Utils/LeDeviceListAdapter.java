package com.atstrack.ats.ats_vhf_receiver.Utils;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.atstrack.ats.ats_vhf_receiver.MainMenuActivity;
import com.atstrack.ats.ats_vhf_receiver.R;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class LeDeviceListAdapter extends RecyclerView.Adapter<LeDeviceListAdapter.MyViewHolder> {

    private ArrayList<BluetoothDevice> mLeDevices;
    private ArrayList<byte[]> mScanRecords;
    private Context context;

    public LeDeviceListAdapter(Context context) {
        mLeDevices = new ArrayList<>();
        mScanRecords = new ArrayList<>();
        this.context = context;
    }

    /**
     * Adds only ATS Vhf Receiver devices to the list.
     *
     * @param device Identifies the remote device.
     * @param scanRecord The content of the advertisement record offered by the remote device.
     */
    public void addDevice(BluetoothDevice device, byte[] scanRecord) {
        if(!mLeDevices.contains(device)) {
            final String deviceName = device.getName();
            if(deviceName != null)
                if(deviceName.contains("ATS Vhf Rec #")) { // add new for filter only ATS Vhf Rec device
                    mLeDevices.add(device);
                    mScanRecords.add(scanRecord);
                }
        }
    }

    /**
     * Gets the percentage of the device's battery.
     *
     * @param scanRecord The content of the advertisement record offered by the remote device.
     * @return Return the battery percentage.
     */
    public String getPercentBattery(byte[] scanRecord) {
        int firstElement = Integer.parseInt(Converters.getDecimalValue(scanRecord[0]));
        return Converters.getDecimalValue(scanRecord[firstElement + 5]);
    }

    /**
     * Remove all remote devices from the list.
     */
    public void clear() {
        mLeDevices.clear();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.receiver_status, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        // Set all remote device information
        BluetoothDevice device = mLeDevices.get(position);
        String percent = getPercentBattery(mScanRecords.get(position));
        final String deviceName = device.getName();
        if (deviceName != null && deviceName.length() > 0)
            holder.deviceName.setText(deviceName);
        else
            holder.deviceName.setText(R.string.unknown_device);
        holder.deviceAddress.setText(device.getAddress());
        holder.percentBattery.setText(percent + "%");
        holder.device = mLeDevices.get(position);
        TableRow.LayoutParams params = new TableRow.LayoutParams();
        params.setMargins(0, 0, 0, 32);
        holder.receiver_status.setLayoutParams(params);
    }

    @Override
    public int getItemCount() {
        return mLeDevices.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        LinearLayout receiver_status;
        LinearLayout device_linearLayout;
        TextView deviceName;
        TextView deviceAddress;
        TextView percentBattery;
        BluetoothDevice device;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            receiver_status = itemView.findViewById(R.id.receiver_status);
            device_linearLayout = itemView.findViewById(R.id.device_linearLayout);
            deviceAddress = itemView.findViewById(R.id.device_address);
            deviceName = itemView.findViewById(R.id.device_name);
            percentBattery = itemView.findViewById(R.id.percent_battery);

            device_linearLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (device == null) return;
                    if (device.getName().contains("#000000")) { // Error, factory setup required
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle("Error");
                        builder.setMessage("Factory Setup Required.");
                        builder.setPositiveButton("OK", null);
                        builder.show();
                    } else { // Connects to device
                        Intent intent = new Intent(context, MainMenuActivity.class);
                        intent.putExtra(MainMenuActivity.EXTRAS_DEVICE_NAME, device.getName());
                        intent.putExtra(MainMenuActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
                        intent.putExtra(MainMenuActivity.EXTRAS_BATTERY, percentBattery.getText().toString());
                        intent.putExtra("menu", false);
                        context.startActivity(intent);
                    }
                }
            });
        }
    }
}
