package com.JS.musictranscribe;

import java.util.ArrayList;

import android.content.Context;
import android.util.Log;

public class AudioCollector extends Audio {

	private final static String TAG = "AudioCollector";
	
	public AudioCollector(int audioSource, int samplingSpeed, boolean isMonoFormat, boolean is16BitFormat, int externalBufferSize, Context context) {
		super(audioSource,samplingSpeed, isMonoFormat, is16BitFormat, externalBufferSize, context);
		
		
		
		
		
	}
	
	public double[][] getNSamples(int n) {
		double[][] sampleArrays = new double[n][getExternalBufferSize()];
		
		if (!startAudioRecording()) { //try to start recorder,
										//if recorder failed to start then 
										//exit early. Function also logs if failed
			return null;
		}
		
		//try { Thread.sleep(171); } catch (InterruptedException e) {}
		
		
		long tmpT;
		
		long a = System.nanoTime();
		recordAudio();
		Log.i(TAG, "CLeaning read took "+  (System.nanoTime() - a)/1000 + "us");
		long t = System.nanoTime();

		for (int i = 0; i < n; i++) {
			
			tmpT = System.nanoTime();
			
			recordAudio(); //updates mRawAudioData, also accessible through getRawAudioArray()
			//for (int j = 0; j < getExternalBufferSize(); j++) {
			//	sampleArrays[i][j] = mRawAudioData[j];
			//}
			Log.i(TAG,"Updating audio data buffer and saving it to another double array took \n" + (System.nanoTime()-tmpT)/1000 + " us");
			
		}
		
		Log.i(TAG,"Total time for " + n + " recording is " + (System.nanoTime()-t)/1000 + " us");
		
		
		stopAudioRecording();
		
		return sampleArrays;
	}
	
	/*
	 * attempts to read as many times as possible within given microSeconds 
	 * --do i need microsecond accuracy?
	 */
	public double[][] getSamplesFor(int microSeconds) {
		
		if (!startAudioRecording()) { //if recorder failed to start then exit early
			return null;
		}
				 
		ArrayList<double[]> samples = new ArrayList<double[]>(); //must ensure inside arrays are all the same size ourselves
		long startTime = System.nanoTime()/1000;
		while (System.nanoTime()/1000 - startTime < microSeconds) {
			recordAudio();
			samples.add(getRawAudioArrayAsDoubles());
		}
		
		stopAudioRecording();
		//does this work????
		return (double[][]) samples.toArray(new double[samples.size()][]);
	}
	
	
	public void writeSamplesToDropbox(double[][] sampleArrays, String fileName) {
		/*
		 * I have to do it this rather unwieldly way because it's not possibly to do network actions on main thread
		 * also have to do the weird passing around because I can't just define an anonymous inner class within this scope
		 * therefore needed a way to pass in sampleArrays and fileNameStart
		 */
		class r implements Runnable{
			double[][] innerSampleArrays;
			String innerFileName;

			public void run() {
				if(isDBLoggedIn() && isDBDataUploadEnabled()){
					Helper_Dropbox.putFile("/sdcard/"+innerFileName+".txt", innerSampleArrays, mDBApi);
				}
				else {
					Log.e(TAG,"DB not logged in or disabled, Logged in: " + 
							isDBLoggedIn() + "; DB enabled: " + isDBDataUploadEnabled());
				}
			}
			
			public r(double[][] sampleArrays, String fileName) {
				innerSampleArrays = sampleArrays;
				innerFileName = fileName;
			}
			
		}
		
		r uploadRunnable = new r(sampleArrays, fileName);
		new Thread(uploadRunnable).start();
	}
	
}
