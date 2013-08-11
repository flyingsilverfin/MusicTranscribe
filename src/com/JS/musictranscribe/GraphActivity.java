package com.JS.musictranscribe;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
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
		ArrayAdapter<String> adapter = new ArrayAdapter<String> (this, 
				android.R.layout.simple_spinner_item, intentExtraKeys);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		graphChoicesSpinner.setAdapter(adapter);
		
		//set up submit button
		submitGraphChoiceButton = (Button) findViewById(R.id.submit_graph_choice_button);
		submitGraphChoiceButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				updateGraph(String.valueOf(graphChoicesSpinner.getSelectedItem()));
			}
		});
		
		//set up graphView
		graphView = new LineGraphView(this, "GRAPH");
		layout = (LinearLayout) findViewById(R.id.graph);
		graphView.setScrollable(true);
		((LineGraphView)graphView).setDrawBackground(true);
		graphView.setBackgroundColor(Color.BLACK);
		
	}

	private String[] getIntentKeysArray() {
		Set<String> keySet = getIntent().getExtras().keySet();
		String[] keys = new String[keySet.size()];
		int counter = 0;
		for (String key : keySet) {
			if (key.startsWith("xAxis")) {
				keys[counter] = key;
				counter++;
			}
		}
		return keys;
	}

	private List<String> getIntentKeysList() {
		return Arrays.asList(getIntentKeysArray());
	}

	
	private void updateGraph(String key) {
		double[] xAxis = getIntent().getDoubleArrayExtra(key);
		double[] data = getIntent().getDoubleArrayExtra(key.substring(6)); //cut out the xAxis_
		graphviewData = new GraphViewData[Math.min(xAxis.length, data.length)]; //min incase the lengths don't match up, cut to shorter datalength
		
		for (int i = 0; i < graphviewData.length; i++) {
			graphviewData[i] = new GraphViewData(xAxis[i],data[i]);
		}
		
		graphView.addSeries(new GraphViewSeries(graphviewData));
		graphView.setViewPort(0, graphviewData.length);
		
		layout.addView(graphView);		
	}
}
