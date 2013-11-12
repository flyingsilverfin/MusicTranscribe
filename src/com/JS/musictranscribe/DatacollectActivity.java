package com.JS.musictranscribe;

import android.annotation.TargetApi;
import android.app.Activity;
import android.media.MediaRecorder.AudioSource;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;




public class DatacollectActivity extends Activity {
	
	//tag
	private static final String TAG = "DatacollectActivity";
	
	//for AudioCollector
	private final int mSAMPLING_SPEED = 44100; //samples per second, 44100 default (guaranteed support on devices)
	
	private final int mEXTERNAL_BUFFER_TIME = 100; //desired milliseconds
	private final int mEXTERNAL_BUFFER_SIZE = Helper.nextLowerPowerOf2((int)(mSAMPLING_SPEED*((float)mEXTERNAL_BUFFER_TIME/1000))); //find next lower power of two
	private final int mACTUAL_EXTERNAL_BUFFER_TIME = mEXTERNAL_BUFFER_SIZE*1000/mSAMPLING_SPEED; //find actual time being measured based on above

	
	//UI 
	private EditText mNumRecordingsEditText;
	private Button mStartNRecordingsButton;
	private EditText mTimedRecordingEditText;
	private Button mStartTimedRecordingButton;
	
	private EditText mNoteNumEditText;
	private Button mGetNoteDataButton;
	
	
	private AudioCollector mAudioCollector;
	
	//Dropbox
	private boolean mIsDBLoggedIn;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_datacollect);
		// Show the Up button in the action bar.
		setupActionBar();
		
		
		
		
		mNumRecordingsEditText = (EditText) findViewById(R.id.num_recordings_edittext);
		mStartNRecordingsButton = (Button) findViewById(R.id.submit_n_recordings_button);
		mStartNRecordingsButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				int num = Integer.parseInt(mNumRecordingsEditText.getText().toString());
				Log.i(TAG,"\tGoing to record: " + num + "times");
				
				double[][] result = mAudioCollector.getNSamples(num);
				
				mAudioCollector.writeSamples(result, num+"recordings");
			}
		});
		
		mTimedRecordingEditText = (EditText) findViewById(R.id.recording_time_edittext);
		mStartTimedRecordingButton = (Button) findViewById(R.id.submit_time_recording_button);
		mStartTimedRecordingButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				int msecs = Integer.parseInt(mTimedRecordingEditText.getText().toString());
				Log.i(TAG,"\tGoing to record for: " + mTimedRecordingEditText.getText().toString() + "milliseconds");
				
				double[][] result = mAudioCollector.getSamplesFor(msecs*1000);
				
				mAudioCollector.writeSamples(result, msecs+"msRecording");
				
			}
		});
		
		mNoteNumEditText = (EditText) findViewById(R.id.note_num_edittext);
		mGetNoteDataButton = (Button) findViewById(R.id.get_note_data_button);
		mGetNoteDataButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				int noteNum = Integer.parseInt(mNoteNumEditText.getText().toString());
				
			}
		});
		
		
		
		
		mAudioCollector = new AudioCollector(AudioSource.MIC, Helper.SAMPLING_SPEED, 
				true, true, Helper.EXTERNAL_BUFFER_SIZE, this);
		
		
		mIsDBLoggedIn = getIntent().getBooleanExtra("DBLoggedIn", false);

		mAudioCollector.setDBLoggedIn(mIsDBLoggedIn);
		if(mAudioCollector.isDBLoggedIn()){ 
			mAudioCollector.setDBDataUpload(true);
		}
	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.datacollect, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
