/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cw.tv_yt.operation;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cw.tv_yt.R;

import java.io.File;
import java.io.FileInputStream;

import androidx.fragment.app.Fragment;

public class Import_fileView extends Fragment
{

    private TextView mTitleViewText;
    private TextView mBodyViewText;
    String filePath;
    static File mFile;
    View mViewFile,mViewFileProgressBar;
    public static boolean isAddingNewFolder = true;
    View rootView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		System.out.println("Import_fileView / _onCreateView");
        rootView = inflater.inflate(R.layout.sd_file_view,container, false);

		mViewFile = rootView.findViewById(R.id.view_file);
		mViewFileProgressBar = rootView.findViewById(R.id.view_file_progress_bar);

		mTitleViewText = (TextView) rootView.findViewById(R.id.view_title);
		mBodyViewText = (TextView) rootView.findViewById(R.id.view_body);
		ProgressBar progressBar = (ProgressBar) rootView.findViewById(R.id.import_progress);

		ImportAsyncTask viewTask = null;

		if(savedInstanceState == null) {
			viewTask = new ImportAsyncTask(getActivity(),filePath,rootView);
			viewTask.setProgressBar(progressBar);
			viewTask.enableSaveDB(false);
			viewTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			mFile = new File(filePath);
			mTitleViewText.setText(mFile.getName());
			mBodyViewText.setText(viewTask.importObject.fileBody);
		}

		//set title color
		mTitleViewText.setTextColor(Color.rgb(255,255,255));
		mTitleViewText.setBackgroundColor(Color.rgb(38,87,51));
		//set body color
		mBodyViewText.setTextColor(Color.rgb(255,255,255));
		mBodyViewText.setBackgroundColor(Color.rgb(38,87,51));

		// back button
		Button backButton = (Button) rootView.findViewById(R.id.view_back);
		backButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_back, 0, 0, 0);

		// confirm button
		Button confirmButton = (Button) rootView.findViewById(R.id.view_confirm);

		// delete button
		Button deleteButton = (Button) rootView.findViewById(R.id.view_delete);
		deleteButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_delete , 0, 0, 0);

		// back
		backButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View view) {
                getActivity().getSupportFragmentManager().popBackStack();
                Import_fileListAct.isBack_fileView = true;
			}
		});

		// delete the file whose content is showing
		deleteButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View view)
			{
				AlertDialog.Builder builder1 = new AlertDialog.Builder(getActivity());
				builder1.setTitle("Confirmation")//TODO locale
						.setMessage("Do you want to delete this file?" +
								" (" + mFile.getName() +")" )
						.setNegativeButton("No", new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface dialog1, int which1) {/*nothing to do*/}
						})
						.setPositiveButton("Yes", new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface dialog1, int which1)
							{
								mFile.delete();
                                getActivity().getSupportFragmentManager().popBackStack();
                                Import_fileListAct.isBack_fileView = true;
							}
						})
						.show();
			}
		});

		// confirm to import view to DB
		confirmButton.setOnClickListener(new View.OnClickListener()
		{

			public void onClick(View view)
			{
				isAddingNewFolder = true;
				ProgressBar progressBar = (ProgressBar) rootView.findViewById(R.id.import_progress);
				ImportAsyncTask confirmTask = new ImportAsyncTask(getActivity(),filePath,rootView);
				confirmTask.setProgressBar(progressBar);
				confirmTask.enableSaveDB(true);
				confirmTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
		});

		// set focus on
        confirmButton.requestFocus();

        return rootView;

	}

	@Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    	System.out.println("Import_fileView / onCreate");
        Bundle arguments = getArguments();
        filePath = arguments.getString("KEY_FILE_PATH");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

}
