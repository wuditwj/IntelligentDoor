package com.njwyt.db;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.njwyt.entity.User;
import com.njwyt.entity.greendao.UserDao;

import java.util.List;

/**
 * Created by Administrator on 2017/9/13.
 */

public class UserDaoHelp {

    /**
     * 增
     * @param context
     * @param user
     */
    public Long insertUser(Context context, User user) {
       UserDao userDao = DBHelp.getInstance(context).getDaoSession().getUserDao();
       return userDao.insert(user);

    }

    /**
     * 删
     * @param context
     * @param userId
     */
    public void deleteUser(Context context, Long userId){
        UserDao userDao = DBHelp.getInstance(context).getDaoSession().getUserDao();
        User findUser = userDao.queryBuilder().where(UserDao.Properties.Id.eq(userId)).build().unique();
        if(findUser != null){
            userDao.deleteByKey(findUser.getId());
        }
    }

    /**
     * 改
     * @param context
     * @param user
     */
    public void upDataUser(Context context,User user){
        UserDao userDao = DBHelp.getInstance(context).getDaoSession().getUserDao();
        User findUser = userDao.queryBuilder().where(UserDao.Properties.Id.eq(user.getId())).build().unique();
        if(findUser != null) {
            findUser.setHeadUrl(user.getHeadUrl());
            findUser.setLevel(user.getLevel());
            findUser.setPassword(user.getPassword());
            findUser.setLanguage(user.getLanguage());
            findUser.setFontSize(user.getFontSize());
            findUser.setTheme(user.getTheme());
            userDao.update(findUser);
        }
    }

    /**
     * 查(根据id)
     * @param context
     * @param userId
     * @return
     */
    public User selectUser(Context context,Long userId){
        UserDao userDao = DBHelp.getInstance(context).getDaoSession().getUserDao();
        User findUser = userDao.queryBuilder().where(UserDao.Properties.Id.eq(userId)).build().unique();
        return findUser;
    }

    /**
     * 查(根据密码)
     * @param context
     * @param userPassWord
     * @return
     */
    public User selectUser(Context context,String userPassWord){
        UserDao userDao = DBHelp.getInstance(context).getDaoSession().getUserDao();
        User findUser = userDao.queryBuilder().where(UserDao.Properties.Password.eq(userPassWord)).build().unique();
        return findUser;
    }

    /**
     * 查全部
     * @param context
     * @return
     */
    public List<User> selectAllUser(Context context) {
        UserDao userDao = DBHelp.getInstance(context).getDaoSession().getUserDao();
        List<User> userList=userDao.loadAll();
        return userList;
    }

}
