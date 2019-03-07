package com.android.unionpay;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

@SuppressLint("NewApi")
public class NotificationMonitorService extends NotificationListenerService {

    // 在收到消息时触发
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        // TODO Auto-generated method stub
        Bundle extras = sbn.getNotification().extras;
        // 获取接收消息APP的包名
        String notificationPkg = sbn.getPackageName();
        // 获取接收消息的抬头
        String notificationTitle = extras.getString(Notification.EXTRA_TITLE);
        // 获取接收消息的内容
        String notificationText = extras.getString(Notification.EXTRA_TEXT);
      if (notificationTitle.equals("动账通知")&&notificationText.contains("通过扫码向您付款")){
          String pre = notificationText.split("元,")[0];
          String parts[] = pre.split("通过扫码向您付款");
          // Toast.makeText(activity, pre, Toast.LENGTH_SHORT).show();
          if (parts.length == 2) {
              final String u = parts[0];
              final String m = parts[1];
              Log.i("","New Push Msg u:" + u + " m:" + m);
              Intent intent =new Intent("com.android.unionpay.chexk");
              intent.putExtra("name",u);
              intent.putExtra("title",m);
             getApplication().sendBroadcast(intent);
          }
      }
        Log.i("XSL_Test", "notificationPkg"+notificationPkg+"Notification posted " + notificationTitle + " & " + notificationText);
    }
    
    // 在删除消息时触发
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // TODO Auto-generated method stub
        Bundle extras = sbn.getNotification().extras;
        // 获取接收消息APP的包名
        String notificationPkg = sbn.getPackageName();
        // 获取接收消息的抬头
        String notificationTitle = extras.getString(Notification.EXTRA_TITLE);
        // 获取接收消息的内容
        String notificationText = extras.getString(Notification.EXTRA_TEXT);
        Log.i("XSL_Test", "Notification removed " + notificationTitle + " & " + notificationText);
    }
}
