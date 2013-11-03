package com.JS.musictranscribe;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;

public class Helper_Dropbox {
	
	private static final String TAG = "DropboxHelper";
	
	final static private String APP_KEY = "qlvxavtmid0xbvb";
	final static private String APP_SECRET = "18z614hq3iky92k"; 
    final static private AccessType ACCESS_TYPE = AccessType.APP_FOLDER; 
    final static public String ACCOUNT_PREFS_NAME = "MusicTranscribeDBPrefs";
    final static public String ACCESS_KEY_NAME = "MusicTranscribe_DB_ACCESS_KEY";
    final static public String ACCESS_SECRET_NAME = "MusicTranscribe_DB_ACCESS_SECRET";
    
    
    public static DropboxAPI<AndroidAuthSession> getDBSession(Context context) {
    	AndroidAuthSession session = buildSession(context);
    	DropboxAPI<AndroidAuthSession> mDBApi = new DropboxAPI<AndroidAuthSession>(session);
    	return mDBApi;
    }
    
    public static String putFile(String fileName, double[] values, DropboxAPI<AndroidAuthSession> mDBApi) {
    	Entry response;
    	try {
        	File resultFile = new File(fileName);
    		if (!resultFile.exists()) {
				resultFile.createNewFile();
			} else {
				Log.d(TAG, "file exists"); //print to get around Logging inabilities
				
			}
    		
    	    BufferedWriter writer = new BufferedWriter(new FileWriter(resultFile));
    	    
    	    for (int i = 0; i < values.length; i++) {
    	    	writer.write(String.valueOf(values[i]));
    	    	writer.write("\n");
    	    }
    	    
    	    writer.flush();
    	    writer.close();
    	        	    
    		FileInputStream fis = new FileInputStream(resultFile);
    		response = mDBApi.putFile(fileName, fis, resultFile.length(), null, null);
    		
        	
        	/*
        	FileOutputStream fop = new FileOutputStream(resultFile);
        	DataOutputStream dos = new DataOutputStream(fop);
        	
    		if (!resultFile.exists()) {
				resultFile.createNewFile();
			}

    		for (int i = 0; i < values.length; i++) {
    			dos.writeDouble(values[i]);
    		}
    		
    		dos.close();
    		fop.flush();
    		fop.close();
    		
    		FileInputStream fis = new FileInputStream(resultFile);
    		response = mDBApi.putFile(fileName, fis, resultFile.length(), null, null);
		*/
    	} catch (IOException e) {
    		System.out.println("Something failed before the actual Dropbox API call");
    		System.out.println(e.toString());
    		return "failed";
    	} catch (DropboxException e) {
    		System.out.println("Dropox.putFile() failed");
    		return "failed";
    	}
    	
    	return response.rev;
    }
    
    //for arrays of data arrays to be written to same file!
    public static String putFile(String fileName, double[][] values, DropboxAPI<AndroidAuthSession> mDBApi) {
    	Entry response;
    	try {
        	File resultFile = new File(fileName);
    		if (!resultFile.exists()) {
				resultFile.createNewFile();
			} else {
				Log.d(TAG, "file exists"); //print to get around Logging inabilities
				
			}
    		
    	    BufferedWriter writer = new BufferedWriter(new FileWriter(resultFile));
    	    
    	    for (int i = 0; i < values.length; i++) {
    	    	for (int j = 0; j < values[i].length; j++) {
	    	    	writer.write(String.valueOf(values[i][j]));
	    	    	writer.write("\n");
    	    	}
    	    	writer.write("\n");
    	    }
    	    
    	    writer.flush();
    	    writer.close();
    	        	    
    		FileInputStream fis = new FileInputStream(resultFile);
    		response = mDBApi.putFile(fileName, fis, resultFile.length(), null, null);
    	
    	} catch (IOException e) {
    		System.out.println("Something failed before the actual Dropbox API call");
    		System.out.println(e.toString());
    		return "failed";
    	} catch (DropboxException e) {
    		System.out.println("Dropox.putFile() failed");
    		return "failed";
    	}
    	
    	return response.rev;
    }
    
    
    public static void storeKeys(String key, String secret, Context context) {
        // Save the access key for later
        SharedPreferences prefs = context.getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        Editor edit = prefs.edit();
        edit.putString(ACCESS_KEY_NAME, key);
        edit.putString(ACCESS_SECRET_NAME, secret);
        edit.commit();
    }
    
    public static void clearKeys(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        Editor edit = prefs.edit();
        edit.clear();
        edit.commit();
    }
    
	private static String[] getKeys(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        String key = prefs.getString(ACCESS_KEY_NAME, null);
        String secret = prefs.getString(ACCESS_SECRET_NAME, null);
        if (key != null && secret != null) {
        	String[] ret = new String[2];
        	ret[0] = key;
        	ret[1] = secret;
        	return ret;
        } else {
        	return null;
        }
	}
    
    private static AndroidAuthSession buildSession(Context context) {
        AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);
        AndroidAuthSession session;

        String[] stored = getKeys(context);
        if (stored != null) {
            AccessTokenPair accessToken = new AccessTokenPair(stored[0], stored[1]);
            session = new AndroidAuthSession(appKeyPair, ACCESS_TYPE, accessToken);
        } else {
            session = new AndroidAuthSession(appKeyPair, ACCESS_TYPE);
        }
        
        return session;
    }
    

	
}
