package com.atstrack.ats.ats_vhf_receiver;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class FreqTablesActivity extends AppCompatActivity {

    @BindView(R.id.btt_LoadTableFromFile)
    Button bttLoadTablesFromFile;
    @BindView(R.id.btt_LoadTablesFromReceiver)
    Button bttLoadTablesFromReceivers;
    @BindView(R.id.btt_EnterDataTablesManually)
    Button bttEnterDataTablesManually;

    final private String TAG = FreqTablesActivity.class.getSimpleName();
    Intent myFileIntent;

    private int[][] tablesReceived =  new int[/*12*/][/*100*/]{
            {458391, 984915, 493012, 180989, 766822, 973743, 398925, 298750, 878432, 634878, 643980},
            {528375, 758925, 893752, 839870, 653921, 958364, 753015, 900283, 658385, 583923, 875439, 620298, 654376, 218734, 578932, 702834, 987200, 620801},
            {752909, 959825, 760283, 750929, 571749, 652830},
            {472390, 757092, 203948, 819499, 167489, 178320, 652834, 374981},
            {723874, 109928, 728700},
            {194894, 343809, 209487, 473984, 742892},
            {589035, 985820, 140194, 100023, 741790, 420390, 641792, 742010, 641890, 400024, 753900, 828361, 173994, 284900},
            {900001, 379004, 649030, 283980, 204792, 347700, 502882, 842048, 100475, 483290, 642848, 742874},
            {742894, 995847, 164803, 857209, 203949, 743987, 423873}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_freq_tables);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.btt_LoadTableFromFile)
    public void onClickLoadTableFromFile(View v){
        myFileIntent = new Intent(Intent.ACTION_GET_CONTENT);
        myFileIntent.setType("*/*");
        startActivityForResult(myFileIntent, 10);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case 10:
                if (resultCode==RESULT_OK) {
                    Log.i(TAG, data.getData().getPath());
                    Intent intent2 = new Intent(this, TableOverviewActivity.class);
                    intent2.putExtra("type", "file");
                    intent2.putExtra("count", tablesReceived.length);
                    for (int i = 0; i < tablesReceived.length; i++)
                        intent2.putExtra("table" + i, tablesReceived[i]);
                    startActivityForResult(intent2, 0);
                }

        }
    }

    @OnClick(R.id.btt_LoadTablesFromReceiver)
    public void onClickLoadTablesFromReceiver(View v){
        Intent intent2 = new Intent(this, TableOverviewActivity.class);
        intent2.putExtra("type", "receiver");
        intent2.putExtra("count", tablesReceived.length);
        for (int i = 0; i < tablesReceived.length; i++)
            intent2.putExtra("table" + i, tablesReceived[i]);
        startActivityForResult(intent2, 0);
    }

    @OnClick(R.id.btt_EnterDataTablesManually)
    public void onClickEnterDataTablesManually(View v){
        Intent intent2 = new Intent(this, TableOverviewActivity.class);
        intent2.putExtra("type", "manually");
        startActivityForResult(intent2, 0);
    }
}
