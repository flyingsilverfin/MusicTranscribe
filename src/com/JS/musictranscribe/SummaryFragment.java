package com.JS.musictranscribe;

import java.util.ArrayList;
import java.util.Arrays;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

/*
 * This activity is intended for development purposes
 * Allows user to pass in a sample and get a visual and numerical summary
 * Shows: graph with audio, FFT graph, 10 top FFT frequencies, average amplitude, temporal length of sample
 * 
 */


public class SummaryFragment extends Fragment {

	private static final String TAG = "SummaryFragment";
	
	private Spinner mGraphSelectionSpinner;
	private TextView mAmplitudeTextView;
	private TextView mSampleLengthTextView;
	private TextView mFrequencySummaryTextView;
	
	private double[] mFftVals;
	private double[] mFftXAxis;
	private double[] mAudioVals;
	private double[] mAudioXAxis;
	
	private double mAmplitude;
	private double mLength;
	
	private LinearLayout mGraphLayout;
	private GraphView mGraphView;
	private GraphViewSeries mGraphData;


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_summary, container, false);
		
		
		mGraphSelectionSpinner = (Spinner) view.findViewById(R.id.summary_graph_spinner);
		//mGraphSelectionSpinner.setOnItemSelectedListener(listener);
		
		mAmplitudeTextView = (TextView) view.findViewById(R.id.amplitude_textview);
		mSampleLengthTextView = (TextView) view.findViewById(R.id.temporal_length_textview);
		mFrequencySummaryTextView = (TextView) view.findViewById(R.id.top_frequencies_textview);
		
		
		//spinner options
		ArrayList<String> options = new ArrayList<String> (Arrays.asList("fft", "audio"));

		ArrayAdapter<String> adapter = new ArrayAdapter<String> (getActivity(), 
				android.R.layout.simple_spinner_item, options);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mGraphSelectionSpinner.setAdapter(adapter);

		mGraphSelectionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
				Log.i(TAG,"selection in summary spinner");
				if (adapterView.getItemAtPosition(position).equals("fft")) {
					updateGraph(mFftXAxis, mFftVals);
				}
				else if (adapterView.getItemAtPosition(position).equals("audio")) {
					updateGraph(Helper.getEachNthInArray(mAudioXAxis,8), Helper.getEachNthInArray(mAudioVals,8));
				}
			}
			
			@Override
			public void onNothingSelected(AdapterView<?> adapterView) {
			}
		});
		
		mGraphView = new LineGraphView(getActivity(), "");		
		
		mGraphView.setScalable(false);
		mGraphView.setScrollable(true);
		mGraphView.getGraphViewStyle().setVerticalLabelsWidth(70);
		
		mGraphData = new GraphViewSeries(new GraphViewData[] {new GraphViewData(1,1)});
		
		mGraphView.addSeries(mGraphData);
		//((LineGraphView)mGraphView).setDrawBackground(false);
		
		mGraphLayout = (LinearLayout) view.findViewById(R.id.summary_fragment_graph);
		mGraphLayout.addView(mGraphView);
		
		mFftVals = getArguments().getDoubleArray("fftVals");
		mAudioVals = getArguments().getDoubleArray("audioVals");
		mFftXAxis = Helper.getFrequencyAxis(((double)2*(mFftVals.length+1)) / Helper.SAMPLING_SPEED, mFftVals.length); //length/samplingspeed = time of sample
		mAudioXAxis = Helper.range(0, 1/((double)Helper.SAMPLING_SPEED), mAudioVals.length);

		mAmplitude = getArguments().getDouble("amplitude");
		mLength = getArguments().getDouble("timeLength");
		
		setAmplitude(mAmplitude);
		setSampleLength(mLength);
		updateGraph(mFftXAxis, mFftVals);
		
		return view;
	}
	
	
	/*
	 * call this if new data has been passed into the fragment
	 */
	public void notifyDataChanged() {
		if(mGraphSelectionSpinner.getSelectedItem().equals("fft")) {
			updateGraph(mFftXAxis, mFftVals);
		}
		else if (mGraphSelectionSpinner.getSelectedItem().equals("audio")) {
			updateGraph(Helper.getEachNthInArray(mAudioXAxis,8), Helper.getEachNthInArray(mAudioVals,8));
		}
	}
	
	
	/*
	 * inputs must be of the same length
	 */
	public void updateGraph(double[] xVals, double[] yVals) {
		if (yVals.length != xVals.length) {
			Log.e(TAG, "Can't update graph with mismatching xVals and yVals");
			return;
		}
		
		GraphViewData[] data = new GraphViewData[yVals.length]; 
		
		for (int i = 0; i < data.length; i++) {
			data[i] = new GraphViewData(xVals[i], yVals[i]);
		}
		
		mGraphData.resetData(data);
				
		mGraphView.setManualYAxisBounds(Math.max(Helper.max(yVals), Math.abs(Helper.min(yVals))),
				-1* Math.max(Helper.max(yVals), Math.abs(Helper.min(yVals))));
		mGraphView.setViewPort(0, xVals[xVals.length-1]/20);
				
	}
	
	
	
	
	public void setAmplitude(double amplitude) {
		mAmplitudeTextView.setText("Average Amplitude: " + amplitude);
	}
	
	public void setSampleLength(double length) {
		mSampleLengthTextView.setText("Length in ms: " + length);
	}
	
	public void setFftVals(double[] fftVals) {
		mFftVals = fftVals;
		mFftXAxis = Helper.getFrequencyAxis(((double)2*(mFftVals.length+1)) / Helper.SAMPLING_SPEED, mFftVals.length); //length/samplingspeed = time of sample
	}
	
	public void setAudioVals(double[] audioVals) {
		mAudioVals = audioVals;
		mAudioXAxis = Helper.range(0, 1/((double)Helper.SAMPLING_SPEED), mAudioVals.length);
	}
	
	public double[] getFftVals() {
		return mFftVals;
	}
	
	public double[] getFftXAxis() {
		return mFftXAxis;
	}
	
	public double[] getAudioVals() {
		return mAudioVals;
	}
	
	public double[] getAudioXAxis() {
		return mAudioXAxis;
	}

}