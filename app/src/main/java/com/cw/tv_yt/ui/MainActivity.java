/*
 * Copyright (c) 2014 The Android Open Source Project
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

package com.cw.tv_yt.ui;

import android.content.Intent;
import android.os.Bundle;

import com.cw.tv_yt.Pref;
import com.cw.tv_yt.R;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

/*
 * MainActivity class that loads MainFragment.
 */
public class MainActivity extends LeanbackActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        System.out.println("-------------------------------------");
        System.out.println("--------New start Main Activity------");
        System.out.println("-------------------------------------");

        //todo temporary mark
//        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
//        if(!sharedPreferences.getBoolean(OnboardingFragment.COMPLETED_ONBOARDING, false)) {
//            // This is the first time running the app, let's go to onboarding
//            startActivity(new Intent(this, OnboardingActivity.class));
//        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        System.out.println("MainActivity / _onActivityResult");
        if(requestCode == MainFragment.VIDEO_DETAILS_INTENT) {
            if(data != null) {
                int action = data.getIntExtra("KEY_DELETE",0);
                if (action == Pref.DB_DELETE)
                {
                    finish();
                    // start new MainActivity
                    Intent new_intent = new Intent(this, MainActivity.class);
                    new_intent.addFlags(FLAG_ACTIVITY_CLEAR_TASK);
                    new_intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                    startActivity(new_intent);
                }
            } else
            {
                //do nothing for non-action case
            }
        }
    }
}
