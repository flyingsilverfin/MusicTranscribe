package com.JS.musictranscribe;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;


/*
 * This class is the superclass of AudioAnalyzer and AudioCollector and some other future ones probably
 * It only implements basic recording and starting/stopping
 * anything beyond that can be added in subclasses
 * however it is usable on its own!
 */

public abstract class Audio {

	//for log
	private static final String TAG = "Audio";
	
	//recorder parameters
	protected int mSamplingSpeed;
	protected int mInternalBufferSize;
	protected int mExternalBufferSize;
	protected int mAudioSource;
	protected boolean mIsMonoFormat; 		//for more user friendliness
	protected boolean mIs16BitFormat;		//for more user friendliness
	protected int mChannelConfig;			//for actual system
	protected int mAudioDataFormat;		//for actual system
	
	//for flushing the recorder out before starting to record actual data
	protected final int mNUM_BUFFER_STARTUP_FLUSHES = 2;
	
	//operation vars
	protected short[] mRawAudioData; 	//to copy out the internal buffer (referred to as external buffer)
	protected Context mContext; //incoming context

	//library-based objects
	protected AudioRecord mRecorder;
	
	//dropbox
	//not used in Audio class, but subclasses do use it
	protected boolean dDBDataUpload = false;
	protected boolean dDBLoggedIn = false;
	protected DropboxAPI<AndroidAuthSession> mDBApi; //this will only be initialized if setDBLoggedIn(true) is called

	
	//-----constructor-----
	public Audio(int audioSource, int samplingSpeed, boolean isMonoFormat, boolean is16BitFormat, int externalBufferSize, Context context) {
		
		//calculates and sets a good size for the internal AudioRecord buffer
		setInternalBufferSize(calcInternalBufferSize(samplingSpeed,
			isMonoFormat, is16BitFormat));

		setSamplingSpeed(samplingSpeed);
		setExternalBufferSize(externalBufferSize);
		setAudioSource(audioSource);
		setChannelConfig(isMonoFormat);
		setAudioDataFormat(is16BitFormat);
		
		setContext(context);
	
		Log.i(TAG, "\tcreating new recorder");
		
		Log.i(TAG,"mAudioSource: " + mAudioSource + ", mSamplingSpeed: " + mSamplingSpeed + ", mChannelConfig: " + mChannelConfig + ", mAudioDataFormat: " +
				mAudioDataFormat + ", InternalBufferSize: " + mInternalBufferSize);
		mRecorder = new AudioRecord(mAudioSource, mSamplingSpeed, mChannelConfig, mAudioDataFormat, mInternalBufferSize);
		
		mRawAudioData = new short[mExternalBufferSize];

	
	
	
	
	}
	
	//---end constructor
	
	
	
	/*
	* Records from microphone
	* defaults into the mAudioData short array
	* returns how many have been read
	*/
	public int recordAudio() {
		int n = mRecorder.read(mRawAudioData, 0, mRawAudioData.length); // copy out new data
		Log.i(TAG,"\tRead " + n + " data");
		return n;
	}
	
	//overloaded in case ever need to copy into a different array
	public int recordAudio(short[] shortArrayToFill) {
		int n = mRecorder.read(shortArrayToFill, 0, shortArrayToFill.length); // copy out new data
		Log.i(TAG,"\tRead " + n + " data");
		Log.i(TAG,"\tLength of array copied into: " + shortArrayToFill.length);
		return n;
	}
	
	
	public boolean startAudioRecording() {
		try {
			mRecorder.startRecording();
		} catch (Throwable e){
			Log.e(TAG, "Recorder failed to start");
			return false;
		}
		return true;
	}
	
	public void stopAudioRecording() {
		mRecorder.stop();
	}
	
	//flush the internal buffer of the recorder (get rid of noise)
	public void cleanBuffer() {
		for ( int i = 0; i < mNUM_BUFFER_STARTUP_FLUSHES; i++) {
			mRecorder.read(new short[mInternalBufferSize],0,mInternalBufferSize);
		}
		Log.i(TAG, "wiped recorder: " + mNUM_BUFFER_STARTUP_FLUSHES+ " times\n");
	}	
	
	//-----setters-----
	private int calcInternalBufferSize(int samplingSpeed, boolean isMono, boolean is16Bit) {
		int minBuffer = 100 + AudioRecord.getMinBufferSize(samplingSpeed, 
				isMono ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO, 
				is16Bit ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT);
		int size = minBuffer < 9192 ? 9192 : minBuffer;
		if (size%2 == 1) {
			Log.d(TAG,"Calculated Internal Buffer is odd, incrementing");
			size++;
		}
		
		//log size of internal buffer size
		int storageTime = (int)(1000.0*mInternalBufferSize/((float) (mIs16BitFormat ? 2 : 1) * mSamplingSpeed));
		return size;
	}
	
	private void setInternalBufferSize(int internalBufferSize) {
		mInternalBufferSize = internalBufferSize;
		Log.i(TAG,"\tInternal Buffer size: " +  mInternalBufferSize + "\n");
		Log.i(TAG, "\tThis will hold " + (int)(1000.0*mInternalBufferSize/((float) (mIs16BitFormat ? 2 : 1) * mSamplingSpeed)) + " ms\n");
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
	
	
	//---end setters
	
	
	//-----Getters-----
	
	public short[] getRawAudioArray() {
		return mRawAudioData;
	}
	
	public short[] getRawAudioArrayCopy() {
		short[] tmpAudioData = new short[mRawAudioData.length];
		for (int i = 0; i < mRawAudioData.length; i++) {
			tmpAudioData[i] = mRawAudioData[i];
		}
		return tmpAudioData;
	}
	
	public double[] getRawAudioArrayAsDoubles() {
		double[] tmpAudioData = new double[mRawAudioData.length];
		for ( int i = 0; i < mRawAudioData.length; i++) {
			tmpAudioData[i] = (double) mRawAudioData[i];
		}
		return tmpAudioData;
	}
	
	public Context getSavedContext() {
		return mContext;
	}
	
	protected int getSamplingSpeed() {
		return mSamplingSpeed;
	}
	
	protected int getInternalBufferSize(){
		return mInternalBufferSize;
	}
	
	//Is the audio recorder paused
	public boolean isRecorderRunning() {
		if (mRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
			return true;
		}
		else {
			return false;
		}
	}
	
	protected int getExternalBufferSize() {
		return mExternalBufferSize;
	}

	//---end getters
	

	
	
	//-----Dropbox-----
	
	/*
	* Intended usage:
	* Dropbox capabilities are enabled or fail from the MainActivty
	* The loggedIn or not logged in value is passed to other activities which can use it here
	* Dropbox starts out disabled and logged out (as far as this class knows)
	* Activity making an Audio object will call this based on a button or something
	* 
	*/
	
	public void setDBLoggedIn(boolean loggedIn) {
		
		dDBLoggedIn = loggedIn;
		if (loggedIn) {
			mDBApi = Helper_Dropbox.getDBSession(mContext);
		}
		else {
			mDBApi = null;
		}
	}

		
	//this is to enable and disable upload even if logged in
	//might not want to always upload when logged in! (speed, convenience, testing, etc)
	public  void setDBDataUpload(boolean enabled) {
		dDBDataUpload = enabled;
	}
	
		
	public boolean isDBDataUploadEnabled() {
		return dDBDataUpload;
	}
	
	public boolean isDBLoggedIn() {
		return dDBLoggedIn;
	}
	
}
