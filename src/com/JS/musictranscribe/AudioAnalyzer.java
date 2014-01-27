package com.JS.musictranscribe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
 

public class AudioAnalyzer extends Audio {

	//for log
	private static final String TAG = "Audio_Analyzer";
	public static final int GRAPH_EVERY_CYCLE_IDNUM = 1; //for returning data back to activity
	
	//threading logic
	private boolean mIsDone = false;
	private boolean mIsRecordingPaused = false;
	private boolean mIsAnalysisDone = true;
	//actual threading objects
	private readerRunnable mReaderRunnable;
	private analyzerRunnable mAnalyzerRunnable;
	private Thread mReaderThread;
	private Thread mAnalyzerThread;

	//parameters for recorder
	private final int mMAX_NOTE_SECONDS = 50; //default

	
	private double[] mIntervalFreqData; //for fft data
	private short[] mCompleteRawData; // to store the max 50 seconds
	private double mAverageSampleAmplitude;
	private Matrix mSampleMatrix;
	private Matrix mDataTranspose;
	private ArrayList<Double> mRecord;

	//data file name to use
	private String mNoteMapDataFile;
	
	//library-based objects
	private DoubleFFT_1D mFFT;
	
	
	//Debug
	public boolean dGraphEveryCycle = false;
	//Dropbox
	private int dDropboxFileCounter = 0;
	
	private long dTimingVar1 = 0;
	private long dTimingVar2;

	
	//constructor
	public AudioAnalyzer(int audioSource, int samplingSpeed, boolean isMonoFormat, boolean is16BitFormat, int externalBufferSize, Context context, String mapFileName ) {

		super(audioSource,samplingSpeed, isMonoFormat, is16BitFormat, externalBufferSize, context);
		
		if (mapFileName == null) {
			Toast.makeText(context, "No file selected", Toast.LENGTH_SHORT);
			Log.e(TAG,"No file name");
		}
		mCompleteRawData = new short[mMAX_NOTE_SECONDS * getSamplingSpeed()];

		mIntervalFreqData = new double[(externalBufferSize/2) - 1]; 
		mFFT = new DoubleFFT_1D(mExternalBufferSize);

		Log.i(TAG,"\tinitializing runnables and threads");
		mReaderRunnable = new readerRunnable();
		mAnalyzerRunnable = new analyzerRunnable();
		mReaderThread = new Thread(mReaderRunnable);
		mAnalyzerThread = new Thread(mAnalyzerRunnable);
		mReaderRunnable.setParallelThread(mAnalyzerThread);
		mAnalyzerRunnable.setParallelThread(mReaderThread);

		
		Log.i(TAG, "Getting data from file");

		mNoteMapDataFile = mapFileName;
		
		if (mNoteMapDataFile != null) {
			Log.d(TAG,"No data file given, skipping matrix initializations");
			HashMap<Integer, Double[]> noteSpectraMap = Helper.getNoteSpectraFromFile(context.getApplicationContext(), mNoteMapDataFile);
			Integer[] noteNums = noteSpectraMap.keySet().toArray(new Integer[1]);
						
			Log.i(TAG, "Building matrices and RREF");
			Matrix dataMatrix = new Matrix(noteSpectraMap.get(noteNums[0]).length, noteNums.length); //map must have values of same length
			Arrays.sort(noteNums);
			Log.i(TAG,"Notes in reference data: ");
			Helper.printArray(noteNums);
			for (int i = 0; i < noteNums.length; i++) {
				dataMatrix.writeCol(i, noteSpectraMap.get(noteNums[i]));
			}
			mDataTranspose = dataMatrix.getTranspose();
			Matrix sqrMatrix = mDataTranspose.multOnLeftOf(dataMatrix);
			mRecord = sqrMatrix.RREF();
			
			mSampleMatrix = new Matrix(mIntervalFreqData.length, 1);
		}


	}


	//returns true if succeeded, false if could not start
	public boolean startRecording() {
		if (mNoteMapDataFile == null) {
			Log.e(TAG,"No data file given. Aborting recording start");
			return false;
		}
		startAudioRecording();
		if (isRecorderRunning()) {
			mIsRecordingPaused = false;
			mIsDone = false;
		}
		else {
			mIsDone = true;
			return false;
		}
		(new Thread() { 
			public void run() { 			
				cleanBuffer();
				Log.i(TAG,"starting reader and analyzer threads");
				mReaderThread.start();
				mAnalyzerThread.start();
			}
		}).start();	
		
		return true;
	}

	//for starting from pause
	public void resumeRecording() {
		(new Thread() {
			public void run() {
				startAudioRecording();
				cleanBuffer();
				if (isRecorderRunning()){
					mIsRecordingPaused = false;
				}
				else {
					mIsRecordingPaused = true;
					return; //stop because recording failed to start
				}
				Log.i(TAG,"Interrupting threads to resume");
				mReaderThread.interrupt();
				mAnalyzerThread.interrupt();
			}
		}).start();
	}
	

	public void pauseRecording() {
		mIsRecordingPaused = true; 
		while (!isReaderThreadPaused()) {} //wait for reader to actually pause before stopping recorder -- weird conflict otherwise
		stopAudioRecording();
	}


	public void finishRecording() {
		mIsDone = true;
		mIsRecordingPaused = true;
		stopAudioRecording();
	}


	@Override
	/*Functionality from superclass:
	* 	Records from microphone
	* 	defaults into the mAudioData short array
	* 	returns how many have been read
	* Added:
	* 	Updates mAverageSampleAmplitude as well
	* Removed:
	* 	No more logging how many data were read
	*/
	public int recordAudio() {
		int n = mRecorder.read(mRawAudioData, 0, mRawAudioData.length); // copy out new data
		mAverageSampleAmplitude = Helper.sumArrayInAbs(mRawAudioData)/n;
		return n;
	}
	
	/*
	 * This is called after the FFT is run
	 * 
	 */
	private void analyze() {
		mSampleMatrix.writeCol(0, mIntervalFreqData);
		Matrix copy = mDataTranspose.multOnLeftOf(mSampleMatrix);
		copy.modifyByRecord(mRecord);
		
		Log.i(TAG,"SOLUTION: ");
		copy.printCol(0);
		
	    /*
     	if (isDBLoggedIn() && isDBDataUploadEnabled()) {
			Log.i(TAG,"attempting to write file");
			Helper_Dropbox.putFile("/sdcard/datafile"+dDropboxFileCounter+".txt", getIntervalFreqData(), mDBApi);
			dDropboxFileCounter++;
		} 
		*/
		
		if (dGraphEveryCycle) {
			pauseRecording(); //somehow this is pausing this thread (or something) too!!!
			Log.i(TAG,""+dGraphEveryCycle);
			makeGraphs(getRawAudioArrayAsDoubles(), mIntervalFreqData);
		}
	}		

	
	/*
	 * This creates 
	 */
	public void makeGraphs(double[] rawAudioData, double[] frequencyData) {
		Log.i(TAG,"Preparing Graphs");
		
		Intent graphIntent = new Intent(mContext, GraphActivity.class);

		graphIntent.putExtra("xAxis_FFT", getHertzAxis(frequencyData.length));
		graphIntent.putExtra("FFT", frequencyData);

		graphIntent.putExtra("xAxis_rawAudio", getTimeAxisInMs(rawAudioData.length));
		graphIntent.putExtra("rawAudio", getRawAudioArrayAsDoubles());

		graphIntent.putExtra("dGraphEveryCycle", isGraphEveryCycle());
		
		((Activity) mContext).startActivityForResult(graphIntent, GRAPH_EVERY_CYCLE_IDNUM);
	}
	



	//get time axis of the data in microseconds
	public double[] getTimeAxisInMs(int numRawData) {
		double[] timeAxis = new double[numRawData];
	
		for (int i = 0; i < numRawData; i++) {
			timeAxis[i] = 1000*1000*((float)i/getSamplingSpeed());
		}
		return timeAxis;
	}

	/*
	 * Explanation: 
	 * lowest frequency = 1 cycle in the full length (time) of the data
	 * since the number of FFT coefficients = ( 1/2 of the number of audio data ) - 1
	 * the -1 is there since we give up one datapoint for summary data in the FFT
	 * 
	 * So basically what we do is:
	 * samplingSpeed/((numFreqData+1) *1) = lowest frequency possible
	 * then the next higher frequency in the FFT is just 2*^, 3*^, 4*above etc 
	 */
	//returns double[] of hertz axis of the FFT
	public double[] getHertzAxis(int numFreqData) {
		double[] frequencyAxis = new double[numFreqData];
		for (int i = 0; i < numFreqData; i++) {
			frequencyAxis[i] = (i+1) * getSamplingSpeed() / ((numFreqData +1)*2);			
			//other way of looking at it: basic hertz = 1/time = 1/(numData/SamplingSpeed)	
		}
		return frequencyAxis;
	}

/*

Need to think about this threading more... right now it's essentially waiting for one to finish before starting the next, don't need threads for that!
What is really wanted is for the reader thread to read, and as soon as its done pass on those values to the other thread for analysis, which happens while the next set of data is being recorded.

The only time the fft thread needs to wait is the very first time, since it likely takes longer than the reader thread anyway. My most basic idea is to have a counter in both threads. Record, copy data into another array for use in the FFT. Interrupt FFT thread, which is sleeping right now. FFT thread starts work on data copy. Increment counter in reader as soon as done interrupting FFT thread. Read again into original array. If recorder counter is ahead of the analyzer counter (by 1), pause with Thread.sleep(). As soon as FFt thread is done analyzing data copy, interrupt reader thread to continue and go to sleep until the Reader thread has copied the new data into array copy. In this setup, only one thread should be limiting and waiting, not both at different times.

Boolean version of above idea:
	Record data, copy data into another array for use in the FFT. Interrupt FFT thread, which is sleeping right now. FFT thread starts work on data copy. Update isAhead boolean in reader as soon as done interrupting FFT thread. Read again into original array. Pause with Thread.sleep(). As soon as FFt thread is done analyzing data copy, interrupt reader thread to continue and go to sleep until the Reader thread has copied the new data into array copy. In this setup, only one thread should be limiting and waiting, not both at different times.

^the problem with this setup is that it assumes the that the reader thread will finish first. Need to think about how to fix that it works for either thread finishing ahead of the other and doesn't break...
(none of above are implemented as of now)

other random TODO's:
	figure out how to put the preferences for the program in a xml file or some other file so it's not in recordActivity.
*/	


//NOT EFFICIENT!!!! SEE ABOVE!!! (maybe, think about it some more)

	//thread the reads data
	private class readerRunnable implements Runnable {
		private boolean mIsPaused;
		private Thread mAnalyzerThread;

		public readerRunnable() {
			//it doesn't actually matter what this starts as
			//it's only used in the pause() method
			//which sets it to true to start with anyway...
			//pause() method should only be run from within run() function
			mIsPaused = true;
		}

		public void run() {
			if (mAnalyzerThread == null) {
				Log.e(TAG, "No parallel thread reference in analyzerRunnable, use setParallelThread(thread) for this");
			}	
			int numRecorded;
			while (!mIsDone) {
				while (!mIsRecordingPaused) { //for external interrupting
					if (mIsAnalysisDone) { //for parallel thread interrupting

						Log.i(TAG, "\tLast Cycle time (micros):                " + (System.nanoTime() - dTimingVar1)/1000);
						dTimingVar1 = System.nanoTime();

						numRecorded = recordAudio(); 	//defined in superclass
														//updates mRawAudioData
						
						long dTmp = System.nanoTime();
						Log.i(TAG, "  Time to read from recorder:       " + (dTmp -dTimingVar1)/1000);

						mIsAnalysisDone = false;
						mAnalyzerThread.interrupt();
						
						Log.i(TAG, "  Other overhead READ thread:       " + (System.nanoTime()-dTmp)/1000);

					}
					pause();
				}
				pause();
			}
		}
		public void pause() {
			mIsPaused = true;
			while (true){
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e){
					mIsPaused = false;
					return;
				}
			}
		}

		public void setParallelThread(Thread analyzerThread) {
			mAnalyzerThread = analyzerThread;
		}
	
		public boolean isPaused() {
			return mIsPaused;
		}
		
		//Debug
		public String getAnalyzerThread() {
			return mAnalyzerThread.toString();
		}
	}
	
	
	//thread that does analysis
	private class analyzerRunnable implements Runnable {
		private boolean mIsPaused;
		private Thread mReaderThread;

		public analyzerRunnable() {
			//it doesn't actually matter what this starts as
			//it's only used in the pause() method
			//which sets it to true to start with anyway...
			//pause() method should only be run from within run() function
			mIsPaused = true;
		}

		public void run() {
			if (mAnalyzerThread == null) {
				Log.e(TAG, "No parallel thread reference in analyzerRunnable, use setParallelThread(thread) for this");
			}			
			while (!mIsDone) {
				while (!mIsRecordingPaused) { //for external interrupting
					if (!mIsAnalysisDone) { //for parallel thread interrupting
						updateIntervalFreqData(); //updates mIntervalFreqData

						analyze();
						mIsAnalysisDone = true;
						mReaderThread.interrupt();
					}
					pause();			
				}
				pause();
			}
		}

		public void pause() {
			mIsPaused = true;	
			while (true) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					mIsPaused = false;
					return;		
				}	
			}
		}

		public void setParallelThread(Thread readerThread) {
			mReaderThread = readerThread;
		}

		public boolean isPaused() {
			return mIsPaused;
		}	
		
		//Debug		
		public String getReaderThread() {
			return mReaderThread.toString();
		}	
	}

	
	/*
	 * This is the actual FFT function
	 * Takes the audioData and updates returns a double[]
	 */
	public double[] getFreqData(short[] audioData) {
		double[] audioDataCopy = getRawAudioArrayAsDoubles(); //get copy of data

		mFFT.realForward(audioDataCopy);

		double[] fftCoeffs = new double[(audioDataCopy.length / 2) - 1];

		for (int i = 2; i < audioData.length; i += 2) {
			fftCoeffs[(i - 2) / 2] = Math.sqrt(Math.pow(audioDataCopy[i], 2)
					+ Math.pow(audioDataCopy[i + 1], 2));
		}
		return fftCoeffs;
	}

	
	public double[] getIntervalFreqData() {
		return mIntervalFreqData;
	}


	//calculate the FFT and save it to the correct private variable
	public void updateIntervalFreqData() {
		mIntervalFreqData = getFreqData(getRawAudioArray());
	}

	public boolean isGraphEveryCycle() {
		return dGraphEveryCycle;
	}
	

	//are the two threads truly paused, and therefore a full cycle of analysis done
	public boolean isThreadingPaused() {
		if (mReaderRunnable.isPaused() && mAnalyzerRunnable.isPaused()) {
			return true;
		}
		return false;
	}

	public boolean isReaderThreadPaused() {
		if (mReaderRunnable.isPaused()) {
			return true;
		}
		return false;
	}
	
	public double getCurrentAvAmplitude() {
		return mAverageSampleAmplitude;
	}
	
	public String getDataFileInUse() {
		return mNoteMapDataFile;
	}
	
	public void setNoteMapDataFile(String filename, Context context) {
		
		Log.i(TAG,"New filename is: " + filename);
		if (!isThreadingPaused()) {
			Log.i(TAG,"Pausing recording. Having to do this is bad coding. Activity shouldn't be allowing to change reference data in the middle of recording... (unless this is a new feature)");
			pauseRecording();
		}
		mNoteMapDataFile = filename;
		
		HashMap<Integer, Double[]> noteSpectraMap = Helper.getNoteSpectraFromFile(context.getApplicationContext(), mNoteMapDataFile);
		Integer[] noteNums = noteSpectraMap.keySet().toArray(new Integer[1]);
					
		Log.i(TAG, "Rebuilding matrices and RREF");
		Matrix dataMatrix = new Matrix(noteSpectraMap.get(noteNums[0]).length, noteNums.length); //map must have values of same length
		Arrays.sort(noteNums);
		Log.i(TAG,"Notes in reference data: ");
		Helper.printArray(noteNums);
		for (int i = 0; i < noteNums.length; i++) {
			dataMatrix.writeCol(i, noteSpectraMap.get(noteNums[i]));
		}
		mDataTranspose = dataMatrix.getTranspose();
		Matrix sqrMatrix = mDataTranspose.multOnLeftOf(dataMatrix);
		mRecord = sqrMatrix.RREF();
		
		mSampleMatrix = new Matrix(mIntervalFreqData.length, 1);
		
		Log.i(TAG,"Ready to resume or start");
		
	}
}
