package com.cw.tv_yt.import_new;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cw.tv_yt.R;
import com.cw.tv_yt.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class Import_fileListAct extends FragmentActivity implements FragmentManager.OnBackStackChangedListener
{
    private List<String> filePathArray = null;
    List<String> fileNames = null;
    ListView listView;
    public FragmentManager fragmentManager;
    public FragmentManager.OnBackStackChangedListener onBackStackChangedListener;
    int linkSrcNum;

    @Override
    public void onCreate(Bundle bundle) 
    {

        linkSrcNum = getIntent().getExtras().getInt("link_source_number");
        doCreate();

        super.onCreate(bundle);
    }


    void doCreate(){
        setContentView(R.layout.sd_file_list);
        System.out.println("Import_fileListAct / _onCreate");

        listView = (ListView) findViewById(R.id.file_list);
        listView.setItemsCanFocus(true);

        // back button
        Button backButton = (Button) findViewById(R.id.view_back);
        backButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_back, 0, 0, 0);

        // do cancel
        backButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                finish();
            }
        });

        fragmentManager = getSupportFragmentManager();
        onBackStackChangedListener = this;
        fragmentManager.addOnBackStackChangedListener(onBackStackChangedListener);

        // data package folder
        // /storage/emulated/0/Android/data/com.cw.tv_yt/files/Documents
        String dirString = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).toString();

        System.out.println("Import_fileListAct / _onResume / dirString = " + dirString);

        getFiles(new File(dirString).listFiles());
    }

    @Override
    public void onResume() {
        super.onResume();
    }


    public static boolean isBack_fileView;
    /**
     *  on Back button pressed
     *
     */
    @Override
    public void onBackPressed()
    {
        System.out.println("Import_fileListAct / _onBackPressed");

        if (isBack_fileView == false)
        {
            getSupportFragmentManager().popBackStack();
            isBack_fileView = true;
        }
        else
            super.onBackPressed();
    }

    @Override
    public void onBackStackChanged() {
        int backStackEntryCount = getFragmentManager().getBackStackEntryCount();
        System.out.println("Import_fileListAct / _onBackStackChanged / backStackEntryCount = " + backStackEntryCount);
        if(backStackEntryCount == 0) // Import_fileView fragment
        {
            if(isBack_fileView)
            {
                String dirString = new File(currFilePath).getParent();
                File dir = new File(dirString);
                getFiles(dir.listFiles());

                View view1 = findViewById(R.id.view_back_btn_bg);
                view1.setVisibility(View.VISIBLE);
                View view2 = findViewById(R.id.file_list_title);
                view2.setVisibility(View.VISIBLE);
            }
        }
    }

    String currFilePath;
    // on list item click
    public void onListItemClick(int position)
    {
        System.out.println("Import_fileListAct / _onListViewItemClick / position = " + position);
        int selectedRow = position;
        if(selectedRow == 0)
        {
        	//root
            getFiles(new File("/").listFiles());
        }
        else
        {
            currFilePath = filePathArray.get(selectedRow);
            System.out.println("Import_fileListAct / _onListViewItemClick / filePath = " + currFilePath);
            final File file = new File(currFilePath);
            if(file.isDirectory())
            {
            	//directory
                getFiles(file.listFiles());
            }
            else
            {
            	// view the selected file's content
            	if( file.isFile() &&
                   (file.getName().contains("JSON") ||
                    file.getName().contains("json")   ))
            	{
//		           	Intent i = new Intent(this, Import_fileView.class);
//		           	i.putExtra("FILE_PATH", filePath);
//		           	startActivity(i);

                    View view1 = findViewById(R.id.view_back_btn_bg);
                    view1.setVisibility(View.GONE);
                    View view2 = findViewById(R.id.file_list_title);
                    view2.setVisibility(View.GONE);

                    isBack_fileView = false;
                    Import_fileView fragment = new Import_fileView();
                    final Bundle args = new Bundle();
                    args.putString("KEY_FILE_PATH", currFilePath);
                    args.putInt("link_source_number", linkSrcNum);
                    fragment.setArguments(args);
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.setCustomAnimations(R.anim.fragment_slide_in_left, R.anim.fragment_slide_out_left, R.anim.fragment_slide_in_right, R.anim.fragment_slide_out_right);
                    transaction.replace(R.id.file_list_linear, fragment,"import_view").addToBackStack("import_view_stack").commit();
            	}
            	else
            	{
            		Toast.makeText(this,"file_not_found",Toast.LENGTH_SHORT).show();
                    String dirString = new File(currFilePath).getParent();
                    File dir = new File(dirString);
                    getFiles(dir.listFiles());
            	}
            }
        }
    }

    private void getFiles(File[] files)
    {
        if(files == null)
        {
        	Toast.makeText(this,"toast_import_SDCard_no_file",Toast.LENGTH_SHORT).show();
        	this.finish();
        }
        else
        {
//        	System.out.println("files length = " + files.length);
            filePathArray = new ArrayList<String>();
            fileNames = new ArrayList<String>();
            filePathArray.add("");
            fileNames.add("ROOT");

            // simple alphabetic sort
//            Arrays.sort(files);

            // sort by modification
//            Arrays.sort(files, new Comparator<File>() {
//                public int compare(File f1, File f2) {
////                    return Long.compare(f1.lastModified(), f2.lastModified());//old first
//                    return Long.compare(f2.lastModified(), f1.lastModified());//new first
//                }
//            });

            // sort by alphabetic
            Arrays.sort(files, new FileNameComparator());

	        for(File file : files)
	        {
                System.out.println("Import_fileListAct / _getFiles / file.getPath() = " + file.getPath());
                System.out.println("Import_fileListAct / _getFiles / file.getName() = " + file.getName());
                filePathArray.add(file.getPath());
                fileNames.add(file.getName());
            }
            FilenameAdapter fileList = new FilenameAdapter(this,
                                                           R.layout.sd_file_list_row,
                                                           fileNames);

            listView.setAdapter(fileList);
        }

    }

    // Directory group and file group, both directory and file are sorted alphabetically
    // cf. https://stackoverflow.com/questions/24404055/sort-filelist-folders-then-files-both-alphabetically-in-android
    private class FileNameComparator implements Comparator<File>{
        public int compare(File lhsS, File rhsS){
            File lhs = new File(lhsS.toString().toLowerCase(Locale.US));
            File rhs= new File(rhsS.toString().toLowerCase(Locale.US));
            if (lhs.isDirectory() && !rhs.isDirectory()){
                // Directory before File
                return -1;
            } else if (!lhs.isDirectory() && rhs.isDirectory()){
                // File after directory
                return 1;
            } else {
                // Otherwise in Alphabetic order...
                return lhs.getName().compareTo(rhs.getName());
            }
        }
    }

    // File name array for setting focus and file name
    class FilenameAdapter extends ArrayAdapter
    {
        public FilenameAdapter(Context context,int resource,List objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView,ViewGroup parent) {
            if(convertView == null)
            {
                convertView = getLayoutInflater().inflate(R.layout.sd_file_list_row, parent, false);
            }

//            String appName = getString(R.string.app_name);
            String appName = getString(R.string.dir_name);
            convertView.setFocusable(true);
            convertView.setClickable(true);
            TextView tv = (TextView)convertView.findViewById(R.id.text1);
            tv.setText(fileNames.get(position));
            if(fileNames.get(position).equalsIgnoreCase("sdcard")   ||
               fileNames.get(position).equalsIgnoreCase(appName)    ||
               fileNames.get(position).equalsIgnoreCase("LiteNote") ||
               fileNames.get(position).equalsIgnoreCase("Download")   )
                tv.setTypeface(null, Typeface.BOLD);
            else
                tv.setTypeface(null, Typeface.NORMAL);

            final int item = position;
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onListItemClick(item);
                }
            });
            return convertView;
        }
    }

}