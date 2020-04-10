package com.njwyt.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.njwyt.entity.greendao.DaoMaster;
import com.njwyt.entity.greendao.DaoSession;


/**
 * Created by Administrator on 2017/9/12.
 */

public class DBHelp {
    private static DBHelp mDBHelp;
    private final DaoSession daoSession;
    private final SQLiteDatabase database;
    private final DaoMaster.DevOpenHelper devOpenHelper;

    public DBHelp(Context context){
        //数据库操作
        devOpenHelper = new DaoMaster.DevOpenHelper(context, "intelligentdoor.db", null);
        database = devOpenHelper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(database);
        daoSession = daoMaster.newSession();
    }
    public static synchronized  DBHelp getInstance(Context context){
            if (mDBHelp == null) {
                mDBHelp=new DBHelp(context);
        }
        return mDBHelp;
    }


    public DaoSession getDaoSession() {
        return daoSession;
    }
    public SQLiteDatabase getDb() {
        return database;
    }




}
