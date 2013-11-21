package com.JS.musictranscribe;


import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.TokenPair;


public class MainActivity extends Activity {

	private static String TAG = "MainActivity";
	
	private Button mGotoRecordingButton;
	private Button mGotoDatacollectButton;
	private Button mLibraryButton;
	private Button mDropboxLoginButton;
	private Button mTestingButton;
	
	//Dropbox
	private DropboxAPI<AndroidAuthSession> mDBApi; 

    private boolean mIsLoggedIn = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        
		setContentView(R.layout.activity_main);

        
		mGotoRecordingButton = (Button) findViewById(R.id.goto_recording_button);
		mGotoRecordingButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, RecordActivity.class);
				intent.putExtra("DBLoggedIn", isLoggedIn());
				startActivity(intent);
			}
		});
		
		mGotoDatacollectButton = (Button)findViewById(R.id.goto_datacollect_activity);
		mGotoDatacollectButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, DatacollectActivity.class);
				intent.putExtra("DBLoggedIn", isLoggedIn());
				startActivity(intent);
				
			}
		});
		
		
		mLibraryButton = (Button) findViewById(R.id.library_button);
		mLibraryButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				
			}
		});
		
		mDropboxLoginButton = (Button) findViewById(R.id.dropbox_login_button);
		mDropboxLoginButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				
				if (!isLoggedIn()) {
					
			        mDBApi = Helper_Dropbox.getDBSession(MainActivity.this);
			        setLoggedIn(mDBApi.getSession().isLinked());
			        if (!isLoggedIn()) {
			        	mDBApi.getSession().startAuthentication(MainActivity.this);
			        }
					//as we are now logged in 
			        mDropboxLoginButton.setText("Log out of Dropbox");
				}
				else {
					mDBApi.getSession().unlink();
					setLoggedIn(false);
					Helper_Dropbox.clearKeys(MainActivity.this);
					//as we are now logged out
					mDropboxLoginButton.setText("Log In to Dropbox");
				}
			}
		});


		
		mTestingButton = (Button) findViewById(R.id.random_matrix_button);
		mTestingButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				
				
				/* Matrix multiplication test:
				
				Log.i(TAG,"Making a new matrix");
				Matrix matrix = new Matrix(3,3);
				matrix.fillRandomly();
				
				matrix.printMatrix();
				System.out.println();
				
				Log.i(TAG,"second new matrix");
				Matrix matrix2 = new Matrix(1,3);
				matrix2.fillRandomly();
				
				matrix2.printMatrix();
				System.out.println();
				
				matrix2 = matrix.multOnRightOf(matrix2);
				
				matrix2.printMatrix();
				
				*/

				/*
				//RREF test:
				Matrix matrix = new Matrix(100,100);
				matrix.fillRandomly();
				
			 	long t = System.nanoTime();
			 	ArrayList<Double> record = matrix.RREF();
				Log.i(TAG, "RREF took: " + Long.toString((System.nanoTime()-t)/1000));	
				Log.i(TAG, "Record length is: " + record.size());
				
				
				Log.i(TAG, "Generating matrix 1");
				Matrix matrix = new Matrix(88, 2024);
				Log.i(TAG,"Filling matrix 1");
				matrix.fillRandomly();
				
				Log.i(TAG,"Generating matrix 2");
				Matrix solMatrix = new Matrix(2024, 1);
				Log.i(TAG,"Filling matrix 2");
				solMatrix.fillRandomly();
				Log.i(TAG,"RREF'ing");
				ArrayList<Double> record = matrix.RREF();

				
				long t = System.nanoTime();
				
				System.out.println("Multiplying");
				solMatrix = matrix.multOnLeftOf(solMatrix);
				
				long t2 = System.nanoTime();
				System.out.println("modifying");
				
				solMatrix.modifyByRecord(record);
				
				Log.i(TAG,"Length of record: " + record.size());
				Log.i(TAG,"Multiplication: " + ((t2-t)/1000) + "us");
				Log.i(TAG,"modification by record: " + (System.nanoTime() - t2)/1000 + "us");
				*/
				
				
				HashMap<Integer, Double[]> noteSpectraMap = Helper.getNoteSpectraFromFile(getApplicationContext(), "default2");
				Integer[] noteNums = noteSpectraMap.keySet().toArray(new Integer[1]);
				
				Matrix dataMatrix = new Matrix(noteSpectraMap.get(noteNums[0]).length, noteNums.length); //map must have values of same length

				Arrays.sort(noteNums);
				
				for (int i = 0; i < noteNums.length; i++) {
					dataMatrix.writeCol(i, noteSpectraMap.get(i));
				}
				
				Matrix dataTranspose = dataMatrix.getTranspose();
				Matrix sqrMatrix = dataTranspose.multOnLeftOf(dataMatrix);
				
				sqrMatrix.printMatrix();
				
								
				
				
			}
		});
		
		ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		Log.i(TAG,"HI");
		Log.i(TAG,"Maximum around of memory allowed for this device: " + activityManager.getMemoryClass());
	}
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	

	//-------------Dropbox stuff  for testing and debugging---------

	
    @Override
    protected void onResume() {
        super.onResume();
        if (mDBApi != null) { //need this because onResume is called right after onCreate,
        						//and if we haven't pressed the button, mDPApi == null!!!
        	AndroidAuthSession session = mDBApi.getSession();
	        
	        
	        // The next part must be inserted in the onResume() method of the
	        // activity from which session.startAuthentication() was called, so
	        // that Dropbox authentication completes properly.
	        if (session.authenticationSuccessful()) {
	            try {
	                // Mandatory call to complete the auth
	                session.finishAuthentication();
	
	                // Store it locally in our app for later use
	                TokenPair tokens = session.getAccessTokenPair();
	                Helper_Dropbox.storeKeys(tokens.key, tokens.secret, this);
	                setLoggedIn(true);
	            } catch (IllegalStateException e) {
	                Log.i(TAG, "Error authenticating", e);
	            }
	        }
        }
        
    }
	
    
    private void setLoggedIn(boolean loggedIn) {
    	mIsLoggedIn = loggedIn;
    }
    
    private boolean isLoggedIn() {
    	return mIsLoggedIn;
    }
}

