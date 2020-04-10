package com.njwyt.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

import java.io.Serializable;

/**
 * Created by jason_samuel on 2017/8/20.
 */

@Entity
public class MessageHistory implements Serializable {
    @Id(autoincrement = true)
    private Long uuid;
    private Long senderId;       // 发送者id
    private Long targetId;       // 目标用户id
    private String datatime;      // 留言时间
    private String videoUrl;    // 视频地址
    private String firstFrameUrl;   // 视频第一帧图片地址
    private boolean isRead;     // 是否已读
    private int duration;       // 视频时长（单位：秒）
    public int getDuration() {
        return this.duration;
    }
    public void setDuration(int duration) {
        this.duration = duration;
    }
    public boolean getIsRead() {
        return this.isRead;
    }
    public void setIsRead(boolean isRead) {
        this.isRead = isRead;
    }
    public String getFirstFrameUrl() {
        return this.firstFrameUrl;
    }
    public void setFirstFrameUrl(String firstFrameUrl) {
        this.firstFrameUrl = firstFrameUrl;
    }
    public String getVideoUrl() {
        return this.videoUrl;
    }
    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }
    public String getDatatime() {
        return this.datatime;
    }
    public void setDatatime(String datatime) {
        this.datatime = datatime;
    }
    public Long getTargetId() {
        return this.targetId;
    }
    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }
    public Long getSenderId() {
        return this.senderId;
    }
    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }
    public Long getUuid() {
        return this.uuid;
    }
    public void setUuid(Long uuid) {
        this.uuid = uuid;
    }
    @Generated(hash = 151461691)
    public MessageHistory(Long uuid, Long senderId, Long targetId, String datatime,
            String videoUrl, String firstFrameUrl, boolean isRead, int duration) {
        this.uuid = uuid;
        this.senderId = senderId;
        this.targetId = targetId;
        this.datatime = datatime;
        this.videoUrl = videoUrl;
        this.firstFrameUrl = firstFrameUrl;
        this.isRead = isRead;
        this.duration = duration;
    }
    @Generated(hash = 1306405643)
    public MessageHistory() {
    }

}
