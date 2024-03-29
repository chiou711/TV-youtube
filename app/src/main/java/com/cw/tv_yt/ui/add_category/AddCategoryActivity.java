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

package com.cw.tv_yt.ui.add_category;

import android.os.Bundle;

import com.cw.tv_yt.R;
import com.cw.tv_yt.ui.LeanbackActivity;

/*
 * AddCategoryActivity that loads VerticalGridFragment
 */
public class AddCategoryActivity extends LeanbackActivity {
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_link_src_grid);
        getWindow().setBackgroundDrawableResource(R.drawable.grid_bg);
    }
}
