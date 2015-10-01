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
package com.streethawk.library.geofence;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.streethawk.library.core.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;
public class SHCoreModuleReceiver extends BroadcastReceiver {
    private final String GEOFENCELIST = "geofences";
    private final String KEY_GEOFENCE = "shKeyGeofenceList";
    public SHCoreModuleReceiver() {
    }

    private void forceClearGeofenceData(Context context) {
        GeofenceDB storeGeofenceDB = new GeofenceDB(context);
        storeGeofenceDB.open();
        storeGeofenceDB.forceDeleteAllRecords();
        storeGeofenceDB.close();
    }

    private void fetchGeofenceList(final Context context) {
        if (null == context)
            return;
        if (Util.isNetworkConnected(context)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    {
                        String installId = Util.getInstallId(context);
                        if (null == installId) {
                            SharedPreferences sharedPreferences = context.getSharedPreferences(Util.SHSHARED_PREF_PERM, Context.MODE_PRIVATE);
                            SharedPreferences.Editor e = sharedPreferences.edit();
                            e.putString(KEY_GEOFENCE, null);
                            e.commit();
                            return;
                        }
                        if (installId.isEmpty()) {
                            SharedPreferences sharedPreferences = context.getSharedPreferences(Util.SHSHARED_PREF_PERM, Context.MODE_PRIVATE);
                            SharedPreferences.Editor e = sharedPreferences.edit();
                            e.putString(KEY_GEOFENCE, null);
                            e.commit();
                            return;
                        }
                        String app_key = Util.getAppKey(context);
                        Bundle query = new Bundle();
                        HashMap<String, String> logMap = new HashMap<String, String>();
                        logMap.put(Util.INSTALL_ID, installId);
                        BufferedReader reader = null;
                        try {
                            URL url = Util.getGeofenceUrl(context);
                            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                            connection.setReadTimeout(10000);
                            connection.setConnectTimeout(15000);
                            connection.setRequestMethod("GET");
                            connection.setDoInput(true);
                            connection.setDoOutput(true);
                            connection.setRequestProperty("X-Installid", installId);
                            connection.setRequestProperty("X-App-Key", app_key);
                            connection.setRequestProperty("User-Agent", app_key + "(" + Util.getLibraryVersion() + ")");
                            OutputStream os = connection.getOutputStream();
                            BufferedWriter writer = new BufferedWriter(
                                    new OutputStreamWriter(os, "UTF-8"));
                            String logs = Util.getPostDataString(logMap);
                            writer.write(logs);
                            writer.flush();
                            writer.close();
                            os.close();
                            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                            String answer = reader.readLine();
                            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                                if (null == answer)
                                    return;
                                if (answer.isEmpty())
                                    return;
                                try {
                                    JSONObject jsonObject = new JSONObject(answer);
                                    JSONArray value = jsonObject.getJSONArray(Util.JSON_VALUE);
                                    SHGeofence instance = SHGeofence.getInstance(context);
                                    instance.stopMonitoring();
                                    forceClearGeofenceData(context);
                                    SharedPreferences sharedPreferences = context.getSharedPreferences(Util.SHSHARED_PREF_PERM, Context.MODE_PRIVATE);
                                    SharedPreferences.Editor e = sharedPreferences.edit();
                                    e.putString(Constants.PARENT_GEOFENCE_ID, null);
                                    e.commit();
                                    instance.storeGeofenceList(value);
                                    instance.startGeofenceMonitoring();
                                } catch (JSONException e) {
                                }

                            } else {
                                SharedPreferences sharedPreferences = context.getSharedPreferences(Util.SHSHARED_PREF_PERM, Context.MODE_PRIVATE);
                                SharedPreferences.Editor e = sharedPreferences.edit();
                                e.putString(KEY_GEOFENCE, null);
                                e.commit();
                            }
                            connection.disconnect();
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }

    }

    private void setGeofenceListTimeStamp(Context context, String value_geofence) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Util.SHSHARED_PREF_PERM, Context.MODE_PRIVATE);
        String currentTimeStamp = sharedPreferences.getString(KEY_GEOFENCE, null);
        if (null != currentTimeStamp && value_geofence.equals(currentTimeStamp)) {
            return;
        } else {
            SharedPreferences.Editor edit = sharedPreferences.edit();
            edit.putString(KEY_GEOFENCE, value_geofence);
            edit.commit();

            if (KEY_GEOFENCE != null) {
                fetchGeofenceList(context);
            } else {
                // Force clear geofence list if server sends null as timestamp
                forceClearGeofenceData(context);
                SharedPreferences.Editor e = sharedPreferences.edit();
                e.putString(Constants.PARENT_GEOFENCE_ID, null);
                e.commit();
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == Util.BROADCAST_SH_APP_STATUS_NOTIFICATION) {
            String installId = intent.getStringExtra(Util.INSTALL_ID);
            if(null==installId)
                return;
            if (installId.equals(Util.getInstallId(context))) {
                String answer = intent.getStringExtra(Util.APP_STATUS_ANSWER);
                try {
                    JSONObject object = new JSONObject(answer);
                    if (object.has(Util.APP_STATUS)) {
                        if (object.get(Util.APP_STATUS) instanceof JSONObject) {
                            JSONObject app_status = object.getJSONObject(Util.APP_STATUS);
                            if (app_status.has(GEOFENCELIST) && !app_status.isNull(GEOFENCELIST)) {
                                Object value_geofencetimeStamp = app_status.get(GEOFENCELIST);
                                if (value_geofencetimeStamp instanceof String) {
                                    setGeofenceListTimeStamp(context, (String) value_geofencetimeStamp);
                                }
                            }
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}