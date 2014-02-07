package com.JS.musictranscribe;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
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
import com.jjoe64.graphview.LineGraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;

public class RecordActivity extends Activity implements MyListFragment.OnSomethingCheckedInterface {

	private static final String TAG = "RecordActivity";
	
	
	private final int mMAX_NOTE_SECONDS = 50; 			// SECONDS
	private final int mFULL_BUFFER_SIZE = Helper.SAMPLING_SPEED*mMAX_NOTE_SECONDS;

	private boolean mIsRecordingPaused = true;
	private boolean mIsRecordingDone = false;
	private boolean mIsFirstToggle = true;
	private Button mRecordingPausePlayButton;	
	private Button mFinishRecordingButton;
	private Button mGraphButton;
	private Button mListFilesButton;
	private AudioAnalyzer mAudioAnalyzer;
	
	private GraphView mGraphView;
	private LinearLayout mGraphLayout;
	private GraphViewSeries mGraphData;
	private double mAmplitude;
	private boolean mIsLiveGraphOn;
	
	//list selection fragment
	private MyListFragment mListFragment;
	private String mActiveFile;

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
		
		mListFilesButton = (Button) findViewById(R.id.list_files_button);
		mListFilesButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (!mListFragment.isVisible()) {
					Log.i(TAG, "starting list fragment");
					FragmentManager fragmentManager = getFragmentManager();
					FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
					fragmentTransaction.add(R.id.empty_fragment, mListFragment);
					fragmentTransaction.addToBackStack(null);
					fragmentTransaction.commit();
				}
			}
		});

		
		//set up graphView
		mGraphView = new BarGraphView(this, "");
		mGraphView.setBackgroundColor(Color.WHITE);
		mGraphView.setScrollable(true);
		mGraphView.setScalable(false);
		mGraphView.setManualYAxisBounds(10,0);
		//mGraphView.setVerticalLabels(new String[] {"10^1","10^2","10^3","10^4"}); //trying to get this working...
		//mGraphView.setViewPort(-1,0.2);
		mGraphView.getGraphViewStyle().setVerticalLabelsWidth(1);
		mGraphData = new GraphViewSeries(new GraphViewData[] { new GraphViewData(0,0) });
		mGraphView.addSeries(mGraphData);
		
		
		mGraphLayout = (LinearLayout) findViewById(R.id.amplitude_graph);
		mGraphLayout.addView(mGraphView);

		
		
		//set up List Fragment for later use
		mListFragment = new MyListFragment();
		
		//check if there's an active file, otherwise can't do anything
		mActiveFile = Helper.getStringPref(Helper.ACTIVE_MAPDATA_FILE_KEY, getApplicationContext());
		if (Helper.getStringPref(Helper.ACTIVE_MAPDATA_FILE_KEY, getApplicationContext()) == null) {
			if (!mListFragment.isVisible()) {
				Log.i(TAG, "starting list fragment");
				FragmentManager fragmentManager = getFragmentManager();
				FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
				fragmentTransaction.add(R.id.empty_fragment, mListFragment);
				fragmentTransaction.addToBackStack(null);
				fragmentTransaction.commit();
			}
		}
		
		Log.i(TAG,"active file: " + mActiveFile);
		
		mAudioAnalyzer = new AudioAnalyzer(AudioSource.MIC, Helper.SAMPLING_SPEED, 
				true, true, Helper.EXTERNAL_BUFFER_SIZE, this, mActiveFile);  //the constructor also has a check to make sure there's a valid file
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
			boolean success = mAudioAnalyzer.startRecording();
			if (success) {
				Log.e(TAG,"Failed to start audio analyzer");
				mIsRecordingPaused = false;
				startLiveAmplitudeGraph();
				mIsFirstToggle = false;
			}
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
		//would it be better to use an interrupt to do this? With boolean get to keep track of if its on or not, I think its better like this.
		(new Thread() {
			public void run() {
				Log.i(TAG,"In run()");
				while (mIsLiveGraphOn) {
					try {
						Thread.sleep(100);
					} catch( InterruptedException e) { Log.d(TAG,e.getMessage());}
					
					
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Log.i(TAG,"Val: " + mAudioAnalyzer.getCurrentAvAmplitude());
							mGraphData.resetData( new GraphViewData[] { 
									new GraphViewData(0, Math.log10(mAudioAnalyzer.getCurrentAvAmplitude()))
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
		stopLiveAmplitudeGraph();
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
	
	//------------------------------
	
	/*
	 * For the MyListFragment communication
	 */
	public void onSomethingChecked() {
		
		//if the thing checked is different from what was selected before then rebuild the AudioAnalyzer
		if (!Helper.getStringPref(Helper.ACTIVE_MAPDATA_FILE_KEY, getApplicationContext()).equals(mActiveFile)) {
			mAudioAnalyzer.setNoteMapDataFile(Helper.getStringPref(Helper.ACTIVE_MAPDATA_FILE_KEY, getApplicationContext()), getApplicationContext());
		}
	}
	
}
