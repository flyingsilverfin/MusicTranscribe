package com.JS.musictranscribe;

import android.util.Log;


/*
 * Basic matrix class
 * m traditionally is rows 
 * n traditionally is columns
 * in loops used j for cycling down rows, and i for elements within a row
 */

public class Matrix {

	private static String TAG = "Matrix";
	
	private double[][] mMatrix;
	private int mHeight;
	private int mWidth;
	
	public Matrix(int m, int n) {
		mMatrix = new double[m][n];
		mHeight = m;
		mWidth = m;
	}
	
	
	private void reduceRow(int m) {
		double[100]
	}
	
	/*
	 * Add two rows of the matrix without returning a copy
	 */
	private void addRowsInPlace(int rowToAddTo, int rowToAdd) {
		for (int i = 0; i < mMatrix[rowToAddTo].length; i++) {
			mMatrix[rowToAddTo][i] += mMatrix[rowToAdd][i];
		}
	}
	
	/*
	 * Returns a copy of added rows
	 * more for external use
	 */
	public double[] addRows(int row1, int row2) {
		double[] result = new double[mWidth];
		for (int i = 0; i < mWidth; i++) {
			result[i] = mMatrix[row1][i] + mMatrix[row2][i];
		}
		return result;
	}
	
	private void multRowInPlace(int m, float factor) {
		for (int i = 0; i < mWidth; i++) {
			mMatrix[m][i] *= factor;
		}
	}
	
	/*
	 * returns a copy of the row multiplied by the factor
	 */
	public double[] multRow(int m, float factor) {
		double[] result = new double[mWidth];
		for (int i = 0; i < mWidth; i++) {
			result[i] = mMatrix[m][i] * factor;
		}
		return result;
	}
	
	public void swapRows(int row1, int row2) {
		double[] tmp = mMatrix[row1];
		mMatrix[row1] = mMatrix[row2];
		mMatrix[row2] = tmp;
	}
	
	
	//-----Getters and Setters basically
	
	/*
	 * only writes as many values as possible, minimum of length of array passed in and length of mMatrix rows 
	 */
	public void writeCol(int n, double[] col){
		int h = mHeight;
		if (col.length != mHeight) {
			Log.d(TAG,"Given column's height does not equal matrix's height, using minimum");
			h = Math.min(mHeight, col.length);
		}
		for (int j = 0; j < h; j++) {
			mMatrix[j][n] = col[j];
		}
	}
	
	/*
	 * accept negatives to count from the end too
	 */
	public double[] getCol(int n) {
		if (n<0) {
			n = Math.abs((mHeight + n))%mHeight;
		}
		else {
			n = n%mHeight;
		}
		double[] col = new double[mHeight];
		for (int j = 0; j < mHeight; j++) {
			col[j] = mMatrix[j][n];
		}
		return col;
	}
	
	
	/*
	 * only writes as many values as possible, minimum of length of array passed in and length of mMatrix rows 
	 */
	public void writeRow(int m, double[] row) {
		int w = mWidth;

		if (row.length != mWidth) {
			Log.d(TAG,"Given row's length does not match matrix rows' lengths, using minimum");
			w = Math.min(mWidth, row.length);
		}
		for (int i = 0; i < w; i++) {
			mMatrix[m][i] = row[i];
		}
	}
	
	/*
	 * accept negatives to count from the end too
	 */
	public double[] getRow(int m) {
		if (m<0) {
			m = Math.abs((mHeight + m))%mHeight;
		}
		else {
			m = m%mHeight;
		}
		double[] row = new double[mWidth];
		for (int i = 0; i < mWidth; i++ ) {
			row[i] = mMatrix[m][i];
		}
		return row;
	}
	
	public void writeElem(int m, int n, double val) {
		mMatrix[m][n] = val;	
	}
	
	public double getElem(int m, int n) {
		return mMatrix[m][n];
	}
	
	
}
