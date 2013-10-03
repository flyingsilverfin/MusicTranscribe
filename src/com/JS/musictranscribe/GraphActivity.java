package com.JS.musictranscribe;

import java.util.Arrays;
import java.util.Collections;
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

	private List<String> intentExtraKeys;
	private Spinner graphChoicesSpinner;
	private Button submitGraphChoiceButton;
	private Button analyzeGraphButton;
	private GraphView graphView;
	private LinearLayout layout;
	private GraphViewData[] graphviewData;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_graph);
		
		//set up the Spinner with graph choices
		graphChoicesSpinner = (Spinner) findViewById(R.id.graph_spinner);
		intentExtraKeys = getIntentKeysList();
		for (int i = 0; i < intentExtraKeys.size(); i++) {
			Log.i(TAG,"choice: " + intentExtraKeys.get(i));
		}
		ArrayAdapter<String> adapter = new ArrayAdapter<String> (this, 
				android.R.layout.simple_spinner_item, intentExtraKeys);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		graphChoicesSpinner.setAdapter(adapter);
		
		Log.i(TAG,"Spinner set up");
		
		//set up submit button
		submitGraphChoiceButton = (Button) findViewById(R.id.submit_graph_choice_button);
		submitGraphChoiceButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				updateGraph(String.valueOf(graphChoicesSpinner.getSelectedItem()));
			}
		});
		
		Log.i(TAG,"Wired Submit Button");
		
		//set up percentage button
		analyzeGraphButton = (Button) findViewById(R.id.percentb);
		analyzeGraphButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(GraphActivity.this, PercentData.class);
				String key2 = String.valueOf(graphChoicesSpinner.getSelectedItem());
				intent.putExtra("key", key2);
				intent.putExtra("xFFT", getIntent().getDoubleArrayExtra(key2));
				intent.putExtra("data", getIntent().getDoubleArrayExtra(key2.substring(6)));
				startActivity(intent);
				
			}
		});

		
		//set up graphView
		graphView = new LineGraphView(this, "");
		graphView.setScalable(false);
//		graphView.setBackgroundColor(Color.WHITE);
		graphView.setBackgroundColor(Color.BLACK);
		graphView.setScrollable(true);
		
		((LineGraphView)graphView).setDrawBackground(false);
		
		layout = (LinearLayout) findViewById(R.id.graph);

		Log.i(TAG,"Graph view set up");

	}

	private String[] getIntentKeysArray() {
		Set<String> keySet = getIntent().getExtras().keySet();
		String[] keys = new String[keySet.size()/2]; // 1/2 because the extras always come in pairs, we only want the x-axis ones
		int counter = 0;
		for (String key : keySet) {
			if (key.startsWith("xAxis")) {
				keys[counter] = key;
				counter++;
			}
		}
		
		
	/*	for (int i = 0; i < 15; i++) {
			Log.i(TAG,"")
		}
		Log.i(TAG, keys[0] + ": " + getIntent().getDoubleArrayExtra(keys[0]).toString());
		Log.i(TAG, keys[1] + ": " + getIntent().getDoubleArrayExtra(keys[1]).toString());
*/
		return keys;
	}

	private List<String> getIntentKeysList() {
		return Arrays.asList(getIntentKeysArray());
	}

	
	private void updateGraph(String key) {
		Log.i(TAG,"updating graph for key" + key + ", " + key.substring(6));

		double[] xAxis = getIntent().getDoubleArrayExtra(key);
		double[] data = getIntent().getDoubleArrayExtra(key.substring(6)); //cut out the xAxis_
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
		graphView.setViewPort(0, graphviewData.length);
		
		//Log.i(TAG,"Clearing Data and adding new");
		
		layout.addView(graphView);		
		//Log.i(TAG,"Added View");

	}
}
