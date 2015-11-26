/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *  Neither the name of copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package no.nordicsemi.android.nrfblejoiner.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.util.ArrayList;

import no.nordicsemi.android.nrfblejoiner.database.DatabaseContract.*;
import no.nordicsemi.android.nrfblejoiner.wifi.WifiNetwork;

public class DatabaseHelper {

    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_WIFI_ENTRIES = "CREATE TABLE " + WifiNetworksEntry.TABLE_NAME + " (" + WifiNetworksEntry._ID + " INTEGER PRIMARY KEY," + WifiNetworksEntry.COLUMN_NAME_SSID + TEXT_TYPE + COMMA_SEP + WifiNetworksEntry.COLUMN_NAME_PASSWORD + TEXT_TYPE + COMMA_SEP + WifiNetworksEntry.COLUMN_NAME_DEFAULT +TEXT_TYPE + ")";
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "WifiNetworks.db";

    private static final String [] WIFI_INFO = new String[] {WifiNetworksEntry._ID, WifiNetworksEntry.COLUMN_NAME_SSID, WifiNetworksEntry.COLUMN_NAME_PASSWORD, WifiNetworksEntry.COLUMN_NAME_DEFAULT};
    private static final String [] DEFAULT_WIFI_INFO = new String[] {WifiNetworksEntry._ID, WifiNetworksEntry.COLUMN_NAME_SSID, WifiNetworksEntry.COLUMN_NAME_PASSWORD, WifiNetworksEntry.COLUMN_NAME_DEFAULT};
    private static final String [] SSID_INFO = new String[] {WifiNetworksEntry._ID, WifiNetworksEntry.COLUMN_NAME_SSID};


    private static SqliteHelper mSqliteHelper;
    private static SQLiteDatabase sqLiteDatabase;


    public DatabaseHelper(final Context context) {
        if(mSqliteHelper == null) {
            mSqliteHelper = new SqliteHelper(context);
            sqLiteDatabase = mSqliteHelper.getWritableDatabase();
        }
    }

    public class SqliteHelper extends SQLiteOpenHelper{

        public SqliteHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_WIFI_ENTRIES);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }

        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }

    public long insertWifiNetwork(final String ssid, final String password, final String defaultRouter){
        final ContentValues content = new ContentValues();
        content.put(WifiNetworksEntry.COLUMN_NAME_SSID, ssid);
        content.put(WifiNetworksEntry.COLUMN_NAME_PASSWORD, password);
        content.put(WifiNetworksEntry.COLUMN_NAME_DEFAULT, defaultRouter);

        return sqLiteDatabase.insert(WifiNetworksEntry.TABLE_NAME, null, content);
    }

    public long updateWifiNetwork(final int rowId, final String ssid, final String password, final String defaultRouter){
        final ContentValues content = new ContentValues();
        content.put(WifiNetworksEntry.COLUMN_NAME_SSID, ssid);
        content.put(WifiNetworksEntry.COLUMN_NAME_PASSWORD, password);
        content.put(WifiNetworksEntry.COLUMN_NAME_DEFAULT, defaultRouter);
        return sqLiteDatabase.update(WifiNetworksEntry.TABLE_NAME, content, WifiNetworksEntry._ID + "=?", new String[]{String.valueOf(rowId)});
    }

    public long changeDefaultWifiNetworkToOther(){
        final ContentValues content = new ContentValues();
        content.put(WifiNetworksEntry.COLUMN_NAME_DEFAULT, "false");
        return sqLiteDatabase.update(WifiNetworksEntry.TABLE_NAME, content, WifiNetworksEntry.COLUMN_NAME_DEFAULT + "=?", new String[]{"true"});
    }

    public long setDefaultWifiNetwork(int rowId, boolean flag){
        final ContentValues content = new ContentValues();
        content.put(WifiNetworksEntry.COLUMN_NAME_DEFAULT, String.valueOf(flag));
        return sqLiteDatabase.update(WifiNetworksEntry.TABLE_NAME, content, WifiNetworksEntry._ID + "=?", new String[]{String.valueOf(rowId)});
    }

    public long deleteWifiNetwork(int rowId){
        return sqLiteDatabase.delete(WifiNetworksEntry.TABLE_NAME, WifiNetworksEntry._ID + "=?", new String[]{String.valueOf(rowId)});
    }

    public WifiNetwork getDefaultWifiNetwork(){
        Cursor cursor = sqLiteDatabase.query(WifiNetworksEntry.TABLE_NAME, DatabaseHelper.DEFAULT_WIFI_INFO, WifiNetworksEntry.COLUMN_NAME_DEFAULT + "=?", new String[]{"true"}, null, null, null, null);
        WifiNetwork wifiNetwork = null;
        int id;
        String ssid;
        String password;
        String defaultRouter;

        while(cursor.moveToNext()){
            id = cursor.getInt(0);
            ssid = cursor.getString(1);
            password = cursor.getString(2);
            defaultRouter = cursor.getString(3);
            if(defaultRouter.equalsIgnoreCase("true")) {
                wifiNetwork = new WifiNetwork(id, ssid, password, defaultRouter);
                break;
            }
        }
        cursor.close();
        return  wifiNetwork;
    }

    public ArrayList<WifiNetwork> getAllWifiNetworks(){
        ArrayList<WifiNetwork> wifi_networks = new ArrayList<>();
        Cursor cursor = sqLiteDatabase.query(WifiNetworksEntry.TABLE_NAME, DatabaseHelper.WIFI_INFO, null, null, null, null, null);

        WifiNetwork wifiNetwork;
        int id;
        String ssid;
        String password;
        String defaultRouter;

        while(cursor.moveToNext()){
            id = cursor.getInt(0);
            ssid = cursor.getString(1);
            password = cursor.getString(2);
            defaultRouter = cursor.getString(3);

            wifiNetwork = new WifiNetwork(id, ssid, password, defaultRouter);
            wifi_networks.add(wifiNetwork);

        }
        cursor.close();
        return  wifi_networks;
    }

    public Cursor getAllSsids(){

        return sqLiteDatabase.query(WifiNetworksEntry.TABLE_NAME, DatabaseHelper.SSID_INFO, null, null, null, null, null);
    }
}
