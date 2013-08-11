package com.JS.musictranscribe;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.util.Log;

public class AudioAnalyzer {

	private static final String TAG = "Audio_Analyzer";
	
	private boolean mIsDone = false;
	private boolean mIsRecordingPaused = false;
	private boolean mIsAnalysisDone = true;

	//parameters
	private final int mMAX_NOTE_SECONDS = 50; //default
	private int mSamplingSpeed;
	private int mInternalBufferSize;
	private int mExternalBufferSize;
	private int mAudioSource;
	private boolean mIsMonoFormat; 		//for more user friendliness
	private boolean mIs16BitFormat;		//for more user friendliness
	private int mChannelConfig;			//for actual system
	private int mAudioDataFormat;		//for actual system

	
	private final int mNUM_BUFFER_STARTUP_FLUSHES = 10;
	
	private short[] mIntervalRawData; 	// to copy out the internal buffer 
	private double[] mIntervalFreqData; //for fft

	private short[] mCompleteRawData; // to store the max 50 seconds

	private readerRunnable mReaderRunnable;
	private analyzerRunnable mAnalyzerRunnable;
	private Thread mReaderThread;
	private Thread mAnalyzerThread;

	private DoubleFFT_1D mFFT;
	private AudioRecord mRecorder;
	
	public AudioAnalyzer(int audioSource, int samplingSpeed, boolean isMonoFormat, boolean is16BitFormat, int externalBufferSize ) {

		setInternalBufferSize(getInternalBufferSize(samplingSpeed,
								isMonoFormat, is16BitFormat));
		logInternalBufferCapacity();

		setSamplingSpeed(samplingSpeed);
		setExternalBufferSize(externalBufferSize);
		setAudioSource(audioSource);
		setChannelConfig(isMonoFormat);
		setAudioDataFormat(is16BitFormat);
		
		
		Log.i(TAG, "creating new recorder");
		mRecorder = new AudioRecord(mAudioSource, mSamplingSpeed, mChannelConfig, mAudioDataFormat, mInternalBufferSize);

		mCompleteRawData = new short[mMAX_NOTE_SECONDS * mSamplingSpeed];

		mIntervalRawData = new short[mExternalBufferSize];
		mIntervalFreqData = new double[mExternalBufferSize]; //same size since we got two components
															//for each freq, to be combined into a single set

		mFFT = new DoubleFFT_1D(mExternalBufferSize);

		mReaderRunnable = new readerRunnable();
		mAnalyzerRunnable = new analyzerRunnable();
		mReaderRunnable.setParallelThread(mAnalyzerThread);
		mAnalyzerRunnable.setParallelThread(mReaderThread);
		mReaderThread = new Thread(mReaderRunnable);
		mAnalyzerThread = new Thread(mAnalyzerRunnable);

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

	public void resumeRecording() {
		(new Thread() {
			public void run() {
				cleanBuffer();
				mIsRecordingPaused = false;
				Log.i(TAG,"Interrupting threads to resume");
				mReaderThread.interrupt();
				mAnalyzerThread.interrupt();
			}
		}).start();
	}
	

	public void pauseRecording() {
		mIsRecordingPaused = true;
		mRecorder.stop();
	}


	public void finishRecording() {
		mIsDone = true;
		mIsRecordingPaused = true;
		mRecorder.stop();
	}

	public void cleanBuffer() {
		for ( int i = 0; i < mNUM_BUFFER_STARTUP_FLUSHES; i++) {
			mRecorder.read(new short[mInternalBufferSize],0,mInternalBufferSize);
		}
		Log.i(TAG, "wiped recorder" + mNUM_BUFFER_STARTUP_FLUSHES+ " times\n");
	}	


	//dummy, graphing only right now
	//not sure if what I wrote here works, hoping it does.	
	public void analyze(short[] rawAudioData) {
		Intent graphIntent = new Intent(this, GraphActivity.class);
		mIntervalFreqData = getFreqData(rawAudioData);
		graphIntent.putExtra("xAxis_FFT",getHertzAxis(rawAudioData.length));
		graphIntent.putExtra("FFT", mIntervalFreqData);

		double[] rawAudioDoubles = new double[rawAudioData.length];
		for (int i = 0; i < rawAudioData.length; i++) {
			rawAudioDoubles[i] = (double) rawAudioData[i];
		}
	
		graphIntent.putExtra("xAxis_rawAudio", getTimeAxisInMs(rawAudioData.length));
		graphIntent.putExtra("rawAudio",rawAudioDoubles);
	
		startActivity(graphIntent);
	}


	public double[] getTimeAxisInMs(int numRawData) {
		double[] timeAxis = new double[numRawData];
	
		for (int i = 0; i < numRawData; i++) {
			timeAxis[i] = 1000*(i/mSamplingSpeed);
		}
		return timeAxis;
	}

	//want to rewrite this function too, not neat
	public double[] getHertzAxis(int numRawData) {
		double[] frequencyAxis = new double[numRawData];
		for (int i = 0; i < numRawData/2; i++) {
			frequencyAxis[i] = i * mSamplingSpeed / (numRawData); // *2  bec there were 2*numFftCoeffs of real/complex, the number passed in is just those combined
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


//NOT EFFICIENT!!!! SEE ABOVE!!!

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
					while (mIsAnalysisDone) { //for parallel thread interrupting
						numRecorded = mRecorder.read(mIntervalRawData, 0, mIntervalRawData.length); // copy out new data
						Log.d(TAG,"\tNumber of data recorded: " + numRecorded);
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

		public boolean isPaused() {
			return mIsPaused;
		}
	}
	
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
					while (!mIsAnalysisDone) { //for parallel thread interrupting
						analyze(mIntervalRawData);
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
	}


	private double[] getFreqData(short[] audioData) {
		double[] audioDataCopy = new double[audioData.length];

		for (int i = 0; i < audioData.length; i++) {
			audioDataCopy[i] = audioData[i];
		}

		mFFT.realForward(audioDataCopy);

		double[] fftCoeffs = new double[audioDataCopy.length / 2];

		for (int i = 2; i < audioData.length; i += 2) {
			fftCoeffs[(i - 2) / 2] = Math.sqrt(Math.pow(audioDataCopy[i], 2)
					+ Math.pow(audioDataCopy[i + 1], 2));
		}
		return fftCoeffs;
	}


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


	public boolean getRecordingState() {
		if (mRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
			return true;
		}
		else {
			return false;
		}
	}


	public void logInternalBufferCapacity() {
		Log.i(TAG,"Internal Buffer size: " +  mInternalBufferSize + "\n");
		int storageTime = (int)(1000*mInternalBufferSize/((float) (mIs16BitFormat ? 2 : 1) * mSamplingSpeed));
		Log.i(TAG, "This will hold " + storageTime + " ms\n");
	}
}
