package com.JS.musictranscribe;

import java.util.List;

public class Helper {

	/*
	 * App-wide Audio options
	 */
	public static int SAMPLING_SPEED = 44100;
	public static  final int DESIRED_EXTERNAL_BUFFER_TIME = 100; //desired milliseconds
	public static  final int EXTERNAL_BUFFER_SIZE = nextLowerPowerOf2((int)(SAMPLING_SPEED*((float)DESIRED_EXTERNAL_BUFFER_TIME/1000))); //find next lower power of two
	public static final int EXTERNAL_BUFFER_TIME = EXTERNAL_BUFFER_SIZE*1000/SAMPLING_SPEED; //find actual time being measured based on above
	
	
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
	
	
}
		
