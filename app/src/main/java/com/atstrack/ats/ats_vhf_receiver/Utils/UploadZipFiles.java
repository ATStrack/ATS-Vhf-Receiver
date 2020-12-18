package com.atstrack.ats.ats_vhf_receiver.Utils;

import android.os.AsyncTask;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import timber.log.Timber;

public class UploadZipFiles extends AsyncTask<Object, Integer, Object> {
    private FTPSClient ftpClient;
    private File[] zipFiles;
    long totalFileSize;
    long totalTransferredBytes;
    int count;
    //private final NumberFormat nf = NumberFormat.getInstance();

    public UploadZipFiles(File[] zipFiles) {
        this.zipFiles = zipFiles;
        this.ftpClient = null;
        this.count = 0;
        this.totalFileSize = 0;
        this.totalTransferredBytes = 0;
        //nf.setMinimumFractionDigits(2);
        //nf.setMaximumFractionDigits(2);
    }

    @Override
    protected void onPreExecute() {
        // TODO Auto-generated method stub
        super.onPreExecute();
        for (File file : zipFiles)
            totalFileSize = totalFileSize + file.length();
    }

    @Override
    protected Object doInBackground(Object... arg0) {
        ftpClient = new FTPSClient("TLS",false);
        try {
            TrustManager[] trustManager = new TrustManager[] { new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                        return null;
                }

                @Override
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                @Override
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                } };
            ftpClient.setTrustManager(trustManager[0]);
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(null, null);
            KeyManager km = kmf.getKeyManagers()[0];
            ftpClient.setKeyManager(km);
            ftpClient.setBufferSize(1024 * 1024);
            ftpClient.setConnectTimeout(100000);
            ftpClient.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
            ftpClient.connect("atswfcvm.cloudapp.net",5003);
            if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                ftpClient.disconnect();
                throw new Exception("Exception in connecting to FTPS Server " + ftpClient.getReplyCode());
            }
            ftpClient.setSoTimeout(100000);
            //login to server
            if (ftpClient.login("atsftp","2019*t3*")){
                ftpClient.execPBSZ(0);
                ftpClient.execPROT("P");
                ftpClient.changeWorkingDirectory("/");
                // 250 = directory successfully changed
                if (ftpClient.getReplyString().contains("250")) {
                    ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
                    ftpClient.enterLocalPassiveMode();
                    BufferedInputStream buffIn;
                    for (File aZip : zipFiles) {
                        buffIn = new BufferedInputStream(new FileInputStream(aZip));
                        if (ftpClient.storeFile(aZip.getName(), buffIn)) {
                            count++;
                            aZip.delete();
                        }
                        try {
                            buffIn.close();
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
            else
                throw new Exception("Exception in login to FTPS Server " + ftpClient.getReplyCode());
        } catch (Exception e) {
            Timber.wtf(e);
            return e;
        } finally {
            try {
                ftpClient.logout();
            } catch (Exception ignored) {
            }
            try {
                ftpClient.disconnect();
            } catch (Exception ignored) {
            }
        }
        // TODO add files to ftp
        if (count == zipFiles.length) return "Success: "+ count + " files uploaded.";
        else return "Something happened: " + count + " of " + zipFiles.length + " files uploaded.";
    }

    @Override
    protected void onPostExecute(Object result) {
        super.onPostExecute(result);
        /*if (result instanceof Exception)
            ;
        else
            ;*/
        System.out.println(result);
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
/*
        UploadActivity.progressBar
                .setProgress((int) (((float) values[0] / totalFileSize) * 100));
        UploadActivity.uploadingSizeTextView.setText(nf
                .format(((float) values[0] / (1024 * 1024)))
                + " mb of "
                + nf.format(((float) totalFileSize / (1024 * 1024)))
                + " mb uploaded");*/
    }
}