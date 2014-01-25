package com.JS.musictranscribe;


public class ListElement {

	private String mTitle;
	private boolean mIsChecked;

	public ListElement(String title, boolean isChecked) {
		mTitle = title;
		mIsChecked = isChecked;
	}
	
	public String getTitle() {
		return mTitle;
	}
	
	public boolean isChecked() {
		return mIsChecked;
	}
	
	public void setChecked(boolean checked) {
		mIsChecked =  checked;
	}

}
