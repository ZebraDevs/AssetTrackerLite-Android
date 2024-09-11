package com.symbol.assettrackerlite;

import android.util.Log;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by XDB687 on 8/31/2017.
 */

public class FTPUploader {
    private FTPSClient mFtpsClient;
    private static final String FTP_TLS_PROTOCOL = "TLS";
    private static final String DATA_CHANNEL_PROTECTION_LEVEL_PRIVATE = "P";

    public FTPUploader(String host, String user, String pwd) throws Exception{
        mFtpsClient = new FTPSClient(FTP_TLS_PROTOCOL , true);
        int reply;
        mFtpsClient.connect(host);
        mFtpsClient.execPROT(DATA_CHANNEL_PROTECTION_LEVEL_PRIVATE);
        reply = mFtpsClient.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            mFtpsClient.disconnect();
            throw new Exception("Exception in connecting to FTP Server");
        }
        mFtpsClient.login(user, pwd);
        mFtpsClient.setFileType(FTP.BINARY_FILE_TYPE);
        mFtpsClient.enterLocalPassiveMode();
    }


    public void uploadFile(String localFileFullName, String fileName, String hostDir) throws Exception {
        try(InputStream input = new FileInputStream(new File(localFileFullName))){
            Log.d("FTPUploader","uploadFile"+hostDir+fileName+input);
            this.mFtpsClient.makeDirectory(hostDir);
            this.mFtpsClient.storeFile(hostDir + fileName, input);
        }
    }

    public void disconnect(){
        if (this.mFtpsClient.isConnected()) {
            try {
                this.mFtpsClient.logout();
                this.mFtpsClient.disconnect();
            } catch (IOException f) {
                // do nothing as file is already saved to server
            }
        }
    }


}
