package com.hyphenate.easecallkit.base;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * author lijian
 * email: Allenlee@easemob.com
 * date: 01/28/2021
 *
 * 有关callkit 用户配置选项
 * defaultHeadImage  用户默认头像(为本地文件绝对路径 或者url)
 * userInfoMap      有关用户信息(key为环信Id , value为EaseCallUserInfo);
 * callTimeOut      呼叫超时时间(单位ms 默认30s)
 * audioFile       震铃文件(本地文件绝对路径)
 * enableRTCToken  是否需要RTC验证(需要声网后台去控制 默认为关闭)
 */
public class EaseCallKitConfig {
    private String defaultHeadImage;
    private Map<String,EaseCallUserInfo> userInfoMap = new HashMap<>();
    private String RingFile;
    private String agoraAppId;
    private long callTimeOut = 30 * 1000;
    private boolean enableRTCToken = false;

    public EaseCallKitConfig(){

    }

    public String getDefaultHeadImage() {
        return defaultHeadImage;
    }

    public void setDefaultHeadImage(String defaultHeadImage) {
        this.defaultHeadImage = defaultHeadImage;
    }

    public Map<String, EaseCallUserInfo> getUserInfoMap() {
        return userInfoMap;
    }

    public void setUserInfoMap(Map<String, EaseCallUserInfo> userMap) {
        userInfoMap.clear();
        if(userMap != null && userMap.size() > 0){
            Set<String> userSet = userMap.keySet();
            for(String userId:userSet){
                EaseCallUserInfo userInfo = userMap.get(userId);
                if(userInfo != null){
                    EaseCallUserInfo newUserInfo = new EaseCallUserInfo(userInfo.getNickName(),userInfo.getHeadImage());
                    userInfoMap.put(userId,newUserInfo);
                }else{
                    userInfoMap.put(userId,null);
                }
            }
        }
    }

    public String getRingFile() {
        return RingFile;
    }

    public void setRingFile(String autoFile) {
        this.RingFile = autoFile;
    }

    public long getCallTimeOut() {
        return callTimeOut;
    }

    public void setCallTimeOut(long callTimeOut) {
        this.callTimeOut = callTimeOut;
    }

    public String getAgoraAppId() { return agoraAppId; }

    public void setAgoraAppId(String agoraAppId) { this.agoraAppId = agoraAppId; }

    public boolean isEnableRTCToken() {
        return enableRTCToken;
    }

    public void setEnableRTCToken(boolean enableRTCToken) {
        this.enableRTCToken = enableRTCToken;
    }

    public void setUserInfo(String userName, EaseCallUserInfo userInfo){
        if(userInfoMap == null){
            userInfoMap = new HashMap<>();
        }
        userInfoMap.put(userName,userInfo);
    }
}
