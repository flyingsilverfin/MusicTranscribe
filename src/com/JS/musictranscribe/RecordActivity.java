package com.JS.musictranscribe;

import android.app.Activity;
import android.media.MediaRecorder.AudioSource;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class RecordActivity extends Activity {

	private static final String TAG = "RecordActivity";
	
	private final int mSAMPLING_SPEED = 44100; //samples per second, 44100 default (guaranteed support on devices)
	
	private final int mMAX_NOTE_SECONDS = 50; 			// SECONDS
	private final int mFULL_BUFFER_SIZE = mSAMPLING_SPEED*mMAX_NOTE_SECONDS;
	private final float mRECORDER_BUFFER_SIZE_MULTIPLIER = 1; //*0.5 bec 16 bit

	private final int mEXTERNAL_BUFFER_TIME = 100; //desired milliseconds
	private final int mEXTERNAL_BUFFER_SIZE = Helper.nextLowerPowerOf2((int)(mSAMPLING_SPEED*((float)mEXTERNAL_BUFFER_TIME/1000))); //find next lower power of two
	private final int mACTUAL_EXTERNAL_BUFFER_TIME = mEXTERNAL_BUFFER_SIZE*1000/mSAMPLING_SPEED; //find actual time being measured based on above
	
	private boolean mIsRecordingPaused = true;
	private boolean mIsRecordingDone = false;
	private boolean mIsFirstToggle = true;
	private Button mRecordingPausePlayButton;	
	private Button mFinishRecordingButton;
	private Button mGraphButton;
	private AudioAnalyzer mAudioAnalyzer;

	//DEBUG
		private Button dGraphEveryCycleToggleButton;
		private boolean dGraphEveryCycle = false;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_record);
		
		mRecordingPausePlayButton = (Button) findViewById(R.id.Record_PausePlay_Button);
		mRecordingPausePlayButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleRecording();
			}
		});

		mGraphButton = (Button) findViewById(R.id.Make_Graph_Button);
		mGraphButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				makeGraphs();
			}
		});

		dGraphEveryCycleToggleButton = (Button) findViewById(R.id.dGraph_Every_Cycle_Button);
		dGraphEveryCycleToggleButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleGraphEveryCycle();
			}
		});

		mAudioAnalyzer = new AudioAnalyzer(AudioSource.MIC, mSAMPLING_SPEED, 
				true, true, mEXTERNAL_BUFFER_SIZE, this); 
				//isMono and is16Bit = true, this = context to pass in for graphing activity source

		Log.i(TAG,"Desired Buffer Time: "+mEXTERNAL_BUFFER_TIME+", Actual buffer time:"+ mACTUAL_EXTERNAL_BUFFER_TIME +" \n\n");

	}
	
	// -----END onCreate

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.record, menu);
		return true;
	}


	private void toggleRecording() {
		if (mIsFirstToggle) { //this only runs the first time
			mIsRecordingPaused = false;
			mAudioAnalyzer.startRecording();
			mIsFirstToggle = false;
		}
		else if (mIsRecordingPaused) {
			mIsRecordingPaused = false;
			mAudioAnalyzer.resumeRecording();
		}
		else if (!mIsRecordingPaused) {
			mIsRecordingPaused = true;
			mAudioAnalyzer.pauseRecording();
		}
		mRecordingPausePlayButton.setText("isPaused: " +( mIsRecordingPaused ? "true" : "false"));


	}

	
	//this is separate because I suspect we will want to send them to editing and or postprocess all of it at this point
	private void finishRecording() {
		if (mIsRecordingDone) {
		}
		else if (!mIsRecordingDone) {
			mIsRecordingDone = true;		
			mAudioAnalyzer.finishRecording();
		}
	}

	private void makeGraphs() {
		if (!mIsRecordingPaused) {
			toggleRecording();
		}
		while (!mAudioAnalyzer.isThreadingPaused()) { //wait until the threading is actually paused to ensure everything is updated
			try {			
				Thread.sleep(1);
			} catch (InterruptedException e) {}
		}
		mAudioAnalyzer.makeGraphs(mAudioAnalyzer.getIntervalRawData(), mAudioAnalyzer.getIntervalFreqData());
	}
				

	private void toggleGraphEveryCycle() {
		if (dGraphEveryCycle) {
			mGraphButton.setEnabled(true); 
			mAudioAnalyzer.dGraphEveryCycle = false;
			dGraphEveryCycle = false;
		} 
		else if (!dGraphEveryCycle) {
			mGraphButton.setEnabled(false); //disable the makeGraph button if graphing every new measurement
			mAudioAnalyzer.dGraphEveryCycle = true;
			dGraphEveryCycle = true;
		}
		dGraphEveryCycleToggleButton.setText("graphEveryCycle: "+ (dGraphEveryCycle ? "true" : "false"));
	}
		
	
}
