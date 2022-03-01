package com.cw.tv_yt.import_new;

import android.os.AsyncTask;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import androidx.fragment.app.FragmentActivity;

import com.cw.tv_yt.R;

public class ImportAsyncTask extends AsyncTask<Void, Integer, Void> {

	ProgressBar bar;
	boolean enableSaveDB;
	FragmentActivity act;
	String filePath;
	File mFile;
	View rootView;
	View mViewFile,mViewFileProgressBar;
	private TextView mTitleViewText;
	private TextView mBodyViewText;
	public ParseJsonToDB importObject;

	public ImportAsyncTask(FragmentActivity _act,String file_path,View root_view){
		act = _act;
		filePath = file_path;
		mFile = new File(file_path);
		rootView = root_view;
		mViewFile = rootView.findViewById(R.id.view_file);
		mViewFileProgressBar = rootView.findViewById(R.id.view_file_progress_bar);

		mTitleViewText = (TextView) rootView.findViewById(R.id.view_title);
		mBodyViewText = (TextView) rootView.findViewById(R.id.view_body);
	}
	public void setProgressBar(ProgressBar bar) {
		this.bar = bar;
		mViewFile.setVisibility(View.GONE);
		mViewFileProgressBar.setVisibility(View.VISIBLE);
		bar.setVisibility(View.VISIBLE);
	}

	public void enableSaveDB(boolean enable)
	{
		enableSaveDB = enable;
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		super.onProgressUpdate(values);
		if (this.bar != null) {
			bar.setProgress(values[0]);
		}
	}

	@Override
	protected Void doInBackground(Void... params)
	{
		mFile = new File(filePath);
		importObject = new ParseJsonToDB(filePath, act);

		if(enableSaveDB)
			importObject.handleParseJsonFileAndInsertDB();
		else
			importObject.handleViewJson();

		while (ParseJsonToDB.isParsing) {
			try { Thread.sleep(100); } catch (InterruptedException e) {}
		}

		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		super.onPostExecute(result);
		bar.setVisibility(View.GONE);
		mViewFile.setVisibility(View.VISIBLE);

		if(enableSaveDB)
		{
			act.getSupportFragmentManager().popBackStack();
			Import_fileListAct.isBack_fileView = true;
			Toast.makeText(act,R.string.import_finish,Toast.LENGTH_SHORT).show();
		}
		else
		{
			// show Import content
			mTitleViewText.setText(mFile.getName());
			mBodyViewText.setText(importObject.fileBody);
		}

	}
}