package com.ray.codescanner;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.bt_scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoCodeScanPage();
            }
        });
    }

    private void gotoCodeScanPage() {
        CodeScanActivity.start(this, 100);
    }
}
