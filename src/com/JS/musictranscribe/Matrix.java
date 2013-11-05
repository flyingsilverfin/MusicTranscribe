package com.JS.musictranscribe;

import java.util.Random;

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
	
	private double tolerance = 0.00000001; //will round to 0 below this number;
	
	public Matrix(int m, int n) {
		mMatrix = new double[m][n];
		mHeight = m;
		mWidth = n;
	}
	
	
	
	//----Functionality----
	
	public int REF() {
		int col = 0;
		int shift = 0;
		for (int j = 0; j < mHeight; j++) {
			col = j + shift; //to check columns, starting at j'th one
			if (col == mWidth-1) { //reached end of matrix
				System.out.println();
				reduceRow(j,col);
				setAllBelowToZero(j);
				break;
			}
			int maxColIndex = findAbsMaxIndexInCol(col, j);
			//System.out.println("Max in col " + col + " is in row " + maxColIndex);
			while (maxColIndex == j && mMatrix[col][maxColIndex] == 0) { //if this is a column of 0's (returned starting index and it's a 0)
					col++;
					shift++;
					maxColIndex = findAbsMaxIndexInCol(col, j); //returns j if j (starting index) is the largest
			}
			if (j != maxColIndex) { //if the current row is not the one with the max (in abs values)
				//System.out.println("Swapping rows: " + j + " and " + maxColIndex);
				swapRows(j, maxColIndex);
			}
			reduceRow(j, col);

			reduceRowsBelow(j, col);
			//printMatrix();

		}
		return col; //return position of the last column worked on (might not be last one)
	}
	
	
	public void RREF() {
		int startingCol = REF();
		int startingRow = findLastNonzeroIndex(startingCol);
		for (int j = startingRow; j >= 0; j--) {
			startingCol = findFirstNonzeroIndexInRow(j);
			reduceRowsAbove(j,startingCol);
		}
	}
	
	
	/*
	 * Add another matrix to this one!
	 */
	public void add(Matrix m) {
		if (m.getHeight() != mHeight || m.getWidth() != mWidth) {
			Log.e(TAG,"Dimesions do not match, Aborting!");
			return;
		}
		for (int j = 0; j < mHeight; j++) {
			addRowsInPlace(j, m.getRow(j));
		}
	}
	
	/*
	 * multiply a given matrix on the left of this matrix
	 * eg. with matricies A and B, where A in input and B is this object, calculate A*B
	 */
	public Matrix multOnRightOf(Matrix m) {
		if (m.getWidth() != mHeight) {
			Log.e(TAG, "Dimensions do not match, trying to multiply matrix of width " + m.getWidth() + 
					"by one of height " + mHeight + ". Aborting!");
			return null;
		}
		
		Matrix result = new Matrix(m.getHeight(), mWidth);
		
		for (int j = 0; j < m.getHeight(); j++) {
			for (int i = 0; i < mWidth; i++) {
				result.writeElem(j, i, dotProd(m.getRow(j), getCol(i)));
			}
		}
		
		return result;
	}
	
	public Matrix multOnLeftOf(Matrix m) {
		if (m.getHeight() != mWidth) {
			Log.e(TAG, "Dimensions do not match, trying to multiply matri of width " + mWidth + 
					"by one of height " + m.getHeight() + ". Aborting!");
		}
		
		Matrix result = new Matrix(mHeight, m.getWidth());
		
		for (int j = 0; j < mHeight; j++) {
			for (int i = 0; i < m.getWidth(); i++) {
				result.writeElem(j, i, dotProd(getRow(j), m.getCol(i)));
			}
		}
		
		return result;
	}
	
	//-----Support functions-----
	
	private void reduceRowsBelow(int m, int colToReduceBy) {

		//int loc = findFirstNonZeroIndexInRow(m); //unreliable!
		int loc = colToReduceBy;
		double val = mMatrix[m][loc];
		for (int j = m+1; j < mHeight; j++) {
			multRowInPlace(j, -1*val/mMatrix[j][loc]); //divide to 1, multiply with val, then negate
			addRowsInPlace(j, m); //add the upper row to the lower one in place to cancel out that column
		}
	}
	
	private void reduceRowsAbove(int m, int colToReduceBy) {
		int loc = colToReduceBy;
		double val = mMatrix[m][loc];
		for (int j = m-1; j >= 0; j--) {
			addRowsInPlace(j,multRow(m,-1*mMatrix[j][loc]/val));	//uses overloaded version
																	//do it this way so if that positions value is 0, no div by 0 occurs
																	//also doesn't change other coefficients, multiplies
																	//lowest row and gets a copy, then adds that to the above row
			
		}		
	}
	
	private void reduceRow(int m, int indexToReduceBy) {
		double factor = 1/mMatrix[m][indexToReduceBy];
		for (int i = 0; i < mWidth; i++) {
			mMatrix[m][i] *= factor;
			if (Math.abs(mMatrix[m][i]) < tolerance) {
				mMatrix[m][i] = 0;
			}
		
		}
	}
	
	private void setAllBelowToZero(int m) {
		for (int j = m+1; j < mHeight; j++) {
			for (int i = 0; i < mWidth; i++) {
				mMatrix[j][i] = 0;
			}
		}
	}
	
	private int findAbsMaxIndexInCol(int n, int startingRow) {
		int largestIndex = startingRow;
		for (int j = startingRow+1; j < mHeight; j++) {
			if (Math.abs(mMatrix[j][n]) > Math.abs(mMatrix[largestIndex][n])) {
				largestIndex = j;
			}
		}
		return largestIndex;
	}
	
	private int findFirstNonzeroIndexInRow(int m) {
		for (int i = 0; i < mWidth; i++) {
			if (mMatrix[m][i] != 0) {
				return i;
			}
		}
		return -1;
	}
	
	private int findLastNonzeroIndex(int n) {
		for (int j = mHeight-1; j >= 0; j--) {
			if (mMatrix[j][n] != 0) {
				return j;
			}
		}
		return -1;
	}
	
	
	
	//-----Row Modification functions-----
	
	/*
	 * Add two rows of the matrix without returning a copy
	 */
	private void addRowsInPlace(int rowToAddTo, int rowToAdd) {
		for (int i = 0; i < mMatrix[rowToAddTo].length; i++) {
			mMatrix[rowToAddTo][i] += mMatrix[rowToAdd][i];
		}
	}
	
	private void addRowsInPlace(int rowToAddTo, double[] rowToAdd) {
		for (int i = 0; i < mMatrix[rowToAddTo].length; i++) {
			mMatrix[rowToAddTo][i] += rowToAdd[i];
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
	
	private void multRowInPlace(int m, double factor) {
		for (int i = 0; i < mWidth; i++) {
			mMatrix[m][i] *= factor;
		}
	}
	
	/*
	 * returns a copy of the row multiplied by the factor
	 */
	public double[] multRow(int m, double factor) {
		double[] result = new double[mWidth];
		for (int i = 0; i < mWidth; i++) {
			result[i] = mMatrix[m][i] * factor;
		}
		return result;
	}
	
	public double dotProd(double[] row1, double[] row2) {
		double res = 0;
		for (int i = 0; i < row1.length; i++) {
			res += (row1[i]*row2[i]);
		}
		return res;
	}
	
	public void swapRows(int row1, int row2) {
		double[] tmp = mMatrix[row1];
		mMatrix[row1] = mMatrix[row2];
		mMatrix[row2] = tmp;
	}
	
	
	//-----Getters and Setters-----
	
	
	/*
	 * Fill the matrix randomly
	 * Used for testing purposes 
	 */
	
	public void fillRandomly() {
		Random random = new Random();
		for (int j = 0; j < mHeight; j++) {
			for (int i = 0; i < mWidth; i++) {
				mMatrix[j][i] = (random.nextDouble()-0.5)*2000000;
			}
		}
	}
	
	
	/*
	 * only writes as many values as possible, minimum of length of array passed in and length of mMatrix rows 
	 */
	public void writeCol(int n, double[] col){
		int h = mHeight;
		if (col.length != mHeight) {
			System.out.println("Given column's height does not equal matrix's height, using minimum");
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
			System.out.println("Given row's length does not match matrix rows' lengths, using minimum");
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
	
	/*
	 * get a reference to the inside double[][] structure
	 */
	public double[][] getMatrixContents() {
		return mMatrix;
	}
	
	/*
	 * get a copy of the matrix's contents in double[][] form
	 */
	public double[][] getMatrixContentsCopy() {
		double[][] copy = new double[mHeight][mWidth];
		for (int j = 0; j < mHeight; j++) {
			for (int i = 0; i < mWidth; i++) {
				copy[j][i] = mMatrix[j][i];
			}
		}
		return copy;
	}
	
	/*
	 * get a copy of the matrix, as a Matrix object
	 */
	public Matrix getMatrixCopy() {
		Matrix copy = new Matrix(mHeight, mWidth);
		for (int j = 0; j < mHeight; j++) {
			copy.writeRow(j, getRow(j));
		}
		return copy;
	}
	
	
	public int getHeight() {
		return mHeight;
	}
	
	public int getWidth() {
		return mWidth;
	}
	
	
	//-----Matrix printing-----
	public void printMatrix() {
		for (int j = 0; j < mHeight; j++) {
			for (int i = 0; i < mWidth; i++) {
				System.out.print(mMatrix[j][i]);
				System.out.print(" ");
			}
			System.out.print("\n");
		}
		System.out.print("\n");
	}
	
	public void printMatrixRow(int m) {
		for (int i = 0; i < mWidth; i++) {
			System.out.printf("%.010f",mMatrix[m][i]);
			System.out.print(" ");
		}
		System.out.println();
	}
}
