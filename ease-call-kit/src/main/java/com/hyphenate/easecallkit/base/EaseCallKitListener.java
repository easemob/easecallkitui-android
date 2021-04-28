package com.hyphenate.easecallkit.base;

import android.content.Context;

import org.json.JSONObject;

import com.hyphenate.easecallkit.EaseCallKit;

/**
 * author lijian
 * email: Allenlee@easemob.com
 * date: 01/14/2021
 */
public interface EaseCallKitListener{
    /**
     * 邀请好友进行多人通话
     * @param context
     * @param users   当前通话中已经存在的成员
     * @param ext     自定义扩展字段
     */
    void onInviteUsers(Context context,String []users,JSONObject ext);


    /**
     * 通话结束
     * @param callType    通话类型
     * @param reason     通话结束原因
     * @param callTime  通话时长
     */
    void onEndCallWithReason(EaseCallType callType, String channelName, EaseCallEndReason reason, long callTime);


    /**
     * 收到通话邀请回调
     * @param callType  通话类型
     * @param userId  邀请方userId
     * @param ext     自定义扩展字段
     */
    void onReceivedCall(EaseCallType callType, String userId, JSONObject ext);


    /**
     * 用户生成Token回调
     * @param userId       用户自己Id(环信Id)
     * @param channelName  频道名称
     * @param agoraAppId   声网appId
     * @param callback     生成的Token回调(成功为Token，失败为 errorCode和errorMsg)
     */
    default void onGenerateToken(String userId,String channelName,String agoraAppId,EaseCallKitTokenCallback callback){};


    /**
     * 通话错误回调
     * @param type            错误类型
     * @param errorCode      错误码
     * @param description   错误描述
     */
    void onCallError(EaseCallKit.EaseCallError type, int errorCode, String description);


    /**
     * 通话邀请消息回调
     *
     */
    void onInViteCallMessageSent();


    /**
     *远端用户加入频道回调
     */
    void onRemoteUserJoinChannel(String channelName, String userName, int uid, EaseGetUserAccountCallback callback);
}