/*
 * Copyright (c) StreetHawk, All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */

package com.streethawk.library.core;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

class Install extends LoggingBase {
    public static class InstallInfo {
        public String installid;
        public String mode;
        public String app;
        public String client_version;
        public String userid;
        public String access_data;
        public String latitude;
        public String longitude;
        public String negative_feedback;
        public String revoked;
        public String operating_system;
        public String os_version;
        public String model;
        public String resolution;
        public String carrier_name;
        public String identifier_for_vendor;

        public String macaddress;
        public String ipaddress;


        public void fillFromJson(Context context, JSONObject item) throws JSONException {
            if (item == null) {
                return;
            }
            if (item.has("installid")) {
                installid = item.getString("installid");
            }
            if (item.has("mode")) {
                mode = item.getString("mode");
            }
            if (item.has("app")) {
                app = item.getString("app");
            }
            if (item.has("client_version")) {
                client_version = item.getString("client_version");
            }
            if (item.has("userid")) {
                userid = item.getString("userid");
            }
            if (item.has("access_data")) {
                access_data = item.getString("access_data");
            }
            if (item.has("latitude")) {
                latitude = item.getString("latitude");
            }
            if (item.has("longitude")) {
                longitude = item.getString("longitude");
            }
            if (item.has("negative_feedback")) {
                negative_feedback = item.getString("negative_feedback");
            }
            if (item.has("revoked")) {
                revoked = item.getString("revoked");
            }
            if (item.has("operating_system")) {
                operating_system = item.getString("operating_system");
            }
            if (item.has("os_version")) {
                os_version = item.getString("os_version");
            }
            if (item.has("model")) {
                model = item.getString("model");
            }
            if (item.has("resolution")) {
                resolution = item.getString("resolution");
            }
            if (item.has("carrier_name")) {
                carrier_name = item.getString("carrier_name");
            }
            if (item.has("identifier_for_vendor")) {
                identifier_for_vendor = item.getString("identifier_for_vendor");
            }
            if (item.has("macaddress")) {
                macaddress = item.getString("macaddress");
            }
            if (item.has("ipaddress")) {
                ipaddress = item.getString("ipaddress");
            }
        }

    } //End of class InstallInfo

    //private Set<PluginBase> mPluginArray;

    public static final int INSTALL_CODE_IGNORE = 0;
    public static final int INSTALL_CODE_REGISTER = 1;
    public static final int INSTALL_CODE_PUSH_TOKEN = 2;
    public static final int INSTALl_CODE_ALERTSETTINGS = 3;


    public static InstallInfo parseInstallInfo(Context context, String json) {
        InstallInfo result = new InstallInfo();
        if (json == null) {
            return result;
        }
        try {
            JSONObject object = new JSONObject(json);
            JSONObject install = object.getJSONObject("value");
            result.fillFromJson(context, install);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return result;
    }

    private static final String SUBTAG = "Install ";
    private static Install mInstall = null;
    private static Context mContext;
    private static final String INSTALL_LOG_ALLOWED = "install_log_allowed";
    private static final String SESSION_ID_KEY = "sessionid";
    private static final String COOKIE_KEY = "Cookie";
    protected static final String APP_KEY = "app_key";
    private final String CLIENT_VERSION = "client_version";
    private final String SH_LIBRARY_VERSION = "sh_version";
    private final String MODEL = "model";
    private final String MODE = "mode";
    private final String MODE_DEV = "dev";
    private final String MODE_REL = "rel";
    private final String MODE_GCM = "gcm";
    private final String OPERATING_SYSTEM = "operating_system";
    private final String OPERATING_SYSTEM_ANDROID = "android";
    private final String OS_VERSION = "os_version";
    private final String ACCESS_DATA = "access_data";
    private final String REVOKED = "revoked";
    private final String LATITUDE = "latitude";
    private final String LONGITUDE = "longitude";
    private final String MACADDRESS = "macaddress";
    private final String IMEINUMBER = "imeinumber";
    private final String CARRIER_NAME = "carrier_name";
    private final String RESOLUTION = "resolution";
    private final String USER_ID = "userid";
    private final String IPADDRESS = "ipaddress";
    private final String NEGATIVE_FEEDBACK = "negative_feedback";
    private final String CREATED = "created";
    private final String MODIFIED = "modified";
    private final String IBEACON = "ibeacons";
    private final String CODE = "code";
    private final String DEVELOPMENT_PLATFORM = "development_platform";
    private final String LIVE = "live";
    private final String FEATURE_LOCATIONS = "feature_locations";
    private final String FEATURE_PUSH = "feature_push";
    private final String FEATURE_BEACONS = "feature_ibeacons";
    private final String UTC_OFFSET = "utc_offset";
    private final String IDENTIFIER_FOR_VENDOR = "identifier_for_vendor";
    private final String WIDTH = "width";
    private final String HEIGHT = "height";

    private ISHEventObserver mEventObserver = null;


    private Install(Context context) {
        super(context);
        mContext = context;
    }

    public static Install getInstance(Context context) {
        if (null == mInstall)
            mInstall = new Install(context);
        return mInstall;
    }

    private String getModelString() {
        String modelStr = "(" + Build.MANUFACTURER + ") " + Build.BRAND + " " + Build.MODEL;
        if (modelStr.length() >= 64) {
            Log.w(Util.TAG, "Resizing modelname to fit 64 chars");
            modelStr = modelStr.substring(0, 63);   // clip model name to 64 chars
        }
        return modelStr;
    }

    public void registerInstallEventObserver(ISHEventObserver obj) {
        mEventObserver = obj;
    }


    /**
     * Use this function if you wish to update sinfle param for the install
     *
     * @param key   param to be updated
     * @param Value value of params
     */
    public void updateInstall(String key, String Value, int code) {
        HashMap<String, String> logMap = new HashMap<String, String>();
        logMap.put(key, Value);
        updateInstall(logMap, code);
    }


    public void updateAckStatusFromCode(int code, boolean status) {

        switch (code) {
            case INSTALL_CODE_IGNORE:
                break;
            case INSTALL_CODE_PUSH_TOKEN:
                SharedPreferences prefs = mContext.getSharedPreferences(Util.SHSHARED_PREF_PERM, Context.MODE_PRIVATE);
                SharedPreferences.Editor e = prefs.edit();
                e.putBoolean(Util.SHGCMREGISTERED, status);
                e.commit();
                break;
        }

    }

    /**
     * Use this function if you wish to update multiple params for thes install
     *
     * @param logMap HashMap< params,value >
     */
    public void updateInstall(HashMap<String, String> logMap, int code) {
        if (!Util.isNetworkConnected(mContext)) {
            Log.e(Util.TAG, SUBTAG + "Failed to Update install No network connection");
            return;
        }
        if (null == logMap) {
            logMap = new HashMap<String, String>();
        }
        try {
            URL url = new URL(buildUri(mContext, ApiMethod.INSTALL_UPDATE, null));
            String installId = Util.getInstallId(mContext);
            if (null == installId) {
                return;
            }
            logMap.put(INSTALL_ID, installId);
            logMap.put(APP_KEY, Util.getAppKey(mContext));
            logMap.put(SH_LIBRARY_VERSION, Util.getLibraryVersion());
            logMap.put(CLIENT_VERSION, Util.getAppVersionName(mContext));
            logMap.put(MODEL, getModelString());
            logMap.put(OPERATING_SYSTEM, OPERATING_SYSTEM_ANDROID);
            logMap.put(OS_VERSION, Build.VERSION.RELEASE);
            flushInstallParamsToServer(url, logMap, code);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void flushInstallParamsToServer(final URL url, final HashMap<String, String> logMap, final int code) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Activity activity = StreetHawk.INSTANCE.getCurrentActivity();
                final String app_key = Util.getAppKey(mContext);
                String installId = Util.getInstallId(mContext);
                BufferedReader reader = null;
                try {
                    HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(15000);
                    connection.setRequestMethod("POST");
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.setRequestProperty("X-Installid", installId);
                    connection.setRequestProperty("X-App-Key", app_key);
                    String libVersion = Util.getLibraryVersion();
                    connection.setRequestProperty("User-Agent", app_key + "(" + libVersion + ")");
                    OutputStream os = connection.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(
                            new OutputStreamWriter(os, "UTF-8"));
                    //String logs = Util.getPostDataString(logMap);
                    String logs = "";
                    boolean first = true;
                    for (Map.Entry<String, String> entry : logMap.entrySet()) {
                        StringBuilder result = new StringBuilder();
                        if (first)
                            first = false;
                        else
                            result.append("&");
                        String key = entry.getKey();
                        String value = entry.getValue();
                        if (null != key) {
                            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                            result.append("=");
                            if (null != value) {
                                result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
                            } else {
                                result.append(URLEncoder.encode("", "UTF-8"));
                            }
                        }
                        logs += result.toString();
                        result = null; //Force GC
                    }

                    writer.write(logs);
                    writer.flush();
                    writer.close();
                    os.close();
                    reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String answer = reader.readLine();
                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        processAppStatusCall(answer);
                        updateAckStatusFromCode(code, true);
                        InstallInfo installInfo = parseInstallInfo(mContext, answer);
                        if (installInfo != null && !TextUtils.isEmpty(installInfo.installid)) {
                            SharedPreferences prefs = mContext.getSharedPreferences(Util.SHSHARED_PREF_PERM, Context.MODE_PRIVATE);
                            SharedPreferences.Editor e = prefs.edit();
                            e.putBoolean(SHINSTALL_STATE, true);
                            e.putString(INSTALL_ID, installInfo.installid);       //App received installid here
                            e.commit();
                            try {
                                AdvertisingId.AdInfo adInfo = AdvertisingId.getAdvertisingIdInfo(mContext);
                                String advId = adInfo.getId();
                                SharedPreferences sharedPreferences = mContext.getSharedPreferences(Util.SHSHARED_PREF_PERM, Context.MODE_PRIVATE);
                                SharedPreferences.Editor ed = sharedPreferences.edit();
                                ed.putString(SHADVERTISEMENTID, advId);
                                ed.commit();
                                StreetHawk.INSTANCE.tagString(KEY_ADV_ID, advId);
                                Bundle extras = new Bundle();
                                extras.putInt(CODE, CODE_UPDATE_CUSTOM_TAG);
                                extras.putString(SHMESSAGE_ID, null);
                                extras.putString(SH_KEY, KEY_ADV_ID);
                                extras.putString(TYPE_STRING, advId);
                                Logging manager = Logging.getLoggingInstance(mContext);
                                manager.addLogsForSending(extras);
                            } catch (Exception ex) {
                                Log.e(Util.TAG, "Cannot process tagging adv id");
                            }

                            if (null != mEventObserver)
                                mEventObserver.onInstallRegistered(installInfo.installid);
                            if (Util.getPlatformType() != Util.PLATFORM_XAMARIN) {
                                //Init modules available
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Class noParams[] = {};
                                        Class[] paramContext = new Class[1];
                                        paramContext[0] = Activity.class;
                                        Class growth = null;
                                        try {
                                            growth = Class.forName("com.streethawk.library.growth.Growth");
                                            Method growthMethod = growth.getMethod("getInstance", paramContext);
                                            Object obj = growthMethod.invoke(null, activity);
                                            if (null != obj) {
                                                Method addGrowthModule = growth.getDeclaredMethod("addGrowthModule", noParams);
                                                addGrowthModule.invoke(obj);
                                            }
                                        } catch (ClassNotFoundException e1) {
                                            Log.w(Util.TAG, "Growth module is not  not present");
                                        } catch (IllegalAccessException e1) {
                                            e1.printStackTrace();
                                        } catch (NoSuchMethodException e1) {
                                            e1.printStackTrace();
                                        } catch (InvocationTargetException e1) {
                                            e1.printStackTrace();
                                        }

                                    }
                                }).start();
                            }
                            int timezone = Util.getTimeZoneOffsetInMinutes();
                            Bundle logParams = new Bundle();
                            logParams.putInt(CODE, CODE_DEVICE_TIMEZONE);
                            logParams.putString(SHMESSAGE_ID, null);
                            logParams.putString(TYPE_NUMERIC, Integer.toString(Util.getTimeZoneOffsetInMinutes()));
                            Logging manager = Logging.getLoggingInstance(mContext);
                            manager.addLogsForSending(logParams);
                            SharedPreferences sharedPreferences = mContext.getSharedPreferences(Util.SHSHARED_PREF_PERM, Context.MODE_PRIVATE);
                            SharedPreferences.Editor edit = sharedPreferences.edit();
                            edit.putInt(SHTIMEZONE, timezone);
                            edit.commit();
                        }
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Class noParams[] = {};
                                Class[] paramContext = new Class[1];
                                paramContext[0] = Context.class;
                                Class push = null;
                                try {
                                    push = Class.forName("com.streethawk.library.push.Push");
                                    Method pushMethod = push.getMethod("getInstance", paramContext);
                                    Object obj = pushMethod.invoke(null, mContext);
                                    if (null != obj) {
                                        Method addPushModule = push.getDeclaredMethod("addPushModule", noParams);
                                        addPushModule.invoke(obj);
                                    }
                                } catch (ClassNotFoundException e1) {
                                    Log.w(Util.TAG, "Push module is not  not present");
                                } catch (IllegalAccessException e1) {
                                    e1.printStackTrace();
                                } catch (NoSuchMethodException e1) {
                                    e1.printStackTrace();
                                } catch (InvocationTargetException e1) {
                                    e1.printStackTrace();
                                }
                            }
                        }).start();

                    } else {
                        processErrorAckFromServer(answer);
                        updateAckStatusFromCode(code, false);
                        Log.e(Util.TAG, "Failed to register install at first run " + answer);
                    }
                    connection.disconnect();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (ProtocolException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }


    private void registerInstallToCorrectHost(final String app_key) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HashMap<String, String> logMap = new HashMap<String, String>();
                logMap.put(APP_KEY, app_key);
                logMap.put(SH_LIBRARY_VERSION, Util.getLibraryVersion());
                logMap.put(CLIENT_VERSION, Util.getAppVersionName(mContext));
                logMap.put(MODEL, getModelString());
                logMap.put(MODE, MODE_GCM);
                logMap.put(OPERATING_SYSTEM, OPERATING_SYSTEM_ANDROID);
                logMap.put(OS_VERSION, Build.VERSION.RELEASE);
                logMap.put(CARRIER_NAME, Util.getCarrierName(mContext));
                int mWidth = 0;
                int mHeight = 0;
                final WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
                Display display = windowManager.getDefaultDisplay();
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    mWidth = display.getWidth();
                    mHeight = display.getHeight();

                } else {
                    try {
                        Point size = new Point();
                        display.getRealSize(size);
                        mWidth = size.x;
                        mHeight = size.y;
                    } catch (Exception e) {
                        mWidth = 0;
                        mHeight = 0;
                    }
                }
                String width;
                String height;
                try {
                    width = Integer.toString(mWidth);
                    height = Integer.toString(mHeight);
                } catch (NumberFormatException e) {
                    width = Integer.toString(0);
                    height = Integer.toString(0);
                }
                logMap.put(WIDTH, width);
                logMap.put(HEIGHT, height);
                logMap.put(DEVELOPMENT_PLATFORM, Util.getPlatformName());
                logMap.put(LIVE, Boolean.toString(Util.isAppInstalledFromPlayStore(mContext)));
                logMap.put(UTC_OFFSET, Integer.toString(Util.getTimeZoneOffsetInMinutes()));
                try {
                    TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
                    if (null != telephonyManager) {
                        logMap.put(IDENTIFIER_FOR_VENDOR, telephonyManager.getDeviceId());
                    }
                } catch (SecurityException e) {
                }
                try {
                    URL url = new URL(buildUri(mContext, ApiMethod.INSTALL_REGISTER, null));
                    flushInstallParamsToServer(url, logMap, INSTALL_CODE_REGISTER);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }).start();
    }

    private void setHostFromRoute(String answer) {
        try {
            JSONObject object = new JSONObject(answer);
            if (object.has(Util.APP_STATUS)) {
                if (object.get(Util.APP_STATUS) instanceof JSONObject) {
                    JSONObject app_status = object.getJSONObject(Util.APP_STATUS);
                    if (app_status.has(STREETHAWK) && !app_status.isNull(STREETHAWK)) {
                        Object value_streethawk = app_status.get(STREETHAWK);
                        if (value_streethawk instanceof Boolean) {
                            setStreethawkState((Boolean) value_streethawk);
                        }
                    }
                    if (app_status.has(HOST) && !app_status.isNull(HOST)) {
                        Object value_host = app_status.get(HOST);
                        if (value_host instanceof String) {
                            setHost((String) value_host);
                        }
                    }
                    if (app_status.has(GROWTH_HOST) && !app_status.isNull(GROWTH_HOST)) {
                        Object value_host = app_status.get(GROWTH_HOST);
                        if (value_host instanceof String) {
                            setGrowthHost((String) value_host);
                        }
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    public void registerInstall() {
        if (!Util.isNetworkConnected(mContext)) {
            Log.e(Util.TAG, SUBTAG + "Failed to register install No network connection");
            return;
        }
        final String app_key = Util.getAppKey(mContext);
        if (null == app_key) {
            Log.e(Util.TAG, SUBTAG + "Failed to register install as Appkey is null");
            return;
        }
        if (app_key.isEmpty()) {
            Log.e(Util.TAG, SUBTAG + "Failed to register install as Appkey is empty");
            return;
        }
        if (Util.getInstallId(mContext) != null) {
            // Install is already registered
            return;
        }
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                final String ROUTE_HOST = "https://route.streethawk.com/v1/apps/status";
                try {
                    Bundle query = new Bundle();
                    query.putString(Util.SHAPP_KEY, app_key);
                    URL url = new URL(ROUTE_HOST);
                    HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(15000);
                    connection.setRequestMethod("GET");
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.setRequestProperty("X-App-Key", app_key);
                    String libVersion = Util.getLibraryVersion();
                    connection.setRequestProperty("X-Version", libVersion);
                    connection.setRequestProperty("User-Agent", app_key + "(" + libVersion + ")");
                    connection.connect();
                    BufferedReader reader = null;
                    reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String answer = reader.readLine();
                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        //Log.e("Anurag","Answer for host "+answer);
                        setHostFromRoute(answer);
                        if (Util.getStreethawkState(mContext))
                            registerInstallToCorrectHost(app_key);
                    } else {
                        processErrorAckFromServer(answer);
                    }
                    connection.disconnect();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();

    }
}