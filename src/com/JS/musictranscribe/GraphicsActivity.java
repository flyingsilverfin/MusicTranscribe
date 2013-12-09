package com.JS.musictranscribe;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.support.v4.app.NavUtils;
import android.annotation.TargetApi;
import android.os.Build;

public class GraphicsActivity extends Activity {
	public boolean notemove = false;
	private Button graphicsPlayButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_graphics);
		// Show the Up button in the action bar.
		setupActionBar();
		RelativeLayout graphicsLayout =(RelativeLayout)findViewById(R.id.graphicsLayout);
		//Set up the graphics canvas
		final GraphicsSurfaceView graphicsView = new GraphicsSurfaceView(this);
		graphicsLayout.addView(graphicsView);
		//setContentView(new GraphicsSurfaceView(this));
		
		graphicsPlayButton = (Button) findViewById(R.id.graphicsActivityPlay);
		graphicsPlayButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(notemove){
					notemove = false;
					graphicsView.noteMove(notemove);
					graphicsPlayButton.setText("Play");
				} else {
					notemove = true;
					graphicsView.noteMove(notemove);
					graphicsPlayButton.setText("Pause");
				}
				
			}
		});
			
		
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
		getMenuInflater().inflate(R.menu.graphics, menu);
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
