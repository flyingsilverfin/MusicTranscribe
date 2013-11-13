package com.JS.musictranscribe;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
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
	public static  final int DESIRED_EXTERNAL_BUFFER_TIME = 100; //desired milliseconds
	public static  final int EXTERNAL_BUFFER_SIZE = nextLowerPowerOf2((int)(SAMPLING_SPEED*((float)DESIRED_EXTERNAL_BUFFER_TIME/1000))); //find next lower power of two
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
	
		String s;
		int key;
		Double[] vals = new Double[EXTERNAL_BUFFER_SIZE/2]; //this is used for FFT data, which is 1/2 number of Audio data
		try {
			s = file.readLine();
			

			while (s != null) { //while not at end of file
				key = Integer.parseInt(s.replace("\n", ""));
				for (int i = 0; i < vals.length; i++) {
					s = file.readLine();
					vals[i] =	Double.parseDouble(s.replace("\n", ""));
				}
				
				noteSpectraMap.put(Integer.valueOf(key), vals);
				
				//finished one set of data, check to make sure next line is a newline
				s = file.readLine();
				while (s != "\n"); {
					s = file.readLine();
				}
				
				s = file.readLine(); 	//this is the next key, gets added next cycle, if didn't hit end of file
			}
			
		} catch (IOException e) {
			
		}
		
		return noteSpectraMap;
	}
	
	public static void writeNewNoteSpectraFile(Context context, String filename, HashMap<Integer, Double[]> noteSpectraMap) {
		BufferedWriter file;
		try {
			file = new BufferedWriter(new OutputStreamWriter(context.openFileOutput(filename, context.MODE_PRIVATE)));
		} catch (FileNotFoundException e) {
			Log.e(TAG, "FAILED! Some file error creating new file \n" + e.toString());
			return;
		}
		
		String key;
		Double[] vals;
		try {
			for (Map.Entry<Integer, Double[]> entry : noteSpectraMap.entrySet()) {
				key = entry.getKey().toString();
				file.write(key + "\n"); //write the key
				vals = entry.getValue();
				for (int i = 0; i < vals.length; i++) { 	//write the array of values
					file.write(vals[i].toString() + "\n");
				}
				file.write("\n"); //end of one entry
			}
		} catch (IOException e) {
			Log.e(TAG,"FAILED! Error writing new file \n" + e.toString());
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
		double[] result = new double[data.length];
		for (int i = 0; i < data[0].length; i++) {
			for (int j = 0; j < data.length; j++) {
				result[i] += data[j][i];
			}
			result[i] /= data.length;
		}
		
		return result;
	}
	
	public static Double[] averageArraysIntoDoubleObjects(double[][] data) {
		Double[] result = new Double[data.length];
		printMatrix(data);
		for (int i = 0; i < data[0].length; i++) {
			for (int j = 0; j < data.length; j++) {
				result[i] += data[j][i];
			}
			result[i] /= data.length;
		}
		
		return result;
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
	
}
		
