package com.JS.musictranscribe;

import java.util.ArrayList;

import com.JS.musictranscribe.ListElementAdapter.OnListElementClickedInterface;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

public class MyListFragment extends Fragment implements ListElementAdapter.OnListElementClickedInterface {

	private static final String TAG = "MyListFragment";

	private Button mDeleteFilesButton;
	private ArrayList<ListElement> mFileListElements;
	private ListElement mSelectedListElement = null;
	private ListView mListView;
	private ListElementAdapter mAdapter;
	
	//for communicating with parent activity
	private OnSomethingCheckedInterface mCommunicatorInterface;
	

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCommunicatorInterface = (OnSomethingCheckedInterface) activity;
        } catch (ClassCastException e) {
            Log.e(TAG, activity.toString() + " must implement OnHeadlineSelectedListener");
            Log.e(TAG,"Bailing!");
            return;
        }
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.file_list_fragment, container, false);

		mFileListElements = Helper.getFileListElements(getActivity());
		String checkedTitle = Helper.getStringPref(Helper.ACTIVE_MAPDATA_FILE_KEY, getActivity()); // this is stored as a string, the title of the file
		for (int i = 0; i < mFileListElements.size(); i++) {
			Log.i(TAG,"FILE " + mFileListElements.get(i).getTitle() + " comp to " + checkedTitle);
			if (mFileListElements.get(i).getTitle().equals(checkedTitle)) {
				mSelectedListElement = mFileListElements.get(i);
				break;
			}
		}

		mAdapter = new ListElementAdapter(getActivity(), R.layout.list_element, mFileListElements, mSelectedListElement, this);

		mListView = (ListView) view.findViewById(R.id.file_list_view);
		mListView.setAdapter(mAdapter);
		Log.i(TAG,"Set Adapter");

		
		mDeleteFilesButton = (Button) view.findViewById(R.id.delete_all_files_button);
		mDeleteFilesButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.i(TAG, "Going to delete files");
				Helper.deleteAllPrivFiles(getActivity());
				mAdapter.clear();
				mAdapter.notifyDataSetChanged();
			}
		});

		return view;
	}
	
	@Override
	public void onElementChecked() {
		mCommunicatorInterface.onSomethingChecked();
	}
	
	public interface OnSomethingCheckedInterface {
		public void onSomethingChecked();
	}

}