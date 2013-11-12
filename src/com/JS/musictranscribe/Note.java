package com.JS.musictranscribe;


public class Note {

	private int mReference;
	private int mLength; 	//in milliseconds
							//decoupling length and static BPM/BPmeasure/BPQN lets you recalculate only when drawing the notes on screen :)
	
	private static int mBeatsPerMinute;
	private static int mBeatsPerMeasure;
	private static int mBeatsPerQuarterNote;

	public Note(int ref, int noteLength) {
		this.mReference = ref;
		this.mLength = noteLength;
	}

	
	
	public String getName() {
		String name = "";
		return name;
	}
	
	public void setBeatsPerMinute(int bpm) {
		mBeatsPerMinute = bpm;
	}
	
	public void setBeatsPerMeasure(int b) {
		mBeatsPerMeasure = b;
	}
	
	public void setBeatsPerQuarterNote(int b) {
		mBeatsPerQuarterNote = b;
	}


}

