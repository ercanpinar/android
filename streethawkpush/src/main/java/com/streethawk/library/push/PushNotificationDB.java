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

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.streethawk.library.core.SHSqliteBase;
import com.streethawk.library.core.Util;

class PushNotificationDB {
    // Start push notification helper
    class PushNotificationHelper extends SHSqliteBase {

        private static final String PUSH_NOTIFICATION_TABLE_NAME = "pushnotification";
        private static final String COLUMN_MSGID = "MsgID";
        private static final String COLUMN_CODE = "code";
        private static final String COLUMN_TITLE = "title";
        private static final String COLUMN_MSG = "msg";
        private static final String COLUMN_DATA = "data";
        private static final String COLUMN_P = "p";
        private static final String COLUMN_O = "o";
        private static final String COLUMN_S = "s";
        private static final String COLUMN_N = "n";
        private static final String COLUMN_SOUND = "sound";
        private static final String COLUMN_BADGE = "badge";

        public PushNotificationHelper(Context context) {
            super(context);
        }
        @Override
        public void onCreate(SQLiteDatabase database) {
           super.onCreate(database);
        }

        @Override
        public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
           super.onUpgrade(database,oldVersion,newVersion);
        }
    }// End class push notification helper

    private SQLiteDatabase mDatabase;
    private PushNotificationHelper mDbHelper;
    private Context mContext;
    private static PushNotificationDB instance = null;

    public static PushNotificationDB getInstance(Context context){
        if(null==instance){
            instance = new PushNotificationDB(context);
        }
        return instance;
    }

    private PushNotificationDB(Context context) {
        this.mContext = context;
        mDbHelper = new PushNotificationHelper(context);
    }

    public void open() throws SQLException {
        mDatabase = mDbHelper.getWritableDatabase();
    }

    public void close() {
        mDbHelper.close();
        if(mDatabase.isOpen())
            mDatabase.close();
    }

    public void storeGcmMessageDatabase(PushNotificationData object) throws IllegalStateException {
        ContentValues values = new ContentValues();
        values.put(PushNotificationHelper.COLUMN_MSGID, object.getMsgId());
        values.put(PushNotificationHelper.COLUMN_CODE, object.getCode());
        values.put(PushNotificationHelper.COLUMN_TITLE, object.getTitle());
        values.put(PushNotificationHelper.COLUMN_MSG, object.getMsg());
        values.put(PushNotificationHelper.COLUMN_DATA, object.getData());
        values.put(PushNotificationHelper.COLUMN_P, object.getPortion());
        values.put(PushNotificationHelper.COLUMN_O, object.getOrientation());
        values.put(PushNotificationHelper.COLUMN_S, object.getSpeed());
        values.put(PushNotificationHelper.COLUMN_N, object.getNoDialog());
        values.put(PushNotificationHelper.COLUMN_SOUND, object.getSound());
        values.put(PushNotificationHelper.COLUMN_BADGE, object.getBadge());
        // Checking agian as it crashed once due to sync issue.

        if(!mDatabase.isOpen()){
            mDatabase.isOpen();
        }
        try {
            mDatabase.insert(PushNotificationHelper.PUSH_NOTIFICATION_TABLE_NAME, null, values);
        }catch(IllegalStateException e){
            e.printStackTrace();
            return;
        }
        mDatabase.close();
    }

    public void forceDeleteAllRecords(){
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(Util.SHSHARED_PREF_PERM, Context.MODE_PRIVATE);
        SharedPreferences.Editor e = sharedPreferences.edit();
        e.putString(Constants.PENDING_DIALOG, null);
        e.commit();
        mDatabase.execSQL("delete from " + PushNotificationHelper.PUSH_NOTIFICATION_TABLE_NAME);
    }

    public void forceStoreNoDialog(String MsgID){
        ContentValues values=new ContentValues();
        values.put(PushNotificationHelper.COLUMN_N,"true");
        mDatabase.update(PushNotificationHelper.PUSH_NOTIFICATION_TABLE_NAME,values,PushNotificationHelper.COLUMN_MSGID+" = "+MsgID,null);
    }

    public void deleteEntry(String MsgID) {
        mDatabase.delete(PushNotificationHelper.PUSH_NOTIFICATION_TABLE_NAME, PushNotificationHelper.COLUMN_MSGID
                + " = " + MsgID, null);
    }

    public boolean getPushNotificationData(final String MsgId,final PushNotificationData obj) {
        PushNotificationHelper helper = new PushNotificationHelper(mContext);
        SQLiteDatabase database = helper.getReadableDatabase();
        if (null == MsgId) {
            database.close();
            helper.close();
            return false;
        } else {
            String query = "select * from " + PushNotificationHelper.PUSH_NOTIFICATION_TABLE_NAME +
                    " where " + PushNotificationHelper.COLUMN_MSGID + " = " + MsgId;
            Cursor cursor = database.rawQuery(query, null);
            if (cursor != null && cursor.moveToFirst()) {
                String Code = cursor.getString(cursor.getColumnIndex(PushNotificationHelper.COLUMN_CODE));
                String Title = cursor.getString(cursor.getColumnIndex(PushNotificationHelper.COLUMN_TITLE));
                String Msg = cursor.getString(cursor.getColumnIndex(PushNotificationHelper.COLUMN_MSG));
                String Data = cursor.getString(cursor.getColumnIndex(PushNotificationHelper.COLUMN_DATA));
                String Portion = cursor.getString(cursor.getColumnIndex(PushNotificationHelper.COLUMN_P));
                String Orientation = cursor.getString(cursor.getColumnIndex(PushNotificationHelper.COLUMN_O));
                String Speed = cursor.getString(cursor.getColumnIndex(PushNotificationHelper.COLUMN_S));
                String NoDialog = cursor.getString(cursor.getColumnIndex(PushNotificationHelper.COLUMN_N));
                String Sound = cursor.getString(cursor.getColumnIndex(PushNotificationHelper.COLUMN_SOUND));
                String Badge = cursor.getString(cursor.getColumnIndex(PushNotificationHelper.COLUMN_BADGE));

                cursor.close();
                database.close();
                helper.close();
                obj.setMsgId(MsgId);
                obj.setCode(Code);
                obj.setTitle(Title);
                obj.setMsg(Msg);
                obj.setData(Data);
                obj.setPortion(Portion);
                obj.setOrientation(Orientation);
                obj.setSpeed(Speed);
                obj.setNoDialog(NoDialog);
                obj.setBadge(Badge);
                obj.setSound(Sound);
            } else {
                Log.e(Util.TAG,"getPushNotificationData msgId " + MsgId + " Not found");
                cursor.close();
                database.close();
                helper.close();
                return false;
            }
        }
        return true;
    }

}
