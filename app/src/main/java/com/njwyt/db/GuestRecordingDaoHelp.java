package com.njwyt.db;

import android.content.Context;

import com.njwyt.entity.GuestRecording;
import com.njwyt.entity.greendao.GuestRecordingDao;
import com.njwyt.utils.FileUtils;

import java.util.List;

/**
 * Created by jason_samuel on 2017/12/27.
 */

public class GuestRecordingDaoHelp {

    private GuestRecordingDao mGuestRecordingDao;

    public GuestRecordingDaoHelp(Context context) {
        mGuestRecordingDao = DBHelp.getInstance(context).getDaoSession().getGuestRecordingDao();
    }

    /**
     * 增
     *
     * @param guestRecording
     */
    public Long insertGuestRecording(GuestRecording guestRecording) {
        return mGuestRecordingDao.insert(guestRecording);

    }

    /**
     * 删
     *
     * @param uuid
     */
    public void deleteGuestRecording(Long uuid) {
        GuestRecording guestRecording = mGuestRecordingDao.queryBuilder().where(GuestRecordingDao.Properties.Uuid.eq(uuid)).build().unique();
        if (guestRecording != null) {

            // 删除留言图片
            if (guestRecording.getGuestPicUrl() != null) {
                FileUtils.deleteFile(guestRecording.getGuestPicUrl());
            }

            // 删除留言录音
            if (guestRecording.getRecordingUrl() != null) {
                FileUtils.deleteFile(guestRecording.getRecordingUrl());
            }

            mGuestRecordingDao.deleteByKey(guestRecording.getUuid());
        }
    }


    /**
     * 改
     *
     * @param guestRecording
     */
    public void updateGuestRecording(GuestRecording guestRecording) {
        mGuestRecordingDao.update(guestRecording);
    }

    /**
     * 查
     */
    public List<GuestRecording> getGuestRecordingList() {
        return mGuestRecordingDao.queryBuilder().orderDesc(GuestRecordingDao.Properties.Datatime).list();
    }
}
