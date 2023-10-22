package com.cxapp.gcmodel;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {
    private static OkHttpClient client = new OkHttpClient();
    private static final String PREFS = "myPrefs";
    private static final String KEY_ADDR = "addr";
    private static final String KEY_SECRET = "secret";
    private static final String TAG = "cxGCashMain";
    private EditText addrEditText;
    private EditText secretEditText;
    private EditText phoneEditText;
    private EditText pinEditText;
    private boolean isBroadcastReceived = true; // 新增一个标志位，表示是否接收到了广播
    private BroadcastReceiver receiver; // 新增一个BroadcastReceiver成员变量

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        g.hhh();
        // 新增注册广播接收器的代码
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("com.cxapp.gcmodel.POST_APP_INFO")) {
                    String isLoginActivity = intent.getStringExtra("Activity");
                    Log.d(TAG, isLoginActivity);
                    if(isLoginActivity.equals("login")){
                        isBroadcastReceived = true;
                    } else {
                        isBroadcastReceived = false;
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter("com.cxapp.gcmodel.POST_APP_INFO");
        registerReceiver(receiver, filter);

        initUIElements();
        loadConf();

        conSerTest();
        saveConf();
        getAppInfo();
    }

    private void initUIElements() {
        addrEditText = findViewById(R.id.SerVerAddrText);
        secretEditText = findViewById(R.id.editTextTextPersonName11);
        phoneEditText = findViewById(R.id.editTextTextPersonName13);
        pinEditText = findViewById(R.id.editTextTextPersonName12);
    }

    private void loadConf() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        addrEditText.setText(sharedPreferences.getString(KEY_ADDR, ""));
        secretEditText.setText(sharedPreferences.getString(KEY_SECRET, ""));
    }

    private void conSerTest() {
        findViewById(R.id.testButton).setOnClickListener(v -> new Thread(() -> {
            String addr = addrEditText.getText().toString();
            if(addr.isEmpty()){
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "请输入服务器地址", Toast.LENGTH_SHORT).show());
                return;
            }
            try {
                handleServerResponse(sendRequest("http://" + addr + "/api/action/heartbeat"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start());
    }

    private void saveConf() {
        findViewById(R.id.button6).setOnClickListener(v -> {
            SharedPreferences sharedPreferences = getSharedPreferences(PREFS, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(KEY_ADDR, addrEditText.getText().toString());
            editor.putString(KEY_SECRET, secretEditText.getText().toString());
            editor.apply();

            Toast.makeText(MainActivity.this, "保存成功", Toast.LENGTH_SHORT).show();
        });
    }

    private void getAppInfo() {
        findViewById(R.id.button7).setOnClickListener(v -> {

            ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setMessage("Loading...");

            new Thread(() -> {

                sendBroadcast(new Intent("com.cxapp.gcmodel.GET_APP_INFO"));
                if (isBroadcastReceived) {
                    runOnUiThread(() -> {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("操作提示")
                                .setMessage("请输入PIN登录APP后再继续操作")
                                .setPositiveButton("确定", null)
                                .show();
                    });
                    return;
                }
                runOnUiThread(progressDialog::show);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                try {
                    JSONObject jsonObject = createJson();
                    String pin = jsonObject.getString("pin");
                    String phone = jsonObject.getString("msisdn");

                    // 检查pin和phone是否为空
                    if (pin.isEmpty() || phone.isEmpty()) {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "获取关键信息失败，请重启GC", Toast.LENGTH_SHORT).show();
                            progressDialog.dismiss(); // 如果手机号或PIN为空，取消ProgressDialog
                        });
                        return;
                    }

                    runOnUiThread(() -> {
                        pinEditText.setText(pin);
                        phoneEditText.setText(phone);
                    });



                    String addr = "http://" + addrEditText.getText().toString() + "/api/plug/init/" + phone;
                    String result = sendRequest(addr, jsonObject.toString());
                    handleServerResponse(result);
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "获取关键信息失败，请重启GCAPP", Toast.LENGTH_SHORT).show());
                } finally {
                    runOnUiThread(progressDialog::dismiss); // 请求完成后取消ProgressDialog
                }
            }).start();
        });
    }


    private JSONObject createJson() throws Exception {
        Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", "cat /sdcard/Download/conf.json"});
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = reader.readLine();
        if (line == null || line.isEmpty()) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "提取失败: 文件为空", Toast.LENGTH_SHORT).show());
            throw new Exception("Failed to extract data from file");
        }

        JSONObject jsonObject = new JSONObject(line);
        if (!verifyJsonKeys(jsonObject)) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "提取失败: 缺少必要的键", Toast.LENGTH_SHORT).show());
            throw new Exception("Failed to extract data: missing keys");
        }

        return jsonObject;
    }

    private boolean verifyJsonKeys(JSONObject jsonObject) {
        String[] requiredKeys = new String[]{"Authorization","X-Env-Info","X-FlowId","X-UDID","pubKey","priKey","callBackPubKey","userId","dfpToken","deviceModel","osVersion","msisdn","pin","deviceId","publicUserId","deviceManufacturer","deviceBrand"};
        for (String key : requiredKeys) {
            if (!jsonObject.has(key) || jsonObject.optString(key).isEmpty()) {
                return false;
            }
        }
        return true;
    }


    private void handleServerResponse(String result) {
        runOnUiThread(() -> {
            try {
                JSONObject jsonObject = new JSONObject(result);
                int code = jsonObject.getInt("code");
                if (code == 0) {
                    Toast.makeText(MainActivity.this, "成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "失败: " + jsonObject.getString("msg"), Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("操作提示");
                builder.setMessage("上报失败 请检查网络或其他原因");
                builder.show();
                e.printStackTrace();
            }
        });
    }

    private String sendRequest(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Secret", secretEditText.getText().toString())
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }
    }

    private String sendRequest(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder().url(url).post(body).addHeader("Secret", secretEditText.getText().toString()).build();

        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }
    }
}
