package com.android.unionpay;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener {


    EditText ip,port;
    Button conncet;
    private TextView lable;
    private  MyBroadcastReceiver mReceiver;
    public  static final String ACTION_CONNECT ="com.chuxin.socket.ACTION_CONNECT";
    public  static final String ACTION_NOTIFI="com.chuxin.socket.ACTION_NOTIFI";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ip=findViewById(R.id.ip);
        port=findViewById(R.id.port);
        lable=findViewById(R.id.lable);
        conncet=findViewById(R.id.connect);
        conncet.setOnClickListener(this);
        IntentFilter filter =new IntentFilter();
        filter.addAction(ACTION_NOTIFI);
        mReceiver =new MyBroadcastReceiver();
        registerReceiver(mReceiver,filter);
        initView();

        boolean bRunning =  isServiceRunning(this, "com.android.unionpay.NotificationMonitorService");
        if (!bRunning){
            Intent upservice = new Intent(this, NotificationMonitorService.class);
            startService(upservice);
        }



    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    private void initView() {
        ip.setText("0.01");
        port.setText("测试");


    }
    public static String getAppVersionName(Context context) {
        String versionName = "";
        try {
            // ---get the package info---
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            versionName = pi.versionName;

            if (versionName == null || versionName.length() <= 0) {
                return "";
            }
        } catch (Exception e) {
            Log.e("VersionInfo", "Exception", e);
        }
        return versionName;
    }



    @Override
    public void onClick(View view) {
        if (view.getId()==R.id.connect) {

            Intent intent = new Intent(ACTION_CONNECT);
            String money =ip.getText().toString();
            String mark =port.getText().toString();
            if (!TextUtils.isEmpty(money)){
                intent.putExtra("money",money);
            }
            if (!TextUtils.isEmpty(mark)){
                intent.putExtra("mark",mark);
            }
            sendBroadcast(intent);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();



    }

    public void open(View view) {
        Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivityForResult(intent, 0);
    }

    private class MyBroadcastReceiver extends BroadcastReceiver {

      @Override
      public void onReceive(Context context, Intent intent) {

          if (ACTION_NOTIFI.equals(intent.getAction())){
              String message =intent.getStringExtra("message");
              if (message!=null)
                lable.setText(lable.getText()+message+"\r\n");
          }
      }
  }

    public static boolean isServiceRunning(Context mContext, String className) {
        boolean isRunning = false;
        ActivityManager activityManager = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);

        List<ActivityManager.RunningServiceInfo> serviceList = activityManager.getRunningServices(100);
        if (serviceList.size() == 0) {
            return false;
        }
        for (int i = 0; i < serviceList.size(); i++) {
            if (serviceList.get(i).service.getClassName().equals(className) == true) {
                isRunning = true;
                break;
            }
        }
        return isRunning;
    }
}
