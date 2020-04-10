package com.njwyt.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

import java.io.Serializable;

/**
 * Created by jason_samuel on 2017/8/19.
 */

@Entity
public class User implements Serializable {
    @Id//(autoincrement = true)
    private Long id;            // 用户id
    private String headUrl;     // 头像地址
    private String password;    // 密码
    private String language;    // 语言(zh/en)
    private int theme;          // 主题
    private int fontSize;       // 字体大小
    private int level;          // 操作等级（难度）

    @Generated(hash = 1133379513)
    public User(Long id, String headUrl, String password, String language,
            int theme, int fontSize, int level) {
        this.id = id;
        this.headUrl = headUrl;
        this.password = password;
        this.language = language;
        this.theme = theme;
        this.fontSize = fontSize;
        this.level = level;
    }

    @Generated(hash = 586692638)
    public User() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHeadUrl() {
        return headUrl;
    }

    public void setHeadUrl(String headUrl) {
        this.headUrl = headUrl;
    }

    public int getLevel() {
        return this.level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getFontSize() {
        return this.fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    public int getTheme() {
        return this.theme;
    }

    public void setTheme(int theme) {
        this.theme = theme;
    }

    public String getLanguage() {
        return this.language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }


}
