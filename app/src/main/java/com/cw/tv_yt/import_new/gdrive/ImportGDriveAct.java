/**
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.cw.tv_yt.import_new.gdrive;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.cw.tv_yt.R;
import com.cw.tv_yt.Utils;
import com.cw.tv_yt.import_new.ParseJsonToDB;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


/**
 * The main {@link Activity} for the Drive API migration sample app.
 *
 *  Created: 2022/3/11
 *    - open Google drive file directly by file picker
 *    - overwrite it with all JSON content
 */
public class ImportGDriveAct extends AppCompatActivity {
    private static final String TAG = "ImportGDriveAct";

    private static final int REQUEST_CODE_SIGN_IN = 1;
    private static final int REQUEST_CODE_OPEN_DOCUMENT = 2;

    private DriveServiceHelper mDriveServiceHelper;

    private EditText mFileTitleEditText;
    private TextView mJsonText;
    final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 97;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // check Read/Write storage permission
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)//api23
        {
            // check permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED)
            {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE  },
                        PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            }
            else
                doCreate();
        }
        else
            doCreate();

    }

    // do create
    void doCreate(){

        setContentView(R.layout.import_gdrive_json);

        // Store the EditText boxes to be updated when files are opened/created/modified.
        mFileTitleEditText = findViewById(R.id.file_title_edittext);

        mJsonText = findViewById(R.id.json_text);
        mJsonText.setMovementMethod(new ScrollingMovementMethod());

        // Set the onClick listeners for the button bar.
        findViewById(R.id.cancel_btn).setOnClickListener(view -> exit());
        findViewById(R.id.import_btn).setOnClickListener(view -> importConfirm());

        //todo request sign in
//        requestSignIn();

        // select file chooser for Google Drive access,
        // due to sign in issue on Android TV platform
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            //todo chooser icon no focus
            startActivityForResult(
                    Intent.createChooser(intent, "Select a File to download"),
                    REQUEST_CODE_OPEN_DOCUMENT);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // callback of granted permission
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        System.out.println("grantResults.length =" + grantResults.length);
        switch (requestCode)
        {
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
            {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    doCreate();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    finish();
                }
                return;
            }//case
        }//switch
    }

    void exit() {
        finish();
    }

    // Import Confirm
    void importConfirm(){

        if(Utils.isEmptyString(jsonContent)) {
            Toast.makeText(this,R.string.toast_no_json_found, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // dialog for confirming
        AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(this);

        View dialogView = getLayoutInflater().inflate(R.layout.confirm_gdrive_json, null);
        dlgBuilder.setTitle(R.string.confirm_dialog_title);
        dlgBuilder.setMessage(R.string.toast_import_gdrive_json);

        dlgBuilder.setView(dialogView);
        final AlertDialog dialog1 = dlgBuilder.create();
        dialog1.show();

        // cancel button
        Button btnCancel = (Button) dialogView.findViewById(R.id.cancel_gdrive_json);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog1.dismiss();
            }
        });

        // continue button
        Button btnContinue = (Button) dialogView.findViewById(R.id.confirm_gdrive_json);
        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //hide the menu
                dialog1.dismiss();

                // import JSON
                importJson();
            }
        });
    }

    // import Google Drive JSON file content
    void importJson() {
        //import Json and Add to DB
        ParseJsonToDB importObject = new ParseJsonToDB(ImportGDriveAct.this);
        JSONObject jsonObj;

        try {
            jsonObj = new JSONObject(jsonContent);
            importObject.parseJsonAndInsertDB(jsonObj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        Log.d(TAG, "_onActivityResult / requestCode = " + requestCode);
        Log.d(TAG, "_onActivityResult / resultCode = " + resultCode);
        Log.d(TAG, "_onActivityResult / resultData (Intent)= " + resultData);
        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                Log.d(TAG, "_onActivityResult / requestCode = REQUEST_CODE_SIGN_IN");
                if (resultCode == Activity.RESULT_OK && resultData != null) {
                    handleSignInResult(resultData);
                } else if (resultCode == Activity.RESULT_CANCELED)
                    Log.d(TAG, "_onActivityResult / resultCode = RESULT_CANCELED");

                handleSignInResult(resultData);
                break;

            case REQUEST_CODE_OPEN_DOCUMENT:
                if (resultCode == Activity.RESULT_OK && resultData != null) {
                      Uri uri = resultData.getData();
                      Log.d(TAG, "uri = " + uri);
                      Log.d(TAG, " getLocalRealPathByUri = " + getLocalRealPathByUri(this,uri));

                      if (uri != null)
                          openFileFromFilePickerSaved(getLocalRealPathByUri(this,uri));
                      else
                          Log.d(TAG, "uri is null");
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, resultData);
    }


    // get local real path by URI
    public static String getLocalRealPathByUri(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        catch (Exception e){
            return null;
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Starts a sign-in activity using {@link #REQUEST_CODE_SIGN_IN}.
     */
    private void requestSignIn() {
        Log.d(TAG, "Requesting sign-in");

        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                        .build();

        GoogleSignInClient client = GoogleSignIn.getClient(this, signInOptions);

        // The result of the sign-in Intent is handled in onActivityResult.
        Intent intent = client.getSignInIntent(); //todo Error original point?

        Log.d(TAG,"intent (=client.getSignInIntent) = " + intent);
        startActivityForResult(intent, REQUEST_CODE_SIGN_IN);
    }

    /**
     * Handles the {@code result} of a completed sign-in activity initiated from {@link
     * #requestSignIn()}.
     */
    private void handleSignInResult(Intent result) {
        Log.d(TAG, "_handleSignInResult / result = " + result);
        GoogleSignIn.getSignedInAccountFromIntent(result)
                .addOnSuccessListener(googleAccount -> {
                        Log.d(TAG, "Signed in as " + googleAccount.getEmail());

                        // Use the authenticated account to sign in to the Drive service.
                        GoogleAccountCredential credential =
                            GoogleAccountCredential.usingOAuth2(
                                    this, Collections.singleton(DriveScopes.DRIVE_FILE));
                        credential.setSelectedAccount(googleAccount.getAccount());

                        Drive googleDriveService =
                            new Drive.Builder(
                                    new NetHttpTransport(),
                                    new GsonFactory(),
                                    credential)
                               .setApplicationName("ImportGDriveAct")
                               .build();

                        // The DriveServiceHelper encapsulates all REST API and SAF functionality.
                        // Its instantiation is required before handling any onClick actions.
                        mDriveServiceHelper = new DriveServiceHelper(googleDriveService);

                        // open file picker
                        openFilePicker();
                    })
                .addOnFailureListener(exception -> Log.e(TAG, "Unable to sign in.", exception));
    }

    /**
     * Opens the Storage Access Framework file picker using {@link #REQUEST_CODE_OPEN_DOCUMENT}.
     */
    private void openFilePicker() {
        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Opening file picker.");

            Intent pickerIntent = mDriveServiceHelper.createJsonFilePickerIntent();
            // The result of the SAF Intent is handled in onActivityResult.
            startActivityForResult(pickerIntent, REQUEST_CODE_OPEN_DOCUMENT);
        }
    }

    // open file with SAF, without using file Id
    Uri uri;

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    String jsonContent;
    /**
     * Opens a file from its {@code uri} returned from the Storage Access Framework file picker
     * initiated by {@link #openFilePicker()}.
     */
    private void openFileFromFilePicker(Uri uri) {

        setUri(uri);

        if (mDriveServiceHelper != null)
        {
            Log.d(TAG, "Opening " + uri.getPath());

            mDriveServiceHelper.openFileUsingStorageAccessFramework(getContentResolver(), uri)
                    .addOnSuccessListener(nameAndContent -> {
                            String name = nameAndContent.first;
                            String content = nameAndContent.second;

                            jsonContent = content;

                            mFileTitleEditText.setText(name);
                            mJsonText.setText(content);

                            // Files opened through SAF cannot be modified.
                            //setReadOnlyMode();

                        })
                    .addOnFailureListener(exception ->
                            Log.e(TAG, "Unable to open file from picker.", exception));
        }
        else
            Log.d(TAG, "Opening failed: " + uri.getPath());
    }


    // open file from file chooser
    private void openFileFromFilePickerSaved(String path) {

        File sdcard = Environment.getExternalStorageDirectory();
        File file = new File(path);
        Log.d("sdcard path =",sdcard.getPath());
        Log.d("file path =",file.getPath());

        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        jsonContent = text.toString();

        mFileTitleEditText.setText(R.string.toast_import_gdrive_json);
        mJsonText.setText(jsonContent);
        setReadOnlyMode();

    }


    /**
     * Updates the UI to read-only mode.
     */
    private void setReadOnlyMode() {
        mFileTitleEditText.setEnabled(false);
        mJsonText.setEnabled(false);
    }

}
