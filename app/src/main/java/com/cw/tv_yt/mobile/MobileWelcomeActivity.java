package com.cw.tv_yt.mobile;

import android.app.Activity;
import android.os.Bundle;

import com.cw.tv_yt.R;
import com.cw.tv_yt.ui.LeanbackActivity;

//todo temp mark
//public class MobileWelcomeActivity extends Activity {
public class MobileWelcomeActivity extends LeanbackActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //todo temp mark
//        setContentView(R.layout.activity_mobile_welcome);
        setContentView(R.layout.main);
    }
}
