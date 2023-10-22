package com.cxapp.gcmodel;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import android.app.AndroidAppHelper;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;

import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class helper extends MainActivity {
    static XC_LoadPackage.LoadPackageParam gloadPackageParam = null;
    static Class<?> encryptedHeaderClass = null;
    public static String Tag = "cxGCash";
    public static String User_id = null;
    public static String Dfp_token = null;
    public static String phoneModel = null;
    public static String osVersion = null;
    public static String msisdn = null;
    public static String mpin = null;
    public static String deviceId = null;
    public static String publicUserId = null;
    public static String DeviceBrand = null;
    public static String DeviceManufacturer = null;
    public static Map<String, Object> instances = new HashMap<>(); // 存储类的实例
    public static synchronized void init(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        handleLoadPackage(loadPackageParam);
        XposedBridge.log("GC插件初始化完成");

    }

    private static void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals("com.globe.gcash.android"))
            return;
        //页面广播

        //页面广播
        findAndHookMethod("android.app.Activity", lpparam.classLoader, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                String currentActivityName = param.thisObject.getClass().getSimpleName();
                XposedBridge.log("当前Activity：" + currentActivityName);

                if (currentActivityName.equals("DashboardContainerActivity")) {
                    // 如果是 "DashboardContainerActivity"，则发送广播
                    Context context = (Context) param.thisObject;
                    Intent broadcastIntent = new Intent("com.cxapp.gcmodel.POST_APP_INFO");
                    broadcastIntent.putExtra("Activity", "home");
                    context.sendBroadcast(broadcastIntent);
                    XposedBridge.log("发送广播home");
                }
            }
        });

        //定义一个广播接收器 执行函数
        // 注册广播接收器
        findAndHookMethod("android.app.Activity", lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                // 获取 Context
                Context context = (Context) param.thisObject;


                // 注册广播接收器
                BroadcastReceiver receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        XposedBridge.log("接收到广播");

                        if (intent.getAction().equals("com.cxapp.gcmodel.GET_APP_INFO")) {
                            // 获取当前Activity的名字
                            String currentActivityName = context.getClass().getSimpleName();
                            XposedBridge.log("当前Activity：" + currentActivityName);
                            // 判断当前Activity是否为 "gcash.module.login.LoginActivity"
                            if (!currentActivityName.equals("DashboardContainerActivity")) {
                                // 如果是 "LoginActivity"，则发送广播
                                Intent broadcastIntent = new Intent("com.cxapp.gcmodel.POST_APP_INFO");
                                broadcastIntent.putExtra("Activity", "login");
                                context.sendBroadcast(broadcastIntent);
                                XposedBridge.log("发送广播login");
                            } else {
                                // 如果不是 "LoginActivity"，则发送广播
                                Intent broadcastIntent = new Intent("com.cxapp.gcmodel.POST_APP_INFO");
                                broadcastIntent.putExtra("Activity", "home");
                                context.sendBroadcast(broadcastIntent);
                                XposedBridge.log("发送广播home");
                                // 广播被接收，执行相关操作
                                String result = null;
                                try {
                                    result = makeInfo();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                // 存储结果到 "conf.json" 文件中
                                try {
                                    File downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
                                    File file = new File(downloadDir, "conf.json");
                                    FileWriter writer = new FileWriter(file);
                                    writer.write(result);
                                    writer.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                };

                IntentFilter filter = new IntentFilter("com.cxapp.gcmodel.GET_APP_INFO");
                context.registerReceiver(receiver, filter);
            }
        });


        // 存在了就别继续赋值了

        if(gloadPackageParam == null){
            gloadPackageParam = lpparam;
        }

        if(encryptedHeaderClass == null){
            encryptedHeaderClass = XposedHelpers.findClass("gcash.common_data.model.encryption.EncryptedHeader", lpparam.classLoader);
        }

        findAndHookMethod(
                "gcash.common_data.utility.preferences.ApplicationConfigPrefImpl",
                lpparam.classLoader,
                "getAccess_token",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object instance = param.thisObject;
                        // 将实例存储到Map中
                        instances.put(param.thisObject.getClass().getName(), instance);
                        XposedBridge.log(Tag + " getAccess_token: " + param.getResult());
                    }
                }
        );
        //gcash.common_data.utility.preferences.HashConfigPrefImpl
        findAndHookMethod(
                "gcash.common_data.utility.preferences.HashConfigPrefImpl",
                lpparam.classLoader,
                "getAgreement_api_flow_id",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object instance = param.thisObject;
                        // 将实例存储到Map中
                        instances.put(param.thisObject.getClass().getName(), instance);
                        XposedBridge.log(Tag + " getAgreement_api_flow_id: " + param.getResult());
                        if(msisdn==null){
                           msisdn = (String) XposedHelpers.callMethod(instance, "getMsisdn");
                           XposedBridge.log(Tag + " getMsisdn: " + msisdn);
                        }
                        if (mpin == null){
                            mpin = (String) XposedHelpers.callMethod(instance, "getPin");
                            XposedBridge.log(Tag + " getPin: " + mpin);
                        }
                    }
                }
        );
        //获取device_id
        findAndHookMethod(
                "gcash.common_presentation.utility.DeviceUtils",
                lpparam.classLoader,
                "getUtdid",
                android.content.Context.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log(Tag + " getDeviceId: " + param.getResult());
                        if (deviceId == null){
                            deviceId = (String) param.getResult();
                            XposedBridge.log(Tag + " getDeviceId: " + deviceId);
                        }
                    }
                }
        );

        //获取user_id
        findAndHookMethod("gcash.common_data.utility.preferences.UserDetailsConfigPrefImpl", lpparam.classLoader, "setUserId", java.lang.String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                if (User_id == null) {
                    User_id = (String) param.args[0];
                    XposedBridge.log(Tag + " setUserId: " + param.args[0]);
                }
            }
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
            }
        });
        //获取publicUser_id
        findAndHookMethod("gcash.common_data.utility.preferences.UserDetailsConfigPrefImpl", lpparam.classLoader, "setPublicUserId", java.lang.String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                if (publicUserId == null) {
                    publicUserId = (String) param.args[0];
                    XposedBridge.log(Tag + " publicUserId: " + param.args[0]);
                }
            }
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
            }
        });
        //获取dfp_token
        XposedHelpers.findAndHookMethod("gcash.common_presentation.utility.GNetworkUtil", lpparam.classLoader, "a", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);

            }
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                if (Dfp_token == null) {
                    Dfp_token = (String) param.getResult();
                    XposedBridge.log(Tag + " dfp_token: " + param.getResult());
                }
            }
        });

        //获取设备机型
        XposedHelpers.findAndHookMethod("gcash.common_presentation.utility.UserAgent", lpparam.classLoader, "getDeviceModel", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
            }
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                Object userAgent = param.thisObject;
                if (phoneModel == null) {
                    phoneModel = (String) param.getResult();
                    XposedBridge.log(Tag + " Device_model: " + param.getResult());
                }
                if (osVersion == null) {
                    String deviceOsVersion = (String) XposedHelpers.callMethod(userAgent, "getDeviceOsVersion");
                    osVersion = deviceOsVersion.split(",")[0];
                    XposedBridge.log(Tag + " Device_osVersion: " + deviceOsVersion);
                }
                if (DeviceBrand == null){
                    DeviceBrand = (String) XposedHelpers.callMethod(userAgent, "getDeviceBrand");
                }
                if (DeviceManufacturer==null){
                    DeviceManufacturer = (String) XposedHelpers.callMethod(userAgent, "getDeviceManufacturer");
                    XposedBridge.log(Tag + " Device_osVersion: " + DeviceManufacturer);
                }
            }
        });




        findAndHookMethod(
                "gcash.common_presentation.utility.GNetworkUtil",
                lpparam.classLoader,
                "getEnvInfo",
                java.util.Map.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object instance = param.thisObject;
                        // 将实例存储到Map中
                        instances.put(param.thisObject.getClass().getName(), instance);
                        XposedBridge.log(Tag + " getEnvInfo: " + param.getResult());
                    }
                }
        );
    }


    // 一个新的方法，接收一个实例的标识符，并使用这个标识符调用getUdid()方法
    public static String callGetUdid() throws Exception {
        Object instance = instances.get("gcash.common_data.utility.preferences.ApplicationConfigPrefImpl");
        if (instance == null) {
            throw new Exception("instance not found");
        }
        String Udid = (String) XposedHelpers.callMethod(instance, "getUdid");
        XposedBridge.log(Tag + " callGetUdid: " + Udid);
        return Udid;
    }
    public static String getAccess_token() throws Exception {
        Object instance = instances.get("gcash.common_data.utility.preferences.ApplicationConfigPrefImpl");
        if (instance == null) {
            throw new Exception("instance not found");
        }
        String Udid = (String) XposedHelpers.callMethod(instance, "getAccess_token");
        XposedBridge.log(Tag + " getAccess_token: " + Udid);
        return Udid;
    }
    public static String getAgreement_api_flow_id() throws Exception {
        Object instance = instances.get("gcash.common_data.utility.preferences.HashConfigPrefImpl");
        if (instance == null) {
            throw new Exception("instance not found");
        }
        String getAgreement_api_flow_id = (String) XposedHelpers.callMethod(instance, "getAgreement_api_flow_id");
        XposedBridge.log(Tag + " getAgreement_api_flow_id: " + getAgreement_api_flow_id);
        return getAgreement_api_flow_id;
    }

    public static String getAgreement_api_public_key() throws Exception {
        Object instance = instances.get("gcash.common_data.utility.preferences.HashConfigPrefImpl");
        if (instance == null) {
            throw new Exception("instance not found");
        }
        String getAgreement_api_public_key = (String) XposedHelpers.callMethod(instance, "getAgreement_api_public_key");
        XposedBridge.log(Tag + " getAgreement_api_public_key: " + getAgreement_api_public_key);
        return getAgreement_api_public_key;
    }

    public static String getAgreement_private_key() throws Exception {
        Object instance = instances.get("gcash.common_data.utility.preferences.HashConfigPrefImpl");
        if (instance == null) {
            throw new Exception("instance not found");
        }
        String getAgreement_private_key = (String) XposedHelpers.callMethod(instance, "getAgreement_private_key");
        XposedBridge.log(Tag + " getAgreement_private_key: " + getAgreement_private_key);
        return getAgreement_private_key;
    }

    public static String getAgreement_public_key() throws Exception {
        Object instance = instances.get("gcash.common_data.utility.preferences.HashConfigPrefImpl");
        if (instance == null) {
            throw new Exception("instance not found");
        }
        String getAgreement_public_key = (String) XposedHelpers.callMethod(instance, "getAgreement_public_key");
        XposedBridge.log(Tag + " getAgreement_public_key: " + getAgreement_public_key);
        return getAgreement_public_key;
    }

    public static String getEnv_Info() throws Exception {
        Object instance = instances.get("gcash.common_presentation.utility.GNetworkUtil");
        if (instance == null) {
            throw new Exception("instance not found");
        }
        Map<String, Object> map = new HashMap<>();
        String getEnvInfo = (String) XposedHelpers.callMethod(instance, "getEnvInfo", map);
        XposedBridge.log(Tag + " getEnvInfo: " + getEnvInfo);
        return getEnvInfo;
    }

    public static String makeInfo() throws Exception {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("Authorization",getAccess_token());
        jsonObject.put("X-Env-Info", base64Encode(getEnv_Info()));
        jsonObject.put("X-FlowId", getAgreement_api_flow_id());
        jsonObject.put("X-UDID", callGetUdid());
        jsonObject.put("pubKey", getAgreement_public_key());
        jsonObject.put("priKey", getAgreement_private_key());
        jsonObject.put("callBackPubKey", getAgreement_api_public_key());
        jsonObject.put("userId", User_id);
        jsonObject.put("dfpToken", Dfp_token);
        jsonObject.put("deviceModel", phoneModel);
        jsonObject.put("osVersion", osVersion);
        jsonObject.put("msisdn", msisdn);
        jsonObject.put("pin", mpin);
        jsonObject.put("deviceId", deviceId);
        jsonObject.put("publicUserId", publicUserId);
        jsonObject.put("deviceManufacturer", DeviceManufacturer);
        jsonObject.put("deviceBrand", DeviceBrand);
        XposedBridge.log(Tag + " makeInfo: " + jsonObject.toString());
        return jsonObject.toString();
    }

    //返回classloader
    public static XC_LoadPackage.LoadPackageParam getLoadPackageParam() throws Exception {
        return gloadPackageParam;
    }

    public static String base64Encode(String input) {
        // 获取输入字符串的字节数组
        byte[] inputBytes = input.getBytes();

        // 对字节数组进行Base64编码
        String encodedString = Base64.encodeToString(inputBytes, Base64.DEFAULT);

        return encodedString;
    }

}
