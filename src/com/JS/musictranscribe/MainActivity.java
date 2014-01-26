package com.JS.musictranscribe;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

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
	
	//Button to start RecordActivity
	private Button mGotoRecordingButton;
	//Button to start DatacollectActivity
	private Button mGotoDatacollectButton;
	//Button to go to LibraryActivity (not written yet)
	private Button mLibraryButton;
	//Button to log in or out of dropbox
	private Button mDropboxLoginButton;
	//Button for quick experimental stuff
	private Button mTestingButton;
	//Button to go to GraphicsActivity (testing for graphics)
	private Button mGotoGraphicsButton;
	
	//boolean to prevent re-initialization of certain values
	private boolean mIsFirstOnCreate = true;
	
	//Dropbox 
	private DropboxAPI<AndroidAuthSession> mDBApi; 
    private boolean mIsLoggedIn = false; //boolean to keep track if Dropbox is logged in
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        
		setContentView(R.layout.activity_main);

        
		mGotoRecordingButton = (Button) findViewById(R.id.goto_recording_button);
		mGotoRecordingButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, RecordActivity.class);
				//Pass along if Dropbox is logged in or not
				intent.putExtra("DBLoggedIn", isLoggedIn());
				startActivity(intent);
			}
		});
		
		mGotoDatacollectButton = (Button)findViewById(R.id.goto_datacollect_activity);
		mGotoDatacollectButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, DatacollectActivity.class);
				//Pass along if Dropbox is logged in or not
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
		
		mGotoGraphicsButton = (Button) findViewById(R.id.goto_graphics);
		mGotoGraphicsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, GraphicsActivity.class);
				startActivity(intent);
			}
		});
		
		mDropboxLoginButton = (Button) findViewById(R.id.dropbox_login_button);
		mDropboxLoginButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				
				if (!isLoggedIn()) {
					//Get a dropbox session
			        mDBApi = Helper_Dropbox.getDBSession(MainActivity.this);
			        //Store if logged into Dropbox not
			        setLoggedIn(mDBApi.getSession().isLinked()); 
			        if (!isLoggedIn()) {
			        	//if not logged in, start authentication process
			        	mDBApi.getSession().startAuthentication(MainActivity.this);
			        }
					//Change text to indicated that we are currently logged in
			        mDropboxLoginButton.setText("Log out of Dropbox");
				}
				else {
					//log out of dropbox
					mDBApi.getSession().unlink();
					//save the logged in state in the boolean
					setLoggedIn(false);
					//Have to delete the keys from the sharedPreferences (Handled in Helper)
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
				Matrix matrix = new Matrix(5,2024);
				matrix.fillRandomly();
				
			 	long t = System.nanoTime();
			 	ArrayList<Double> record = matrix.RREF();
				Log.i(TAG, "RREF took: " + Long.toString((System.nanoTime()-t)/1000));	
				Log.i(TAG, "Record length is: " + record.size());
				*/
				
				
				//get the note - frequency spectrum HashMap saved in the file
				HashMap<Integer, Double[]> noteSpectraMap = Helper.getNoteSpectraFromFile(getApplicationContext(), Helper.getStringPref(Helper.ACTIVE_MAPDATA_FILE_KEY, getApplicationContext()));
				//get all the note ID numbers we have
				Integer[] noteNums = noteSpectraMap.keySet().toArray(new Integer[1]);
				//make a new matrix for the frequency spectra we just read
				Matrix dataMatrix = new Matrix(noteSpectraMap.get(noteNums[0]).length, noteNums.length); //map's Double[] arrays must have same length!
				//put the note numbers in order
				Arrays.sort(noteNums);
				
				//go through the notes and put the frequency spectra in the matrix
				for (int i = 0; i < noteNums.length; i++) {
					dataMatrix.writeCol(i, noteSpectraMap.get(noteNums[i]));
				}
				//do transpose
				Matrix dataTranspose = dataMatrix.getTranspose();
				//do math according to the formulas
				Matrix sqrMatrix = dataTranspose.multOnLeftOf(dataMatrix);
				//RREF and save the process used to do this (huge optimization)
				ArrayList<Double> record = sqrMatrix.RREF();
				
				//This is just the audio of note 42 that I manually plugged in
				double[] note42Audio = {-284.0, -284.0, -269.0, -266.0, -290.0, -260.0, -254.0, -230.0, -230.0, -197.0, -184.0, -145.0, -106.0, -81.0, -40.0, -12.0, 15.0, 52.0, 83.0, 74.0, 91.0, 99.0, 92.0, 79.0, 75.0, 69.0, 44.0, 34.0, 11.0, -1.0, -38.0, -63.0, -65.0, -59.0, -81.0, -98.0, -124.0, -132.0, -152.0, -179.0, -172.0, -180.0, -202.0, -203.0, -241.0, -254.0, -289.0, -305.0, -322.0, -334.0, -353.0, -375.0, -371.0, -370.0, -381.0, -383.0, -360.0, -364.0, -344.0, -344.0, -331.0, -309.0, -291.0, -258.0, -215.0, -193.0, -192.0, -153.0, -99.0, -82.0, -53.0, -32.0, 9.0, 27.0, 45.0, 48.0, 66.0, 66.0, 58.0, 43.0, 32.0, 24.0, -10.0, -18.0, -24.0, -39.0, -42.0, -38.0, -46.0, -37.0, -16.0, -14.0, 5.0, 19.0, 21.0, 60.0, 83.0, 101.0, 106.0, 155.0, 164.0, 179.0, 198.0, 230.0, 247.0, 265.0, 281.0, 307.0, 318.0, 322.0, 346.0, 372.0, 395.0, 409.0, 412.0, 437.0, 445.0, 466.0, 475.0, 471.0, 458.0, 485.0, 470.0, 475.0, 478.0, 474.0, 468.0, 483.0, 469.0, 444.0, 433.0, 423.0, 378.0, 378.0, 340.0, 272.0, 212.0, 242.0, 183.0, 149.0, 106.0, 71.0, 53.0, 23.0, -4.0, -41.0, -57.0, -70.0, -88.0, -109.0, -118.0, -124.0, -126.0, -112.0, -100.0, -114.0, -97.0, -69.0, -55.0, -22.0, 1.0, 27.0, 65.0, 90.0, 134.0, 165.0, 192.0, 229.0, 241.0, 253.0, 272.0, 268.0, 273.0, 272.0, 276.0, 248.0, 233.0, 216.0, 206.0, 177.0, 166.0, 144.0, 132.0, 130.0, 112.0, 97.0, 86.0, 65.0, 65.0, 53.0, 18.0, 17.0, -2.0, -27.0, -49.0, -69.0, -91.0, -118.0, -130.0, -157.0, -148.0, -166.0, -191.0, -201.0, -216.0, -213.0, -215.0, -213.0, -207.0, -210.0, -215.0, -191.0, -202.0, -179.0, -137.0, -127.0, -97.0, -53.0, -15.0, 22.0, 61.0, 84.0, 77.0, 109.0, 94.0, 115.0, 127.0, 124.0, 122.0, 114.0, 96.0, 90.0, 74.0, 42.0, 44.0, 35.0, 38.0, 34.0, 40.0, 36.0, 39.0, 46.0, 63.0, 90.0, 97.0, 120.0, 120.0, 161.0, 161.0, 176.0, 201.0, 212.0, 236.0, 247.0, 279.0, 282.0, 312.0, 316.0, 334.0, 360.0, 391.0, 411.0, 418.0, 450.0, 458.0, 467.0, 471.0, 472.0, 475.0, 484.0, 496.0, 499.0, 508.0, 497.0, 497.0, 498.0, 483.0, 474.0, 470.0, 460.0, 458.0, 428.0, 398.0, 394.0, 354.0, 293.0, 256.0, 239.0, 105.0, 114.0, 18.0, 53.0, 13.0, -83.0, -126.0, -88.0, -130.0, -113.0, -129.0, -150.0, -165.0, -198.0, -180.0, -204.0, -179.0, -182.0, -180.0, -159.0, -141.0, -115.0, -95.0, -68.0, -39.0, -18.0, 36.0, 55.0, 85.0, 111.0, 138.0, 126.0, 129.0, 117.0, 114.0, 119.0, 125.0, 152.0, 90.0, 146.0, 90.0, 41.0, 42.0, -30.0, -158.0, -334.0, -531.0, -525.0, -365.0, -482.0, -354.0, 130.0, 237.0, -89.0, -153.0, -75.0, -180.0, -59.0, 331.0, 144.0, -292.0, -13.0, 245.0, -137.0, -356.0, -287.0, -345.0, -164.0, 315.0, 407.0, 227.0, 405.0, 108.0, -645.0, -875.0, -533.0, -176.0, -56.0, -48.0, -196.0, -160.0, -62.0, -570.0, -1321.0, -1330.0, -1084.0, -1179.0, -914.0, -356.0, 40.0, 221.0, 111.0, -219.0, -226.0, 78.0, 75.0, 164.0, 404.0, 467.0, 480.0, 624.0, 554.0, 314.0, 149.0, -31.0, -147.0, -48.0, 46.0, 66.0, 118.0, 65.0, -148.0, -392.0, -587.0, -668.0, -558.0, -345.0, -198.0, -3.0, 199.0, 298.0, 361.0, 360.0, 306.0, 236.0, 212.0, 249.0, 402.0, 563.0, 597.0, 562.0, 623.0, 605.0, 479.0, 382.0, 369.0, 398.0, 424.0, 437.0, 360.0, 356.0, 438.0, 414.0, 378.0, 356.0, 297.0, 212.0, 181.0, 154.0, 90.0, 22.0, 17.0, 19.0, 6.0, -2.0, -27.0, -36.0, -59.0, -91.0, -126.0, -132.0, -126.0, -127.0, -133.0, -158.0, -194.0, -193.0, -218.0, -235.0, -217.0, -186.0, -193.0, -144.0, -146.0, -166.0, -168.0, -164.0, -171.0, -148.0, -111.0, -82.0, -27.0, 42.0, 52.0, 55.0, 47.0, 14.0, 44.0, 60.0, 70.0, 89.0, 110.0, 143.0, 135.0, 106.0, 100.0, 97.0, 73.0, 62.0, 64.0, 63.0, 66.0, 59.0, 40.0, 15.0, -34.0, -96.0, -127.0, -175.0, -201.0, -219.0, -236.0, -251.0, -266.0, -283.0, -304.0, -323.0, -336.0, -362.0, -352.0, -359.0, -362.0, -342.0, -339.0, -316.0, -294.0, -272.0, -246.0, -228.0, -214.0, -159.0, -132.0, -101.0, -62.0, -48.0, -14.0, 7.0, 18.0, 3.0, 23.0, -3.0, -21.0, 2.0, -19.0, -11.0, -8.0, -12.0, -33.0, -25.0, -33.0, -29.0, -27.0, -37.0, -35.0, -15.0, -15.0, -19.0, -22.0, -25.0, -23.0, -32.0, -16.0, 6.0, 9.0, 48.0, 60.0, 92.0, 94.0, 114.0, 139.0, 164.0, 200.0, 225.0, 246.0, 275.0, 295.0, 297.0, 306.0, 307.0, 341.0, 356.0, 357.0, 371.0, 389.0, 408.0, 428.0, 439.0, 429.0, 428.0, 432.0, 415.0, 418.0, 411.0, 383.0, 353.0, 319.0, 277.0, 262.0, 202.0, 168.0, 138.0, 90.0, 63.0, 50.0, 5.0, -34.0, -66.0, -122.0, -144.0, -167.0, -200.0, -231.0, -248.0, -261.0, -261.0, -270.0, -275.0, -269.0, -278.0, -264.0, -240.0, -196.0, -180.0, -150.0, -100.0, -78.0, -51.0, -28.0, 1.0, 18.0, 56.0, 42.0, 58.0, 71.0, 77.0, 77.0, 69.0, 64.0, 42.0, 39.0, 30.0, 26.0, 2.0, -3.0, 4.0, -21.0, -17.0, -37.0, -71.0, -73.0, -97.0, -130.0, -135.0, -162.0, -182.0, -212.0, -245.0, -269.0, -277.0, -298.0, -316.0, -337.0, -368.0, -404.0, -398.0, -433.0, -456.0, -474.0, -483.0, -501.0, -496.0, -499.0, -493.0, -489.0, -465.0, -432.0, -422.0, -391.0, -352.0, -333.0, -293.0, -255.0, -219.0, -196.0, -162.0, -156.0, -151.0, -141.0, -145.0, -132.0, -122.0, -139.0, -158.0, -147.0, -170.0, -175.0, -182.0, -179.0, -183.0, -188.0, -177.0, -193.0, -175.0, -167.0, -158.0, -152.0, -159.0, -145.0, -137.0, -122.0, -107.0, -104.0, -82.0, -58.0, -55.0, -30.0, -8.0, 18.0, 43.0, 49.0, 63.0, 81.0, 111.0, 117.0, 124.0, 141.0, 133.0, 153.0, 183.0, 191.0, 211.0, 239.0, 234.0, 260.0, 282.0, 283.0, 303.0, 325.0, 323.0, 314.0, 325.0, 311.0, 310.0, 300.0, 267.0, 253.0, 218.0, 185.0, 157.0, 131.0, 101.0, 72.0, 29.0, 23.0, -30.0, -79.0, -101.0, -123.0, -164.0, -188.0, -215.0, -261.0, -278.0, -300.0, -330.0, -335.0, -358.0, -363.0, -350.0, -342.0, -328.0, -306.0, -278.0, -254.0, -228.0, -193.0, -167.0, -155.0, -118.0, -81.0, -60.0, -59.0, -45.0, -38.0, -31.0, -37.0, -54.0, -62.0, -59.0, -61.0, -77.0, -61.0, -78.0, -85.0, -84.0, -93.0, -101.0, -123.0, -122.0, -142.0, -141.0, -167.0, -178.0, -198.0, -215.0, -237.0, -242.0, -265.0, -298.0, -312.0, -342.0, -350.0, -352.0, -402.0, -404.0, -420.0, -444.0, -467.0, -477.0, -492.0, -502.0, -492.0, -488.0, -488.0, -476.0, -462.0, -441.0, -408.0, -373.0, -339.0, -308.0, -251.0, -220.0, -205.0, -171.0, -170.0, -154.0, -131.0, -131.0, -142.0, -130.0, -138.0, -145.0, -145.0, -141.0, -143.0, -144.0, -145.0, -152.0, -122.0, -136.0, -149.0, -137.0, -128.0, -133.0, -111.0, -116.0, -117.0, -103.0, -107.0, -103.0, -86.0, -58.0, -50.0, -37.0, -22.0, -2.0, 34.0, 32.0, 79.0, 90.0, 115.0, 124.0, 126.0, 157.0, 168.0, 177.0, 209.0, 224.0, 222.0, 237.0, 265.0, 261.0, 285.0, 321.0, 337.0, 348.0, 352.0, 350.0, 361.0, 359.0, 359.0, 339.0, 350.0, 333.0, 304.0, 289.0, 266.0, 254.0, 210.0, 191.0, 158.0, 134.0, 99.0, 57.0, 52.0, -5.0, -41.0, -77.0, -101.0, -131.0, -168.0, -197.0, -236.0, -248.0, -270.0, -239.0, -250.0, -242.0, -236.0, -225.0, -198.0, -172.0, -158.0, -111.0, -89.0, -68.0, -54.0, -9.0, 17.0, 28.0, 28.0, 36.0, 48.0, 66.0, 54.0, 55.0, 60.0, 59.0, 59.0, 64.0, 72.0, 76.0, 70.0, 61.0, 58.0, 59.0, 45.0, 47.0, 16.0, 21.0, -6.0, -27.0, -42.0, -51.0, -73.0, -77.0, -99.0, -130.0, -127.0, -174.0, -197.0, -221.0, -232.0, -255.0, -284.0, -310.0, -331.0, -339.0, -360.0, -392.0, -365.0, -358.0, -365.0, -357.0, -334.0, -308.0, -279.0, -245.0, -208.0, -187.0, -134.0, -127.0, -87.0, -85.0, -67.0, -47.0, -25.0, -21.0, -27.0, -12.0, -34.0, -30.0, -33.0, -20.0, -41.0, -68.0, -50.0, -51.0, -42.0, -30.0, -51.0, -35.0, -26.0, -46.0, -50.0, -38.0, -36.0, -32.0, -20.0, 9.0, 16.0, 27.0, 36.0, 63.0, 61.0, 83.0, 101.0, 125.0, 140.0, 157.0, 173.0, 195.0, 197.0, 205.0, 236.0, 250.0, 241.0, 285.0, 292.0, 304.0, 328.0, 344.0, 371.0, 401.0, 423.0, 414.0, 449.0, 455.0, 461.0, 455.0, 464.0, 438.0, 424.0, 425.0, 411.0, 388.0, 365.0, 342.0, 316.0, 321.0, 279.0, 240.0, 223.0, 189.0, 140.0, 104.0, 76.0, 32.0, 7.0, -37.0, -69.0, -92.0, -114.0, -137.0, -154.0, -163.0, -177.0, -181.0, -155.0, -129.0, -122.0, -95.0, -81.0, -65.0, -24.0, -10.0, 23.0, 39.0, 54.0, 77.0, 93.0, 86.0, 86.0, 96.0, 99.0, 117.0, 115.0, 124.0, 122.0, 123.0, 121.0, 120.0, 116.0, 93.0, 103.0, 88.0, 75.0, 67.0, 50.0, 28.0, 23.0, -4.0, -22.0, -31.0, -65.0, -72.0, -97.0, -103.0, -129.0, -151.0, -185.0, -222.0, -243.0, -262.0, -311.0, -329.0, -358.0, -386.0, -378.0, -406.0, -407.0, -406.0, -387.0, -375.0, -362.0, -326.0, -303.0, -249.0, -229.0, -203.0, -182.0, -157.0, -121.0, -115.0, -108.0, -96.0, -91.0, -82.0, -62.0, -84.0, -72.0, -69.0, -81.0, -80.0, -83.0, -70.0, -69.0, -62.0, -61.0, -68.0, -62.0, -89.0, -86.0, -69.0, -81.0, -85.0, -70.0, -70.0, -59.0, -57.0, -38.0, -20.0, -29.0, 6.0, 19.0, 39.0, 50.0, 67.0, 69.0, 73.0, 87.0, 93.0, 126.0, 132.0, 137.0, 155.0, 182.0, 183.0, 215.0, 232.0, 253.0, 285.0, 291.0, 314.0, 348.0, 349.0, 342.0, 351.0, 365.0, 353.0, 350.0, 349.0, 351.0, 340.0, 342.0, 313.0, 309.0, 269.0, 244.0, 230.0, 198.0, 172.0, 132.0, 101.0, 60.0, 27.0, -4.0, -29.0, -108.0, -127.0, -152.0, -199.0, -220.0, -255.0, -293.0, -284.0, -287.0, -298.0, -279.0, -251.0, -254.0, -235.0, -209.0, -192.0, -165.0, -128.0, -126.0, -100.0, -97.0, -89.0, -60.0, -52.0, -49.0, -29.0, -26.0, -18.0, 4.0, 9.0, 8.0, 13.0, -8.0, -6.0, -13.0, -12.0, -20.0, -24.0, -45.0, -49.0, -49.0, -68.0, -102.0, -94.0, -109.0, -122.0, -121.0, -144.0, -166.0, -173.0, -194.0, -219.0, -243.0, -282.0, -300.0, -326.0, -371.0, -397.0, -421.0, -428.0, -456.0, -475.0, -477.0, -478.0, -479.0, -444.0, -434.0, -400.0, -381.0, -352.0, -335.0, -294.0, -258.0, -244.0, -206.0, -190.0, -191.0, -162.0, -166.0, -157.0, -146.0, -127.0, -125.0, -133.0, -127.0, -130.0, -117.0, -141.0, -136.0, -130.0, -117.0, -130.0, -135.0, -134.0, -135.0, -135.0, -140.0, -123.0, -124.0, -126.0, -124.0, -125.0, -85.0, -85.0, -71.0, -59.0, -34.0, -29.0, -3.0, 27.0, 20.0, 42.0, 57.0, 50.0, 67.0, 86.0, 96.0, 120.0, 144.0, 166.0, 181.0, 205.0, 231.0, 253.0, 269.0, 280.0, 299.0, 312.0, 335.0, 340.0, 331.0, 344.0, 355.0, 372.0, 346.0, 362.0, 341.0, 338.0, 316.0, 306.0, 289.0, 264.0, 242.0, 229.0, 213.0, 178.0, 143.0, 115.0, 58.0, 12.0, -20.0, -58.0, -119.0, -158.0, -184.0, -191.0, -226.0, -226.0, -227.0, -227.0, -214.0, -207.0, -193.0, -167.0, -143.0, -134.0, -104.0, -94.0, -75.0, -53.0, -47.0, -28.0, -29.0, -16.0, -8.0, 14.0, 21.0, 20.0, 41.0, 38.0, 41.0, 43.0, 49.0, 61.0, 56.0, 55.0, 44.0, 33.0, 4.0, -4.0, 6.0, -17.0, -8.0, -24.0, -19.0, -40.0, -60.0, -70.0, -79.0, -103.0, -125.0, -149.0, -180.0, -208.0, -229.0, -261.0, -294.0, -319.0, -357.0, -373.0, -396.0, -410.0, -427.0, -416.0, -414.0, -402.0, -384.0, -374.0, -354.0, -319.0, -295.0, -264.0, -233.0, -206.0, -194.0, -160.0, -160.0, -137.0, -128.0, -120.0, -120.0, -105.0, -102.0, -98.0, -105.0, -114.0, -106.0, -97.0, -103.0, -117.0, -107.0, -104.0, -128.0, -134.0, -141.0, -136.0, -137.0, -120.0, -139.0, -110.0, -133.0, -114.0, -91.0, -69.0, -49.0, -61.0, -49.0, -26.0, -8.0, -6.0, 16.0, 23.0, 33.0, 42.0, 66.0, 74.0, 80.0, 80.0, 120.0, 136.0, 157.0, 178.0, 193.0, 221.0, 236.0, 253.0, 269.0, 284.0, 305.0, 329.0, 329.0, 323.0, 335.0, 336.0, 331.0, 333.0, 331.0, 308.0, 303.0, 294.0, 280.0, 261.0, 244.0, 195.0, 174.0, 144.0, 107.0, 67.0, 23.0, -16.0, -55.0, -93.0, -149.0, -185.0, -229.0, -261.0, -257.0, -286.0, -290.0, -299.0, -305.0, -294.0, -272.0, -261.0, -253.0, -224.0, -229.0, -185.0, -186.0, -172.0, -139.0, -125.0, -119.0, -107.0, -95.0, -72.0, -67.0, -63.0, -55.0, -50.0, -36.0, -39.0, -30.0, -29.0, -47.0, -50.0, -60.0, -66.0, -83.0, -104.0, -79.0, -94.0, -91.0, -92.0, -106.0, -119.0, -118.0, -139.0, -141.0, -161.0, -198.0, -215.0, -230.0, -285.0, -333.0, -358.0, -394.0, -419.0, -443.0, -459.0, -496.0, -467.0, -485.0, -480.0, -472.0, -484.0, -473.0, -442.0, -419.0, -398.0, -379.0, -358.0, -333.0, -287.0, -289.0, -274.0, -245.0, -221.0, -208.0, -186.0, -169.0, -165.0, -159.0, -156.0, -140.0, -132.0, -135.0, -113.0, -125.0, -121.0, -120.0, -132.0, -142.0, -122.0, -158.0, -144.0, -160.0, -150.0, -144.0, -129.0, -107.0, -113.0, -94.0, -85.0, -69.0, -51.0, -24.0, -37.0, -11.0, 6.0, 10.0, 26.0, 29.0, 44.0, 62.0, 80.0, 99.0, 105.0, 155.0, 172.0, 184.0, 195.0, 234.0, 250.0, 273.0, 284.0, 317.0, 328.0, 323.0, 337.0, 366.0, 376.0, 363.0, 377.0, 390.0, 383.0, 379.0, 395.0, 388.0, 360.0, 365.0, 356.0, 337.0, 306.0, 277.0, 251.0, 219.0, 175.0, 113.0, 87.0, 26.0, -5.0, -56.0, -72.0, -106.0, -133.0, -142.0, -150.0, -162.0, -165.0, -169.0, -161.0, -129.0, -146.0, -145.0, -97.0, -94.0, -82.0, -64.0, -51.0, -42.0, -6.0, -4.0, 17.0, 50.0, 50.0, 68.0, 93.0, 101.0, 106.0, 122.0, 117.0, 115.0, 124.0, 122.0, 114.0, 111.0, 108.0, 92.0, 86.0, 73.0, 93.0, 94.0, 90.0, 99.0, 90.0, 87.0, 62.0, 42.0, 19.0, 1.0, -22.0, -44.0, -75.0, -107.0, -149.0, -180.0, -225.0, -222.0, -258.0, -277.0, -279.0, -273.0, -287.0, -279.0, -247.0, -247.0, -224.0, -202.0, -186.0, -168.0, -157.0, -147.0, -100.0, -86.0, -75.0, -72.0, -107.0, -37.0, 22.0, 12.0, 47.0, 45.0, 35.0, 7.0, 12.0, 74.0, 32.0, 66.0, 83.0, 89.0, 66.0, 61.0, 58.0, 71.0, 49.0, 56.0, 58.0, 59.0, 77.0, 57.0, 89.0, 82.0, 109.0, 104.0, 124.0, 132.0, 133.0, 148.0, 152.0, 159.0, 164.0, 164.0, 166.0, 195.0, 205.0, 217.0, 253.0, 280.0, 293.0, 318.0, 342.0, 374.0, 381.0, 393.0, 439.0, 441.0, 456.0, 471.0, 483.0, 487.0, 509.0, 522.0, 510.0, 512.0, 528.0, 538.0, 546.0, 549.0, 528.0, 528.0, 509.0, 480.0, 465.0, 428.0, 396.0, 360.0, 309.0, 255.0, 216.0, 159.0, 118.0, 86.0, 56.0, 34.0, -1.0, -23.0, -64.0, -52.0, -84.0, -83.0, -87.0, -69.0, -65.0, -72.0, -47.0, -33.0, -26.0, -7.0, 11.0, 24.0, 33.0, 74.0, 76.0, 92.0, 98.0, 101.0, 123.0, 144.0, 129.0, 125.0, 134.0, 99.0, 132.0, 126.0, 119.0, 110.0, 116.0, 114.0, 135.0, 135.0, 114.0, 132.0, 115.0, 133.0, 111.0, 108.0, 87.0, 75.0, 48.0, 34.0, 7.0, -27.0, -60.0, -98.0, -137.0, -173.0, -207.0, -224.0, -243.0, -265.0, -297.0, -292.0, -312.0, -296.0, -304.0, -281.0, -263.0, -247.0, -210.0, -208.0, -188.0, -174.0, -164.0, -134.0, -117.0, -83.0, -68.0, -57.0, -33.0, -29.0, -10.0, 27.0, 25.0, 22.0, 27.0, 39.0, 38.0, 18.0, 25.0, 20.0, 26.0, 19.0, 23.0, 19.0, 1.0, 4.0, 17.0, 5.0, -8.0, 37.0, 37.0, 39.0, 46.0, 39.0, 49.0, 61.0, 62.0, 52.0, 79.0, 78.0, 89.0, 108.0, 99.0, 114.0, 131.0, 156.0, 171.0, 199.0, 218.0, 228.0, 255.0, 278.0, 305.0, 330.0, 351.0, 363.0, 368.0, 379.0, 404.0, 418.0, 427.0, 432.0, 442.0, 478.0, 479.0, 490.0, 505.0, 517.0, 504.0, 505.0, 490.0, 462.0, 437.0, 409.0, 376.0, 331.0, 294.0, 238.0, 205.0, 162.0, 109.0, 62.0, 37.0, -5.0, -28.0, -34.0, -68.0, -61.0, -97.0, -103.0, -107.0, -102.0, -99.0, -91.0, -103.0, -84.0, -73.0, -60.0, -35.0, -50.0, -2.0, -40.0, 58.0, 63.0, 83.0, 96.0, 100.0, 114.0, 112.0, 118.0, 111.0, 103.0, 106.0, 112.0, 105.0, 112.0, 115.0, 91.0, 105.0, 105.0, 112.0, 108.0, 122.0, 119.0, 117.0, 107.0, 81.0, 53.0, 71.0, 46.0, 16.0, -37.0, -77.0, -112.0, -138.0, -173.0, -204.0, -249.0, -277.0, -283.0, -293.0, -317.0, -334.0, -322.0, -318.0, -313.0, -309.0, -298.0, -275.0, -259.0, -247.0, -213.0, -189.0, -211.0, -164.0, -174.0, -157.0, -119.0, -119.0, -86.0, -73.0, -79.0, -73.0, -46.0, -55.0, -57.0, -65.0, -72.0, -80.0, -80.0, -88.0, -91.0, -89.0, -91.0, -101.0, -74.0, -97.0, -75.0, -92.0, -55.0, -45.0, -45.0, -44.0, -39.0, -46.0, -26.0, -46.0, -29.0, -16.0, -36.0, -33.0, -11.0, -9.0, -3.0, 12.0, 19.0, 27.0, 55.0, 100.0, 114.0, 115.0, 164.0, 160.0, 183.0, 189.0, 197.0, 233.0, 256.0, 273.0, 270.0, 306.0, 305.0, 326.0, 333.0, 362.0, 359.0, 377.0, 403.0, 398.0, 390.0, 372.0, 353.0, 341.0, 311.0, 299.0, 245.0, 211.0, 157.0, 116.0, 81.0, 38.0, 3.0, -15.0, -67.0, -77.0, -108.0, -133.0, -145.0, -147.0, -145.0, -156.0, -167.0, -184.0, -207.0, -181.0, -169.0, -169.0, -171.0, -141.0, -122.0, -108.0, -93.0, -78.0, -50.0, -37.0, -23.0, -33.0, -4.0, 7.0, -8.0, -10.0, -5.0, -8.0, 17.0, 5.0, -1.0, 28.0, 16.0, 24.0, 42.0, 45.0, 38.0, 50.0, 53.0, 39.0, 27.0, 31.0, 2.0, -9.0, -29.0, -47.0, -87.0, -123.0, -174.0, -199.0, -227.0, -258.0, -293.0, -305.0, -337.0, -350.0, -364.0, -356.0, -379.0, -383.0, -385.0, -369.0, -354.0, -352.0, -332.0, -312.0, -290.0, -281.0, -247.0, -217.0, -199.0, -178.0, -146.0, -134.0, -113.0, -105.0, -107.0, -74.0, -91.0, -80.0, -88.0, -88.0, -93.0, -91.0, -80.0, -84.0, -79.0, -86.0, -80.0, -70.0, -70.0, -67.0, -63.0, -56.0, -56.0, -45.0, -42.0, -28.0, -21.0, -27.0, -32.0, -44.0, -26.0, -8.0, -11.0, -7.0, 10.0, 7.0, 17.0, 44.0, 57.0, 57.0, 82.0, 99.0, 120.0, 130.0, 157.0, 189.0, 193.0, 203.0, 210.0, 238.0, 263.0, 266.0, 299.0, 328.0, 348.0, 356.0, 382.0, 403.0, 403.0, 416.0, 438.0, 421.0, 431.0, 429.0, 416.0, 374.0, 356.0, 329.0, 295.0, 262.0, 216.0, 184.0, 147.0, 110.0, 53.0, 34.0, -2.0, -26.0, -39.0, -67.0, -99.0, -117.0, -119.0, -153.0, -136.0, -152.0, -163.0, -167.0, -155.0, -147.0, -123.0, -104.0, -102.0, -73.0, -66.0, -51.0, -24.0, -17.0, 8.0, 6.0, -3.0, -6.0, -8.0, -15.0, -2.0, -5.0, 12.0, 5.0, 26.0, 31.0, 38.0, 41.0, 60.0, 67.0, 63.0, 69.0, 74.0, 79.0, 72.0, 60.0, 45.0, 36.0, -1.0, -30.0, -59.0, -103.0, -145.0, -164.0, -201.0, -256.0, -266.0, -308.0, -321.0, -327.0, -343.0, -365.0, -370.0, -362.0, -378.0, -357.0, -362.0, -361.0, -348.0, -325.0, -318.0, -306.0, -276.0, -270.0, -233.0, -222.0, -190.0, -190.0, -166.0, -139.0, -145.0, -113.0, -118.0, -114.0, -107.0, -103.0, -100.0, -106.0, -89.0, -111.0, -98.0, -114.0, -114.0, -118.0, -118.0, -123.0, -96.0, -86.0, -102.0, -78.0, -76.0, -76.0, -81.0, -88.0, -69.0, -82.0, -89.0, -78.0, -75.0, -65.0, -41.0, -52.0, -32.0, -35.0, -19.0, 6.0, 34.0, 43.0, 53.0, 83.0, 88.0, 123.0, 125.0, 169.0, 163.0, 177.0, 196.0, 219.0, 257.0, 257.0, 272.0, 312.0, 354.0, 367.0, 364.0, 402.0, 411.0, 418.0, 417.0, 400.0, 396.0, 375.0, 345.0, 320.0, 280.0, 243.0, 217.0, 188.0, 150.0, 111.0, 69.0, 41.0, 11.0, -30.0, -55.0, -84.0, -110.0, -124.0, -140.0, -169.0, -172.0, -178.0, -174.0, -175.0, -172.0, -161.0, -140.0, -134.0, -128.0, -110.0, -79.0, -76.0, -68.0, -54.0, -45.0, -37.0, -44.0, -25.0, 1.0, -10.0, -9.0, -23.0, 0.0, 1.0, 2.0, 4.0, 26.0, 39.0, 61.0, 69.0, 86.0, 97.0, 108.0, 106.0, 96.0, 89.0, 68.0, 42.0, 34.0, -16.0, -35.0, -55.0, -99.0, -129.0, -164.0, -200.0, -225.0, -239.0, -264.0, -272.0, -269.0, -303.0, -317.0, -319.0, -326.0, -337.0, -330.0, -320.0, -304.0, -291.0, -266.0, -242.0, -230.0, -215.0, -190.0, -164.0, -148.0, -123.0, -108.0, -85.0, -87.0, -83.0, -74.0, -63.0, -71.0, -59.0, -63.0, -65.0, -60.0, -56.0, -54.0, -35.0, -40.0, -37.0, -27.0, -12.0, -8.0, -6.0, 16.0, -12.0, -3.0, -2.0, -6.0, 3.0, 0.0, -9.0, 1.0, 3.0, 18.0, 32.0, 11.0, 51.0, 73.0, 67.0, 85.0, 96.0, 106.0, 125.0, 152.0, 148.0, 168.0, 175.0, 197.0, 215.0, 225.0, 233.0, 264.0, 288.0, 292.0, 337.0, 357.0, 394.0, 432.0, 459.0, 476.0, 496.0, 500.0, 496.0, 511.0, 499.0, 481.0, 450.0, 443.0, 415.0, 407.0, 370.0, 326.0, 277.0, 256.0, 243.0, 205.0, 174.0, 147.0, 104.0, 80.0, 45.0, 17.0, -23.0, -22.0, -37.0, -62.0, -49.0, -60.0, -59.0, -38.0, -35.0, -25.0, 14.0, 14.0, 21.0, 45.0, 52.0, 58.0, 84.0, 85.0, 83.0, 91.0, 103.0, 105.0, 95.0, 91.0, 108.0, 106.0, 106.0, 126.0, 135.0, 155.0, 145.0, 175.0, 193.0, 193.0, 213.0, 218.0, 212.0, 213.0, 199.0, 184.0, 161.0, 137.0, 110.0, 97.0, 57.0, 39.0, -14.0, -42.0, -61.0, -79.0, -103.0, -112.0, -154.0, -184.0, -173.0, -204.0, -207.0, -210.0, -212.0, -211.0, -204.0, -183.0, -157.0, -151.0, -142.0, -101.0, -107.0, -74.0, -39.0, -33.0, -6.0, 11.0, 5.0, 42.0, 40.0, 34.0, 45.0, 39.0, 49.0, 51.0, 41.0, 41.0, 46.0, 43.0, 44.0, 60.0, 65.0, 68.0, 57.0, 84.0, 84.0, 76.0, 76.0, 70.0, 74.0, 82.0, 72.0, 55.0, 57.0, 88.0, 86.0, 70.0, 77.0, 94.0, 108.0, 116.0, 124.0, 149.0, 157.0, 185.0, 196.0, 210.0, 216.0, 224.0, 231.0, 242.0, 255.0, 261.0, 271.0, 294.0, 321.0, 340.0, 361.0, 410.0, 423.0, 458.0, 487.0, 509.0, 520.0, 532.0, 530.0, 540.0, 529.0, 527.0, 521.0, 494.0, 469.0, 451.0, 426.0, 393.0, 356.0, 322.0, 277.0, 262.0, 221.0, 192.0, 151.0, 114.0, 74.0, 52.0, 15.0, -17.0, -29.0, -47.0, -56.0, -52.0, -73.0, -54.0, -29.0, -35.0, -36.0, -14.0, -15.0, 11.0, 11.0, 23.0, 26.0, 35.0, 42.0, 46.0, 41.0, 37.0, 49.0, 47.0, 53.0, 67.0, 78.0, 91.0, 85.0, 111.0, 135.0, 147.0, 164.0, 185.0, 183.0, 189.0, 181.0, 175.0, 146.0, 132.0, 139.0, 87.0, 64.0, 46.0, 13.0, -20.0, -47.0, -83.0, -109.0, -133.0, -166.0, -188.0, -213.0, -246.0, -261.0, -272.0, -271.0, -294.0, -307.0, -311.0, -308.0, -301.0, -285.0, -261.0, -252.0, -230.0, -209.0, -200.0, -166.0, -158.0, -142.0, -132.0, -116.0, -85.0, -71.0, -83.0, -83.0, -75.0, -74.0, -60.0, -53.0, -55.0, -60.0, -56.0, -55.0, -46.0, -12.0, -32.0, -34.0, -8.0, -23.0, -29.0, -28.0, -40.0, -54.0, -45.0, -52.0, -44.0, -48.0, -56.0, -42.0, -38.0, -24.0, -20.0, -13.0, -18.0, 26.0, 10.0, 34.0, 51.0, 39.0, 64.0, 79.0, 80.0, 79.0, 102.0, 114.0, 131.0, 162.0, 182.0, 209.0, 235.0, 271.0, 307.0, 335.0, 355.0, 398.0, 415.0, 435.0, 445.0, 447.0, 470.0, 469.0, 462.0, 438.0, 430.0, 414.0, 399.0, 387.0, 366.0, 318.0, 291.0, 254.0, 218.0, 210.0, 158.0, 107.0, 76.0, 50.0, 22.0, -21.0, -5.0, -41.0, -62.0, -69.0, -62.0, -56.0, -53.0, -52.0, -48.0, -38.0, -30.0, -24.0, -8.0, 22.0, 12.0, 27.0, 40.0, 27.0, 27.0, 33.0, 41.0, 21.0, 18.0, 26.0, 27.0, 44.0, 69.0, 80.0, 101.0, 120.0, 134.0, 155.0, 169.0, 170.0, 181.0, 187.0, 181.0, 153.0, 163.0, 139.0, 119.0, 102.0, 71.0, 32.0, 13.0, -15.0, -39.0, -51.0, -94.0, -126.0, -140.0, -163.0, -194.0, -214.0, -235.0, -260.0, -245.0, -277.0, -275.0, -236.0, -237.0, -220.0, -211.0, -196.0, -185.0, -154.0, -142.0, -149.0, -122.0, -80.0, -97.0, -73.0, -74.0, -58.0, -33.0, -31.0, -37.0, -32.0, -25.0, -23.0, -10.0, 13.0, 9.0, 7.0, 14.0, 28.0, 23.0, 29.0, 31.0, 27.0, 51.0, 67.0, 22.0, 34.0, 37.0, 12.0, 24.0, 35.0, 19.0, 24.0, 45.0, 28.0, 60.0, 53.0, 72.0, 79.0, 93.0, 113.0, 102.0, 114.0, 111.0, 116.0, 127.0, 144.0, 143.0, 162.0, 161.0, 204.0, 246.0, 254.0, 283.0, 315.0, 348.0, 387.0, 395.0, 432.0, 466.0, 473.0, 496.0, 510.0, 498.0, 515.0, 518.0, 497.0, 488.0, 481.0, 471.0, 452.0, 428.0, 396.0, 370.0, 344.0, 289.0, 265.0, 231.0, 180.0, 137.0, 113.0, 75.0, 47.0, 23.0, 4.0, 11.0, 5.0, -4.0, -16.0, 4.0, -4.0, -12.0, -14.0, 5.0, 6.0, 23.0, 36.0, 33.0, 43.0, 44.0, 27.0, 30.0, 30.0, 25.0, 27.0, 27.0, 39.0, 56.0, 67.0, 90.0, 105.0, 123.0, 129.0, 160.0, 176.0, 170.0, 176.0, 199.0, 182.0, 180.0, 164.0, 150.0, 154.0, 140.0, 127.0, 103.0, 92.0, 46.0, 11.0, -7.0, -46.0, -64.0, -108.0, -146.0, -164.0, -188.0, -217.0, -224.0, -263.0, -274.0, -267.0, -279.0, -290.0, -257.0, -252.0, -230.0, -218.0, -228.0, -204.0, -169.0, -158.0, -132.0, -129.0, -111.0, -93.0, -72.0, -69.0, -59.0, -72.0, -46.0, -54.0, -48.0, -33.0, -32.0, -25.0, -10.0, 2.0, -5.0, 12.0, 18.0, 8.0, 27.0, 15.0, -7.0, 8.0, 5.0, -17.0, -14.0, -12.0, -17.0, -26.0, -10.0, -11.0, 9.0, 21.0, 29.0, 36.0, 39.0, 42.0, 46.0, 51.0, 66.0, 64.0, 76.0, 75.0, 76.0, 94.0, 110.0, 119.0, 143.0, 170.0, 202.0, 248.0, 268.0, 288.0, 324.0, 374.0, 390.0, 418.0, 453.0, 471.0, 490.0, 506.0, 514.0, 513.0, 527.0, 508.0, 538.0, 526.0, 499.0, 491.0, 476.0, 442.0, 421.0, 387.0, 328.0, 311.0, 266.0, 235.0, 190.0, 174.0, 126.0, 80.0, 69.0, 34.0, 16.0, 16.0, 26.0, 34.0, 30.0, 36.0, 45.0, 32.0, 34.0, 60.0, 60.0, 49.0, 49.0, 61.0, 43.0, 45.0, 41.0, 50.0, 57.0, 48.0, 58.0, 74.0, 92.0, 101.0, 132.0, 147.0, 166.0, 176.0, 173.0, 218.0, 212.0, 216.0, 233.0, 240.0, 232.0, 233.0, 230.0, 204.0, 216.0, 180.0, 190.0, 144.0, 115.0, 96.0, 76.0, 50.0, 17.0, -12.0, -44.0, -77.0, -86.0, -129.0, -142.0, -163.0, -177.0, -186.0, -172.0, -188.0, -161.0, -161.0, -155.0, -135.0, -142.0, -134.0, -100.0, -74.0, -74.0, -58.0, -44.0, -30.0, -16.0, -3.0, -5.0, 10.0, 34.0, 22.0, 41.0, 41.0, 47.0, 72.0, 89.0, 100.0, 90.0, 113.0, 103.0, 101.0, 91.0, 109.0, 93.0, 90.0, 87.0, 81.0, 87.0, 73.0, 70.0, 85.0, 98.0, 76.0, 94.0, 120.0, 119.0, 116.0, 135.0, 134.0, 131.0, 135.0, 123.0, 151.0, 120.0, 131.0, 143.0, 159.0, 177.0, 180.0, 193.0, 225.0, 254.0, 277.0, 321.0, 343.0, 368.0, 406.0, 416.0, 440.0, 462.0, 473.0, 501.0, 531.0, 546.0, 537.0, 567.0, 559.0, 567.0, 554.0, 541.0, 516.0, 505.0, 476.0, 454.0, 420.0, 393.0, 353.0, 336.0, 293.0, 241.0, 197.0, 156.0, 140.0, 108.0, 91.0, 83.0, 51.0, 46.0, 49.0, 61.0, 55.0, 78.0, 76.0, 79.0, 79.0, 79.0, 65.0, 67.0, 61.0, 60.0, 61.0, 53.0, 61.0, 60.0, 82.0, 85.0, 100.0, 114.0, 105.0, 130.0, 143.0, 180.0, 195.0, 209.0, 220.0, 243.0, 253.0, 248.0, 248.0, 251.0, 262.0, 248.0, 263.0, 242.0, 229.0, 210.0, 198.0, 165.0, 136.0, 114.0, 80.0, 45.0, -7.0, -26.0, -70.0, -74.0, -104.0, -126.0, -139.0, -156.0, -151.0, -162.0, -158.0, -176.0, -144.0, -153.0, -134.0, -119.0, -115.0, -102.0, -84.0, -58.0, -64.0, -44.0, -40.0, -35.0, -19.0, -11.0, -2.0, 15.0, 31.0, 40.0, 56.0, 70.0, 85.0, 78.0, 90.0, 111.0, 122.0, 105.0, 107.0, 105.0, 94.0, 103.0, 82.0, 87.0, 93.0, 95.0, 99.0, 89.0, 115.0, 106.0, 119.0, 121.0, 138.0, 122.0, 141.0, 129.0, 142.0, 129.0, 137.0, 126.0, 126.0, 131.0, 135.0, 139.0, 136.0, 147.0, 170.0, 187.0, 210.0, 252.0, 273.0, 287.0, 328.0, 350.0, 382.0, 412.0, 432.0, 463.0, 494.0, 493.0, 515.0, 554.0, 562.0, 559.0, 566.0, 581.0, 577.0, 575.0, 544.0, 548.0, 515.0, 484.0, 448.0, 430.0, 395.0, 352.0, 334.0, 281.0, 225.0, 205.0, 188.0, 164.0, 151.0, 108.0, 110.0, 104.0, 85.0, 83.0, 89.0, 78.0, 86.0, 89.0, 64.0, 52.0, 73.0, 36.0, 51.0, 40.0, 41.0, 36.0, 40.0, 42.0, 32.0, 56.0, 52.0, 85.0, 93.0, 100.0, 129.0, 142.0, 151.0, 167.0, 161.0, 177.0, 172.0, 191.0, 200.0, 187.0, 195.0, 199.0, 181.0, 176.0, 173.0, 159.0, 136.0, 107.0, 75.0, 50.0, 28.0, 2.0, -48.0, -69.0, -95.0, -126.0, -151.0, -166.0, -201.0, -220.0, -218.0, -225.0, -216.0, -226.0, -235.0, -235.0, -220.0, -211.0, -204.0, -199.0, -193.0, -163.0, -152.0, -160.0, -151.0, -143.0, -136.0, -127.0, -125.0, -109.0, -91.0, -78.0, -71.0, -55.0, -60.0, -38.0, -35.0, -28.0, -34.0, -44.0, -53.0, -49.0, -27.0, -37.0, -42.0, -49.0, -56.0, -41.0, -46.0, -48.0, -31.0, -22.0, -22.0, -5.0, 0.0, -1.0, -3.0, -5.0, -7.0, -26.0, -26.0, -29.0, -33.0, -25.0, -11.0, -34.0, -1.0, 5.0, 17.0, 38.0, 49.0, 74.0, 100.0, 121.0, 147.0, 177.0, 205.0, 227.0, 279.0, 280.0, 323.0, 342.0, 362.0, 377.0, 401.0, 407.0, 419.0, 423.0, 423.0, 436.0, 419.0, 393.0, 391.0, 360.0, 320.0, 288.0, 241.0, 206.0, 187.0, 132.0, 98.0, 76.0, 45.0, 4.0, 8.0, -6.0, -27.0, -33.0, -46.0, -39.0, -45.0, -47.0, -47.0, -57.0, -41.0, -56.0, -53.0, -76.0, -86.0, -90.0, -93.0, -93.0, -101.0, -92.0, -106.0, -85.0, -76.0, -67.0, -50.0, -33.0, -16.0, 6.0, 29.0, 49.0, 61.0, 75.0, 97.0, 99.0, 115.0, 142.0, 144.0, 150.0, 156.0, 126.0, 110.0, 108.0, 89.0, 51.0, 29.0, 18.0, -9.0, -36.0, -71.0, -100.0, -139.0, -153.0, -178.0, -206.0, -228.0, -253.0, -270.0, -280.0, -275.0, -287.0, -269.0, -278.0, -281.0, -269.0, -258.0, -250.0, -233.0, -213.0, -209.0, -209.0, -199.0, -198.0, -202.0, -183.0, -172.0, -155.0, -135.0, -128.0, -121.0, -106.0, -99.0, -87.0, -81.0, -83.0, -79.0, -78.0, -91.0, -74.0, -85.0, -95.0, -86.0, -91.0, -83.0, -87.0, -85.0, -65.0, -56.0, -78.0, -45.0, -50.0, -53.0, -42.0, -51.0, -64.0, -53.0, -65.0, -76.0, -65.0, -66.0, -76.0, -44.0, -52.0, -42.0, -45.0, -17.0, -15.0, 12.0, 36.0, 55.0, 70.0, 94.0, 152.0, 151.0, 192.0, 215.0, 248.0, 281.0, 317.0, 327.0, 351.0, 373.0, 383.0, 404.0, 414.0, 402.0, 403.0, 396.0, 390.0, 363.0, 349.0, 308.0, 297.0, 254.0, 227.0, 170.0, 157.0, 117.0, 86.0, 61.0, 27.0, 12.0, 5.0, -1.0, -2.0, -14.0, -9.0, -15.0, -10.0, -15.0, -38.0, -47.0, -62.0, -68.0, -70.0, -80.0, -101.0, -75.0, -77.0, -73.0, -85.0, -75.0, -71.0, -54.0, -44.0, -31.0, -40.0, -12.0, -16.0, 28.0, 23.0, 42.0, 75.0, 101.0, 115.0, 108.0, 122.0, 137.0, 128.0, 137.0, 146.0, 118.0, 99.0, 82.0, 63.0, 38.0, 1.0, -31.0, -43.0, -79.0, -115.0, -154.0, -162.0, -211.0, -235.0, -256.0, -264.0, -277.0, -295.0, -297.0, -280.0, -282.0, -283.0, -264.0, -272.0, -270.0, -268.0, -269.0, -259.0, -257.0, -258.0, -225.0, -231.0, -232.0, -211.0, -194.0, -164.0, -161.0, -148.0, -124.0, -102.0, -106.0, -107.0, -96.0, -103.0, -103.0, -114.0, -99.0, -108.0, -116.0, -116.0, -115.0, -112.0, -105.0, -101.0, -91.0, -74.0, -72.0, -84.0, -78.0, -78.0, -77.0, -86.0, -71.0, -96.0, -92.0, -102.0, -120.0, -118.0, -130.0, -132.0, -115.0, -100.0, -100.0, -73.0, -51.0, -54.0, -55.0, -34.0, -10.0, -12.0, 44.0, 49.0, 87.0, 102.0, 114.0, 175.0, 210.0, 232.0, 258.0, 297.0, 300.0, 315.0, 345.0, 348.0, 352.0, 365.0, 353.0, 340.0, 305.0, 291.0, 253.0, 236.0, 210.0, 170.0, 138.0, 101.0, 63.0, 31.0, 14.0, -12.0, -30.0, -28.0, -44.0, -55.0, -57.0, -65.0, -80.0, -74.0, -83.0, -97.0, -102.0, -128.0, -143.0, -144.0, -154.0, -149.0, -158.0, -156.0, -164.0, -162.0, -163.0, -164.0, -138.0, -147.0, -123.0, -104.0, -92.0, -93.0, -67.0, -37.0, -7.0, -14.0, 0.0, 23.0, 45.0, 61.0, 76.0, 74.0, 71.0, 86.0, 68.0, 40.0, 38.0, 15.0, -2.0, -26.0, -59.0, -106.0, -128.0, -154.0, -188.0, -208.0, -228.0, -248.0, -261.0, -272.0, -289.0, -303.0, -290.0, -301.0, -304.0, -319.0, -317.0, -324.0, -315.0, -306.0, -292.0, -297.0, -288.0, -296.0, -288.0, -252.0, -248.0, -230.0, -218.0, -226.0, -210.0, -183.0, -184.0, -163.0, -132.0, -140.0, -136.0, -131.0, -137.0, -131.0, -124.0, -139.0, -145.0, -147.0, -145.0, -121.0, -108.0, -123.0, -115.0, -105.0, -87.0, -92.0, -91.0, -87.0, -86.0, -96.0, -93.0, -95.0, -104.0, -116.0, -103.0, -108.0, -105.0, -104.0, -99.0, -108.0, -108.0, -85.0, -89.0, -68.0, -66.0, -45.0, -44.0, -10.0, 22.0, 53.0, 79.0, 130.0, 141.0, 160.0, 199.0, 241.0, 263.0, 300.0, 326.0, 352.0, 375.0, 386.0, 412.0, 399.0, 398.0, 400.0, 369.0, 364.0, 325.0, 293.0, 262.0, 242.0, 208.0, 173.0, 146.0, 131.0, 117.0, 97.0, 77.0, 69.0, 53.0, 36.0, 24.0, 24.0, 20.0, 11.0, 7.0, -11.0, -44.0, -34.0, -52.0, -62.0, -60.0, -71.0, -79.0, -88.0, -98.0, -87.0, -79.0, -88.0, -90.0, -84.0, -44.0, -38.0, -28.0, -18.0, 3.0, 11.0, 38.0, 53.0, 71.0, 112.0, 125.0, 130.0, 149.0, 154.0};
				//take the FFT of the above audio sample
				double[] note42fft = Helper.fft(note42Audio);
				//Make a 1 column matrix
				Matrix test = new Matrix(note42fft.length, 1);
				test.writeCol(0, note42fft);
				//finish doing the math
				test = dataTranspose.multOnLeftOf(test);
				//apply the same RREF
				test.modifyByRecord(record);
				//print how much of each note from the spetra file/hashMap is in the recorded sample!
				test.printMatrix();
				
				
			}
		});
		
		
		if (mIsFirstOnCreate) {
			//Check max memory for this device
			ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
			Log.i(TAG,"Maximum around of memory allowed for this device: " + activityManager.getMemoryClass());
			
			//Initialize the active note spectra file to null in the sharedPreferences
			Helper.setStringPref(Helper.ACTIVE_MAPDATA_FILE_KEY, null, getApplicationContext());
			
			//set boolean so this doesn't run again
			mIsFirstOnCreate = false;
		}
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

