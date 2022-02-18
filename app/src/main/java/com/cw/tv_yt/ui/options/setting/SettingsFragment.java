/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.cw.tv_yt.ui.options.setting;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.preference.PreferenceFragment;
import androidx.leanback.preference.LeanbackPreferenceFragment;
import androidx.leanback.preference.LeanbackSettingsFragment;
import androidx.preference.DialogPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.cw.tv_yt.R;
import com.cw.tv_yt.Utils;
import com.cw.tv_yt.data.DbHelper;
import com.cw.tv_yt.data.VideoContract;
import com.cw.tv_yt.data.VideoProvider;
import com.cw.tv_yt.ui.MainActivity;

import java.util.Objects;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class SettingsFragment extends LeanbackSettingsFragment
        implements DialogPreference.TargetFragment {
    private final static String PREFERENCE_RESOURCE_ID = "preferenceResource";
    private final static String PREFERENCE_ROOT = "root";
    private PreferenceFragment mPreferenceFragment;

    @Override
    public void onPreferenceStartInitialScreen() {
        mPreferenceFragment = buildPreferenceFragment(R.xml.settings, null);
        startPreferenceFragment(mPreferenceFragment);
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragment preferenceFragment,
        Preference preference) {
        return false;
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragment preferenceFragment,
        PreferenceScreen preferenceScreen) {
        PreferenceFragment frag = buildPreferenceFragment(R.xml.settings,
            preferenceScreen.getKey());
        startPreferenceFragment(frag);
        return true;
    }

    @Override
    public Preference findPreference(CharSequence charSequence) {
        return mPreferenceFragment.findPreference(charSequence);
    }

    private PreferenceFragment buildPreferenceFragment(int preferenceResId, String root) {
        PreferenceFragment fragment = new PrefFragment();
        Bundle args = new Bundle();
        args.putInt(PREFERENCE_RESOURCE_ID, preferenceResId);
        args.putString(PREFERENCE_ROOT, root);
        fragment.setArguments(args);
        return fragment;
    }

    public static class PrefFragment extends LeanbackPreferenceFragment {

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            String root = getArguments().getString(PREFERENCE_ROOT, null);
            int prefResId = getArguments().getInt(PREFERENCE_RESOURCE_ID);
            if (root == null) {
                addPreferencesFromResource(prefResId);
            } else {
                setPreferencesFromResource(prefResId, root);
            }
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
//            if (preference.getKey().equals(getString(R.string.pref_key_login))) {
//                // Open an AuthenticationActivity
//                startActivity(new Intent(getActivity(), AuthenticationActivity.class));
//            }

            if (preference.getKey().equals(getString(R.string.pref_key_renew))) {
                Utils.setPref_link_source_number(getActivity(), 2);
                startRenewFetchService();

                // remove reference keys
                Utils.removePref_focus_category_number(getActivity());

                int countVideoTables = Utils.getVideoTablesCount(getActivity());

                // remove category name key
                for (int i = 1; i <= countVideoTables; i++)
                    Utils.removePref_category_name(getActivity(), i);
            }

            return super.onPreferenceTreeClick(preference);
        }

        // start fetch service by URL string
        private void startRenewFetchService() {
            System.out.println("SelectLinkSrcFragment / _startFetchService");
            // delete database
            try {
                System.out.println("SelectLinkSrcFragment / _startFetchService / will delete DB");
                Objects.requireNonNull(getActivity()).deleteDatabase(DbHelper.DATABASE_NAME);

                ContentResolver resolver = getActivity().getContentResolver();
                ContentProviderClient client = resolver.acquireContentProviderClient(VideoContract.CONTENT_AUTHORITY);
                assert client != null;
                VideoProvider provider = (VideoProvider) client.getLocalContentProvider();

                assert provider != null;
                provider.mContentResolver = resolver;
                provider.mOpenHelper.close();

                provider.mOpenHelper = new DbHelper(getActivity());
                provider.mOpenHelper.setWriteAheadLoggingEnabled(false);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    client.close();
                else
                    client.release();

            } catch (Exception e) {
                e.printStackTrace();
            }

            // start new MainActivity
            Intent new_intent = new Intent(getActivity(), MainActivity.class);
            new_intent.addFlags(FLAG_ACTIVITY_CLEAR_TASK);
            new_intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            Objects.requireNonNull(getActivity()).startActivity(new_intent);
        }

    }
}