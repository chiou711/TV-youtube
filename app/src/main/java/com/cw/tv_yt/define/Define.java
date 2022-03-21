/*
 * Copyright (C) 2022 CW Chiu
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

package com.cw.tv_yt.define;

/*
 * Created by CW on 2022/03/01
 * Modified by CW on 2022/03/01
 *
 */
public class Define {

    // --- setting ---
    // auto play by list (default: by list)
    public final static boolean DEFAULT_AUTO_PLAY_BY_LIST = true;

    // auto play by category (default: by list)
    public final static boolean DEFAULT_AUTO_PLAY_BY_CATEGORY = false;

    // show duration (default: disabled, show duration could cause card view display lag)
    public final static boolean DEFAULT_SHOW_YOUTUBE_DURATION = false;

    // select file manager app (default: disabled, user enabled this will need advanced operation)
    public final static boolean DEFAULT_SEL_FILE_MGR_APP = false;

    // initial number of default URL: db_source_id_x
    public final static int INIT_SOURCE_LINK_NUMBER = 1;

    // initial category number
    public final static int INIT_CATEGORY_NUMBER = 1;

    // --- time ---
    // delay time for getting YouTube duration
    public final static int DEFAULT_DELAY_GET_DURATION = 80;

    // count down seconds to play next
    public final static int DEFAULT_COUNT_DOWN_TIME_TO_PLAY_NEXT = 5;
}
