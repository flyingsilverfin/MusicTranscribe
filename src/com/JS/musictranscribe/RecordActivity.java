package com.JS.musictranscribe;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaRecorder.AudioSource;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.jjoe64.graphview.BarGraphView;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;

public class RecordActivity extends Activity {

	private static final String TAG = "RecordActivity";
	
	
	private final int mMAX_NOTE_SECONDS = 50; 			// SECONDS
	private final int mFULL_BUFFER_SIZE = Helper.SAMPLING_SPEED*mMAX_NOTE_SECONDS;

	private boolean mIsRecordingPaused = true;
	private boolean mIsRecordingDone = false;
	private boolean mIsFirstToggle = true;
	private Button mRecordingPausePlayButton;	
	private Button mFinishRecordingButton;
	private Button mGraphButton;
	private AudioAnalyzer mAudioAnalyzer;
	
	private GraphView mGraphView;
	private LinearLayout mGraphLayout;
	private GraphViewSeries mGraphData;
	private double mAmplitude;
	private boolean mIsLiveGraphOn;

	//DEBUG
		private Button dGraphEveryCycleToggleButton;
		private boolean dGraphEveryCycle = false;
		
		private Button dDBDataUploadToggleButton;
		private boolean dDBDataUploadEnabled = false;
		
		private boolean mIsDBLoggedIn;

	    
	
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
		
		/*
		 * Dropbox is disabled for now.
		
		dDBDataUploadToggleButton = (Button) findViewById(R.id.dDropbox_Upload_Button);
		dDBDataUploadToggleButton.setEnabled(false); //only enable if DB is accessible
		dDBDataUploadToggleButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleDBDataUpload();
			}
		});
		
		*/

		
		//set up graphView
		mGraphView = new BarGraphView(this, "Amplitude");
		mGraphView.setScalable(false);
		mGraphView.setBackgroundColor(Color.WHITE);
		mGraphView.setScrollable(false);
		mGraphView.setScalable(false);
		mGraphView.setManualYAxisBounds(1000,0);
		mGraphLayout = (LinearLayout) findViewById(R.id.amplitude_graph);
		mGraphData = new GraphViewSeries("Amplitude Data", null, new GraphViewData[] { new GraphViewData(0,0) });
		mGraphView.addSeries(mGraphData);
		
		mGraphLayout.addView(mGraphView);
		
		
		
		mAudioAnalyzer = new AudioAnalyzer(AudioSource.MIC, Helper.SAMPLING_SPEED, 
				true, true, Helper.EXTERNAL_BUFFER_SIZE, this); 
				//isMono and is16Bit = true, this = context to pass in for graphing activity source

		
		mIsDBLoggedIn = getIntent().getBooleanExtra("DBLoggedIn", false);
		if (mIsDBLoggedIn) {
			//dDBDataUploadToggleButton.setEnabled(true);
			mAudioAnalyzer.setDBLoggedIn(true);
		}
		else {
			//dDBDataUploadToggleButton.setEnabled(false);
			mAudioAnalyzer.setDBLoggedIn(false);
		}
	}
	
	// -----END onCreate

	/*
	 * This is here to catch returning back buttons
	 * Checks to see if graphEveryCycle is enabled
	 * if yes, then start recorder right away again!
	 */
	@Override
	public void onResume() {
		super.onResume();
		if (mAudioAnalyzer.isGraphEveryCycle()) {
			Log.i(TAG,"GraphEveryCycle is TRUE, Resuming Recording");
			resumeRecording();
		}
	}
	
	
	// -----END onResume
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.record, menu);
		return true;
	}

	
	/*
	 * Required to be overridden to get intent back from sub-activity
	 */
	@Override 
	public void onActivityResult(int requestCode, int resultCode, Intent returnIntent) {     
	  super.onActivityResult(requestCode, resultCode, returnIntent); 
	  switch(requestCode) { 
	    case (AudioAnalyzer.GRAPH_EVERY_CYCLE_IDNUM) : { 
	      if (resultCode == Activity.RESULT_OK) { 
		      dGraphEveryCycle = returnIntent.getBooleanExtra("disableGraphContinously", true);
		      toggleGraphEveryCycle(); //updates based on dGraphEveryCycle so it's safe this way
		      pauseRecording();
	      } 
	      break; 
    	} 
	  } 
	}

	private void toggleRecording() {
		if (mIsFirstToggle) { //this only runs the first time
			mIsRecordingPaused = false;
			startLiveAmplitudeGraph();
			mAudioAnalyzer.startRecording();
			mIsFirstToggle = false;
		}
		else if (mIsRecordingPaused) {
			mIsRecordingPaused = false;
			startLiveAmplitudeGraph();
			mAudioAnalyzer.resumeRecording();
		}
		else if (!mIsRecordingPaused) {
			mIsRecordingPaused = true;
			stopLiveAmplitudeGraph();
			mAudioAnalyzer.pauseRecording();
		}
		mRecordingPausePlayButton.setText("paused: " +( mIsRecordingPaused ? "true" : "false"));
	}
	
	private void pauseRecording() {
		mIsRecordingPaused = false;
		toggleRecording();
	}
	
	private void resumeRecording() {
		mIsRecordingPaused = true;
		toggleRecording();
	}

	private void startLiveAmplitudeGraph() {
		mIsLiveGraphOn = true;
		Log.i(TAG,"going to make bar graph!");
		//would it be better to use an interrupt to do this? With boolean get to keep track of if its on or not, I think its better like this.
		(new Thread() {
			public void run() {
				Log.i(TAG,"In run()");
				while (mIsLiveGraphOn) {
					try {
						Log.i(TAG,"In try");
						Thread.sleep(100);
					} catch( InterruptedException e) { Log.d(TAG,e.getMessage());}
					Log.i(TAG,"out of try, resetting Data");
					
					
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							mGraphData.resetData( new GraphViewData[] { 
									new GraphViewData(0, mAudioAnalyzer.getCurrentAvAmplitude()),
								});							
						}
					});
					
				}
			}
		}).start();
	}
	
	private void stopLiveAmplitudeGraph() {
		mIsLiveGraphOn = false;
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
		mAudioAnalyzer.makeGraphs(mAudioAnalyzer.getRawAudioArrayAsDoubles(), mAudioAnalyzer.getIntervalFreqData());
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
		dGraphEveryCycleToggleButton.setText("ContGraph: "+ (dGraphEveryCycle ? "true" : "false"));
	}
		
	private void toggleDBDataUpload() {
		if (dDBDataUploadEnabled) {
			setDBDataUpload(false);
			dDBDataUploadToggleButton.setText("Enable DB");
		}
		else {
			dDBDataUploadToggleButton.setText("Disable DB");
			setDBDataUpload(true);
		}
	}

	
	private void setDBDataUpload(boolean uploadEnabled) {
		dDBDataUploadEnabled = uploadEnabled;
		mAudioAnalyzer.setDBDataUpload(uploadEnabled);
	}
	
	private boolean isDBDataUploadEnabled() {
		return dDBDataUploadEnabled;
	}
}
