package com.njwyt.db;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.njwyt.entity.MessageHistory;
import com.njwyt.entity.greendao.MessageHistoryDao;
import com.njwyt.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017/9/13.
 */

public class MessageHistoryDaoHelp {

    private MessageHistoryDao messageHistoryDao;

    public MessageHistoryDaoHelp(Context context) {
        messageHistoryDao = DBHelp.getInstance(context).getDaoSession().getMessageHistoryDao();
    }

    /**
     * 添加一条历史记录
     */
    public void insertHistory(MessageHistory messageHistory) {
        messageHistoryDao.insert(messageHistory);
    }

    /**
     * 根据历史记录的uuid删除条历史记录，与视频记录
     */
    public void deleteHistoryById(Long uuid) {
        MessageHistory findHistory = messageHistoryDao.queryBuilder().where(MessageHistoryDao.Properties.Uuid.eq(uuid)).build().unique();
        if (findHistory != null) {
            if (findHistory.getVideoUrl() != null) {
                FileUtils.deleteFile(findHistory.getVideoUrl());
            }
            if (findHistory.getFirstFrameUrl() != null) {
                FileUtils.deleteFile(findHistory.getFirstFrameUrl());
            }
            messageHistoryDao.deleteByKey(findHistory.getUuid());
        }
    }

    /**
     * 根据用户id删除所有关于该用户的数据
     *
     * @param userId
     */
    public void deleteHistoryByUserId(Long userId) {
        List<MessageHistory> messageHistoryList =
                messageHistoryDao.queryBuilder()
                        .whereOr(MessageHistoryDao.Properties.SenderId.eq(userId),
                                MessageHistoryDao.Properties.TargetId.eq(userId))
                        .list();
        // 逐个删除视频
        for (MessageHistory m : messageHistoryList) {
            deleteHistoryById(m.getUuid());
        }
    }

    /**
     * 修改指定历史数据
     */
    public void upDataMessageHistory(MessageHistory messageHistory) {
        messageHistoryDao.update(messageHistory);
    }

    /**
     * 根据uuid获取一条历史记录
     */
    public MessageHistory getMessageHistoryById(Long uuid) {
        MessageHistory findHistory = messageHistoryDao.queryBuilder().where(MessageHistoryDao.Properties.Uuid.eq(uuid)).build().unique();
        return findHistory;
    }

    /**
     * 获取所有的历史记录
     */
    public List<MessageHistory> getMessageHistoryList() {
        return messageHistoryDao.loadAll();
    }

    /**
     * 根据发送者id和接收者id获取历史记录
     *
     * @param senderUserId 发送者id
     * @param targetUserId 接收者id
     * @return
     */
    public List<MessageHistory> getMessageHistoryListByTargetId(Long senderUserId, Long targetUserId, String date) {
        return messageHistoryDao.queryBuilder()
                .where(MessageHistoryDao.Properties.TargetId.eq(targetUserId),
                        MessageHistoryDao.Properties.SenderId.eq(senderUserId),
                        MessageHistoryDao.Properties.Datatime.like(date + "%"))
                .orderDesc(MessageHistoryDao.Properties.Datatime)
                .list();
    }

    /**
     * 根据用户id获取所有关于该用户所有的收发记录
     *
     * @param userId 用户id
     * @return
     */
    public List<MessageHistory> getMessageHistoryListByUserId(Long userId, String date) {
        return messageHistoryDao.queryBuilder()
                .whereOr(MessageHistoryDao.Properties.TargetId.eq(userId), MessageHistoryDao.Properties.SenderId.eq(userId))
                .where(MessageHistoryDao.Properties.Datatime.like(date + "%"))
                .orderDesc(MessageHistoryDao.Properties.Datatime)
                .list();
    }

    /**
     * 根据发送者与接收者id，获得历史列表里的所有日期
     *
     * @param senderUserId 发送者id
     * @param targetUserId 接收者id
     */
    public List<String> getMessageHistoryDateList(Long senderUserId, Long targetUserId) {
        String sql = "select date(DATATIME) as DAY from MESSAGE_HISTORY where SENDER_ID = ? and TARGET_ID = ? group by DAY order by DAY desc";
        Cursor cursor = messageHistoryDao.getDatabase().rawQuery(sql, new String[]{senderUserId + "", targetUserId + ""});
        List<String> dateList = new ArrayList<>();
        while (cursor.moveToNext()) {
            String datatime = cursor.getString(0);
            dateList.add(datatime);
        }
        cursor.close();
        return dateList;
    }


    /**
     * 获得所有关于该id的历史列表里的所有日期
     *
     * @param userId 发送者id
     */
    public List<String> getMessageHistoryDateListByUserId(Long userId) {
        String sql = "select date(DATATIME) as DAY from MESSAGE_HISTORY where SENDER_ID = ? or TARGET_ID = ? group by DAY order by DAY desc";
        Cursor cursor = messageHistoryDao.getDatabase().rawQuery(sql, new String[]{userId + "", userId + ""});
        List<String> dateList = new ArrayList<>();
        while (cursor.moveToNext()) {
            String datatime = cursor.getString(0);
            dateList.add(datatime);
        }
        cursor.close();
        return dateList;
    }

    /**
     * 通过用户id查询该用户未读消息总数
     *
     * @param userId
     * @return
     */
    public long getUnreadMessageCountByUserId(Long userId) {
        return messageHistoryDao.queryBuilder()
                .where(MessageHistoryDao.Properties.TargetId.eq(userId), MessageHistoryDao.Properties.IsRead.eq(0))
                .count();
    }
}
