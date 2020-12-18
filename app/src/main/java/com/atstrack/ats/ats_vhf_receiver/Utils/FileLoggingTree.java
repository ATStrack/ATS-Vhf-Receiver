package com.atstrack.ats.ats_vhf_receiver.Utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import timber.log.Timber;

public class FileLoggingTree extends Timber.DebugTree{
    private static final String TAG = FileLoggingTree.class.getSimpleName();
    private static File direct = new File(Environment.getExternalStorageDirectory().toString() + File.separator + "atstrack","logs");
    private FileOutputStream fos;
    private Context context;
    private File file;

    public FileLoggingTree(Context context) {
        this.context = context;
    }

    @Override
    protected void log(int priority, String tag, @NonNull String message, Throwable t) {
        boolean outcome;
        try {
            if (!direct.exists()) {
                Log.d("DIRECTORY CHECK", "Folder 'atstrack/logs' doesn't exist, creating...");
                outcome = direct.mkdirs();
                if(!outcome) throw new Exception("Folder 'atstrack/logs' can't be created.");
                direct.setReadable(true);
                direct.setWritable(true);
                Log.d("DIRECTORY CHECK", "Folder created: " + outcome);
            }

            String fileNameTimeStamp = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            String logTimeStamp = new SimpleDateFormat("E MMM dd yyyy 'at' hh:mm:ss:SSS aaa", Locale.getDefault()).format(new Date());
            String fileName = fileNameTimeStamp + ".html";

            file = new File(Environment.getExternalStorageDirectory().toString() + File.separator + "atstrack" + File.separator + "logs", fileName);
            file.createNewFile();

            if (file.exists()) {
                fos = new FileOutputStream(file, true);
                fos.write(("<p style=\"background:lightgray;\"><strong style=\"background:lightblue;\">&nbsp&nbsp" + logTimeStamp + " :&nbsp&nbsp</strong>&nbsp&nbsp" + tag + ": "+ message +"</p>").getBytes());
                fos.close();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error while logging into file: " + e.toString());
            Toast.makeText(context,"Error while logging into file: " + e.getMessage(),Toast.LENGTH_LONG);
        }

    }
}
