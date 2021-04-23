package com.hyphenate.easecallkit.base;

/**
 * 用户账号信息
 * uid      加入声网频道的uId
 * userName  uid对应的环信Id
 */
public class EaseUserAccount {
    private int uid;
    private String userName;

    public EaseUserAccount(){

    }

    public EaseUserAccount(int uid,String userName){
        this.uid = uid;
        this.userName = userName;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
