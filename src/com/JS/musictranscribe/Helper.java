package com.JS.musictranscribe;

public class Helper {

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
	
	
}
