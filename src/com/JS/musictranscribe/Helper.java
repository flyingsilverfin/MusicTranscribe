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
	
}
