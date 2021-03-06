/* Copyright (c) StreetHawk, All rights reserved. This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation; either version 3.0 of the License, or (at your option) any later version. This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details. You should have received a copy of the GNU Lesser General Public License along with this library. */
package com.streethawk.library.geofence;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.streethawk.library.core.Logging;
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
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

public class SHCoreModuleReceiver extends BroadcastReceiver implements Constants {
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

    /**
     * Fetch geofence list from the server
     *
     * @param context
     */
    private void fetchGeofenceList(final Context context) {
        if (null == context) return;
        if (Util.isNetworkConnected(context)) new Thread(new Runnable() {
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
                        String libVersion = Util.getLibraryVersion();
                        URL url = Util.getGeofenceUrl(context);
                        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                        connection.setReadTimeout(10000);
                        connection.setConnectTimeout(15000);
                        connection.setRequestMethod("GET");
                        connection.setDoInput(true);
                        connection.setDoOutput(true);
                        connection.setRequestProperty("X-Installid", installId);
                        connection.setRequestProperty("X-App-Key", app_key);
                        connection.setRequestProperty("X-Version", libVersion);
                        connection.setRequestProperty("User-Agent", app_key + "(" + libVersion + ")");
                        OutputStream os = connection.getOutputStream();
                        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8")); /*String logs = Util.getPostDataString(logMap);*/
                        String logs = "";
                        boolean first = true;
                        for (Map.Entry<String, String> entry : logMap.entrySet()) {
                            StringBuilder result = new StringBuilder();
                            if (first) first = false;
                            else result.append("&");
                            String key = entry.getKey();
                            String value = entry.getValue();
                            if (null != key) {
                                result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                                result.append("=");
                                if (null != value)
                                    result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
                                else result.append(URLEncoder.encode("", "UTF-8"));
                            }
                            logs += result.toString();
                            result = null; /*Force GC*/
                        }
                        writer.write(logs);
                        writer.flush();
                        writer.close();
                        os.close();
                        reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        String answer = reader.readLine();
                        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                            if (null == answer) return;
                            if (answer.isEmpty()) return;
                            try {
                                JSONObject jsonObject = new JSONObject(answer);
                                JSONArray value = jsonObject.getJSONArray(Util.JSON_VALUE);
                                StreetHawkLocationService instance = StreetHawkLocationService.getInstance(context);
                                instance.stopMonitoring();
                                forceClearGeofenceData(context);
                                SharedPreferences sharedPreferences = context.getSharedPreferences(Util.SHSHARED_PREF_PERM, Context.MODE_PRIVATE);
                                if (Util.getSHDebugFlag(context)) {
                                    Log.d(Util.TAG, "Saved geofence list " + value);
                                }
                                instance.storeGeofenceData(value);
                                boolean status = sharedPreferences.getBoolean(IS_GEOFENCE_ENABLE, false);
                                if (status) {
                                    ArrayList<GeofenceData> geofenceList = new ArrayList<GeofenceData>();
                                    instance.storeGeofenceListForMonitoring(geofenceList);
                                    instance.startGeofenceMonitoring();
                                }

                            } catch (JSONException e) {
                            }
                            Logging.getLoggingInstance(context).processAppStatusCall(answer);
                        } else {
                            SharedPreferences sharedPreferences = context.getSharedPreferences(Util.SHSHARED_PREF_PERM, Context.MODE_PRIVATE);
                            SharedPreferences.Editor e = sharedPreferences.edit();
                            e.putString(KEY_GEOFENCE, null);
                            e.commit();
                            Logging.getLoggingInstance(context).processErrorAckFromServer(answer);
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

    /**
     * Check for timestamp of geofence. Return if same as before, else store the received geofence timestamp and fetch geofence tree
     *
     * @param context
     * @param value_geofence
     */
    private void setGeofenceListTimeStamp(Context context, String value_geofence) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Util.SHSHARED_PREF_PERM, Context.MODE_PRIVATE);
        String currentTimeStamp = sharedPreferences.getString(KEY_GEOFENCE, null);
        if (null == value_geofence)
            return;
        if (currentTimeStamp != null) {
            if (value_geofence.equals(currentTimeStamp))
                return;
            else {
                SharedPreferences.Editor edit = sharedPreferences.edit();
                edit.putString(KEY_GEOFENCE, value_geofence);
                edit.commit();
                fetchGeofenceList(context);
            }
        } else {
            SharedPreferences.Editor edit = sharedPreferences.edit();
            edit.putString(KEY_GEOFENCE, value_geofence);
            edit.commit();
            fetchGeofenceList(context);
        }
    }


    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();
        if (action == Util.BROADCAST_SH_APP_STATUS_NOTIFICATION) {
            String installId = intent.getStringExtra(Util.INSTALL_ID);
            if (null == installId)
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
        if (action.equals("android.location.PROVIDERS_CHANGED")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }
            final Handler delay = new Handler();
            delay.postDelayed(new Runnable() {
                                  @Override
                                  public void run() {
                                      LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                                      if (!(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))) {
                                          try {
                                              Bundle extras = new Bundle();
                                              extras.putInt(Util.CODE, CODE_USER_DISABLES_LOCATION);
                                              Logging.getLoggingInstance(context).addLogsForSending(extras);
                                              StreetHawkLocationService.getInstance(context).stopMonitoring();
                                              GeofenceService.notifyAllGeofenceExit(context);
                                              if (Util.getSHDebugFlag(context)) {
                                                  Log.d(Util.TAG, "  " + "Sending locations disabled by server");
                                              }
                                          } catch (Exception e) {
                                              e.printStackTrace();
                                          }
                                      } else {
                                          SharedPreferences sharedPreferences = context.getSharedPreferences(Util.SHSHARED_PREF_PERM, Context.MODE_PRIVATE);
                                          final boolean status = sharedPreferences.getBoolean(IS_GEOFENCE_ENABLE, false);
                                          final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
                                          executor.schedule(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    try {
                                                                        Thread.sleep(1000);
                                                                    } catch (InterruptedException e) {
                                                                        e.printStackTrace();
                                                                    }
                                                                    if (status) {
                                                                        StreetHawkLocationService.getInstance(context).startGeofenceMonitoring();
                                                                    }
                                                                    Location location = StreetHawkLocationService.getInstance(context).getLastKnownLocation(context);
                                                                    if (null != location) {
                                                                        double lat = location.getLatitude();
                                                                        double lng = location.getLongitude();
                                                                        if (lat == 0 && lng == 0)
                                                                            return;
                                                                        Bundle extras = new Bundle();
                                                                        extras.putInt(Util.CODE, CODE_LOCATION_UPDATES);
                                                                        extras.putString(Util.SHMESSAGE_ID, null);
                                                                        extras.putString(LOCAL_TIME, Util.getFormattedDateTime(System.currentTimeMillis(), false));
                                                                        Logging.getLoggingInstance(context).addLogsForSending(extras);
                                                                        if (Util.getSHDebugFlag(context)) {
                                                                            Log.d(Util.TAG, "  " + "*** Sending Location update on location enable ***");
                                                                        }
                                                                    }
                                                                }
                                                            }

                                                  , 1, TimeUnit.MINUTES);
                                      }
                                  }
                              }

                    , 2000);
        }
        if (action.equals("com.streethawk.intent.action.gcm.STREETHAWK_LOCATIONS")) {
            if (null != intent) {
                try {
                    String packageName = intent.getStringExtra(SHPACKAGENAME);
                    if (!packageName.equals(context.getPackageName())) {
                        return;
                    }
                } catch (Exception e) {
                    return;
                }
                Location location = StreetHawkLocationService.getInstance(context).getLastKnownLocation(context);
                if (null != location) {
                    double lat = location.getLatitude();
                    double lng = location.getLongitude();
                    if (lat == 0 && lng == 0)
                        return;
                    Bundle extras = new Bundle();
                    extras.putInt(Util.CODE, CODE_PERIODIC_LOCATION_UPDATE);
                    extras.putString(Util.SHMESSAGE_ID, null);
                    extras.putString(LOCAL_TIME, Util.getFormattedDateTime(System.currentTimeMillis(), false));
                    Logging.getLoggingInstance(context).addLogsForSending(extras);
                    extras.putInt(Util.CODE, CODE_LOCATION_UPDATES);
                    extras.putString(Util.SHMESSAGE_ID, null);
                    extras.putString(LOCAL_TIME, Util.getFormattedDateTime(System.currentTimeMillis(), false));
                    Logging.getLoggingInstance(context).addLogsForSending(extras);
                    if (Util.getSHDebugFlag(context)) {
                        Log.d(Util.TAG, "  " + "*** Sending locaiton update on periodic task ***");
                    }
                }
            }
        }
    }
}
