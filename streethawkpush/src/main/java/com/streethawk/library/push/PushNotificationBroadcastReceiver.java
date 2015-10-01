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
package com.streethawk.library.push;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.streethawk.library.core.Logging;
import com.streethawk.library.core.StreetHawk;
import com.streethawk.library.core.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;


public class PushNotificationBroadcastReceiver extends BroadcastReceiver {

    private final String SUBTAG = "PushNotificationBroadcastReceiver";
    public PushNotificationBroadcastReceiver() {}
    private static ISHObserver appGcmReceiverList;
    private final String PROJECT_NUMBER = "project_number";
    private final String PUSH = "push";


    public static void updateAppGcmReceiverList(ISHObserver object) {
        appGcmReceiverList = object;
    }


    /**
     * Function displays badges to app icons.
     * Note that not all devices support badges and hence function is ignored for non supporting devices
     *
     * @param BadgeCount
     */
    public static void displayBadge(Context context, int BadgeCount) {
        String deviceManufacturer = Build.MANUFACTURER;
        String packgaeName = context.getPackageName().toString();
        ComponentName componentName = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName()).getComponent();
        String launcherActivityName = componentName.getClassName();

        if (deviceManufacturer.equalsIgnoreCase("GENYMOTION")) {
            String modelStr = "(" + Build.MANUFACTURER + ") " + Build.BRAND + " " + Build.MODEL;
            if (modelStr.toLowerCase().contains("samsung"))
                deviceManufacturer = "SAMSUNG";
            else if (modelStr.toLowerCase().contains("sony"))
                deviceManufacturer = "SONY";
            else if (modelStr.toLowerCase().contains("htc"))
                deviceManufacturer = "HTC";
            else if (modelStr.toLowerCase().contains("lge"))
                deviceManufacturer = "lge";
            else
                deviceManufacturer = "UNKNOWN";
        }
        if (deviceManufacturer.equalsIgnoreCase("SONY")) {
            Intent badgeIntent = new Intent();
            try {
                badgeIntent.setAction("com.sonyericsson.home.action.UPDATE_BADGE");
                badgeIntent.putExtra("com.sonyericsson.home.intent.extra.badge.ACTIVITY_NAME", launcherActivityName);
                badgeIntent.putExtra("com.sonyericsson.home.intent.extra.badge.SHOW_MESSAGE", true);
                badgeIntent.putExtra("com.sonyericsson.home.intent.extra.badge.MESSAGE", Integer.toString(BadgeCount));
                badgeIntent.putExtra("com.sonyericsson.home.intent.extra.badge.PACKAGE_NAME", packgaeName);
                context.sendBroadcast(badgeIntent);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        } else if (deviceManufacturer.equalsIgnoreCase("SAMSUNG")) {
            try {
                ContentResolver samsungCR = context.getContentResolver();
                Uri badgeURI = Uri.parse("content://com.sec.badge/apps");
                ContentValues samsungCV = new ContentValues();
                samsungCV.put("package", packgaeName);
                samsungCV.put("class", launcherActivityName);
                samsungCV.put("badgecount", Integer.valueOf(BadgeCount));
                String str = "package=? AND class=?";
                String[] arrayOfString = new String[2];
                arrayOfString[0] = packgaeName;
                arrayOfString[1] = launcherActivityName;
                int update = samsungCR.update(badgeURI, samsungCV, str, arrayOfString);
                if (update == 0) {
                    samsungCR.insert(badgeURI, samsungCV);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        } else if (deviceManufacturer.equalsIgnoreCase("HTC")) {
            try {
                Intent htcIntent = new Intent("com.htc.launcher.action.UPDATE_SHORTCUT");
                htcIntent.putExtra("packagename", packgaeName);
                htcIntent.putExtra("count", BadgeCount);
                context.sendBroadcast(htcIntent);
                Intent notificationIntent = new Intent("com.htc.launcher.action.SET_NOTIFICATION");
                ComponentName htcComponentName = new ComponentName(context, StreetHawk.INSTANCE.getCurrentActivity().toString());
                notificationIntent.putExtra("com.htc.launcher.extra.COMPONENT", htcComponentName.flattenToShortString());
                notificationIntent.putExtra("com.htc.launcher.extra.COUNT", 99);
                context.sendBroadcast(notificationIntent);
            } catch (Exception e) {

                e.printStackTrace();

                return;
            }
        } else if (deviceManufacturer.equalsIgnoreCase("LGE")) {
            Intent intent = new Intent("android.intent.action.BADGE_COUNT_UPDATE");
            intent.putExtra("badge_count", BadgeCount);
            intent.putExtra("badge_count_package_name", packgaeName);
            intent.putExtra("badge_count_class_name", launcherActivityName);
            context.sendBroadcast(intent);
        } else {

            Log.i(Util.TAG, "Badges are not supported for " + Build.MANUFACTURER + " " + Build.MODEL);

            return;
        }
    }

    /**
     * Function returns true is permission is available in manifest
     * @param context
     * @param code
     * @return
     */
    public static boolean isPermissionAvailable(Context context, int code) {
        switch (code) {
            case NotificationBase.CODE_IBEACON:
                if ((Util.isPermissionAvailable(context, android.Manifest.permission.BLUETOOTH) == -1) &&
                        (Util.isPermissionAvailable(context, android.Manifest.permission.BLUETOOTH) == -1)) {
                    Log.w(Util.TAG, "App is missing Bluetooth permissions in AndroidManifest.xml");
                    return false;
                }
                break;
            case NotificationBase.CODE_CALL_TELEPHONE_NUMBER:
                if ((Util.isPermissionAvailable(context, android.Manifest.permission.CALL_PHONE) == -1)) {
                    Log.w(Util.TAG, "Please add CALL_PHONE permission in AndroidManifest.xml");
                    return false;
                }
                break;
            default:
                return true;
        }
        return true;
    }


    @Override
    public void onReceive(final Context context, Intent intent) {
        // Check for appStatus
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
                            if (app_status.has(PROJECT_NUMBER) && !app_status.isNull(PROJECT_NUMBER)) {
                                Object value_project_number = app_status.get(PROJECT_NUMBER);
                                if (value_project_number instanceof String) {
                                    String newSenderID = (String)value_project_number;
                                    if(value_project_number==null)
                                        return;
                                    if(newSenderID.isEmpty())
                                        return;
                                    SharedPreferences sharedPreferences = context.getSharedPreferences(Util.SHSHARED_PREF_PERM, Context.MODE_PRIVATE);
                                    String stored_sender_key = sharedPreferences.getString(Constants.SHGCM_SENDER_KEY_APP, null);
                                    if(null==stored_sender_key){
                                        SharedPreferences.Editor e = sharedPreferences.edit();
                                        e.putString(Constants.SHGCM_SENDER_KEY_APP, newSenderID);
                                        e.putString(Constants.PROPERTY_REG_ID, null);
                                        e.commit();
                                        Push.getInstance(context).reRegister(newSenderID);
                                    }else{
                                        if(!(stored_sender_key.equals(value_project_number))){
                                            SharedPreferences.Editor e = sharedPreferences.edit();
                                            e.putString(Constants.SHGCM_SENDER_KEY_APP, newSenderID);
                                            e.putString(Constants.PROPERTY_REG_ID, null);
                                            e.commit();
                                            Push.getInstance(context).reRegister(newSenderID);
                                        }
                                    }
                                }
                            }
                            if (object.has(PUSH)) {
                                String error = Integer.toString(-1);
                                String code = error;
                                String messageId = error;
                                String orientation = error;
                                String speed = error;
                                String portion = error;
                                String noConfirm = null;
                                String installid = null;
                                String data = null;
                                String aps = null;
                                String titleLength = null;
                                if (object.get(PUSH) instanceof JSONObject) {
                                    JSONObject push = object.getJSONObject(PUSH);
                                    if (push.has(Constants.PUSH_CODE) && !push.isNull(Constants.PUSH_CODE)) {
                                        code = push.get(Constants.PUSH_CODE).toString();
                                    }
                                    if (push.has(Constants.PUSH_MSG_ID) && !push.isNull(Constants.PUSH_MSG_ID)) {
                                        messageId = push.get(Constants.PUSH_MSG_ID).toString();
                                    }
                                    if (push.has(Constants.PUSH_DATA) && !push.isNull(Constants.PUSH_DATA)) {
                                        data = push.get(Constants.PUSH_DATA).toString();
                                    }
                                    if (push.has(Constants.PUSH_TITLE_LENGTH) && !push.isNull(Constants.PUSH_TITLE_LENGTH)) {
                                        titleLength = push.get(Constants.PUSH_TITLE_LENGTH).toString();
                                    }
                                    if (push.has(Constants.PUSH_SHOW_DIALOG) && !push.isNull(Constants.PUSH_SHOW_DIALOG)) {
                                        noConfirm = push.get(Constants.PUSH_SHOW_DIALOG).toString();
                                    }
                                    if (push.has(Constants.PUSH_ORIENTATION) && !push.isNull(Constants.PUSH_ORIENTATION)) {
                                        orientation = push.get(Constants.PUSH_ORIENTATION).toString();
                                    }
                                    if (push.has(Constants.PUSH_PORTION) && !push.isNull(Constants.PUSH_PORTION)) {
                                        portion = push.get(Constants.PUSH_PORTION).toString();
                                    }
                                    if (push.has(Constants.PUSH_SPEED) && !push.isNull(Constants.PUSH_SPEED)) {
                                        speed = push.get(Constants.PUSH_SPEED).toString();
                                    }
                                    if (push.has(Constants.PUSH_INSTALLID) && !push.isNull(Constants.PUSH_INSTALLID)) {
                                        installid = push.get(Constants.PUSH_INSTALLID).toString();
                                    }
                                    if (push.has(Constants.PUSH_APS) && !push.isNull(Constants.PUSH_APS)) {
                                        aps = push.get(Constants.PUSH_APS).toString();
                                    }

                                    final Intent broadcastIntent = new Intent();
                                    broadcastIntent.setAction("com.google.android.c2dm.intent.RECEIVE");
                                    broadcastIntent.putExtra(Constants.PUSH_CODE, code);
                                    broadcastIntent.putExtra(Constants.PUSH_MSG_ID, messageId);
                                    broadcastIntent.putExtra(Constants.PUSH_APS, aps);
                                    broadcastIntent.putExtra(Constants.PUSH_DATA, data);
                                    broadcastIntent.putExtra(Constants.PUSH_ORIENTATION, orientation);
                                    broadcastIntent.putExtra(Constants.PUSH_PORTION, portion);
                                    broadcastIntent.putExtra(Constants.PUSH_SPEED, speed);
                                    broadcastIntent.putExtra(Constants.PUSH_SHOW_DIALOG, noConfirm);
                                    broadcastIntent.putExtra(Constants.PUSH_TITLE_LENGTH, titleLength);
                                    if (null == installid || installid.isEmpty())
                                        installid = Util.getInstallId(context);
                                    broadcastIntent.putExtra(Constants.PUSH_INSTALLID, installid);
                                    context.sendBroadcast(broadcastIntent);
                                }
                            }
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        if (intent.getAction().equals(Constants.BROADCAST_SH_PUSH_NOTIFICATION)) {
            final Bundle extras = intent.getExtras();
            boolean forceToBg = false;
            String msgID = extras.getString(Util.MSGID);
            if (null == msgID) {
                return;
            } else {
                SharedPreferences sharedPreferences = context.getSharedPreferences(Util.SHSHARED_PREF_PERM, Context.MODE_PRIVATE);
                boolean isCustomDialog = sharedPreferences.getBoolean(Constants.SHUSECUSTOMDIALOG_FLAG, false);
                if (sharedPreferences.getString(Constants.PENDING_DIALOG, null) != null) {
                    forceToBg = true;
                }
                PushNotificationDB database = PushNotificationDB.getInstance(context);
                database.open();
                PushNotificationData pushData = new PushNotificationData();
                database.getPushNotificationData(msgID, pushData);
                database.close();
                if (null == pushData) {
                    clearPendingDialogFlagAndDB(context, msgID);
                    return;
                }
                // Display badge
                int badgeCnt = 0;
                try {
                    badgeCnt = Integer.parseInt(pushData.getBadge());
                } catch (NumberFormatException e) {
                    badgeCnt = 0;
                }
                displayBadge(context, badgeCnt);
                int code = 0;
                try {
                    code = Integer.parseInt(pushData.getCode());
                } catch (NumberFormatException e) {
                    return;
                }
                if (code == NotificationBase.CODE_REQUEST_THE_APP_STATUS){
                    Logging.getLoggingInstance(context).checkAppState();
                    return;
                }
                if (!Push.getInstance(context).isUsePush()) {
                    Log.i(Util.TAG, "GCM is disabled in code.Developer has called shSetGcmSupport(false)");
                    return;
                }
                boolean enable_push = sharedPreferences.getBoolean(Constants.SHGCM_FLAG, true);
                if (enable_push) {
                    String title = pushData.getTitle();
                    String msg = pushData.getMsg();
                    String data = pushData.getData();
                    sendAcknowledgement(context, msgID);
                    if (code == NotificationBase.CODE_CUSTOM_JSON_FROM_SERVER) {
                        if (null == appGcmReceiverList) {
                            Log.e(Util.TAG, "No object registered for class implementing ISHObserver. Use registerSHObserver");
                            NotificationBase.sendResultBroadcast(context, msgID, Constants.STREETHAWK_DECLINED);
                            return;
                        } else {
                            NotificationBase.sendResultBroadcast(context, msgID, Constants.STREETHAWK_ACCEPTED);
                            handleCustomJsonFromServer(title, msg, data);
                        }
                    } else if (code == NotificationBase.CODE_REQUEST_THE_APP_STATUS) {
                        Logging.getLoggingInstance(context).checkAppState();
                    } else {
                        // return if permission is missing
                        if (!(isPermissionAvailable(context, code))) {
                            clearPendingDialogFlagAndDB(context, msgID);
                            return;
                        }
                        // Storing msgid for pending dialog
                        SharedPreferences.Editor e = sharedPreferences.edit();
                        e.putString(Constants.PENDING_DIALOG, pushData.getMsgId());
                        e.commit();
                        if (forceToBg || checkIfBG(context)) {
                            // storing package name to distinguish between broadcast received
                            extras.putString(Constants.SHPACKAGENAME, context.getPackageName());
                            switch (code) {
                                case NotificationBase.CODE_IBEACON:
                                    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                                    if (bluetoothAdapter != null) {
                                        boolean isEnabled = bluetoothAdapter.isEnabled();
                                        if (!isEnabled) {
                                            SHBackgroundNotification notificationBeacon = new SHBackgroundNotification(context, pushData);
                                            notificationBeacon.display();
                                        } else {
                                            clearPendingDialogFlagAndDB(context, msgID);
                                            NotificationBase.sendResultBroadcast(context, msgID, Constants.STREETHAWK_ACCEPTED);
                                        }
                                    }
                                    break;
                                case NotificationBase.CODE_ENABLE_LOCATION:
                                    LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                                    if (!(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))) {
                                        SHBackgroundNotification notificationBeacon = new SHBackgroundNotification(context, pushData);
                                        notificationBeacon.display();
                                    } else {
                                        clearPendingDialogFlagAndDB(context, msgID);
                                        NotificationBase.sendResultBroadcast(context, msgID, Constants.STREETHAWK_ACCEPTED);
                                    }
                                    break;
                                default:
                                    SHBackgroundNotification notification = new SHBackgroundNotification(context, pushData);
                                    notification.display();
                                    break;
                            }
                        } else {
                            if (null == appGcmReceiverList) {
                                Log.w(Util.TAG, "No object registered for class implementing ISHObserver. Use registerSHObserver");
                            }else {
                                PushDataForApplication pushDataForApplication = new PushDataForApplication();
                                pushDataForApplication.convertPushDataToPushDataForApp(pushData, pushDataForApplication);
                                appGcmReceiverList.onReceivePushData(pushDataForApplication);
                                if(isCustomDialog) {
                                    Log.i(Util.TAG, "Developer choose to handle push using custom dialog");
                                    return;
                                }
                            }
                            switch (code) {
                                case NotificationBase.CODE_IBEACON:
                                    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                                    if (bluetoothAdapter != null) {
                                        boolean isEnabled = bluetoothAdapter.isEnabled();
                                        if (!isEnabled) {
                                            SHForegroundNotification alert = SHForegroundNotification.getDialogInstance(context, pushData);
                                            alert.display(pushData);
                                        } else {
                                            clearPendingDialogFlagAndDB(context, msgID);
                                            NotificationBase.sendResultBroadcast(context, msgID, Constants.STREETHAWK_ACCEPTED);
                                        }
                                    }
                                    break;
                                case NotificationBase.CODE_ENABLE_LOCATION:
                                    LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                                    if (!(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))) {
                                        SHForegroundNotification alert = SHForegroundNotification.getDialogInstance(context, pushData);
                                        alert.display(pushData);
                                    } else {
                                        //ignoring message and hence clearing
                                        clearPendingDialogFlagAndDB(context, msgID);
                                        NotificationBase.sendResultBroadcast(context, msgID, Constants.STREETHAWK_ACCEPTED);
                                    }
                                    break;
                                default:
                                    SHForegroundNotification alert = SHForegroundNotification.getDialogInstance(context, pushData);
                                    alert.display(pushData);
                                    break;
                            }
                        }
                    }
                }
                // Release the wake lock provided by the WakefulBroadcastReceiver.
                GCMReceiver.completeWakefulIntent(intent);
            }

        } else {
            int DISMISS_BADGE = 0;
            int code = 0;
            boolean sendResult = true;
            Bundle extras = intent.getExtras();
            String msgId = extras.getString(Constants.PENDING_DIALOG);
            String packageName = intent.getStringExtra(Constants.SHPACKAGENAME);
            if (!(context.getPackageName().equals(packageName)))
                return;
            if (msgId == null)
                return;
            boolean fromBG = extras.getBoolean(Constants.FROMBG, false);
            PushNotificationDB dbObject = PushNotificationDB.getInstance(context);
            dbObject.open();
            PushNotificationData dataObject = new PushNotificationData();
            boolean error = dbObject.getPushNotificationData(msgId, dataObject);
            dbObject.close();

            dbObject = null;
            if (null == dataObject) {
                return;
            }
            int result = -2;
            if (fromBG) {
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(Integer.parseInt(msgId));
            }
            try {
                code = Integer.parseInt(dataObject.getCode());
            } catch (NumberFormatException e) {
                code = 0;
            } catch (Exception e) {
                e.printStackTrace();
                code = 0;
            }
            if (intent.getAction().equals(Constants.BROADCAST_STREETHAWK_ACCEPTED)) {
                result = Constants.STREETHAWK_ACCEPTED;
                displayBadge(context, DISMISS_BADGE);
                if (fromBG) {
                    bgActionPositive(context, packageName, dataObject);
                    colapseNotification(context);
                    int tempCode = 0;
                    try {
                        tempCode = Integer.parseInt(dataObject.getCode());
                    } catch (NumberFormatException e) {
                        return;
                    }
                    if (tempCode == NotificationBase.CODE_OPEN_URL || tempCode == NotificationBase.CODE_SIMPLE_PROMPT)
                        sendResult = false;
                } else {
                    switch (code) {
                        case NotificationBase.CODE_OPEN_URL:
                            float p = 0.0f;
                            int o = -1;
                            int s = -1;
                            try {
                                p = Float.parseFloat(dataObject.getPortion());
                            } catch (Exception e) {
                                p = 0.0f;
                            }
                            try {
                                o = Integer.parseInt(dataObject.getOrientation());
                            } catch (Exception e) {
                                o = -1;
                            }
                            try {
                                s = (int) Float.parseFloat(dataObject.getSpeed());
                            } catch (Exception e) {
                                s = -1;
                            }
                            if (!((p > 0 && p < 1) || (o > 0 && o < 4) || (s > 0))) {
                                clearPendingDialogFlagAndDB(context, msgId);
                                // clear if not in app slide

                            } else {
                                SharedPreferences sharedPreferences = context.getSharedPreferences(Util.SHSHARED_PREF_PERM, Context.MODE_PRIVATE);
                                if (null == sharedPreferences.getString(Constants.PENDING_DIALOG, null)) {
                                    clearPendingDialogFlagAndDB(context, msgId);
                                } else {
                                    // this is to prevent sending of two push result in 8000 in app with confirmation

                                    return;
                                }
                            }
                            break;
                        case NotificationBase.CODE_SIMPLE_PROMPT:
                            if (fromBG)
                                return;
                        default:
                            clearPendingDialogFlagAndDB(context, msgId);
                    }
                }
            }
            if (intent.getAction().equals(Constants.BROADCAST_STREETHAWK_DECLINED)) {
                displayBadge(context, DISMISS_BADGE);
                clearPendingDialogFlagAndDB(context, msgId);
                result = Constants.STREETHAWK_DECLINED;
                sendResult = true;

            }
            if (intent.getAction().equals(Constants.BROADCAST_STREETHAWK_POSTPONED)) {
                displayBadge(context, DISMISS_BADGE);
                result = Constants.STREETHAWK_POSTPONED;
                clearPendingDialogFlagAndDB(context, msgId);
                sendResult = true;
            }
            // schedule sending of queued broadcast only if we have sent result of previous one.
            if (sendResult) {
                if (null == appGcmReceiverList) {
                    Log.w(Util.TAG, "No object registered for class implementing ISHObserver. Use registerSHObserver");
                } else {
                    PushDataForApplication pushDataForApplication = new PushDataForApplication();
                    pushDataForApplication.convertPushDataToPushDataForApp(dataObject, pushDataForApplication);
                    appGcmReceiverList.onReceiveResult(pushDataForApplication,result);
                }
                sendResultLog(context, msgId, result, code);
            }
            dataObject = null;
        }
    } //End of onReceive


    private Boolean isIncorrectPackage(Context context, String receivedPackageName) {
        if (null == receivedPackageName)
            return true;
        if (!(receivedPackageName.equals(context.getPackageName()))) {
            return true;
        }
        return false;

    }

    /**
     * Clear pending dialog clears db and pending dialog flag
     *
     * @param context
     * @param msgId
     */
    public void clearPendingDialogFlagAndDB(Context context, String msgId) {
        PushNotificationDB dbObject = PushNotificationDB.getInstance(context);
        dbObject.open();
        dbObject.deleteEntry(msgId);
        dbObject.close();
        SharedPreferences sharedPreferences = context.getSharedPreferences(Util.SHSHARED_PREF_PERM, Context.MODE_PRIVATE);
        SharedPreferences.Editor e = sharedPreferences.edit();
        e.putString(Constants.PENDING_DIALOG, null);
        e.commit();
    }

    private void colapseNotification(Context context) {
        Intent colapseNotificationIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        context.sendBroadcast(colapseNotificationIntent);
    }

    private void bgActionPositive(Context context, String receivedPackageName, PushNotificationData pushObject) {
        int code = 0;
        try {
            code = Integer.parseInt(pushObject.getCode());
        } catch (Exception e) {
            e.printStackTrace();
            code = 0;
        }
        String data = pushObject.getData();
        String msgId = pushObject.getMsgId();
        if (isIncorrectPackage(context, receivedPackageName)) {
            return;
        }
        switch (code) {
            case NotificationBase.CODE_LAUNCH_ACTIVITY:
            case NotificationBase.CODE_USER_REGISTRATION_SCREEN:
            case NotificationBase.CODE_USER_LOGIN_SCREEN:
                clearPendingDialogFlagAndDB(context, msgId);
                if (data.isEmpty()) {
                    if (code == NotificationBase.CODE_USER_REGISTRATION_SCREEN)
                        data = Constants.REGISTER_FRIENDLY_NAME;
                    if (code == NotificationBase.CODE_USER_LOGIN_SCREEN)
                        data = Constants.LOGIN_FRIENDLY_NAME;
                }
                if (Util.getPlatformType() == Constants.PLATFORM_PHONEGAP ||
                        Util.getPlatformType() == Constants.PLATFORM_TITANIUM ||
                        Util.getPlatformType() == Constants.PLATFORM_UNITY) {
                    launchActivityPG(context, data);
                } else {
                    launchActivity(context, data);
                }
                break;
            case NotificationBase.CODE_RATE_APP:
            case NotificationBase.CODE_UPDATE_APP:
                clearPendingDialogFlagAndDB(context, msgId);
                handleRateUpdate(context);
                break;
            case NotificationBase.CODE_CALL_TELEPHONE_NUMBER:
                clearPendingDialogFlagAndDB(context, msgId);
                handleCall(context, data);
                break;
            case NotificationBase.CODE_IBEACON:
                clearPendingDialogFlagAndDB(context, msgId);
                final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (bluetoothAdapter != null) {
                    boolean isEnabled = bluetoothAdapter.isEnabled();
                    if (!isEnabled) {
                        bluetoothAdapter.enable();
                        Toast.makeText(context, NotificationBase.getStringtoDisplay(context, NotificationBase.TYPE_BT_ENABLE_TOAST), Toast.LENGTH_LONG).show();
                    }

                }
                break;
            case NotificationBase.CODE_OPEN_URL:
                startApp(context);
                break;
            case NotificationBase.CODE_ENABLE_LOCATION:
                clearPendingDialogFlagAndDB(context, msgId);
                Intent locintent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                locintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(locintent);
                break;
            case NotificationBase.CODE_FEEDBACK:
                String fbdata = data;
                if (null == fbdata) {
                    if (fbdata.isEmpty()) {
                        handleFeedbackBg(context, null, msgId);
                    }
                } else {
                    try {
                        String FEEDBACK_LIST_CONTENT = "c";
                        JSONObject json = new JSONObject(fbdata);
                        JSONArray array = null;
                        array = json.getJSONArray(FEEDBACK_LIST_CONTENT);
                        // Check if list is empty. if so then launch feedbackactivity
                        if (null == array) {
                            if (array.length() == 0) {
                                handleFeedbackBg(context, null, pushObject.getMsgId());
                            }
                        } else {
                            // No exception, start the app
                            startApp(context);
                        }
                    } catch (JSONException e) {
                        handleFeedbackBg(context, null, pushObject.getMsgId());
                    }
                }
                break;
            case NotificationBase.CODE_SIMPLE_PROMPT:
                startApp(context);
                break;
            default:
                clearPendingDialogFlagAndDB(context, msgId);
                startApp(context);
                break;
        }

    }

    private String getRunningPackage(Context mContext) {
        String packageName = mContext.getPackageName();
        ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(10);
        for (ActivityManager.RunningTaskInfo str : taskInfo) {
            if (str.topActivity.toString().contains(packageName))
                return str.topActivity.getClassName();
        }
        return null;
    }

    // Implemented deeplink here
    private void launchActivity(Context mContext, String friendlyName) {
        if (null == friendlyName)
            friendlyName = mContext.getApplicationContext().getPackageName();
        final SharedPreferences activityPrefs = mContext.getSharedPreferences(Constants.SHSHARED_PREF_FRNDLST, Context.MODE_PRIVATE);
        String tempActivityName = activityPrefs.getString(friendlyName, null);
        if (null == tempActivityName) {
            if (friendlyName.contains("://")) {
                try {
                    Intent deepLinkIntent = new Intent();
                    deepLinkIntent.setAction("android.intent.action.VIEW");
                    deepLinkIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    deepLinkIntent.setData(Uri.parse(friendlyName));
                    mContext.startActivity(deepLinkIntent);
                } catch (ActivityNotFoundException e) {
                    Log.e(Util.TAG,SUBTAG+ "Incorrect link" + friendlyName);
                    tempActivityName = " ";
                }
            }
            // Either we have received activityName or ""
            tempActivityName = friendlyName;
        }
        final String activityName = tempActivityName;
        final PackageManager pm = mContext.getPackageManager();
        Intent LauncherIntent = null;
        if (activityName.isEmpty()) {
            // application needs to launched
            //1. check if application is running in BG
            String packageName = getRunningPackage(mContext);
            if (null == packageName) {
                LauncherIntent = pm.getLaunchIntentForPackage(mContext.getPackageName());
            } else {
                try {
                    final Class<?> classname = Class.forName(packageName);
                    LauncherIntent = new Intent(mContext.getApplicationContext(), classname);
                } catch (ClassNotFoundException e1) {
                    LauncherIntent = pm.getLaunchIntentForPackage(mContext.getApplicationContext().getPackageName());
                    e1.printStackTrace();
                }
            }
        } else {
            //Here we have a qualified name for we will try to launch it
            try {
                final Class<?> classname = Class.forName(activityName);
                LauncherIntent = new Intent(mContext.getApplicationContext(), classname);
            } catch (ClassNotFoundException e) {
                String packageName = getRunningPackage(mContext);
                if (null == packageName) {
                    //SendErrorLog(mContext.getApplicationContext(), extras, StreethawkText.STREETHAWK_ERROR_INVALID_ACTIVITY + activityName);
                    LauncherIntent = pm.getLaunchIntentForPackage(mContext.getApplicationContext().getPackageName());
                } else {
                    try {
                        final Class<?> classname = Class.forName(packageName);
                        LauncherIntent = new Intent(mContext.getApplicationContext(), classname);
                    } catch (ClassNotFoundException e1) {
                        //SendErrorLog(mContext.getApplicationContext(), extras, StreethawkText.STREETHAWK_ERROR_INVALID_ACTIVITY + activityName);
                        LauncherIntent = pm.getLaunchIntentForPackage(mContext.getApplicationContext().getPackageName());
                    }
                }
            }
        }
        LauncherIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        LauncherIntent.putExtra(Constants.SHOW_PENDING_DIALOG, true);
        mContext.startActivity(LauncherIntent);
    }


    private void handleRateUpdate(Context mContext) {
        try {
            Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + mContext.getPackageName()));
            marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(marketIntent);
        } catch (android.content.ActivityNotFoundException anfe) {
            Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + mContext.getPackageName()));
            marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(marketIntent);
        }
    }

    private void startApp(Context mContext) {
        Intent intent = null;
        String packageName = getRunningPackage(mContext);
        final PackageManager pm = mContext.getPackageManager();
        if (null == packageName) {
            intent = pm.getLaunchIntentForPackage(mContext.getApplicationContext().getPackageName());
        } else {
            try {
                final Class<?> classname = Class.forName(packageName);
                intent = new Intent(mContext.getApplicationContext(), classname);
            } catch (ClassNotFoundException e1) {
                intent = pm.getLaunchIntentForPackage(mContext.getApplicationContext().getPackageName());
            }
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(Constants.SHOW_PENDING_DIALOG, true);
        mContext.startActivity(intent);
    }

    private void launchActivityPG(Context context, String data) {
        final SharedPreferences activityPrefs = context.getSharedPreferences(Constants.SHSHARED_PREF_FRNDLST, Context.MODE_PRIVATE);
        String tempactivityName = activityPrefs.getString(data, null);
        if (null == tempactivityName) {
            // Either we have received activityName or ""
            tempactivityName = data;
        }
        SharedPreferences.Editor e = activityPrefs.edit();
        e.putString(Constants.PHONEGAP_URL, tempactivityName);
        e.commit();
        startApp(context);
    }

    private void handleCall(Context mContext, String PhoneNumber) {
        Intent callIntentDirect = new Intent(Intent.ACTION_CALL);
        callIntentDirect.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        callIntentDirect.setData(Uri.parse("tel:" + PhoneNumber));
        mContext.startActivity(callIntentDirect);
    }

    private void handleFeedbackBg(Context context, String data, String msgId) {
        PushNotificationDB dbObject = PushNotificationDB.getInstance(context);
        dbObject.open();
        dbObject.forceStoreNoDialog(msgId);
        dbObject.close();
        Intent intent = new Intent(context, SHFeedbackActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Bundle extras = new Bundle();
        extras.putString("SHFeedbackActyTitle", data);
        extras.putString("StreethawkText.MSGID", msgId);
        intent.putExtras(extras);
        context.startActivity(intent);
    }

    private void sendAcknowledgement(Context context, String msgId) {
        try {
            Bundle params = new Bundle();
            params.putString(Util.SHMESSAGE_ID, msgId);
            params.putString(Util.CODE, Integer.toString(NotificationBase.CODE_PUSH_ACK));
            Logging manager = Logging.getLoggingInstance(context);
            manager.addLogsForSending(params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleCustomJsonFromServer(String title, String msg, String json) {
        if (null == appGcmReceiverList) {
            appGcmReceiverList.shReceivedRawJSON(title, msg, json);
        }
    }

    private void sendResultLog(Context context, String msgId, int result, int code) {
        try {
            Bundle params = new Bundle();
            params.putString(Util.SHMESSAGE_ID, msgId);
            params.putString(Util.TYPE_NUMERIC, Integer.toString(code));
            params.putString(NotificationBase.SHRESULT, Integer.toString(result));
            params.putString(Util.CODE, Integer.toString(NotificationBase.CODE_PUSH_RESULT));
            Logging manager = Logging.getLoggingInstance(context);
            manager.addLogsForSending(params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Function to check is app is backgrounded
     *
     * @return
     */

    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    private boolean checkIfBG(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Util.SHSHARED_PREF_PERM, Context.MODE_PRIVATE);
        if (sharedPreferences.getBoolean(Constants.SHFORCEPUSHTOBG, false))
            return true;
        ActivityManager activitymanager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> services = activitymanager.getRunningTasks(Integer.MAX_VALUE);
        if (!(services.get(0).topActivity.getPackageName().toString().equalsIgnoreCase(context.getPackageName().toString()))) {
            return true;
        } else {
            if (null == context)
                return true;
            // app on top check if screen is live
            PowerManager powerManager = (PowerManager) context.getSystemService(context.POWER_SERVICE);
            // if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            if (powerManager.isScreenOn())
                return false;
            else
                return true;
        }
    }

}

