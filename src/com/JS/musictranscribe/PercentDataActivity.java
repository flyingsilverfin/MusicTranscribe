package com.JS.musictranscribe;

import java.util.ArrayList;
import java.util.Collections;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

public class PercentDataActivity extends Activity {
	double[] xaxis;
	double[] data;
	double[][] fft = new double[2048][2];
	ArrayList<Double> datal = new ArrayList<Double>();
	double[][] ordered = new double[2048][2];
	String key;
	private String update_string;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_percent_data);
		//key = getIntent().getStringExtra("key");
		xaxis = getIntent().getDoubleArrayExtra("xFFT");
		data = getIntent().getDoubleArrayExtra("data");		
		/* Debugging code
		for(double x : xaxis)
			Log.d("PercentData", String.valueOf(x));
		
		for(double y : data)
			Log.d("PercentData", String.valueOf(y));
		
		Log.d("PercentData", "Array lengths: " + xaxis.length + " " + data.length);
		*/
		
		for (int i = 0; i < xaxis.length; i++){
			fft[i][0] = xaxis[i];
			fft[i][1] = data[i];
			//Log.d("PercentData", "Array: " + fft[i][0] + ", " + fft[i][1]);
			datal.add(data[i]);
		}

		Collections.sort(datal, Collections.reverseOrder());
		
		int amount = 10; //number of points to eval
		for (int i = 0; i < amount; i++){
			for(int j = 0; j < 2048; j++){
				if(datal.get(i).equals(fft[j][1]) && ordered[i][0] == 0){
					ordered[i][0] = fft[j][0];
					ordered[i][1] = datal.get(i);
					j = 2048;
				}
			}
		}
		
		for(int i = 0; i < amount; i++){
			Log.d("PercentData", "Ordered: " + ordered[i][0] + ", " + ordered[i][1]);
		}
		
		String percents = "";
		
		for(int i = 0; i < amount; i++){
			percents += (i+1) + " = " + ordered[i][1] + " at " + ordered[i][0] + " Hz (" + ((double)((int)(10000*(ordered[i][1]/ordered[0][1]))))/100 + "%)\n";
		}
		
		((TextView)findViewById(R.id.percentID)).setText(percents);
		
		
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.percent_data, menu);
		return true;
	}

}
