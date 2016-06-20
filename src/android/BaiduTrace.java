package com.bsy.cordovaPlugin;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.baidu.trace.LBSTraceClient;
import com.baidu.trace.OnStartTraceListener;
import com.baidu.trace.OnStopTraceListener;
import com.baidu.trace.OnTrackListener;
import com.baidu.trace.Trace;
import com.cordovaPlugin.JsonUtil;

import android.widget.Toast;

import java.util.Map;

public class BaiduTrace extends CordovaPlugin {
    private LBSTraceClient client;
    private Trace trace;
    static String debugTag = "BaiduTrace";
    private JSONObject options;

    private boolean startTrace(JSONArray args, final CallbackContext callbackContext) {
        options = new JSONObject();
        long serviceId;
        String entityName;
        try {
            options = args.getJSONObject(0);
            Log.v(debugTag, options.toString());
        } catch (JSONException e) {
            Log.v(debugTag, "options 未传入");
        }
        Context ctx = cordova.getActivity().getApplicationContext();
        //实例化轨迹服务客户端
        client = new LBSTraceClient(ctx);
        try {
            //鹰眼服务ID
            serviceId = options.getLong("serviceId"); //开发者创建的鹰眼服务ID
            //entity标识
            entityName = options.getString("entityName");
            //轨迹服务类型（0 : 不上传位置数据，也不接收报警信息； 1 : 不上传位置数据，但接收报警信息；2 : 上传位置数据，且接收报警信息）
        } catch (JSONException e) {
            Log.v(debugTag, "不存在参数");
            return false;
        }

        int traceType = 2;

        trace = new Trace(ctx, serviceId, entityName, traceType);
        //实例化轨迹服务

        client.setOnTrackListener(new OnTrackListener() {
            @Override
            public void onRequestFailedCallback(String s) {
                Log.v(debugTag, "请求失败, 请求查询历史?");
            }

            @Override
            public Map onTrackAttrCallback() {
                try {
                    // 配置返回的自定义属性
                    JSONObject customAttr = options.getJSONObject("customAttr");
                    Log.v(debugTag, customAttr.toString());
                    return JsonUtil.json2Map(customAttr);
                } catch (JSONException e) {
                    Log.v(debugTag, "customAttr 参数");
                }
                return super.onTrackAttrCallback();
            }
        });

        //开启轨迹服务
        client.startTrace(trace, new OnStartTraceListener() {
            //开启轨迹服务回调接口（arg0 : 消息编码，arg1 : 消息内容，详情查看类参考）
            @Override
            public void onTraceCallback(int arg0, String arg1) {
                // TODO Auto-generated method stub
                showMessage("开启轨迹服务回调接口消息 [消息编码 : " + arg0 + "，消息内容 : " + arg1 + "]", Integer.valueOf(arg0));
                if (0 == arg0 || 10006 == arg0 || 10008 == arg0) {
                    callbackContext.success();
                } else {
                    callbackContext.error(arg1);
                }
            }

            // 轨迹服务推送接口（用于接收服务端推送消息，arg0 : 消息类型，arg1 : 消息内容，详情查看类参考）
            @Override
            public void onTracePushCallback(byte arg0, String arg1) {
                // TODO Auto-generated method stub
                Log.v(debugTag, "轨迹服务推送,并未使用");
            }
        });

        return true;
    }

    private boolean stopTrace(final CallbackContext callbackContext) {
        if (client == null || trace == null) {
            return false;
        }

        //停止轨迹服务
        client.stopTrace(trace, new OnStopTraceListener() {
            // 轨迹服务停止成功
            @Override
            public void onStopTraceSuccess() {
                callbackContext.success();
            }

            // 轨迹服务停止失败（arg0 : 错误编码，arg1 : 消息内容，详情查看类参考）
            @Override
            public void onStopTraceFailed(int arg0, String arg1) {
                callbackContext.error(arg1);
            }
        });

        return true;
    }

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        Log.i("leon", "插件调用");
        if (action.equals("startTrace")) {
            startTrace(args, callbackContext);
            return true;
        } else if (action.equals("stopTraceListener")) {
            stopTrace(callbackContext);
            return true;
        }
        return false;
    }

    private void showMessage(final String message, final Integer errorNo) {
        Toast.makeText(cordova.getActivity().getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }
}
