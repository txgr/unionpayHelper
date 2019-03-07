package com.android.unionpay;

import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.widget.Toast;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainHook implements IXposedHookLoadPackage {
    public static boolean UNIONPAY_HOOK = false;
    public static ClassLoader mClassLoader;
    public static Activity activity;
    public static Service pushService;
    public static Application app;

    public static MyHandler handler;
    public static Class UPPushService;
    public static boolean AUTO = false;
    public static boolean MIHOOK = false;

    public static final String checkOrder = "com.android.unionpay.chexk";

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {


        if ("com.unionpay".equals(loadPackageParam.packageName)) {
            try {
                if (!MIHOOK) {
                    XposedBridge.hookAllMethods(XposedHelpers.findClass("com.unionpay.push.receiver.miui.UPPushEventReceiverMiui", loadPackageParam.classLoader), "onNotificationMessageArrived", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            XposedBridge.log("UPPushEventReceiverMiui onNotificationMessageArrived");
                            if (param.args != null && param.args.length > 0) {
                                XposedBridge.log("onNotificationMessageArrived" + new Gson().toJson(param.args[1]));
                                String s = (String) XposedHelpers.getObjectField(param.args[1], "c");
                                JSONObject object = new JSONObject(s);
                                JSONObject body = object.optJSONObject("body");
                                String mTitle = body.optString("title");
                                String mContent = body.optString("alert");
                                if (("动账通知").equals(mTitle) && mContent.contains("向您付款")) {

                                    String pre = mContent.split("元,")[0];
                                    String parts[] = pre.split("通过扫码向您付款");
                                    // Toast.makeText(activity, pre, Toast.LENGTH_SHORT).show();
                                    if (parts.length == 2) {
                                        final String u = parts[0];
                                        final String m = parts[1];
                                        mlog("New Push Msg u:" + u + " m:" + m);
                                        Intent intent = new Intent(checkOrder);
                                        intent.putExtra("name", u);
                                        intent.putExtra("title", m);
                                        getContext().sendBroadcast(intent);
                                    }
                                }
                            }
                        }
                    });
                    MIHOOK = true;
                    XposedBridge.log("MIUI 通知");
                }
            } catch (XposedHelpers.ClassNotFoundError e) {
                XposedBridge.log("非 MIUI 通知");
            }


            XposedBridge.log("loadPackageParam.processName =" + loadPackageParam.processName);
            mClassLoader = loadPackageParam.classLoader;
            XposedHelpers.findAndHookMethod(ClassLoader.class, "loadClass", String.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    String cls_name = (String) param.args[0];
                    if (cls_name.equals("com.unionpay.push.UPPushService")) {
                        if (UPPushService != null) return;
                        UPPushService = (Class) param.getResult();
                        hookPushService(UPPushService);
                    }
                    if (cls_name.equals("com.unionpay.push.receiver.miui.UPPushEventReceiverMiui")) {

                        XposedBridge.hookAllMethods((Class) param.getResult(), "onNotificationMessageArrived", new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                super.beforeHookedMethod(param);
                                XposedBridge.log("UPPushEventReceiverMiui onNotificationMessageArrived");
                                if (param.args != null && param.args.length > 0) {
                                    XposedBridge.log("onNotificationMessageArrived" + new Gson().toJson(param.args[1]));
                                    String s = (String) XposedHelpers.getObjectField(param.args[1], "c");
                                    JSONObject object = new JSONObject(s);
                                    JSONObject body = object.optJSONObject("body");
                                    String mTitle = body.optString("title");
                                    String mContent = body.optString("alert");
                                    if (("动账通知").equals(mTitle) && mContent.contains("向您付款")) {

                                        String pre = mContent.split("元,")[0];
                                        String parts[] = pre.split("通过扫码向您付款");
                                        // Toast.makeText(activity, pre, Toast.LENGTH_SHORT).show();
                                        if (parts.length == 2) {
                                            final String u = parts[0];
                                            final String m = parts[1];
                                            mlog("New Push Msg u:" + u + " m:" + m);
                                            Intent intent = new Intent(checkOrder);
                                            intent.putExtra("name", u);
                                            intent.putExtra("title", m);
                                            getContext().sendBroadcast(intent);
                                        }
                                    }
                                }
                            }
                        });
                    }
                }
            });
            XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.thisObject.getClass().toString().contains("com.unionpay.activity.UPActivityMain")) {
                        if (UNIONPAY_HOOK) return;
                        UNIONPAY_HOOK = true;
                        activity = (Activity) param.thisObject;
                        app = activity.getApplication();
                        IntentFilter filter = new IntentFilter();
                        filter.addAction("com.chuxin.socket.ACTION_CONNECT");
                        filter.addAction(checkOrder);
                        activity.registerReceiver(new MyBroadcastReceiver(), filter);
                        handler = new MyHandler(activity.getMainLooper());
                        // getVirtualCardNum(null);

                    }
                }
            });
        }

    }

    private void hookPushUPPushReceiver(Class upPushService) {
        XposedBridge.hookAllMethods(upPushService, "onReceive", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                if (param.args.length > 0) {
                    Object context = param.args[0];
                    Intent intent = (Intent) param.args[1];
                    XposedBridge.log(intent.getAction());

                }

            }
        });
    }


    private void hookPushService(Class upPushService) {
        XposedBridge.hookAllMethods(upPushService, "a", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                XposedBridge.log("UPPushService a");
                if (param.args != null && param.args.length > 0) {
                    Object uPPushMessage = param.args[0];
                    Object mText = XposedHelpers.callMethod(uPPushMessage, "getText");
                    String re = new Gson().toJson(mText);
                    XposedBridge.log("mText =" + re);
                    if (TextUtils.isEmpty(re)) return;
                    JSONObject object = new JSONObject(re);
                    String mTitle = object.optString("mTitle");
                    String mContent = object.optString("mContent");
                    if (("动账通知").equals(mTitle) && mContent.contains("向您付款")) {

                        String pre = mContent.split("元,")[0];
                        String parts[] = pre.split("通过扫码向您付款");
                        // Toast.makeText(activity, pre, Toast.LENGTH_SHORT).show();
                        if (parts.length == 2) {
                            final String u = parts[0];
                            final String m = parts[1];
                            mlog("New Push Msg u:" + u + " m:" + m);
                            Intent intent = new Intent(checkOrder);
                            intent.putExtra("name", u);
                            intent.putExtra("title", m);
                            getContext().sendBroadcast(intent);
                        }
                    }

                }

            }
        });


    }

    public static String encvirtualCardNo;
    public static Long time;

    public static void getVirtualCardNum(final GetCardNumListener listener) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mlog("GetVirtualCardNum");
                    String str2 = "https://pay.95516.com/pay-web/restlet/qr/p2pPay/getInitInfo?cardNo=&cityCode=" + Enc(getcityCd());
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder()
                            .url(str2)
                            .header("X-Tingyun-Id", getXTid())
                            .header("X-Tingyun-Lib-Type-N-ST", "0;" + System.currentTimeMillis())
                            .header("sid", getSid())
                            .header("urid", geturid())
                            .header("cityCd", getcityCd())
                            .header("locale", "zh-CN")
                            .header("User-Agent", "Android CHSP")
                            .header("dfpSessionId", getDfpSessionId())
                            .header("gray", getgray())
                            .header("key_session_id", "")
                            .header("Host", "pay.95516.com")
                            .build();
                    Response response = client.newCall(request).execute();
                    String RSP = response.body().string();
                    mlog("GetVirtualCardNum str2=>" + str2 + " RSP=>" + RSP);
                    String Rsp = Dec(RSP);
                    mlog("GetVirtualCardNum str2=>" + str2 + " RSP=>" + Rsp);
                    try {
                        encvirtualCardNo = Enc(new JSONObject(Rsp).getJSONObject("params").getJSONArray("cardList").getJSONObject(0).getString("virtualCardNo"));
                        mlog("encvirtualCardNo" + encvirtualCardNo);
                        if (listener != null) {
                            listener.success(encvirtualCardNo);
                        }
                    } catch (Throwable e) {
                        mlog(e);
                        if (listener != null) {
                            listener.error(e.getMessage() + e.getCause());
                        }
                    }


                } catch (Throwable e) {
                    mlog(e);
                    if (listener != null) {
                        listener.error(e.getMessage() + e.getCause());
                    }
                }
            }
        }).start();

    }

    public static String Dec(String src) {
        try {
            return (String) XposedHelpers.callStaticMethod(XposedHelpers.findClass("com.unionpay.encrypt.IJniInterface", mClassLoader), "decryptMsg", src);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String Enc(String src) {
        try {
            return (String) XposedHelpers.callStaticMethod(XposedHelpers.findClass("com.unionpay.encrypt.IJniInterface", mClassLoader), "encryptMsg", src);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return "";
    }

    private static String getXTid() {
        try {
            Class m_s = XposedHelpers.findClass("com.networkbench.agent.impl.m.s", mClassLoader);
            Object f = XposedHelpers.callStaticMethod(m_s, "f");
            Object h = XposedHelpers.callMethod(f, "H");
            mlog("h=>" + h);
            Object i = XposedHelpers.callStaticMethod(m_s, "I");

            String xtid = m_s.getDeclaredMethod("a", String.class, int.class).invoke(null, h, i).toString();
            mlog("xtid:" + xtid + "");
            return xtid;
        } catch (Throwable e) {
            mlog(e);
        }
        return null;
    }

    private static String getSid() {
        String sid = "";
        try {
            Object b = XposedHelpers.callStaticMethod(XposedHelpers.findClass("com.unionpay.network.aa", mClassLoader), "b");
            sid = XposedHelpers.callMethod(b, "e").toString();
        } catch (Throwable e) {
            mlog(e);
        }
        mlog("sid:" + sid + "");
        return sid;
    }

    private static String geturid() {
        String Cacheurid = "";
        try {
            Class data_d = XposedHelpers.findClass("com.unionpay.data.d", mClassLoader);
            Object o = XposedHelpers.callStaticMethod(data_d, "a", new Class[]{Context.class}, new Object[]{activity});
            String v1_2 = XposedHelpers.callMethod(XposedHelpers.callMethod(o, "A"), "getHashUserId").toString();
            if (!TextUtils.isEmpty(v1_2) && v1_2.length() >= 15) {
                Cacheurid = v1_2.substring(v1_2.length() - 15);
            }
        } catch (Throwable e) {
            mlog(e);
        }
        mlog("Cacheurid:" + Cacheurid + "");
        return Cacheurid;
    }

    private static String getDfpSessionId() {
        String CacheDfpSessionId = "";
        try {
            Object o = XposedHelpers.callStaticMethod(XposedHelpers.findClass("com.unionpay.service.b", mClassLoader), "d");
            mlog("o=>" + o);
            CacheDfpSessionId = o.toString();
        } catch (Throwable e) {
            mlog(e);
        }
        mlog("CacheDfpSessionId:" + CacheDfpSessionId + "");
        return CacheDfpSessionId;
    }

    private static String getcityCd() {
        String CachecityCd = "";
        try {
            CachecityCd = XposedHelpers.callStaticMethod(XposedHelpers.findClass("com.unionpay.location.a", mClassLoader), "i").toString();
        } catch (Throwable e) {
            mlog(e);
        }
        mlog("CachecityCd:" + CachecityCd + "");
        return CachecityCd;
    }

    private static String getgray() {
        String Cachegray = "";
        try {
            Object b = XposedHelpers.callStaticMethod(XposedHelpers.findClass("com.unionpay.network.aa", mClassLoader), "b");
            Cachegray = XposedHelpers.callMethod(b, "d").toString();
        } catch (Throwable e) {
            mlog(e);
        }
        mlog("Cachegray:" + Cachegray + "");
        return Cachegray;
    }


    private class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("com.chuxin.socket.ACTION_CONNECT")) {
                String money = intent.getStringExtra("money");
                String mark = intent.getStringExtra("mark");
                if (money == null) {
                    money = "0.01";
                }
                if (mark == null) {
                    mark = "测试";
                }
                mlog("money =" + money + "mark=" + mark);
                //  if (encvirtualCardNo==null||System.currentTimeMillis()-time>=60000){
                //   mlog("encvirtualCardNo"+encvirtualCardNo);
                final String finalMoney = money;
                final String finalMark = mark;
                getVirtualCardNum(new GetCardNumListener() {
                    @Override
                    public void success(String re) {
                        time = System.currentTimeMillis();
                        GenQrCode(finalMoney, finalMark);
                    }

                    @Override
                    public void error(String error) {

                    }
                });
                //   mlog("encvirtualCardNo"+encvirtualCardNo);
                //  }
              /*  else {

                    GenQrCode(money, mark);
                }
*/
            } else if (intent.getAction().equals(checkOrder)) {
                final String name = intent.getStringExtra("name");
                final String title = intent.getStringExtra("title");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String s = CheckNewOrder(name, title);
                        Message message = new Message();
                        message.what = 1;
                        message.obj = "收到支付信息" + s;
                        handler.sendMessage(message);
                    }
                }).start();
            }
        }
    }

    public static void GenQrCode(final String money, final String mark) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String mark1 = mark;
                    String money1 = new BigDecimal(money).setScale(2, RoundingMode.HALF_UP).toPlainString().replace(".", "");
                    mlog("FORRECODE GenQrCode:0 money:" + money1 + " mark:" + mark1);
                    String str2 = "https://pay.95516.com/pay-web/restlet/qr/p2pPay/applyQrCode?txnAmt=" + Enc(money1) + "&cityCode=" + Enc(getcityCd()) + "&comments=" + Enc(mark1) + "&virtualCardNo=" + encvirtualCardNo;
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder()
                            .url(str2)
                            .header("X-Tingyun-Id", getXTid())
                            .header("X-Tingyun-Lib-Type-N-ST", "0;" + System.currentTimeMillis())
                            .header("sid", getSid())
                            .header("urid", geturid())
                            .header("cityCd", getcityCd())
                            .header("locale", "zh-CN")
                            .header("User-Agent", "Android CHSP")
                            .header("dfpSessionId", getDfpSessionId())
                            .header("gray", getgray())
                            .header("key_session_id", "")
                            .header("Host", "pay.95516.com")
                            .build();

                    Response response = client.newCall(request).execute();
                    String RSP = response.body().string();
                    mlog("GenQrCode str2=>" + str2 + " RSP=>" + RSP);
                    String Rsp = Dec(RSP);
                    mlog(" RSP=>" + Rsp);

                    try {
                        JSONObject o = new JSONObject(Rsp);
                        o.put("mark", mark);
                        o.put("money", money);
                        Message message = new Message();
                        message.what = 1;
                        message.obj = "生成二维码:" + o.toString();
                        handler.sendMessage(message);
                    } catch (Throwable e) {
                        mlog(e);
                    }

                } catch (Throwable e) {
                    Message message = new Message();
                    message.what = 4;
                    message.obj = money + "-" + mark;
                    handler.sendMessageDelayed(message, 3000);
                    mlog(e);

                }

            }

        }).start();
    }

    public static String CheckNewOrder(String user, String money) {
        try {
            mlog("FORRECODE CheckNewOrder user:" + user + " money:" + money);
            String str2 = "https://wallet.95516.com/app/inApp/order/list?currentPage=" + Enc("1") + "&month=" + Enc("0") + "&orderStatus=" + Enc("0") + "&orderType=" + Enc("A30000") + "&pageSize=" + Enc("10") + "";
            OkHttpClient client = null;
            OkHttpClient.Builder builder = new OkHttpClient().newBuilder();
            builder.connectTimeout(50, TimeUnit.SECONDS);
            builder.writeTimeout(50, TimeUnit.SECONDS);
            builder.readTimeout(50, TimeUnit.SECONDS);
            client = builder.build();
            Request request = new Request.Builder()
                    .url(str2)
                    .header("X-Tingyun-Id", getXTid())
                    .header("X-Tingyun-Lib-Type-N-ST", "0;" + System.currentTimeMillis())
                    .header("sid", getSid())
                    .header("urid", geturid())
                    .header("cityCd", getcityCd())
                    .header("locale", "zh-CN")
                    .header("User-Agent", "Android CHSP")
                    .header("dfpSessionId", getDfpSessionId())
                    .header("gray", getgray())
                    .header("Accept", "*/*")
                    .header("key_session_id", "")
                    .header("Host", "wallet.95516.com")
                    .build();
            Response response = client.newCall(request).execute();
            String RSP = response.body().string();
            mlog("CheckNewOrder str2=>" + str2 + " RSP=>" + RSP);
            String DecRsp = Dec(RSP);
            mlog("CheckNewOrder str2=>" + str2 + " DecRSP=>" + DecRsp);
            //这里有很多笔，可以自己调整同步逻辑s
            JSONArray o = new JSONObject(DecRsp).getJSONObject("params").getJSONArray("uporders");
            for (int i = 0; i < o.length(); i++) {
                JSONObject p = o.getJSONObject(i);
                String orderid = p.getString("orderId");
                if (p.getString("amount").equals(money) && p.getString("title").contains(user)) {
                    return DoOrderInfoGet(orderid);
                }

            }
            Message message = new Message();
            message.what = 2;
            message.obj = money + "-" + user;
            handler.sendMessageDelayed(message, 3000);
            return "5秒重新查询";
        } catch (Throwable e) {
            Message message = new Message();
            message.what = 2;
            message.obj = money + "-" + user;
            handler.sendMessageDelayed(message, 3000);

            mlog(e);
            return "ERR:" + e.getLocalizedMessage();
        }
    }

    public static String DoOrderInfoGet(String orderid) {
        if (orderid.length() > 5) {
            try {
                String args = "{\"orderType\":\"21\",\"transTp\":\"simple\",\"orderId\":\"" + orderid + "\"}";
                String str2 = "https://wallet.95516.com/app/inApp/order/detail";
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(str2)
                        .header("X-Tingyun-Id", getXTid())
                        .header("X-Tingyun-Lib-Type-N-ST", "0;" + System.currentTimeMillis())
                        .header("sid", getSid())
                        .header("urid", geturid())
                        .header("cityCd", getcityCd())
                        .header("locale", "zh-CN")
                        .header("User-Agent", "Android CHSP")
                        .header("dfpSessionId", getDfpSessionId())
                        .header("gray", getgray())
                        .header("Accept", "*/*")
                        .header("key_session_id", "")
                        .header("Content-Type", "application/json; charset=utf-8")
                        .header("Host", "wallet.95516.com")
                        .post(RequestBody.create(null, Enc(args)))
                        .build();
                Response response = client.newCall(request).execute();
                String RSP = response.body().string();
                mlog("DoOrderInfoGet str2=>" + str2 + " RSP=>" + RSP);
                String DecRsp = Dec(RSP);
                mlog("FORRECODE DoOrderInfoGet str2=>" + str2 + " DecRSP=>" + DecRsp);
                //这里有很多笔，可以自己调整同步逻辑s
                JSONObject params = new JSONObject(DecRsp).getJSONObject("params");
                String orderDetail = params.getString("orderDetail");
                mlog("FORRECODE DoOrderInfoGet str2=>" + str2 + " orderDetail=>" + orderDetail);
                JSONObject o = new JSONObject(orderDetail);
                String u = o.getString("payUserName");
                String mark = o.getString("postScript");
                String totalAmount = params.getString("totalAmount");
                mlog("FORRECODE DoOrderInfoGet str2=>" + str2 + " u:" + u + " mark:" + mark + " totalAmount:" + totalAmount);
                return params.toString();
            } catch (Throwable e) {
                Message message = new Message();
                message.what = 3;
                message.obj = orderid;
                handler.sendMessageDelayed(message, 3000);

                mlog(e);
                return "ERR:" + e.getLocalizedMessage();
            }
        } else {
            return "ERROR_ORDER:" + orderid;
        }
    }

    private static class MyHandler extends Handler {
        public MyHandler(Looper mainLooper) {
            super(mainLooper);
        }

        public MyHandler() {

        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                String message = (String) msg.obj;
                Intent intent = new Intent("com.chuxin.socket.ACTION_NOTIFI");
                intent.putExtra("message", message);
                mlog(message);


                if (app == null) {
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    getContext().sendBroadcast(intent);
                } else {
                    Toast.makeText(app, message, Toast.LENGTH_SHORT).show();
                    app.sendBroadcast(intent);
                }

            } else if (msg.what == 2) {
                String s = (String) msg.obj;
                String amount = s.split("-")[0];
                String name = s.split("-")[1];
                CheckNewOrder(name, amount);
            }
            if (msg.what == 3) {
                String s = (String) msg.obj;
                DoOrderInfoGet(s);
            } else if (msg.what == 4) {
                String s = (String) msg.obj;
                String amount = s.split("-")[0];
                String remark = s.split("-")[1];
                GenQrCode(amount, remark);
            }
        }
    }

    public static void mlog(String s) {
        XposedBridge.log(s);
    }

    public static void mlog(Throwable s) {
        mlog(s.getMessage() + "--" + s.getCause());
    }

    public static Context getContext() {
        try {
            Class<?> ActivityThread =
                    Class.forName("android.app.ActivityThread");

            Method method = ActivityThread.getMethod("currentActivityThread");
            Object currentActivityThread = method.invoke(ActivityThread);//获取currentActivityThread 对象
            Method method2 = currentActivityThread.getClass().getMethod("getApplication");
            Context context = (Context) method2.invoke(currentActivityThread);//获取 Context对象
            XposedBridge.log("Context " + context);
            return context;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }
}
