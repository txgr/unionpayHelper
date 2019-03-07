package com.android.unionpay;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by Administrator on 2018/3/16.
 */

public class LogUtil {
    public static void logPrivateFiled(Object thisObject, String s) {

        Field fields[] =thisObject.getClass().getDeclaredFields();
        logFileds(thisObject, s, fields);
    }
    public static void logPublicFiled(Object thisObject, String s) {

        Field fields[] =thisObject.getClass().getFields();
        logFileds(thisObject, s, fields);
    }
    private static void logFileds(Object thisObject, String s, Field[] fields) {
        for (Field f:fields){
        Object obj=    XposedHelpers.getObjectField(thisObject,f.getName());
        if (obj!=null ){
            if (obj instanceof TextView){
                TextView tv = (TextView) obj;
                log(s,f.getName()+" textview  -"+tv.getText());
            }else
            if (obj instanceof Button){
                Button tv = (Button) obj;
                log(s,f.getName()+" Button  -"+tv.getText());
            }else
            if (obj instanceof EditText){
                EditText tv = (EditText) obj;
                log(s,f.getName()+" EditText  -"+tv.getText());
            }else {
                log(s,f.getName()+"  = "+ obj);
            }

        }else {
          log(s,f.getName()+" = null");
        }
        }
    }

    private static void log(String index, String log) {
        XposedBridge.log(index+ " : "+log);
    }

    public static void logParams(XC_MethodHook.MethodHookParam param, String index) {
        int i=0;
        for (Object o:param.args){
            i++;
            if (o!=null){
                if (o instanceof Intent){
                    for (String key :( ((Intent) o).getExtras()).keySet()){
                       // XposedBridge.log("H5Activity key="+key+" value ="+((Bundle)o).get(key));
                        XposedBridge.log(index+" 参数 "+i+" bunlide  key2="+key+" value ="+(((Intent) o).getExtras()).get(key));
                    }
                }else
                log(index," 参数 "+i+o);
            }else {
                log(index," 参数 "+i+" null" );
            }
        }
    }

    public static void logArrayFileds(Object thisObject, String[] fileds, String payinfo1) {
        for (String s:fileds){
            Object obj=    XposedHelpers.getObjectField(thisObject,s);
            if (obj!=null ){
                if (obj instanceof TextView){
                    TextView tv = (TextView) obj;
                    log(payinfo1,s+" textview  -"+tv.getText());
                }else
                if (obj instanceof Button){
                    Button tv = (Button) obj;
                    log(payinfo1,s+" Button  -"+tv.getText());
                }else
                if (obj instanceof EditText){
                    EditText tv = (EditText) obj;
                    log(payinfo1,s+" EditText  -"+tv.getText());
                }else {
                    log(payinfo1,s+"  = "+ obj);
                }

            }else {
                log(payinfo1,s+" = null");
            }
        }
    }

    public static void logIntent(Intent intent, String luckyMoneyDetailUI) {
        Bundle bundle =intent.getExtras();
        if (bundle!=null){
            for (String key :bundle.keySet()){
                try {

                    log(luckyMoneyDetailUI," key :"+key+" value :"+bundle.get(key)+" class="+(bundle.get(key)==null?"":bundle.get(key).getClass()));
                }catch (Exception e){
                     log("Exception"," "+e.getMessage());
                }

            }
        }else
        log(luckyMoneyDetailUI," 数据空");
    }

    public static String getHtml(String path, String sessionid) throws Exception {
        // 通过网络地址创建URL对象
        URL url = new URL(path);
        // 根据URL
        // 打开连接，URL.openConnection函数会根据URL的类型，返回不同的URLConnection子类的对象，这里URL是一个http，因此实际返回的是HttpURLConnection
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        // 设定URL的请求类别，有POST、GET 两类
        conn.setRequestMethod("GET");
        //设置从主机读取数据超时（单位：毫秒）
        conn.setConnectTimeout(5000);
        //设置连接主机超时（单位：毫秒）
        conn.setReadTimeout(5000);
        // 通过打开的连接读取的输入流,获取html数据
        InputStream inStream = conn.getInputStream();
        // 得到html的二进制数据
        byte[] data = readInputStream(inStream);
        // 是用指定的字符集解码指定的字节数组构造一个新的字符串
        String html = new String(data, "utf-8");
        return html;
    }


    public static byte[] readInputStream(InputStream inStream) throws Exception {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = 0;
        while ((len = inStream.read(buffer)) != -1) {
            outStream.write(buffer, 0, len);
        }
        inStream.close();
        return outStream.toByteArray();
    }

    public static void log(String s) {
        XposedBridge.log(s);
    }
}
