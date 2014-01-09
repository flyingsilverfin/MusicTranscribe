package com.JS.musictranscribe;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
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
	
	public static final String defaultNoteSpectraFile = "defaultNotes";
	
	
	//------------------ File and Data Storage Functions ------------------
	public static HashMap<Integer, Double[]> getNoteSpectraFromFile(Context context, String noteSpectraFileName) {
		HashMap<Integer, Double[]> noteSpectraMap = new HashMap<Integer, Double[]>();
		
		BufferedReader file;
		try {
			file= new BufferedReader(new InputStreamReader(context.openFileInput(noteSpectraFileName)));
		} catch (FileNotFoundException e){
			Log.e(TAG, "File not found \n" + e.toString());
			return noteSpectraMap;
		}
		
	
		ArrayList<Double> firstVals = new ArrayList<Double>(); //need this to see how long each data chunk is
		Double[] vals;
		
		Log.i(TAG, "Reading map file: " + noteSpectraFileName);
		try {
			String s = file.readLine(); //STRIPS NEWLINES
			Integer key = Integer.parseInt(s);
			
			s = file.readLine();
			while (s.length() != 0) { //while not at end of first chunk of data. DOES read newline
				firstVals.add(Double.valueOf(s));
				s = file.readLine();
			}
			
			int size = firstVals.size();
			Log.i(TAG, "Size of these data chunks is: " + size);
			vals = new Double[size];
			
			//copy the ArrayList into the new array
			for (int i = 0; i < size; i++) {
				vals[i] = firstVals.get(i);
			}
			
			noteSpectraMap.put(key, vals);
			s = file.readLine(); //either get next key or get null
			while (s != null) {
				vals = new Double[size]; //have to initialize new array since the map only references the arrays internally, doesn't store copies. If this isn't done, all the values will be the same array
				key = Integer.parseInt(s);
				for (int i = 0; i < size; i++) {
					s = file.readLine();
					vals[i] = Double.valueOf(s);
				}
				
				noteSpectraMap.put(key,vals);
				
				s = file.readLine(); //skip the newline between chunks of data
				s = file.readLine(); //either this is the next key or null

			}
			

		} catch (IOException e) {
			Log.e(TAG,"FAILED! Some error writing file");
		}
		
		
		return noteSpectraMap;
	}
	
	public static void writeNewNoteSpectraFile(Context context, String filename, HashMap<Integer, Double[]> noteSpectraMap) throws  Exception {
		for (String f : context.fileList()) {
			if (filename.equals(f)) {
				Log.e(TAG,"File exists!");
				throw new Exception("File Exists");
			}
		}
		
		BufferedWriter file;
		try {
			file = new BufferedWriter(new OutputStreamWriter(context.openFileOutput(filename, context.MODE_PRIVATE)));
			Log.i(TAG,"Created new File object");
		} catch (FileNotFoundException e) {
			Log.e(TAG, "FAILED! Some file error creating new file \n" + e.toString());
			throw new Exception("Could not create file");
		}
		
		String key;
		Double[] vals;
		try {
			for (Map.Entry<Integer, Double[]> entry : noteSpectraMap.entrySet()) {
				key = entry.getKey().toString();
				file.write(key + "\n"); //write the key
				vals = entry.getValue();
				Log.i(TAG,"Writing " + vals.length + " entries for current note: ");
				for (int i = 0; i < vals.length; i++) { 	//write the array of values
					file.write(vals[i].toString() + "\n");
				}
				file.write("\n"); //end of one entry
			}
		} catch (IOException e) {
			Log.e(TAG,"FAILED! Error writing new file \n" + e.toString());
			throw new Exception("Error writing file");
		}
		
		file.close();
	}
	
	public static String[] listAllPrivFiles(Context context) {
		String[] files = context.fileList();
		return files;
	}
	
	public static void deleteAllPrivFiles(Context context) {
		String[] files =  context.fileList();
		
		for (String file : files) {
			if (file.equals("default")) { //default file should not be deleted
				continue;
			}
			context.deleteFile(file);
		}
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
	
	
	public static double[] getFrequencyAxis(float lengthInMs, int num){
		double[] frequencyAxis = new double[num];
		frequencyAxis[0] = 1/lengthInMs; //base frequency
		
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
		
