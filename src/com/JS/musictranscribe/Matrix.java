package com.JS.musictranscribe;

import java.util.ArrayList;
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
	
	public ArrayList<Double> REF() {
		
		ArrayList<Double> record = new ArrayList<Double>();
		
		int col = 0;
		int shift = 0;
		for (int j = 0; j < mHeight; j++) {
			col = j + shift; //to check columns, starting at j'th one
			if (col == mWidth-1) { //reached end of matrix
				reduceRow(j, col, record);
				setAllBelowToZero(j);
				break;
			}
			int maxColIndex = findAbsMaxIndexInCol(col, j);
			while (maxColIndex == j && mMatrix[col][maxColIndex] == 0) { //if this is a column of 0's (returned starting index and it's a 0)
					col++;
					shift++;
					maxColIndex = findAbsMaxIndexInCol(col, j); //returns j if j (starting index) is the largest
			}
			if (j != maxColIndex) { //if the current row is not the one with the max (in abs values)
				swapRows(j, maxColIndex, record);
			}
			reduceRow(j, col, record);

			reduceRowsBelow(j, col, record);

		}
		record.add(0, (double) col);
		return record; //return position of the last column worked on (might not be last one)
	}
	
	
	public ArrayList<Double> RREF() {
		ArrayList<Double> record = REF();
		int startingCol = record.get(0).intValue(); //get the starting col that's been saved
		record.remove(0);							//remove the first index to get purely the record
		int startingRow = findLastNonzeroIndex(startingCol);
		for (int j = startingRow; j >= 0; j--) {
			startingCol = findFirstNonzeroIndexInRow(j);
			reduceRowsAbove(j,startingCol, record);
		}
		return record;
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
	
	
	/*
	 * Must be coming from a matrix RREF or REF with the same height, otherwise will not work!
	 */
	public void modifyByRecord(ArrayList<Double> record) {
		
		record.remove(record.size()-1); //remove last element since there's nothing that follows, its an empty command
		
		int recordLen = record.size();
		int counter = 0;
		
		double command;
		double prevCommand = -999;
		
		boolean goingUpward = false;
		
		while (counter < recordLen) {
			command = record.get(counter);

			if (command == prevCommand) { //there will be a command with no parameters (last one), immediately followed by same command (same row)
				goingUpward = true;
			}
			if (!goingUpward) {
				if (command == -2.0) {
					swapRows(record.get(counter+1).intValue(), record.get(counter+2).intValue());
					counter += 3;
				}
				
				else if (command == -1.0) {
					multRowInPlace(record.get(counter+1).intValue(), record.get(counter+2));
					counter += 3;
				}
				
				else {
					int opRow = record.get(counter).intValue();
					int c = 1; //counter for counter :)
					for (int j = opRow + 1; j < mHeight; j++) {
						
						addRowsInPlace(j, multRow(opRow, record.get(counter+c)));
						c++;
					}
					counter += c; // (mHeight - opRow );
				}
			}
			else { //if we're going upward
				if (command == -2.0) {
					swapRows(record.get(counter+1).intValue(), record.get(counter+2).intValue());
					counter += 3;
				}
				
				else if (command == -1.0) {
					multRowInPlace(record.get(counter+1).intValue(), record.get(counter+2));
					counter += 3;
				}
				
				else {
					int opRow = record.get(counter).intValue();
					int c = 1; //counter for counter :)
					for (int j = opRow-1; j >= 0; j--) {
						addRowsInPlace(j, multRow(opRow, record.get(counter+c)));
						c++;
					}
					counter += c; // (mHeight - opRow ); //same thing
				}
			}
			prevCommand = command;
			
		}
		
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
	
	/*
	 * Overloaded for record keeping
	 */
	private void reduceRowsBelow(int m, int colToReduceBy, ArrayList<Double> record) {
		//still have to implement the record usage
		
	//	if (m == (mHeight - 1)) { //if its the last row, then return now. Not doing this causes an error in the addToRecord 
																			//because there are no parameters following it
	//		return;
	//	}
		addToRecord(record, 0, m, 000); //last param is just a blank
		
		//int loc = findFirstNonZeroIndexInRow(m); //unreliable!
		int loc = colToReduceBy;
		double val = mMatrix[m][loc];
		for (int j = m+1; j < mHeight; j++) {
			double factor = -1*mMatrix[j][loc]/val; //factor to multiply m by to add to j to get 0 in loc
			addToRecord(record, 1, factor, 000); //last param is blank
			addRowsInPlace(j, multRow(m, factor)); //add the upper row to the lower one in place to cancel out that column
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
	
	/*
	 * overloaded for record keeping
	 */
	private void reduceRowsAbove(int m, int colToReduceBy, ArrayList<Double> record) {
		int loc = colToReduceBy;
		double val = mMatrix[m][loc];
		addToRecord(record, 0, m, 000);
		for (int j = m-1; j >= 0; j--) {
			double factor = -1*mMatrix[j][loc]/val;
			addToRecord(record, 1, factor, 000);
			addRowsInPlace(j, multRow(m,factor));	//uses overloaded version
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
	
	//overloaded for record keeping
	private void reduceRow(int m, int indexToReduceBy, ArrayList<Double> record) {
		double factor = 1/mMatrix[m][indexToReduceBy];
		addToRecord(record, -1, m, factor);
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
	
	
	/*
	 * Codes:
	 * Multiply a single row: 	-1, followed by row and value
	 * Swap rows:				-2, followed by row1 and row 2
	 * No code: 				first value is the row r, followed by (matrixHeight - 1 - r)
	 * 								 values of factors to multiply r by and add to following rows
	 */
	
	private void addToRecord(ArrayList<Double> record, int opCode, double param1, double param2) {
		
		if (opCode == -2) { //code for SWAPping rows is -2
			record.add(-2.0);
			record.add(param1);	//row1 to swap
			record.add(param2);	//row2 to swap
		}
		
		else if (opCode == -1) { 	//code for MULTiplying a single row is -1
			record.add(-1.0);
			record.add(param1);	//the row
			record.add(param2);	//the factor for that row
		}
	
		//only requires 1 parameter, the current row
		else if (opCode == 0) { //code to initialize new row multiplication & addition
			record.add(param1); //record the row we're starting on
		}
		
		//only requires 1 parameter, the factor of the 'current' row to use
		else if (opCode == 1) { //continue the previous row mult & additions, only requires 1 param
			record.add(param1);
		}
		
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
	
	/*
	 * overloaded for record keeping
	 */
	public void swapRows(int row1, int row2, ArrayList<Double> record) {
		addToRecord(record, -2, row1, row2);
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
			Log.e(TAG, "Given column's height does not equal matrix's height, using minimum");
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
			n = Math.abs((mWidth + n))%mWidth;
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
			Log.e(TAG, "Given row's length does not match matrix rows' lengths, using minimum");
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
	public Matrix getCopy() {
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
