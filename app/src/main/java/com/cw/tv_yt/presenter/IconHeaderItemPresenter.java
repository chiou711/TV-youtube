/*
 * Copyright (c) 2016 The Android Open Source Project
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

package com.cw.tv_yt.presenter;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.fragment.app.FragmentActivity;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.RowHeaderPresenter;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.cw.tv_yt.R;
import com.cw.tv_yt.Utils;

import static com.cw.tv_yt.ui.MainFragment.getPlaylistsCount;

public class IconHeaderItemPresenter extends RowHeaderPresenter {

    private float mUnselectedAlpha;
    FragmentActivity act;

    public IconHeaderItemPresenter(FragmentActivity _act){
        act = _act;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup) {
        mUnselectedAlpha = viewGroup.getResources()
                .getFraction(R.fraction.lb_browse_header_unselect_alpha, 1, 1);
        LayoutInflater inflater = (LayoutInflater) viewGroup.getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.icon_header_item, null);
        view.setAlpha(mUnselectedAlpha); // Initialize icons to be at half-opacity.

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        HeaderItem headerItem = ((ListRow) item).getHeaderItem();
        View rootView = viewHolder.view;
        rootView.setFocusable(true);

        long id = ((ListRow) item).getId();
//        System.out.println(" IconHeaderItemPresenter /　_onLongClick / id　= " + id);

        // for playlist rows
        if( (id >= 1) && (id <= getPlaylistsCount()) )  {
            // set header icon
            ImageView iconView = (ImageView) rootView.findViewById(R.id.header_icon);
            Drawable icon = rootView.getResources().getDrawable(R.drawable.lb_ic_actions_right_arrow, null);
            iconView.setImageDrawable(icon);

            // long click listener for Delete playlist
            rootView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    // delete playlist row
                    if(Build.VERSION.SDK_INT >= 26)
                        Utils.confirmDeletePlaylist(act,headerItem.getName());
                    return false;
                }
            });
        }

        TextView label = (TextView) rootView.findViewById(R.id.header_label);
        label.setText(headerItem.getName());
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        // no op
    }

    // TODO: This is a temporary fix. Remove me when leanback onCreateViewHolder no longer sets the
    // mUnselectAlpha, and also assumes the xml inflation will return a RowHeaderView.
    @Override
    protected void onSelectLevelChanged(RowHeaderPresenter.ViewHolder holder) {
        holder.view.setAlpha(mUnselectedAlpha + holder.getSelectLevel() *
                (1.0f - mUnselectedAlpha));
    }
}
