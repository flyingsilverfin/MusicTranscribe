package com.JS.musictranscribe;

import java.util.ArrayList;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

public class ListElementAdapter extends ArrayAdapter<ListElement> {

	private static final String TAG = "ListElementAdapter";
	private Context mContext;
	private int mLayoutResId;
	private ArrayList<ListElement> mElements;
	private ListElement mSelectedElement;
	
	//for communicating with parent class (MyListFragment)
	private OnListElementClickedInterface mCommunicatorInterface;
	

	public ListElementAdapter(Context context, int layoutResId,
			ArrayList<ListElement> elements, ListElement selectedListElement, OnListElementClickedInterface communicatorInterface) {
		super(context, layoutResId, elements);
		mContext = context;
		mLayoutResId = layoutResId;
		mElements = elements;
		mSelectedElement = selectedListElement;
		mCommunicatorInterface = communicatorInterface;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) { //need final int for mElements.get(position)
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = inflater.inflate(mLayoutResId, parent, false);
		final CheckBox checkbox = (CheckBox) rowView.findViewById(R.id.element_checkbox);
		checkbox.setChecked(mElements.get(position).isChecked());
		TextView title = (TextView) rowView.findViewById(R.id.element_title);
		title.setText(mElements.get(position).getTitle());

		rowView.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.i(TAG, "Item Clicked!");
				if (mSelectedElement != null) { //would be null the first time around
					mSelectedElement.setChecked(false);
				} //do above first in case we clicked the same thing, so that it doesn't unclick
				
				mElements.get(position).setChecked(true);
				//uncheck previously checked ListElement
				//set the tracker variable to the new checked ListElement
				mSelectedElement = mElements.get(position);
				//update the preference for persistence
				Helper.setStringPref(Helper.ACTIVE_MAPDATA_FILE_KEY, mSelectedElement.getTitle(), getContext());
				notifyDataSetChanged(); //somehow this detects if the checkbox has changed. keeping a reference to checkbox above alive? No idea...
				mCommunicatorInterface.onElementChecked();
			}
		});
		return rowView;
	}
	
	
	public interface OnListElementClickedInterface {
		public void onElementChecked();
	}

}
