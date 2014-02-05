package com.JS.musictranscribe;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;


/*
 * used to input pairs of extras -- MUST BE DOUBLES!!!!!!
 * x-axis, y-axis datasets, will be paired correspondingly into (x,y)
 * for x-axis, start name of extra with xAxis_NAME
 * y-axis just name NAME,
 * eg:
 * putExtra(xAxis_data1,xVals)
 * putExtra(data1,yVals)
 */

public class GraphActivity extends Activity {

	private static final String TAG = "GraphActivity";

	private List<String> yAxisKeys;
	private Spinner graphChoicesSpinner;
	private Button mSubmitGraphChoiceButton;
	private Button mPercentActivityButton;
	private Button mDisableContGraphing;
	private GraphView graphView;
	private LinearLayout layout;
	private GraphViewData[] graphviewData;
	
	private boolean dGraphEveryCycle;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_graph);
		

	
		
		//set up the Spinner with graph choices
		graphChoicesSpinner = (Spinner) findViewById(R.id.graph_spinner);
		yAxisKeys = getYAxisKeys();
		for (int i = 0; i < yAxisKeys.size(); i++) {
			Log.i(TAG,"choice: " + yAxisKeys.get(i));
		}
		
		ArrayAdapter<String> adapter = new ArrayAdapter<String> (this, 
				android.R.layout.simple_spinner_item, yAxisKeys);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		graphChoicesSpinner.setAdapter(adapter);
		
		Log.i(TAG,"Spinner set up");
		
		//set up submit button
		mSubmitGraphChoiceButton = (Button) findViewById(R.id.submit_graph_choice_button);
		mSubmitGraphChoiceButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				updateGraph(String.valueOf(graphChoicesSpinner.getSelectedItem()));
			}
		});
		
		Log.i(TAG,"Wired Submit Button");
		
		
		dGraphEveryCycle = getIntent().getBooleanExtra("dGraphEveryCycle", false);
		mDisableContGraphing = (Button) findViewById(R.id.disable_cont_graphing_button);
		if (!dGraphEveryCycle) {
			mDisableContGraphing.setEnabled(false);
		}
		else {
			mDisableContGraphing.setEnabled(true);
		}
		
		mDisableContGraphing.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				Intent resultIntent = new Intent();
				resultIntent.putExtra("disableGraphContinously", true);
				setResult(Activity.RESULT_OK, resultIntent);
				finish();
				
			}
		});

		
		//set up graphView
		graphView = new LineGraphView(this, "");
		
		
		graphView.setScalable(false);
		graphView.setBackgroundColor(Color.WHITE);
		graphView.setScrollable(true);
		
		((LineGraphView)graphView).setDrawBackground(false);
		
		layout = (LinearLayout) findViewById(R.id.graph);

		Log.i(TAG,"Graph view set up");

	}


	
	private void updateGraph(String key) {
		Log.i(TAG,"updating graph for key: xAxis_" + key + ", " + key);

		double[] data = getIntent().getDoubleArrayExtra(key);
		double[] xAxis = getIntent().getDoubleArrayExtra("xAxis_" + key); //add on the xAxis

		graphviewData = new GraphViewData[Math.min(xAxis.length, data.length)]; //min in case the lengths don't match up, cut to shorter datalength
		
		for (int i = 0; i < graphviewData.length; i++) {
			graphviewData[i] = new GraphViewData(xAxis[i],data[i]);
		}
	
		graphView.removeAllSeries();
		
		graphView.setManualYAxisBounds(Math.max(Helper.max(data), Math.abs(Helper.min(data))),
				-1* Math.max(Helper.max(data), Math.abs(Helper.min(data))));
		
		layout.removeAllViews();
		
		Log.i(TAG,"Adding series");
		graphView.addSeries(new GraphViewSeries(graphviewData));
		
		if (key.equals("FFT")) {
			graphView.setViewPort(20, 2000);
		}
		else {
			System.out.println(data[graphviewData.length-1]);
			graphView.setViewPort(0, xAxis[graphviewData.length-1]);
		}
		
		//Log.i(TAG,"Clearing Data and adding new");
		
		layout.addView(graphView);		
		//Log.i(TAG,"Added View");

	}

	
	private List<String> getXAxisKeys() {
		List<String> all = getAllIntentKeys();
		List<String> xAxisKeys = new ArrayList<String>();
		for (String key : all) {
			if (key.startsWith("xAxis_")) {
				xAxisKeys.add(key);
			}
		}
		return xAxisKeys;
	}

	private List<String> getYAxisKeys() {
		List<String> xKeys = getXAxisKeys();
		List<String> yAxisKeys = new ArrayList<String>();
		List<String> allIntentKeys = getAllIntentKeys();
		
		for (String xKey : xKeys) {
			if (Helper.listHas(allIntentKeys, xKey.substring(6))) { //substring will get the yKey name that's supposed to exist
				yAxisKeys.add(xKey.substring(6));
			}
			else {
				Log.e(TAG,"xAxis key " + xKey + " has no corresponding yAxis key in Intent");
			}
		}
		return yAxisKeys;
	}
	
	//could be useful later for passing lots of options through intent
	private List<String> getAllIntentBoolKeys() {
		List<String> boolKeys = new ArrayList<String>();
		for (String key : getAllIntentKeys()) {
			if (key.startsWith("bool")) {
				boolKeys.add(key);
			}
		}
		return boolKeys;
	}
	private List<String> getAllIntentKeys() {
		List<String> keys = new ArrayList<String>();
		Set<String> keySet = getIntent().getExtras().keySet();
		for(String key : keySet) {
			keys.add(key);
		}
		return keys;
	}
	
}
