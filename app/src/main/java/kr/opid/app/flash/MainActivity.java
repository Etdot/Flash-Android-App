package kr.opid.app.flash;

/*
 * Copyright 2013 Leon Cheng
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import kr.opid.app.flash.CircularProgressBar.ProgressAnimationListener;


public class MainActivity extends Activity {
    private Camera mCamera = null;
    private Camera.Parameters p;
    boolean isOpenFlash;
    private CircularProgressBar circularProgressBarAndButton;
    private int beforeBattery = 0;
    private int currentBattery = 0;
    private RelativeLayout mainLayout;
    private String appVersion;

    private void init() {
        mainLayout = (RelativeLayout) findViewById(R.id.main_layout);
        circularProgressBarAndButton = (CircularProgressBar) findViewById(R.id.batteryprogressbar);

        currentBattery = (int) getBatteryLevel();
        isOpenFlash = false;

        try {
            PackageInfo i = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
            appVersion = i.versionName;
        } catch (PackageManager.NameNotFoundException e) {
        }


        // Progress Bar Initialization
        circularProgressBarAndButton.animateProgressTo(0, currentBattery, new ProgressAnimationListener() {

            @Override
            public void onAnimationStart() {
            }

            @Override
            public void onAnimationProgress(int progress) {
                circularProgressBarAndButton.setTitle(currentBattery + "%");
            }

            @Override
            public void onAnimationFinish() {
                circularProgressBarAndButton.setSubTitle("battery remaining");
            }
        });


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        circularProgressBarAndButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isOpenFlash == false) {
                    mCamera = Camera.open();
                    p = mCamera.getParameters();
                    p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    mCamera.setParameters(p);
                    mCamera.startPreview();

                    mainLayout.setBackgroundResource(R.drawable.bglayout_on);
                    isOpenFlash = true;
                    beforeBattery = currentBattery;
                    Toast.makeText(getApplicationContext(), "Turn on.", Toast.LENGTH_SHORT).show();
                } else {
                    p = mCamera.getParameters();
                    p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    mCamera.setParameters(p);
                    mCamera.stopPreview();
                    mCamera.release();

                    mainLayout.setBackgroundResource(R.drawable.bglayout_off);
                    isOpenFlash = false;
                    currentBattery = (int) getBatteryLevel();
                    uiChangingHandler.sendEmptyMessage(1);
                    Toast.makeText(getApplicationContext(), "Turn off.", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    public float getBatteryLevel() {
        Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        // Error checking that probably isn't needed but I added just in case.
        if (level == -1 || scale == -1) {
            return 50.0f;
        }

        return ((float) level / (float) scale) * 100.0f;
    }

    // This UI handler is not use, because of the TemplateServiceListener.
    private Handler uiChangingHandler = new Handler() {
        public void handleMessage(Message msg) {
            circularProgressBarAndButton.animateProgressTo(beforeBattery - 20, currentBattery, new ProgressAnimationListener() {
                @Override
                public void onAnimationStart() {
                }

                @Override
                public void onAnimationFinish() {
                    circularProgressBarAndButton.setTitle(currentBattery + "%");
                }

                @Override
                public void onAnimationProgress(int progress) {
                }
            });
        }

    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, Menu.NONE, "Contact Us");

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case 1:
                Intent it = new Intent(Intent.ACTION_SEND);
                it.setType("plain/text");

                String[] mailaddr = {"opid911@gmail.com"};
                it.putExtra(Intent.EXTRA_EMAIL, mailaddr);
                it.putExtra(Intent.EXTRA_SUBJECT, "[flash]");
                it.putExtra(Intent.EXTRA_TEXT, "\n\n" + "v" + appVersion);
                startActivity(it);
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        if (mCamera != null) {
            mCamera.release();
        }
        super.onPause();
    }

}
