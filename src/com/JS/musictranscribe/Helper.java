package com.JS.musictranscribe;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class Helper {

	private static final String TAG = "Helper";
	
	/*
	 * App-wide Audio options
	 */
	public static int SAMPLING_SPEED = 44100;
	public static final int DESIRED_EXTERNAL_BUFFER_TIME = 100; //desired milliseconds
	public static final int EXTERNAL_BUFFER_SIZE = nextLowerPowerOf2((int)(SAMPLING_SPEED*((float)DESIRED_EXTERNAL_BUFFER_TIME/1000))); //find next lower power of two
	public static final int EXTERNAL_BUFFER_TIME = EXTERNAL_BUFFER_SIZE*1000/SAMPLING_SPEED; //find actual time being measured based on above
	
	//this is accessed from everywhere: key for sharedPreferences to get active note spectra file
	public static final String ACTIVE_MAPDATA_FILE_KEY = "activeDataFile";
	
	
	//PROTECTED FILES LIST HERE! ie. files that cannot be deleted from the private storage
	public static final ArrayList<String> protectedFiles = new ArrayList<String>(Arrays.asList("default", "one_oct_white"));
	
	
	//------------------ File and Data Storage Functions ------------------
	/*
	 * For the noteSpectraFile functions;
	 * This is how the note spectra files are formatted:
	 * "note number
	 * fftval0
	 * fftval1
	 * fftval2
	 * .
	 * .
	 * .
	 * fftval2047 (usually. designed to be flexible. getNoteSpectraFromFile() looks for following newline)
	 * [newline]
	 * note number
	 * fftval0
	 * .
	 * .
	 * .
	 * fftval2047
	 * ..."
	 */
	public static HashMap<Integer, Double[]> getNoteSpectraFromFile(Context context, String noteSpectraFileName) {
		HashMap<Integer, Double[]> noteSpectraMap = new HashMap<Integer, Double[]>();
		
		BufferedReader file;
		try {
			//open a buffered reader of the indicated file
			file= new BufferedReader(new InputStreamReader(context.openFileInput(noteSpectraFileName)));
		} catch (FileNotFoundException e){
			Log.e(TAG, "File not found \n" + e.toString());
			//return empty hashmap
			return noteSpectraMap;
		}
		
		//
		ArrayList<Double> firstVals = new ArrayList<Double>(); //need this to see how long each data chunk is
		Double[] vals;
		
		Log.i(TAG, "Reading map file: " + noteSpectraFileName);
		try {
			String s = file.readLine();	//STRIPS NEWLINES ON ITS OWN
			Integer key = Integer.parseInt(s);	//key for first note
			
			s = file.readLine();
			//while not at end of first chunk of data. DOESN'T read newline, so length != 0 works! If == 0, nothing in line!
			while (s.length() != 0) {	
				//get that fft value
				firstVals.add(Double.valueOf(s)); 	
				//read the next line. If it's blank, exit loop
				s = file.readLine();	
			}
			
			//this is why we're using arraylist (slower): don't know how many data per note are stored
			int size = firstVals.size(); 
			Log.i(TAG, "Size of these data chunks is: " + size);
			//now we can use faster arrays
			vals = new Double[size]; 
			
			//copy the ArrayList into the new array
			for (int i = 0; i < size; i++) {
				//copy into Double[] since that's what the map takes
				vals[i] = firstVals.get(i); 
			}
			
			//save that key, value pair
			noteSpectraMap.put(key, vals); 
			s = file.readLine(); //either get next key or get null (EOF)
			while (s != null) {
				//have to initialize new array since the map only references the arrays internally, doesn't store copies. If this isn't done, all the values will be the same array
				vals = new Double[size];
				key = Integer.parseInt(s);
				//get the next 'size' number of values from the file
				for (int i = 0; i < size; i++) {
					s = file.readLine();
					vals[i] = Double.valueOf(s);
				}
				//save key, value pair
				noteSpectraMap.put(key,vals);
				
				//skip the newline between chunks of data
				s = file.readLine(); 
				//either this is the next key or null
				s = file.readLine(); 
			}
			

		} catch (IOException e) {
			Log.e(TAG,"FAILED! Some error writing file");
		}
		
		return noteSpectraMap;
	}
	
	public static void writeNewNoteSpectraFile(Context context, String filename, HashMap<Integer, Double[]> noteSpectraMap) throws  Exception {
		//check if file exists!
		if (listHas(getFileList(context), filename)) {
			Log.e(TAG,"File exists!");
			throw new Exception("File Exists");
		}

		
		BufferedWriter file;
		try {
			//open buffered writer for that new file
			file = new BufferedWriter(new OutputStreamWriter(context.openFileOutput(filename, context.MODE_PRIVATE)));
			Log.i(TAG,"Created new File object");
		} catch (FileNotFoundException e) {
			Log.e(TAG, "FAILED! Some file error creating new file \n" + e.toString());
			throw new Exception("Could not create file");
		}
		
		String key;
		Double[] vals;
		try {
			//copy out key, value entries from the noteSpectra map
			for (Map.Entry<Integer, Double[]> entry : noteSpectraMap.entrySet()) {
				key = entry.getKey().toString();
				//write the key
				file.write(key + "\n"); 
				//get the values
				vals = entry.getValue();
				//write the array of values
				for (int i = 0; i < vals.length; i++) { 	
					file.write(vals[i].toString() + "\n");
				}
				file.write("\n"); //end of one entry
			}
		} catch (IOException e) {
			Log.e(TAG,"FAILED! Error writing new file \n" + e.toString());
			throw new Exception("Error writing file");
		}
		//don't forget to flush!
		file.close();
	}
	
	
	/*
	 * Function gets the private files,
	 * returns ListElement objects. Designed for use with MyListFragment and ListElementAdapter.
	 * Tells each ListElement if its the active one as saved in the sharedPreferences 
	 */
	public static ArrayList<ListElement> getFileListElements(Context context) {
		ArrayList<ListElement> files = new ArrayList<ListElement>();
		for (String s : getFileListAsArray(context)) {
			if (s.equals(getStringPref(ACTIVE_MAPDATA_FILE_KEY, context))) {
				files.add(new ListElement(s, true));
			}
			else {
				files.add(new ListElement(s, false));
			}
		}
		
		return files;		
	}
	
	
	/*
	 * Return array of private file names
	 */
	public static String[] getFileListAsArray(Context context) {
		String[] files = context.fileList();
		return files;
	}
	
	/*
	 * Return ArrayList of private file names
	 */
	public static ArrayList<String> getFileList(Context context) {
		return new ArrayList<String>(Arrays.asList(getFileListAsArray(context)));
	}
	
	/*
	 * Delete all the private files
	 */
	public static void deleteAllPrivFiles(Context context) {
		String[] files =  context.fileList();
		
		for (String file : files) {
			//protected file should not be deleted. protectedFiles defined at top of this file
			if (listHas(protectedFiles, file)) { 
				continue;
			}
			context.deleteFile(file);
		}
	}
	
	
	/*
	 * Following functions: easy interfaces for sharedPreferences
	 */
	public static String getStringPref(String key, Context context) {
		SharedPreferences prefs = context.getSharedPreferences("com.JS.musictranscribe", Context.MODE_PRIVATE);
		return prefs.getString(key, null);
	}
	
	public static boolean getBoolPref(String key, Context context) {
		SharedPreferences prefs = context.getSharedPreferences("com.JS.musictranscribe", Context.MODE_PRIVATE);
		return prefs.getBoolean(key, false);
	}
	
	public static int getIntPref(String key, Context context) {
		SharedPreferences prefs = context.getSharedPreferences("com.JS.musictranscribe", Context.MODE_PRIVATE);
		return prefs.getInt(key, -1);
	}
	
	public static float getFloatPref(String key, Context context) {
		SharedPreferences prefs = context.getSharedPreferences("com.JS.musictranscribe", Context.MODE_PRIVATE);
		return prefs.getFloat(key, -1);
	}
	
	
	public static void setStringPref(String key, String value, Context context) {
		Editor editor = context.getSharedPreferences("com.JS.musictranscribe", Context.MODE_PRIVATE).edit();
		editor.putString(key,  value);
		editor.commit();
	}
	
	public static void setBoolPref(String key, Boolean value, Context context) {
		Editor editor = context.getSharedPreferences("com.JS.musictranscribe", Context.MODE_PRIVATE).edit();
		editor.putBoolean(key,  value);
		editor.commit();
	}
	
	public static void setIntPref(String key, int value, Context context) {
		Editor editor = context.getSharedPreferences("com.JS.musictranscribe", Context.MODE_PRIVATE).edit();
		editor.putInt(key,  value);
		editor.commit();
	}
	
	public static void setFloatPref(String key, float value, Context context) {
		Editor editor = context.getSharedPreferences("com.JS.musictranscribe", Context.MODE_PRIVATE).edit();
		editor.putFloat(key,  value);
		editor.commit();
	}

	

	
	//------------------ Math Functions ------------------
	
	public static int nextLowerPowerOf2(int num) {
		int r = 1; 
		num = num >> 1;
		while (num > 0) {
			num = num >> 1;
			r = r << 1;
		}
		return r;
	}
	
	
	public static double max(double[] arr) {
		double highest = arr[0];
		for ( int i = 1; i < arr.length; i++) {
			if (arr[i] > highest) {
				highest = arr[i];
			}
		}
		return highest;
	}
	
	public static double min(double[] arr) {
		double lowest = arr[0];
		for ( int i = 1; i < arr.length; i++) {
			if (arr[i] < lowest) {
				lowest = arr[i];
			}
		}
		return lowest;
	}	
	
	//------------------ String Functions ------------------
	
	public static String join(String[] s, String glue) {
		String result = "";
		for (int i = 0; i < s.length-1; i++) {
			result += s[i];
			result += glue;
		}
		result += s[s.length-1];
		return result;
	}
	
	//------------------ List Functions ------------------
	
	public static int findInList(List<String> l, String toFind) {
		return l.indexOf(toFind);
	}
	
	public static boolean listHas(List<String> l, String toFind) {
		if ( findInList(l, toFind) == -1 ) {
			return false;
		}
		else {
			return true;
		}
	}
	
	
	//------------------ Array Operations ------------------
	
	public static double sumArray(double[] arr) {
		double res = 0;
		for (int i = 0; i < arr.length; i++) {
			res += arr[i];
		}
		return res;
	}
	
	public static double sumArray(short[] arr) {
		double res = 0;
		for (int i = 0; i < arr.length; i++) {
			res += arr[i];
		}
		return res;
	}
	
	public static double sumArrayInAbs(double[] arr) {
		double res = 0;
		for (int i = 0; i < arr.length; i++) {
			res += Math.abs(arr[i]);
		}
		return res;
	}
	
	public static double sumArrayInAbs(short[] arr) {
		double res = 0;
		for (int i = 0; i < arr.length; i++) {
			res += Math.abs(arr[i]);
		}
		return res;
	}
	
	public static double[] getEachNthInArray(double[] arr, int step) {
		double[] res = new double[(int)arr.length/step];
		for (int i = 0; i < res.length; i++) {
			res[i] = arr[i*step];
		}
		return res;
	}
	
	public static double[] averageArrays(double[][] data) { //must be a regularly shaped matrix (all same length)
		double[] result = new double[data[0].length];
		for (int i = 0; i < data[0].length; i++) {
			for (int j = 0; j < data.length; j++) {
				result[i] += data[j][i];
			}
			result[i] /= data.length;
		}
		
		return result;
	}
	
	public static Double[] averageArraysIntoDoubleObjects(double[][] data) {
		Double[] result = new Double[data[0].length];
		for (int i = 0; i < data[0].length; i++) {
			result[i] = 0.0;
			for (int j = 0; j < data.length; j++) {
				result[i] += data[j][i];
			}
			result[i] /= data.length;
		}
		
		return result;
	}
	
	public static double[] joinArrays(double[][] arrs) {
		int len = 0;
		for (int i = 0; i < arrs.length; i++) {
			len += arrs[i].length;
		}
		double[] arr = new double[len];
		int counter = 0;
		for (int i = 0; i < arrs.length; i++) {
			for (int j = 0; j < arrs[i].length; j++) {
				arr[counter] = arrs[i][j];
				counter++;
			}
		}
		
		return arr;
	}
	
	
	public static double[] range(int start, double step, int num) {
		double[] arr = new double[num];
		for (int i = 0; i < num; i++ ){
			arr[i] = start + i*step;
		}
		return arr;
	}
	
	public static double[] toDoublePrimitiveArray(Double[] arr) {
		double[] res = new double[arr.length];
		for (int i = 0; i < arr.length; i++) {
			res[i] = arr[i];
		}
		return res;
	}
	
	//------------------ Audio Functions ------------------

	public static double[] fft(double[] data) {
		double[] dataCopy = new double[data.length];
		DoubleFFT_1D f = new DoubleFFT_1D(data.length);
		for (int i = 0; i < data.length; i++) {
			dataCopy[i] = data[i];
		}
		
		f.realForward(dataCopy);
		
		double[] fftCoeffs = new double[(dataCopy.length/2) - 1];

		for (int i = 2; i < dataCopy.length; i += 2) { //skip the first two, they are total and # data or somthing
			fftCoeffs[(i - 2) / 2] = Math.sqrt(Math.pow(dataCopy[i], 2)
					+ Math.pow(dataCopy[i + 1], 2));
		}
		return fftCoeffs;
	}
	
	
	public static double[] getFrequencyAxis(double lengthInS, int num){
		double[] frequencyAxis = new double[num];
		Log.i(TAG,"time length: " + lengthInS);
		frequencyAxis[0] = 1/lengthInS; //base frequency
		Log.i(TAG,"base frequency : " + frequencyAxis[0]);
		for (int i = 1; i < num; i++) {
			frequencyAxis[i] = frequencyAxis[0] * (i+1);
		}
		
		return frequencyAxis;
	}
	
	
	//------------------ Matrix Functions ------------------
	
	public static  void printMatrix(double[][] matrix) {
		for (int j = 0; j < matrix.length; j++) {
			for (int i = 0; i < matrix[j].length; i++) {
				System.out.print(matrix[j][i]);
				System.out.print(" ");
			}
			System.out.print("\n");
		}
		System.out.print("\n");
	}
	
	public static void printArray(double[] array) {
		for (int i = 0; i < array.length; i++) {
			System.out.print(array[i]);
			System.out.print(", ");
		}
		System.out.print("\n");
	}
	
	public static void printArray(Integer[] array) {
		for (int i = 0; i < array.length; i++) {
			System.out.print(array[i]);
			System.out.print(", ");
		}
		System.out.print("\n");
	}
	
	public static void printArray(Double[] array) {
		for (int i = 0; i < array.length; i++) {
			System.out.print(array[i]);
			System.out.print(", ");
		}
		System.out.print("\n");
	}
	
}
		
