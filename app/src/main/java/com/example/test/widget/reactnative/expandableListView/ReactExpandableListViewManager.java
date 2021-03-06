package com.example.test.widget.reactnative.expandableListView;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.example.test.adapter.ExpandableListAdapter;
import com.example.test.bean.AppUpdate;
import com.example.test.common.Constants;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.UiThreadUtil;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.UIManagerModule;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.events.Event;
import com.facebook.react.uimanager.events.EventDispatcher;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Springever on 2017/5/18.
 */
@ReactModule(name = ReactExpandableListViewManager.REACT_CLASS)
public class ReactExpandableListViewManager extends SimpleViewManager<ExpandableListView> implements ExpandableListAdapter.Callback,LifecycleEventListener {

    private final static String TAG = "ReactExpandableListViewManager";

    public static final String REACT_CLASS = "RCTExpandableListView";//和ReactNative的js组件名字一致

    private ExpandableListView expandableListView;

    private ExpandableListAdapter mUpdateAdapter;

    private Activity activity;

    private ThemedReactContext reactContext;

    private static final String NAME_ENTITIES = "entities";

    private static String PREF_IGNORE = "ignore";

    private static final String JSON_UPAPPITEMS = "upappitems";

    private static final String JSON_IGNOREAPPITEMS = "ignoreappitems";

    private static final int VALUE_IGNORE = 1;

    public List<AppUpdate> mUpdates = new ArrayList<AppUpdate>();

    public List<AppUpdate> mIgnores = new ArrayList<AppUpdate>();

    public static final int COMMAND_GO_BACK = 1;

    public static final int COMMAND_POST_MESSAGE = 5;

    public static final int COMMAND_INJECT_JAVASCRIPT = 6;

    public ReactExpandableListViewManager() {

    }

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    protected ExpandableListView createViewInstance(ThemedReactContext reactContext) {
        expandableListView = new ReactExpandableListView(reactContext);
        mUpdateAdapter = new ExpandableListAdapter(reactContext);
        mUpdateAdapter.registerCallback(this);//注册回调函数
        expandableListView.setAdapter(mUpdateAdapter);
        expandableListView.setCacheColorHint(Color.TRANSPARENT);//点击时候不会变黑
        expandableListView.setGroupIndicator(null);//去掉左边图标
        expandableListView.expandGroup(ExpandableListAdapter.GROUP_UPDATE);//触发展开
        expandableListView.expandGroup(ExpandableListAdapter.GROUP_IGNORE);//触发展开
        activity = reactContext.getCurrentActivity();
        this.reactContext = reactContext;
        showData();
        return expandableListView;
    }

    @ReactProp(name = "layoutWidth")
    public void setLayoutWidth(ExpandableListView view, int layoutWidth) {

    }

    @ReactProp(name = "layoutHeight")
    public void setLayoutHeight(ExpandableListView view, int layoutHeight) {

    }

    public void showData() {
        final Thread t = Thread.currentThread();
        Observable.create(new Observable.OnSubscribe<JSONObject>() {
            @Override
            public void call(Subscriber<? super JSONObject> subscriber) {
                byte[] bytes = readFromAsset(activity, "preload/update.json");
                JSONObject jsonObj = null;
                if (bytes != null) {
                    try {
                        jsonObj = new JSONObject(new String(bytes));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                subscriber.onNext(jsonObj);
                subscriber.onCompleted();
            }
        })
                .subscribeOn(Schedulers.io()) // 指定 subscribe() 发生在 IO 线程
                .observeOn(AndroidSchedulers.mainThread()) // 指定 Subscriber 的回调发生在主线程
                .subscribe(new Observer<JSONObject>() {
                    @Override
                    public void onNext(JSONObject jsonObj) {
                        JSONObject entities = jsonObj.optJSONObject(NAME_ENTITIES);
                        if (entities != null) {
                            try {
                                readFromJSON(entities);
                                //上次忽略更新的应用
                                SharedPreferences pref = activity.getSharedPreferences(PREF_IGNORE, 0);
                                Set<String> ignoreSet = pref.getAll().keySet();
                                List<AppUpdate> update = new ArrayList<AppUpdate>();
                                List<AppUpdate> ignore = new ArrayList<AppUpdate>();
                                if (mUpdates != null) {
                                    for (AppUpdate au : mUpdates) {
                                        //比较本地应用
                                        //int status = getXXX(au.mPackageName, au.mVersionCode, au.mVersion);
                                        //if (status != STATUS_INSTALLED_OLD_VERSION)
                                        //    continue;
                                        if (ignoreSet.contains(au.mPackageName)) {
                                            ignore.add(au);
                                        } else {
                                            update.add(au);
                                        }
                                    }
                                }
                                mUpdateAdapter.setData(update, ignore);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {

                    }
                });
        /*
        JSONObject jsonObj = null;
        try {
            byte[] bytes = readFromAsset(this, "preload/update.json");
            if (bytes != null) {
                jsonObj = new JSONObject(new String(bytes));
            } else {

            }
            JSONObject entities = jsonObj.optJSONObject(NAME_ENTITIES);
            if (entities != null) {
                readFromJSON(entities);
                //上次忽略更新的应用
                SharedPreferences pref = getSharedPreferences(PREF_IGNORE, 0);
                Set<String> ignoreSet = pref.getAll().keySet();
                List<AppUpdate> update = new ArrayList<AppUpdate>();
                List<AppUpdate> ignore = new ArrayList<AppUpdate>();
                if (mUpdates != null) {
                    for (AppUpdate au : mUpdates) {
                        //比较本地应用
                        //int status = getXXX(au.mPackageName, au.mVersionCode, au.mVersion);
                        //if (status != STATUS_INSTALLED_OLD_VERSION)
                        //    continue;
                        if (ignoreSet.contains(au.mPackageName)) {
                            ignore.add(au);
                        } else {
                            update.add(au);
                        }
                    }
                }
                mUpdateAdapter.setData(update, ignore);
            }
        } catch (Exception e) {
        }
        */
    }

    public static byte[] readFromAsset(Context context, String fileName) {
        byte[] ret = null;
        InputStream instream = null;
        try {
            instream = context.getAssets().open(fileName);
            byte[] buffer = new byte[8192];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int len = -1;
            while ((len = instream.read(buffer)) >= 0)
                baos.write(buffer, 0, len);
            baos.flush();
            ret = baos.toByteArray();
            baos.close();
        } catch (IOException e) {
        } finally {
            try {
                if (instream != null)
                    instream.close();
            } catch (IOException e) {
            }
        }
        return ret;
    }


    public void readFromJSON(JSONObject jsonObj) throws JSONException {
        mUpdates.clear();
        Object upAppItemObj = jsonObj.opt(JSON_UPAPPITEMS);
        if (upAppItemObj != null) {
            // 兼容两种更新接口数据
            if (upAppItemObj instanceof JSONArray) {
                parseUpdateArrayData(mUpdates, (JSONArray) upAppItemObj);
            } else if (upAppItemObj instanceof JSONObject) {
                int objCount = ((JSONObject) upAppItemObj).length();
                parseUpdateObjData(mUpdates, (JSONObject) upAppItemObj, objCount);
            } else {
                // Can't resolve upappitems, do nothing.
            }
        }

        mIgnores.clear();
        Object ignoreAppItemObj = jsonObj.opt(JSON_IGNOREAPPITEMS);
        if (ignoreAppItemObj != null) {
            if (ignoreAppItemObj instanceof JSONArray) {
                parseUpdateArrayData(mIgnores, (JSONArray) ignoreAppItemObj);
            } else if (ignoreAppItemObj instanceof JSONObject) {
                int objCount = ((JSONObject) ignoreAppItemObj).length();
                parseUpdateObjData(mIgnores, (JSONObject) ignoreAppItemObj, objCount);
            } else {
                // Can't resolve ignoreappitems, do nothing.
            }
        }
    }


    private void parseUpdateObjData(List<AppUpdate> outList, JSONObject jsonObj, int objCount) {
        int length = objCount;
        if (jsonObj == null || objCount <= 0)
            return;
        for (int pos = 0; pos < length; ++pos) {
            JSONObject updateObj = jsonObj.optJSONObject(String.valueOf(pos));
            if (updateObj == null)
                continue;
            try {
                AppUpdate update = new AppUpdate();
                update.readFromJSON(updateObj);
                outList.add(update);
            } catch (JSONException e) {
                continue;
            }
        }
    }

    private void parseUpdateArrayData(List<AppUpdate> outList, JSONArray jsonObj) {
        JSONArray updateArray = jsonObj;
        int length = 0;
        if (updateArray != null && (length = updateArray.length()) > 0) {
            for (int pos = 0; pos < length; ++pos) {
                JSONObject updateObj = updateArray.optJSONObject(pos);
                if (updateObj == null)
                    continue;
                try {
                    AppUpdate update = new AppUpdate();
                    update.readFromJSON(updateObj);
                    outList.add(update);
                } catch (JSONException e) {
                    continue;
                }
            }
        }
    }

    public JSONObject generateJSONObject() throws JSONException {
        JSONObject ret = new JSONObject();
        JSONArray array = new JSONArray();
        for (AppUpdate update : mUpdates) {
            if (update == null)
                continue;
            JSONObject updateObj = update.generateJSONObject();
            array.put(updateObj);
        }
        ret.put(JSON_UPAPPITEMS, array);
        array = new JSONArray();
        for (AppUpdate update : mIgnores) {
            if (update == null)
                continue;
            JSONObject updateObj = update.generateJSONObject();
            array.put(updateObj);
        }
        ret.put(JSON_IGNOREAPPITEMS, array);
        return ret;
    }

    @Override
    public void onUpdate(ExpandableListAdapter.UpdateInfoHolder updateInfo) {//更新
        Log.d(TAG, "onUpdate");
        emitExpandListViewEvent(expandableListView);
    }

    @Override
    public void onIgnore(AppUpdate item) {//忽略
        Log.d(TAG, "onIgnore");
        if (item == null || TextUtils.isEmpty(item.mPackageName))
            return;
        SharedPreferences pref = activity.getSharedPreferences(PREF_IGNORE, 0);
        pref.edit().putInt(item.mPackageName, VALUE_IGNORE).commit();
        showData();
    }

    @Override
    public void onRemoveIgnore(AppUpdate item) {//取消忽略
        Log.d(TAG, "onRemoveIgnore");
        if (item == null || TextUtils.isEmpty(item.mPackageName))
            return;

        SharedPreferences pref = activity.getSharedPreferences(Constants.PREF_IGNORE, 0);
        pref.edit().remove(item.mPackageName).commit();
        showData();
    }

    //组装发送到js的数据
    private WritableMap createExpandListViewEvent(ExpandableListView expandListView) {
        WritableMap event = Arguments.createMap();
        event.putDouble("target", 2222222);
        event.putString("url", "22222");
        event.putBoolean("loading", true);
        event.putDouble("width", expandListView.getWidth());
        return event;
    }

    public void emitExpandListViewEvent(ExpandableListView expandListView) {
        dispatchEvent(expandableListView, new ReactExpandListViewEvent(expandListView.getId(), createExpandListViewEvent(expandListView)));
    }

    //发送事件到js
    private static void dispatchEvent(ExpandableListView expandableListView, Event event) {
        ReactContext reactContext = (ReactContext) expandableListView.getContext();
        EventDispatcher eventDispatcher =
                reactContext.getNativeModule(UIManagerModule.class).getEventDispatcher();
        eventDispatcher.dispatchEvent(event);
    }

    //注册事件（ReactExpandListViewEvent）
    @Override
    public Map<String, Object> getExportedCustomDirectEventTypeConstants() {
        return MapBuilder.<String, Object>builder()
                .put("topExpandListViewClick", MapBuilder.of("registrationName", "onExpandListViewClick"))
                .build();
    }

    //注册命令
    @Override
    public @Nullable
    Map<String, Integer> getCommandsMap() {
        return MapBuilder.of(
                "goExpand", COMMAND_GO_BACK,
                "postMessageExpand", COMMAND_POST_MESSAGE,
                "injectJavaScriptExpand", COMMAND_INJECT_JAVASCRIPT
        );
    }

    //接收命令；js通过命令来操作(比如ReactNative页面上有个
    // 前进按钮点击后可以触发这些命令）
    @Override
    public void receiveCommand(ExpandableListView root, int commandId, @Nullable ReadableArray args) {
        switch (commandId) {
            case COMMAND_GO_BACK:
                UiThreadUtil.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(activity, "hello", Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            case COMMAND_POST_MESSAGE:
                try {
                    JSONObject eventInitDict = new JSONObject();
                    eventInitDict.put("data", args.getString(0));
                    Toast.makeText(activity,eventInitDict.toString(),Toast.LENGTH_SHORT);
                    /*
                    root.loadUrl("javascript:(function () {" +
                            "var event;" +
                            "var data = " + eventInitDict.toString() + ";" +
                            "try {" +
                            "event = new MessageEvent('message', data);" +
                            "} catch (e) {" +
                            "event = document.createEvent('MessageEvent');" +
                            "event.initMessageEvent('message', true, true, data.data, data.origin, data.lastEventId, data.source);" +
                            "}" +
                            "document.dispatchEvent(event);" +
                            "})();");
                            */
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                break;
            case COMMAND_INJECT_JAVASCRIPT:
                //root.loadUrl("javascript:" + args.getString(0));
                break;
        }
    }

    //注销时调用
    @Override
    public void onDropViewInstance(ExpandableListView expandListView) {
        super.onDropViewInstance(expandListView);
    }

    //侦听Activity的onResume
    @Override
    public void onHostResume() {

    }

    //侦听Activity的onPause
    @Override
    public void onHostPause() {

    }

    //侦听Activity的onDestroy
    @Override
    public void onHostDestroy() {

    }
}