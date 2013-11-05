package com.JS.musictranscribe;


import android.app.Activity;
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
	private Button mRandomMatrixRREFButton;
	
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
		
		mRandomMatrixRREFButton = (Button) findViewById(R.id.random_matrix_button);
		mRandomMatrixRREFButton.setOnClickListener(new View.OnClickListener() {
			
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

				//RREF test:
				Matrix matrix = new Matrix(100,100);
				matrix.fillRandomly();
				
			 	long t = System.nanoTime();
			 	matrix.RREF();
				Log.i(TAG, "RREF took: " + Long.toString((System.nanoTime()-t)/1000));				
			}
		});
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

