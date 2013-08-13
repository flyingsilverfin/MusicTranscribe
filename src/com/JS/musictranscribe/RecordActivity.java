package com.JS.musictranscribe;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class RecordActivity extends Activity {

	private static final String TAG = "RecordActivity";
	
	private final int SAMPLING_SPEED = 44100; //samples per second, 44100 default (guaranteed support on devices)
	
	private final int MAX_NOTE_SECONDS = 50; 			// SECONDS
	private final int FULL_BUFFER_SIZE = SAMPLING_SPEED*MAX_NOTE_SECONDS;
	private final float RECORDER_BUFFER_SIZE_MULTIPLIER = 1; //*0.5 bec 16 bit

	private final int EXTERNAL_BUFFER_TIME = 100; //desired milliseconds
	private final int EXTERNAL_BUFFER_SIZE = Helper.nextLowerPowerOf2((int)(SAMPLING_SPEED*((float)EXTERNAL_BUFFER_TIME/1000))); //find next lower power of two
	private final int ACTUAL_EXTERNAL_BUFFER_TIME = EXTERNAL_BUFFER_SIZE*1000/SAMPLING_SPEED; //find actual time being measured based on above
	
	private boolean mIsRecordingPaused = true;
	private boolean mIsRecordingDone = false;
	private boolean isFirstToggle = true;
	private Button mRecordingPausePlayButton;	
	private Button mFinishRecordingButton;
	private Button mGraphButton;
	private AudioAnalyzer mAudioAnalyzer;

	//DEBUG
		private Button D_graphEveryCycleToggleButton;
		private boolean D_graphEveryCycle = false;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_record);
		
		mRecordingPausePlayButton = (Button) findViewById(R.id.Record_PausePlay_Button);
		mRecordingPausePlayButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleRecording();
				mRecordingPausePlayButton.setText("isPaused: " +( mIsRecordingPaused ? "true" : "false"));
			}
		});

		mGraphButton = (Button) findViewById(R.id.Make_Graph_Button);
		mGraphButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				makeGraphs();
			}
		});

		D_graphEveryCycleToggleButton = (Button) findViewById(R.id.D_Graph_Every_Cycle_Button);
		D_graphEveryCycleToggleButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleGraphEveryCycle();
				D_graphEveryCycleToggleButton.setText("graphEveryCycle: "+ (D_graphEveryCycle ? "true" : "false"));
			}
		});
//IN PROGRESS!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

		Log.i(TAG,"Desired Buffer Time: "+EXTERNAL_BUFFER_TIME+", Actual buffer time:"+ACTUAL_EXTERNAL_BUFFER_TIME+" \n\n");

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.record, menu);
		return true;
	}


	private void toggleRecording() {		
		if (mIsRecordingPaused) {
			mIsRecordingPaused = false;
			mAudioAnalyzer.resumeRecording();
		}
		else if (!mIsRecordingPaused) {
			mIsRecordingPaused = true;
			mAudioAnalyzer.pauseRecording();
		}

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
		if (D_graphEveryCycle) {
			mGraphButton.setEnabled(true); 
			mAudioAnalyzer.D_graphEveryCycle = false;
			D_graphEveryCycle = false;
		} 
		else if (!D_graphEveryCycle) {
			mGraphButton.setEnabled(false); //disable the makeGraph button if graphing every new measurement
			mAudioAnalyzer.D_graphEveryCycle = true;
			D_graphEveryCycle = true;
		}
	}
		
	
}
