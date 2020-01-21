/*
 * Copyright (c) 2015 The Android Open Source Project
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

import android.content.res.Resources;
import android.graphics.Color;
import androidx.leanback.widget.Presenter;
import androidx.core.content.ContextCompat;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cw.tv_yt.R;
import com.cw.tv_yt.Utils;
import com.cw.tv_yt.ui.MainFragment;

import java.util.List;

public class GridItemPresenter extends Presenter {
    private final MainFragment mainFragment;

    List<String> mCategoryNames;
    public GridItemPresenter(MainFragment mainFragment, List<String> category_names) {
        this.mainFragment = mainFragment;
        mCategoryNames = category_names;
    }

    public GridItemPresenter(MainFragment mainFragment) {
        this.mainFragment = mainFragment;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        TextView view = new TextView(parent.getContext());

        Resources res = parent.getResources();
        int width = res.getDimensionPixelSize(R.dimen.grid_item_width);
        int height = res.getDimensionPixelSize(R.dimen.grid_item_height);

        view.setLayoutParams(new ViewGroup.LayoutParams(width, height));
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        view.setBackgroundColor(ContextCompat.getColor(parent.getContext(),
                R.color.default_background));
        view.setTextColor(Color.WHITE);
        view.setGravity(Gravity.CENTER);

        view.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
//                System.out.println("currentNavPosition = " + MainFragment.currentNavPosition);
//                System.out.println("hasFocus = " + hasFocus);
//                if(v.isSelected()) {
//                    System.out.println("v is selected");
//                }else {
//                    System.out.println("v is not selected");
//                }
                if(hasFocus)
                    view.setBackgroundColor(ContextCompat.getColor(parent.getContext(),
                            R.color.selected_background));
                else
                    view.setBackgroundColor(ContextCompat.getColor(parent.getContext(),
                            R.color.default_background));
            }
        });

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        ((TextView) viewHolder.view).setText(((String) item));

        // highlight category name
        try {
            int cate_num = Utils.getPref_focus_category_number(mainFragment.getContext());
            String cate_name = Utils.getPref_category_name(mainFragment.getContext(), cate_num);
            if (item.toString().equalsIgnoreCase(cate_name))
                ((TextView) viewHolder.view).setTextColor(mainFragment.getResources().getColor(R.color.current_preference_background));
            else
                ((TextView) viewHolder.view).setTextColor(mainFragment.getResources().getColor(R.color.lb_tv_white));
        } catch(Exception e)
        {
            e.printStackTrace();
        }

        // set item view id
        if(mCategoryNames != null) {
            for (int i = 0; i < mCategoryNames.size(); i++) {
                if (((String) item).equalsIgnoreCase(mCategoryNames.get(i))) {
                    viewHolder.view.setId(i);
                    break;
                }
            }
        }
    }


    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
    }


}
