package com.JS.musictranscribe;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
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
	
	private boolean isRecording = false;
	private boolean isFirstToggle = true;
	private Button recordButton;	
	private AudioAnalyzer audioAnalyzer;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_record);
		
		recordButton = (Button) findViewById(R.id.Record_Button);
		
		recordButton.setOnClickListener(new View.onClickListener() {
			@Override
			public void onClick(View v) {
				toggleRecording
//---------------------------------------------------------------- working on this part sometime --------------

		Log.i(TAG,"Desired Buffer Time: %d, Actual buffer time: %d\n\n",EXTERNAL_BUFFER_TIME,ACTUAL_EXTERNAL_BUFFER_TIME);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.record, menu);
		return true;
	}


}
