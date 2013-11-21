package com.JS.musictranscribe;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
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
	
	public static void deleteAllPrivFiles(Context context) {
		String[] files =  context.fileList();
		
		for (String file : files) {
			if (file.equals("default")) { //default file should not be deleted
				continue;
			}
			context.deleteFile(file);
		}
	}
	
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
	
	
	public double[] getFrequencyAxis(float lengthInMs, int num){
		double[] frequencyAxis = new double[num];
		frequencyAxis[0] = 1/lengthInMs; //base frequency
		
		for (int i = 1; i < num; i++) {
			frequencyAxis[i] = frequencyAxis[0] * (i+1);
		}
		
		return frequencyAxis;
	}
	
	
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
	
}
		
