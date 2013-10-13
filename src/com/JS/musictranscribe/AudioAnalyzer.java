package com.JS.musictranscribe;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;

public class AudioAnalyzer {

	//for log
	private static final String TAG = "Audio_Analyzer";
	public static final int GRAPH_EVERY_CYCLE_IDNUM = 1;
	
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
	private int mSamplingSpeed;
	private int mInternalBufferSize;
	private int mExternalBufferSize;
	private int mAudioSource;
	private boolean mIsMonoFormat; 		//for more user friendliness
	private boolean mIs16BitFormat;		//for more user friendliness
	private int mChannelConfig;			//for actual system
	private int mAudioDataFormat;		//for actual system

	//for flushing the recorder out before starting to record actual data
	private final int mNUM_BUFFER_STARTUP_FLUSHES = 10;
	
	private short[] mIntervalRawData; 	// to copy out the internal buffer 
	private double[] mIntervalFreqData; //for fft data

	private short[] mCompleteRawData; // to store the max 50 seconds

	//Incoming context
	private Context mContext;
	
	//library-based objects
	private DoubleFFT_1D mFFT;
	private AudioRecord mRecorder;
	

	//debugging
	public boolean dGraphEveryCycle = false;
	private boolean dDBDataUpload = false;
	private boolean dDBLoggedIn = false;
	private DropboxAPI<AndroidAuthSession> mDBApi; //this will only be initialized if setDBLoggedIn(true) is called
	private int dDropboxFileCounter = 0;

	
	//constructor
	public AudioAnalyzer(int audioSource, int samplingSpeed, boolean isMonoFormat, boolean is16BitFormat, int externalBufferSize, Context context ) {

		setInternalBufferSize(getInternalBufferSize(samplingSpeed,
								isMonoFormat, is16BitFormat));

		setSamplingSpeed(samplingSpeed);
		setExternalBufferSize(externalBufferSize);
		setAudioSource(audioSource);
		setChannelConfig(isMonoFormat);
		setAudioDataFormat(is16BitFormat);
		
		logInternalBufferCapacity();
		

		setContext(context);
		
		
		Log.i(TAG,"\tTesting time: " + System.nanoTime());
		Log.i(TAG,"\tSubsequent time: " + System.nanoTime());
		
		Log.i(TAG, "creating new recorder");
		mRecorder = new AudioRecord(mAudioSource, mSamplingSpeed, mChannelConfig, mAudioDataFormat, mInternalBufferSize);
		
		
		mCompleteRawData = new short[mMAX_NOTE_SECONDS * mSamplingSpeed];

		mIntervalRawData = new short[mExternalBufferSize];
		mIntervalFreqData = new double[mExternalBufferSize/2]; //same size since we got two components
															//for each freq, to be combined into a single set

		mFFT = new DoubleFFT_1D(mExternalBufferSize);

		Log.i(TAG,"initializing runnables and threads");
		mReaderRunnable = new readerRunnable();
		mAnalyzerRunnable = new analyzerRunnable();
		mReaderThread = new Thread(mReaderRunnable);
		mAnalyzerThread = new Thread(mAnalyzerRunnable);
		mReaderRunnable.setParallelThread(mAnalyzerThread);
		mAnalyzerRunnable.setParallelThread(mReaderThread);

		
		Log.i(TAG, "Initializing dropbox API settings");

	}



	//returns true if succeeded, false if could not start
	public boolean startRecording() {
		try {
			mRecorder.startRecording();
			mIsRecordingPaused = false;
			mIsDone = false;
		} catch (Throwable e) {
			Log.e(TAG, "Recorder failed to start");
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
				cleanBuffer();
				mIsRecordingPaused = false;
				try {
					mRecorder.startRecording();
				} catch (Throwable e) {
					Log.e(TAG,"Failed to resume recorder");
					return;
				}
				Log.i(TAG,"Interrupting threads to resume");
				mReaderThread.interrupt();
				mAnalyzerThread.interrupt();
			}
		}).start();
	}
	

	public void pauseRecording() {
		mIsRecordingPaused = true; 
		while (!isReaderPaused()) {} //wait for reader to actually pause before stopping recorder -- weird conflict otherwise
		mRecorder.stop();
	}


	public void finishRecording() {
		mIsDone = true;
		mIsRecordingPaused = true;
		mRecorder.stop();
	}

	//flush the internal buffer of the recorder (get rid of noise)
	public void cleanBuffer() {
		for ( int i = 0; i < mNUM_BUFFER_STARTUP_FLUSHES; i++) {
			mRecorder.read(new short[mInternalBufferSize],0,mInternalBufferSize);
		}
		Log.i(TAG, "wiped recorder: " + mNUM_BUFFER_STARTUP_FLUSHES+ " times\n");
	}	


	
	/*
	 * This is called after the FFT is run
	 * 
	 */
	private void analyze() {
		if (isDBLoggedIn() && isDBDataUploadEnabled()) {
			Log.i(TAG,"attempting to write file");
			DropboxHelper.putFile("/sdcard/datafile"+dDropboxFileCounter+".txt", getIntervalFreqData(), mDBApi);
			dDropboxFileCounter++;
		}
		
		if (dGraphEveryCycle) {
			pauseRecording(); //somehow this is pausing this thread (or something) too!!!
			Log.i(TAG,""+dGraphEveryCycle);
			makeGraphs(getRawAudioDataDoubles(), mIntervalFreqData);
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
		graphIntent.putExtra("rawAudio", rawAudioData);

		graphIntent.putExtra("dGraphEveryCycle", isGraphEveryCycle());
		
		((Activity) mContext).startActivityForResult(graphIntent, GRAPH_EVERY_CYCLE_IDNUM);
	}
	



	//get time axis of the data in microseconds
	public double[] getTimeAxisInMs(int numRawData) {
		double[] timeAxis = new double[numRawData];
	
		for (int i = 0; i < numRawData; i++) {
			timeAxis[i] = 1000*1000*((float)i/mSamplingSpeed);
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
			frequencyAxis[i] = (i+1) * mSamplingSpeed / ((numFreqData +1)*2);			
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
			mIsPaused = false;
		}

		public void run() {
			if (mAnalyzerThread == null) {
				Log.e(TAG, "No parallel thread reference in analyzerRunnable, use setParallelThread(thread) for this");
			}	
			int numRecorded;
			while (!mIsDone) {
				while (!mIsRecordingPaused) { //for external interrupting
					if (mIsAnalysisDone) { //for parallel thread interrupting
						numRecorded = mRecorder.read(mIntervalRawData, 0, mIntervalRawData.length); // copy out new data
						Log.d(TAG,"\tNumber of data recorded: " + numRecorded + "\n mIsRecordingPaused:" + mIsRecordingPaused);
						mIsAnalysisDone = false;
						mAnalyzerThread.interrupt();
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
			mIsPaused = false;
		}

		public void run() {
			if (mAnalyzerThread == null) {
				Log.e(TAG, "No parallel thread reference in analyzerRunnable, use setParallelThread(thread) for this");
			}			
			while (!mIsDone) {
				while (!mIsRecordingPaused) { //for external interrupting
					if (!mIsAnalysisDone) { //for parallel thread interrupting
						updateIntervalFreqData();

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
		double[] audioDataCopy = getRawAudioDataDoubles(); //get copy of data

		mFFT.realForward(audioDataCopy);

		double[] fftCoeffs = new double[(audioDataCopy.length / 2) - 1];

		for (int i = 2; i < audioData.length; i += 2) {
			fftCoeffs[(i - 2) / 2] = Math.sqrt(Math.pow(audioDataCopy[i], 2)
					+ Math.pow(audioDataCopy[i + 1], 2));
		}
		return fftCoeffs;
	}

	//get copy of Audio Data -- not needed right now, could be used later
	public short[] getRawAudioDataShorts() {
		short[] tmpAudioData = new short[mIntervalRawData.length];
		for (int i = 0; i < mIntervalRawData.length; i++) {
			tmpAudioData[i] = mIntervalRawData[i];
		}
		return tmpAudioData;
	}
	
	public double[]  getRawAudioDataDoubles() {
		double[] tmpAudioData = new double[mIntervalRawData.length];
		for ( int i = 0; i < mIntervalRawData.length; i++) {
			tmpAudioData[i] = (double) mIntervalRawData[i];
		}
		return tmpAudioData;
	}
	
	
	public short[] getIntervalRawData() {
		return mIntervalRawData;
	}
	
	public double[] getIntervalFreqData() {
		return mIntervalFreqData;
	}


	//calculate the FFT and save it to the correct private variable
	public void updateIntervalFreqData() {
		mIntervalFreqData = getFreqData(mIntervalRawData);
	}

	//get an appropriate internal recorder buffer size
	private int getInternalBufferSize(int samplingSpeed, boolean isMono, boolean is16Bit) {
		int size = 100+AudioRecord.getMinBufferSize(samplingSpeed, 
				isMono ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO, 
				is16Bit ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT);
		if (size%2 == 1) {
			Log.d(TAG,"Calculated Internal Buffer is odd, incrementing");
			size++;
		}
		return size;
	}
	
	private void setInternalBufferSize(int internalBufferSize) {
		mInternalBufferSize = internalBufferSize;
	}
	

	private void setSamplingSpeed(int SamplingSpeed) {
		mSamplingSpeed = SamplingSpeed;
	}

	private void setExternalBufferSize(int externalBufferSize) {
		mExternalBufferSize = externalBufferSize;
	}
	
	private void setAudioSource(int AudioSource) {
		mAudioSource = AudioSource;
	}

	private void setChannelConfig(boolean isMono) {
		mIsMonoFormat = isMono;
		mChannelConfig = isMono ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO;
	}

	private void setAudioDataFormat(boolean is16Bit) {
		mIs16BitFormat = is16Bit;
		mAudioDataFormat = is16Bit ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT;
	}
	
	public void setContext(Context c) {
		mContext = c;
	}
	
	public void setDBDataUpload(boolean enabled) {
		dDBDataUpload = enabled;
	}
	
	public void setDBLoggedIn(boolean loggedIn) {
		dDBLoggedIn = loggedIn;
		if (loggedIn) {
			mDBApi = DropboxHelper.getDBSession(mContext);
		}
		else {
			mDBApi = null;
		}
	}
	
	public boolean isDBDataUploadEnabled() {
		return dDBDataUpload;
	}
	
	public boolean isDBLoggedIn() {
		return dDBLoggedIn;
	}

	public boolean isGraphEveryCycle() {
		return dGraphEveryCycle;
	}
	
	//is the actual AudioRecord recorder paused
	public boolean isRecorderRunning() {
		if (mRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
			return true;
		}
		else {
			return false;
		}
	}

	//are the two threads truly paused, and therefore a full cycle of analysis done
	public boolean isThreadingPaused() {
		if (mReaderRunnable.isPaused() && mAnalyzerRunnable.isPaused()) {
			return true;
		}
		return false;
	}

	public boolean isReaderPaused() {
		if (mReaderRunnable.isPaused()) {
			return true;
		}
		return false;
	}

	public void logInternalBufferCapacity() {
		Log.i(TAG,"Internal Buffer size: " +  mInternalBufferSize + "\n");
		int storageTime = (int)(1000.0*mInternalBufferSize/((float) (mIs16BitFormat ? 2 : 1) * mSamplingSpeed));
		Log.i(TAG, "This will hold " + storageTime + " ms\n");
	}
	
	
	
	

	
	
}
