/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.silenceinstall;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.content.pm.PackageInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.IPackageDeleteObserver;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import java.io.File;
import android.util.Log;
import android.widget.Toast;
import android.net.Uri;
import java.lang.reflect.Method;
import android.provider.Settings;
import android.content.Intent;
import android.content.Context;
import android.os.Vibrator;

public class SilenceInstallActivity extends Activity {

    private static String TAG = "SilenceInstall_csf";
    private Button install = null;
    private Button uninstall = null;
    private Button candraw = null;
    private Button vibrator = null;
    private PackageManager mPm;
    //private String APK_NAME = "SensorBox.apk";
    private String APK_NAME = "com.tencent.minihd.qq_5.8.0_351.apk";
    private final int INSTALL_COMPLETE = 1;
    private final int UNINSTALL_COMPLETE = 2;
    final static int SUCCEEDED = 1;
    final static int FAILED = 0;
    private String packageName = "";
    private static final int REQUEST_CODE = 1;
    private  Vibrator vib;

	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_silence_install);
		install = (Button)findViewById(R.id.install_button);
        uninstall = (Button)findViewById(R.id.uninstall_button);
        candraw = (Button)findViewById(R.id.candraw_button);
        vibrator = (Button)findViewById(R.id.vibrator_button);
        install.setOnClickListener(listener1);
        uninstall.setOnClickListener(listener2);
        candraw.setOnClickListener(listener3);
        vibrator.setOnClickListener(listener4);
        vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
    }

    private OnClickListener listener4 = new  OnClickListener(){
        public void onClick(View v) {
            Log.i(TAG, "onClick Vibrator");
            vibrate();
        }
    };

    private OnClickListener listener3 = new  OnClickListener(){
        public void onClick(View v) {
            if (commonROMPermissionCheck(getApplicationContext())) {
                Log.i(TAG, "commonROMPermissionCheck = true");
            } else {
                Log.i(TAG, "commonROMPermissionCheck = false");
                requestAlertWindowPermission();
            }
        }
    };

    public void vibrate()
    {
        if(vib.hasVibrator())
        {
            Log.i(TAG, "hasVibrator = true");
            vib.vibrate(2000);
         }
    }

    //判断权限
    private boolean commonROMPermissionCheck(Context context) {
        Boolean result = true;
        if (Build.VERSION.SDK_INT >= 23) {
            try {
                Class clazz = Settings.class;
                Method canDrawOverlays = clazz.getDeclaredMethod("canDrawOverlays", Context.class);
                result = (Boolean) canDrawOverlays.invoke(null, context);
            } catch (Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }
        return result;
    }

    //申请权限
    private void requestAlertWindowPermission() {
        Log.i(TAG, "requestAlertWindowPermission");
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, REQUEST_CODE);
    }

    //处理回调
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if (Settings.canDrawOverlays(this)) {
                Log.i(TAG, "onActivityResult granted");
            } else {
                Log.i(TAG, "onActivityResult ungranted");
            }
        }
    }

    private OnClickListener listener1 = new  OnClickListener(){
        public void onClick(View v) {
            //String fileName =  Environment.getExternalStorageDirectory() + "/" + APK_NAME;
            String fileName = "/data/data/com.android.silenceinstall/cache/" + APK_NAME;//只有放在应用自己的data路径下才能正常通过getPackageArchiveInfo解析安装包信息.
            //String fileName ="/mnt/sdcard/" + APK_NAME; //放这个路径可以安装但如果要使用getPackageArchiveInfo来解析会报Permission denied
            Uri uri = Uri.fromFile(new File(fileName));

            int installFlags = 0;
            PackageManager pm = getPackageManager();
            PackageInfo info = pm.getPackageArchiveInfo(fileName, PackageManager.GET_ACTIVITIES);
            if(info != null){
                String version = info.versionName==null?"0":info.versionName;
            	ApplicationInfo appInfo = info.applicationInfo;
                appInfo.sourceDir = fileName;
                appInfo.publicSourceDir = fileName;
            	String appName = pm.getApplicationLabel(appInfo).toString();
                //String appName = appInfo.loadLabel(pm).toString();//这个方法和上面的方法均可获取appName
            	packageName = appInfo.packageName;
                Log.i(TAG, "appInfo = " + appInfo.toString());
                Log.i(TAG, "version = " + version);
            	Log.i(TAG, "appName = " + appName);
            	Log.i(TAG, "packageName = " + packageName);
                if (isApkInstalled(packageName)) {
                    installFlags |= PackageManager.INSTALL_REPLACE_EXISTING;
                }
            } else {
            	Log.i(TAG, "info = null");
            }
            Log.i(TAG, "fileName = " + fileName);
            PackageInstallObserver observer = new PackageInstallObserver();
            pm.installPackage(uri, observer, installFlags, packageName);
        }
    };

    class PackageInstallObserver extends IPackageInstallObserver.Stub {
        public void packageInstalled(String packageName, int returnCode) {
        	Log.i(TAG, packageName + "Installed!");
            Message msg = mHandler.obtainMessage(INSTALL_COMPLETE);
            msg.arg1 = returnCode;
            mHandler.sendMessage(msg);
        }
    };

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case INSTALL_COMPLETE:
                	if(msg.arg1 == SUCCEEDED) {
                        Toast.makeText(getApplicationContext(), "install apk SUCCEEDED", Toast.LENGTH_SHORT).show();
                    } else {}
                    break;
                case UNINSTALL_COMPLETE:
                    if(msg.arg1 == SUCCEEDED) {
                        Toast.makeText(getApplicationContext(), "uninstall apk SUCCEEDED", Toast.LENGTH_SHORT).show();
                    } else {}
                    break; 
                default:
                    break;
            }
        }
    };

    private OnClickListener listener2 = new  OnClickListener(){
        public void onClick(View v) {
            PackageManager pm = getPackageManager();
            try {
                PackageInfo pi = pm.getPackageInfo(packageName,PackageManager.GET_UNINSTALLED_PACKAGES);
                if(pi != null) {
                    PackageDeleteObserver observer = new PackageDeleteObserver();
                    pm.deletePackage(packageName, observer, 0);
                } else {}
            } catch (NameNotFoundException e) {
                Toast.makeText(getApplicationContext(), "dont't installed " + packageName, Toast.LENGTH_SHORT).show();
                Log.i(TAG, "dont't installed " + packageName);
            }
        }
    };

    private class PackageDeleteObserver extends IPackageDeleteObserver.Stub {
        public void packageDeleted(String packageName, int returnCode) {
            Message msg = mHandler.obtainMessage(UNINSTALL_COMPLETE);
            msg.arg1 = returnCode;
            mHandler.sendMessage(msg);          
        }   
    }

    private boolean isApkInstalled(String packagename){
        PackageManager pm = getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(packagename, PackageManager.GET_UNINSTALLED_PACKAGES);
            if (pi != null) {
                Log.i(TAG, "pi = " + pi.toString());
                return true;
            }
            return false;
        } catch (NameNotFoundException e) {
            return false;
        }
    }
}
