package com.quiethours.discord;

import android.os.Bundle;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(QuietHoursPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
