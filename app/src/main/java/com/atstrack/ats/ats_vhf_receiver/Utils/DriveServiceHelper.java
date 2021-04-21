package com.atstrack.ats.ats_vhf_receiver.Utils;

import android.util.Pair;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DriveServiceHelper {

    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private Drive mDriveService;

    public DriveServiceHelper(Drive mDriveService) {
        this.mDriveService = mDriveService;
    }

    /**
     * Sends a file from the local storage to the cloud.
     * @param path The directory path of the file to be saved to the cloud.
     * @param name The name of the file in that directory path.
     * @return Return the file id if it was saved successfully.
     */
    public Task<String> createFile(String path, String name) {
        return Tasks.call(mExecutor, () -> {
            File fileMetaData = new File();
            fileMetaData.setName(name.substring(0, name.length() - 4));

            java.io.File file = new java.io.File(path + "/" + name);

            FileContent fileContent = new FileContent("text/plain", file);

            File myFile = null;

            try {
                myFile = mDriveService.files().create(fileMetaData, fileContent).execute();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (myFile == null) {
                throw new IOException("Null result when requesting file creation.");
            }

            return myFile.getId();
        });
    }

    public Task<Pair<String, String>> readFile(String fileId) {
        return Tasks.call(mExecutor, () -> {
            // Retrieve the metadata as a File object.
            File metadata = mDriveService.files().get(fileId).execute();
            String name = metadata.getName();

            // Stream the file contents to a String.
            try (InputStream is = mDriveService.files().get(fileId).executeMediaAsInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                StringBuilder stringBuilder = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                String contents = stringBuilder.toString();

                return Pair.create(name, contents);
            }
        });
    }

    public Task<OutputStream> downloadFile(String fileId) {
        return Tasks.call(mExecutor, () -> {
            OutputStream outputStream = new ByteArrayOutputStream();
            mDriveService.files().get(fileId).executeMediaAndDownloadTo(outputStream);
            return outputStream;
        });
    }
}
