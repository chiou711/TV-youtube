package com.cw.tv_yt.ui;

import android.content.Intent;

import com.cw.tv_yt.ui.options.browse_category.SearchActivity;

import androidx.fragment.app.FragmentActivity;

/**
 * This parent class contains common methods that run in every activity such as search.
 */
public abstract class LeanbackActivity extends FragmentActivity {
    @Override
    public boolean onSearchRequested() {
        startActivity(new Intent(this, SearchActivity.class));
        return true;
    }
}
