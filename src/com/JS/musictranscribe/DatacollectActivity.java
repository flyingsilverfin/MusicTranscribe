package com.JS.musictranscribe;

import java.util.HashMap;

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
import android.widget.TextView;




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
	
	private TextView mStatusTextView;
	
	private Button mStartNewMapButton;
	private HashMap<Integer, Double[]> mNoteSpectraMap;
	private int mNumSamplesForThisMap;
	
	private EditText mNewNoteMapNameEditText;
	private Button mSaveNewMapButton;
	
	private EditText mLoadNoteMapEditText;
	private Button mLoadNewMapButton;
	private Button mListAllFilesButton;
	private Button mDeleteAllFilesButton;
	
	
	private AudioCollector mAudioCollector;
	
	//Dropbox
	private boolean mIsDBLoggedIn;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_datacollect);
		// Show the Up button in the action bar.
		setupActionBar();
		
		
		mNoteSpectraMap = new HashMap<Integer, Double[]>();
		mNumSamplesForThisMap = -1;
		
		
		mNumRecordingsEditText = (EditText) findViewById(R.id.num_recordings_edittext);
		mStartNRecordingsButton = (Button) findViewById(R.id.submit_n_recordings_button);
		mStartNRecordingsButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				int num = Integer.parseInt(mNumRecordingsEditText.getText().toString());
				disableInputs();
				Log.i(TAG,"\tGoing to record: " + num + "times");
				double[][] result = mAudioCollector.getNSamples(num);
				mAudioCollector.writeSamplesToDropbox(result, num+"recordings");
				status("Recorded " + result.length + " recordings and uploaded to Dropbox if logged in");
				enableInputs();
			}
		});
		
		mTimedRecordingEditText = (EditText) findViewById(R.id.recording_time_edittext);
		mStartTimedRecordingButton = (Button) findViewById(R.id.submit_time_recording_button);
		mStartTimedRecordingButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				int msecs = Integer.parseInt(mTimedRecordingEditText.getText().toString());
				disableInputs();
				Log.i(TAG,"\tGoing to record for: " + mTimedRecordingEditText.getText().toString() + "milliseconds");
				double[][] result = mAudioCollector.getSamplesFor(msecs*1000);
				status("Recorded " + result.length + " recordings in " +  mTimedRecordingEditText.getText().toString() +
						" ms and uploaded to Dropbox if logged in");
				mAudioCollector.writeSamplesToDropbox(result, msecs+"msRecording");
				enableInputs();
				
			}
		});
		
		
		mStartNewMapButton = (Button) findViewById(R.id.start_new_map_button);
		mStartNewMapButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Log.i(TAG, "Referencing HashMap to new empty HashMap");
				status("Initialized new map");
				mNoteSpectraMap = new HashMap<Integer, Double[]>();
				mNumSamplesForThisMap = -1;
			}
		});
		
		
		mNewNoteMapNameEditText = (EditText) findViewById(R.id.new_note_map_name_edittext);
		mSaveNewMapButton = (Button) findViewById(R.id.save_new_map_button);
		mSaveNewMapButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				if (mNoteSpectraMap.keySet().size() == 0) {
					Log.i(TAG,"Note map is empty, aborting");
					status("Note map is empty! Aborting.");
					return;
				}
				
				String fileName;
				fileName = mNewNoteMapNameEditText.getText().toString();
				if (fileName.length() == 0) {
					Log.e(TAG,"no name entered!");
					status("Enter a name!");
					return;
				}
				
				try{
					Helper.writeNewNoteSpectraFile(getApplicationContext(), fileName, mNoteSpectraMap);
					status("Wrote data to new file " + fileName);
				} catch (Exception e) {
					status(e.getMessage());
				}
			}
		});
		
		
		mNoteNumEditText = (EditText) findViewById(R.id.note_num_edittext);
		mGetNoteDataButton = (Button) findViewById(R.id.get_note_data_button);
		mStatusTextView = (TextView) findViewById(R.id.status_textview);
		
		mGetNoteDataButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new Thread() {
					public void run() {
						
						int n;
						try {
							n = Integer.parseInt(mNoteNumEditText.getText().toString());
						} catch (NumberFormatException e) {
							Log.e(TAG,"invalid note number!");
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									status("Not a valid number");
								}
							});
							return; //exit out!
						}
						
						final int noteNum = n;
						
						if (noteNum < 1 || noteNum > 88) {	//use human counting, 1-88 allowed inclusive
							Log.e(TAG,"noteNum is out of range");
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									status("Note must be in range 1-88 inclusive");
								}
							});
							return; //exit out!
						}
						
						
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								disableInputs();
							}
						});
						
						
						Log.i(TAG, "Recording...");
						
						double[][] samples;
						if (mNumSamplesForThisMap == -1) { //if this is the first recording for this map
							samples = mAudioCollector.getSamplesFor(3000000);
							mNumSamplesForThisMap = samples.length;
							Log.i(TAG, "First time for new map, got " + mNumSamplesForThisMap + " samples");
						}
						else {
							Log.i(TAG, "Getting another " + mNumSamplesForThisMap + " samples");
							samples = mAudioCollector.getNSamples(mNumSamplesForThisMap);
						}
						
						Log.i(TAG,"Averaging samples &  doing FFT");
						Double[] fft = mAudioCollector.fftIntoDoubleObjects(Helper.averageArrays(samples));
						
						Log.i(TAG,"Adding to hashmap");
						if (mNoteSpectraMap.containsKey(Integer.valueOf(noteNum))) {
							Log.i(TAG, "Overwriting previous hashmap entry for note " + noteNum);
							mNoteSpectraMap.remove(Integer.valueOf(noteNum)); //remove to overwrite if exists
						}
						
						mNoteSpectraMap.put(Integer.valueOf(noteNum), fft);	
												
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								status("Finished recording and saving " + mNumSamplesForThisMap + " averaged samples in 3 seconds for note #" + noteNum);
								enableInputs();
							}
						});		
						
					}
				}.start();
				
			}
		});
		
		mLoadNoteMapEditText = (EditText) findViewById(R.id.load_note_map_edittext);
		mLoadNewMapButton = (Button) findViewById(R.id.load_note_map_button);
		mLoadNewMapButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				String fileName;
				fileName = mLoadNoteMapEditText.getText().toString();
				if (fileName.length() == 0) {
					Log.e(TAG,"no name entered!");
					status("Enter a name!");
					return;
				}
				
				mNoteSpectraMap = Helper.getNoteSpectraFromFile(getApplicationContext(), fileName);
				status("Retrieved " + mNoteSpectraMap.keySet().size() + " key/data sets from file " + fileName);
				Log.i(TAG, "Keys/data sets retrieved: " + mNoteSpectraMap.keySet().toString());
			}
		});
		
		mListAllFilesButton = (Button) findViewById(R.id.list_all_private_files_button);
		mListAllFilesButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				String[] files = Helper.listAllPrivFiles(getApplicationContext());
				String f = Helper.join(files,", ");
				Log.i(TAG, "Private files: " + f);
				mStatusTextView.setText(f);
			}
		});
		
		mDeleteAllFilesButton = (Button) findViewById(R.id.delete_all_private_files_button);
		mDeleteAllFilesButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Helper.deleteAllPrivFiles(getApplicationContext());
				status("Deleted all data files!");
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

	
	
	private void disableInputs() {
		mNumRecordingsEditText.setEnabled(false);
		mStartNRecordingsButton.setEnabled(false);
		
		mTimedRecordingEditText.setEnabled(false);
		mStartTimedRecordingButton.setEnabled(false);
		
		mNoteNumEditText.setEnabled(false);
		mGetNoteDataButton.setEnabled(false);
		
		mStartNewMapButton.setEnabled(false);
		mNewNoteMapNameEditText.setEnabled(false);
		mSaveNewMapButton.setEnabled(false);
		
		mLoadNoteMapEditText.setEnabled(false);
		mLoadNewMapButton.setEnabled(false);
		
		mDeleteAllFilesButton.setEnabled(false);
		
	}
	
	private void enableInputs() {
		mNumRecordingsEditText.setEnabled(true);
		mStartNRecordingsButton.setEnabled(true);
		
		mTimedRecordingEditText.setEnabled(true);
		mStartTimedRecordingButton.setEnabled(true);
		
		mNoteNumEditText.setEnabled(true);
		mGetNoteDataButton.setEnabled(true);
		
		mStartNewMapButton.setEnabled(true);
		mNewNoteMapNameEditText.setEnabled(true);
		mSaveNewMapButton.setEnabled(true);
		
		mLoadNoteMapEditText.setEnabled(true);
		mLoadNewMapButton.setEnabled(true);
		
		mDeleteAllFilesButton.setEnabled(true);
		
	}
	
	
	private void status(String msg) {
		mStatusTextView.setText(msg);
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
