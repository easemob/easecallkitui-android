package easemob.hyphenate.calluikit.base;

import android.content.Context;

import easemob.hyphenate.calluikit.EaseCallUIKit;

/**
 * author lijian
 * email: Allenlee@easemob.com
 * date: 01/14/2021
 */
public interface EaseCallKitListener{
    /**
     * 邀请好友进行多人通话
     * @param context
     * @param users    当前通话中已经存在的成员
     */
    void onInviteUsers(Context context,String []users);


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
     */
    void onRevivedCall(EaseCallType callType, String userId);


    /**
     * 通话错误回调
     * @param type            错误类型
     * @param errorCode      错误码
     * @param description   错误描述
     */
    void onCallError(EaseCallUIKit.EaseCallError type, int errorCode, String description);
}