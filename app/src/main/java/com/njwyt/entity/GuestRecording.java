package com.njwyt.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;

import java.io.Serializable;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Transient;

/**
 * Created by jason_samuel on 2017/12/27.
 * 门外访客留言
 */
@Entity
public class GuestRecording implements Serializable {

    @Id(autoincrement = true)
    private Long uuid;              // 录音id
    private String datatime;        // 来访时间
    private String recordingUrl;    // 录音地址
    private String guestPicUrl;     // 访客照片地址
    private int duration;           // 录音时长（单位：秒）
    private boolean isRead;         // 是否已读

    @Transient  // 不插入数据库
    private boolean isSelected;     // 是否别选中

    @Generated(hash = 100823835)
    public GuestRecording(Long uuid, String datatime, String recordingUrl,
            String guestPicUrl, int duration, boolean isRead) {
        this.uuid = uuid;
        this.datatime = datatime;
        this.recordingUrl = recordingUrl;
        this.guestPicUrl = guestPicUrl;
        this.duration = duration;
        this.isRead = isRead;
    }

    @Generated(hash = 1121314784)
    public GuestRecording() {
    }

    public Long getUuid() {
        return uuid;
    }

    public void setUuid(Long uuid) {
        this.uuid = uuid;
    }

    public String getDatatime() {
        return datatime;
    }

    public void setDatatime(String datatime) {
        this.datatime = datatime;
    }

    public String getRecordingUrl() {
        return recordingUrl;
    }

    public void setRecordingUrl(String recordingUrl) {
        this.recordingUrl = recordingUrl;
    }

    public String getGuestPicUrl() {
        return guestPicUrl;
    }

    public void setGuestPicUrl(String guestPicUrl) {
        this.guestPicUrl = guestPicUrl;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public boolean getIsRead() {
        return this.isRead;
    }

    public void setIsRead(boolean isRead) {
        this.isRead = isRead;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
