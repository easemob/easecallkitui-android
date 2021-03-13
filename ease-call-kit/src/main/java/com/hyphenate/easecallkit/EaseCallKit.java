package com.hyphenate.easecallkit;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.hyphenate.EMCallBack;
import com.hyphenate.EMMessageListener;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMCmdMessageBody;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.exceptions.HyphenateException;
import com.hyphenate.util.EMLog;
import com.hyphenate.util.EasyUtils;

import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.hyphenate.easecallkit.base.EaseCallKitConfig;
import com.hyphenate.easecallkit.event.AlertEvent;
import com.hyphenate.easecallkit.event.AnswerEvent;
import com.hyphenate.easecallkit.event.BaseEvent;
import com.hyphenate.easecallkit.event.CallCancelEvent;
import com.hyphenate.easecallkit.event.ConfirmCallEvent;
import com.hyphenate.easecallkit.base.EaseCallInfo;
import com.hyphenate.easecallkit.event.InviteEvent;
import com.hyphenate.easecallkit.livedatas.EaseLiveDataBus;
import com.hyphenate.easecallkit.ui.EaseMultipleVideoActivity;
import com.hyphenate.easecallkit.ui.EaseVideoCallActivity;
import com.hyphenate.easecallkit.utils.EaseCallAction;
import com.hyphenate.easecallkit.base.EaseCallKitListener;
import com.hyphenate.easecallkit.base.EaseCallType;
import com.hyphenate.easecallkit.utils.EaseCallKitNotifier;
import com.hyphenate.easecallkit.utils.EaseCallState;
import com.hyphenate.easecallkit.utils.EaseMsgUtils;
import com.hyphenate.easecallkit.utils.EaseCallKitUtils;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.hyphenate.easecallkit.utils.EaseCallKitUtils.isAppRunningForeground;
import static com.hyphenate.easecallkit.utils.EaseMsgUtils.CALL_INVITE_EXT;


/**
 * author lijian
 * email: Allenlee@easemob.com
 * date: 01/11/2021
 */
public class EaseCallKit {
    private static final String TAG = EaseCallKit.class.getSimpleName();
    private static EaseCallKit instance = null;
    private boolean callKitInit = false;
    private Context appContext = null;
    private EMMessageListener messageListener = null;
    private EaseCallType callType = EaseCallType.SINGLE_VIDEO_CALL;
    private EaseCallState callState = EaseCallState.CALL_IDLE;
    private String channelName;
    private String fromUserId; //被叫获取主叫的
    public static String deviceId = "android_";
    public  String clallee_devId;
    private String callID = null;
    private JSONObject inviteExt = null;
    private EaseCallInfo callInfo = new EaseCallInfo();
    private TimeHandler timeHandler;
    private Map<String,EaseCallInfo> callInfoMap = new HashMap<>();
    private EaseCallKitListener callListener;
    private static boolean isComingCall = true;
    private ArrayList<String> inviteeUsers = new ArrayList<>();
    private EaseCallKitConfig  callKitConfig;
    private EaseMultipleVideoActivity multipleVideoActivity;
    private EaseCallKitNotifier notifier;

    private EaseCallKit() {}

    public static EaseCallKit getInstance() {
        if(instance == null) {
            synchronized (EaseCallKit.class) {
                if(instance == null) {
                    instance = new EaseCallKit();
                }
            }
        }
        return instance;
    }

    /**
     * init 初始化
     * @param context
     * @return
     */
    public synchronized boolean init(Context context, EaseCallKitConfig config) {
        if(callKitInit) {
            return true;
        }
        removeMessageListener();
        appContext = context;
        if (!isMainProcess(appContext)) {
            Log.e(TAG, "enter the service process!");
            return false;
        }

        //获取设备序列号
        deviceId += EaseCallKitUtils.getPhoneSign();
        timeHandler = new TimeHandler();

        //设置callkit配置项
        callKitConfig = new EaseCallKitConfig();
        callKitConfig.setUserInfoMap(config.getUserInfoMap());
        callKitConfig.setDefaultHeadImage(config.getDefaultHeadImage());
        callKitConfig.setCallTimeOut(config.getCallTimeOut());
        callKitConfig.setRingFile(config.getRingFile());
        callKitConfig.setEnableRTCToken(config.isEnableRTCToken());

        //init notifier
        initNotifier();

        //增加接收消息回调
        addMessageListener();
        callKitInit = true;
        return true;
    }


    /**
     * 获取当前callKitConfig
     *
     */
     public EaseCallKitConfig getCallKitConfig(){
         return  callKitConfig;
     }


    private void initNotifier(){
        notifier = new EaseCallKitNotifier(appContext);
    }


    /**
     * 通话错误类型
     *
    */
    public enum EaseCallError{
         PROCESS_ERROR, //业务逻辑异常
         RTC_ERROR, //音视频异常
         IM_ERROR  //IM异常
    }


    public enum CALL_PROCESS_ERROR {
        CALL_STATE_ERROR(0),
        CALL_TYPE_ERROR(1),
        CALL_PARAM_ERROR(2),
        CALL_RECEIVE_ERROR(3);

        public int code;
        CALL_PROCESS_ERROR(int code) {
            this.code = code;
        }
    }


    /**
     * 加入1v1通话
     * @param type 通话类型(只能为SINGLE_VOICE_CALL或SINGLE_VIDEO_CALL类型）
     * @param user 被叫用户ID(也就是环信ID)
     * @param ext  扩展字段(用户扩展字段)
     */
    public void startSingleCall(final EaseCallType type, final String user,final  Map<String, Object> ext){
        if(callState != EaseCallState.CALL_IDLE){
            if(callListener != null){
                callListener.onCallError(EaseCallError.PROCESS_ERROR,CALL_PROCESS_ERROR.CALL_STATE_ERROR.code,"current state is busy");
            }
            return;
        }
        if(type == EaseCallType.CONFERENCE_CALL){
            if(callListener != null){
                callListener.onCallError(EaseCallError.PROCESS_ERROR,CALL_PROCESS_ERROR.CALL_TYPE_ERROR.code,"call type is error");
            }
            return;
        }
        if(user != null && user.length() == 0){
            if(callListener != null){
                callListener.onCallError(EaseCallError.PROCESS_ERROR,CALL_PROCESS_ERROR.CALL_PARAM_ERROR.code,"user is null");
            }
            return;
        }
        callType = type;
        //改为主动呼叫状态
        callState = EaseCallState.CALL_OUTGOING;
        fromUserId = user;
        if(ext != null){
            inviteExt = EaseCallKitUtils.convertMapToJSONObject(ext);
        }

        //开始1V1通话
        EaseVideoCallActivity callActivity = new EaseVideoCallActivity();
        Intent intent = new Intent(appContext, callActivity.getClass()).addFlags(FLAG_ACTIVITY_NEW_TASK);
        Bundle bundle = new Bundle();
        isComingCall = false;
        bundle.putBoolean("isComingCall", false);
        bundle.putString("username", user);
        channelName = EaseCallKitUtils.getRandomString(10);
        bundle.putString("channelName", channelName);
        intent.putExtras(bundle);
        appContext.startActivity(intent);
    }


    /**
     * 邀请加入多人通话
     * @param users 用户ID列表(环信ID列表)
     * @param ext  扩展字段(用户扩展字段)
     */
    public void startInviteMultipleCall(final String[] users,final Map<String, Object> ext){
        if(callState != EaseCallState.CALL_IDLE && callType != EaseCallType.CONFERENCE_CALL){
            if(callListener != null){
                callListener.onCallError(EaseCallError.PROCESS_ERROR,CALL_PROCESS_ERROR.CALL_STATE_ERROR.code,"current state is busy");
            }
            return;
        }
        if(users == null || users.length  == 0) {
            if(!isDestroy(multipleVideoActivity)){
                inviteeUsers.clear();
                Intent intent = new Intent(appContext, multipleVideoActivity.getClass())
                        .addFlags(FLAG_ACTIVITY_NEW_TASK);
                appContext.startActivity(intent);
            }else{
                if(callListener != null){
                    callListener.onCallError(EaseCallError.PROCESS_ERROR,CALL_PROCESS_ERROR.CALL_PARAM_ERROR.code,"users is null");
                }
            }
        }else{
            callType = EaseCallType.CONFERENCE_CALL;
            inviteeUsers.clear();
            for(String user:users){
                inviteeUsers.add(user);
            }
            //还没有加入会议 创建会议
            if(isDestroy(multipleVideoActivity)){
                if(users != null && users.length > 0){
                    //改为主动呼叫状态
                    if(ext != null){
                        inviteExt = EaseCallKitUtils.convertMapToJSONObject(ext);
                    }
                    callState = EaseCallState.CALL_OUTGOING;
                    multipleVideoActivity = new EaseMultipleVideoActivity();
                    Intent intent = new Intent(appContext, multipleVideoActivity.getClass()).addFlags(FLAG_ACTIVITY_NEW_TASK);
                    Bundle bundle = new Bundle();
                    isComingCall = false;
                    bundle.putBoolean("isComingCall", false);
                    channelName = EaseCallKitUtils.getRandomString(10);
                    bundle.putString("channelName", channelName);
                    intent.putExtras(bundle);
                    appContext.startActivity(intent);
                }
            }else{
                //邀请成员加入
                Intent intent = new Intent(appContext, multipleVideoActivity.getClass())
                        .addFlags(FLAG_ACTIVITY_NEW_TASK);
                appContext.startActivity(intent);
            }
        }
    }


    /**
     * 增加消息监听
     */
    private void addMessageListener() {
        this.messageListener = new EMMessageListener() {
            @Override
            public void onMessageReceived(List<EMMessage> messages) {
                for(EMMessage message: messages){
                    String messageType = message.getStringAttribute(EaseMsgUtils.CALL_MSG_TYPE, "");
                    EMLog.d(TAG,"Receive msg:" + message.getMsgId() + " from:" + message.getFrom()+ "  messageType:"+ messageType);
                    //有关通话控制信令
                    if(messageType.equals(EaseMsgUtils.CALL_MSG_INFO) && !(message.getFrom().equals(EMClient.getInstance().getCurrentUser()))){
                        String action = message.getStringAttribute(EaseMsgUtils.CALL_ACTION, "");
                        String callerDevId = message.getStringAttribute(EaseMsgUtils.CALL_DEVICE_ID, "");
                        String fromCallId = message.getStringAttribute(EaseMsgUtils.CLL_ID, "");
                        String fromUser = message.getFrom();
                        String channel = message.getStringAttribute(EaseMsgUtils.CALL_CHANNELNAME, "");
                        JSONObject ext = null;
                        try {
                            ext = message.getJSONObjectAttribute(CALL_INVITE_EXT);
                        } catch (HyphenateException exception) {
                            exception.printStackTrace();
                        }

                        if(action == null || callerDevId == null || fromCallId == null || fromUser ==null || channel == null){
                            if(callListener != null){
                                callListener.onCallError(EaseCallError.PROCESS_ERROR,CALL_PROCESS_ERROR.CALL_RECEIVE_ERROR.code,"receive message error");
                            }
                            continue;
                        }
                        EaseCallAction callAction = EaseCallAction.getfrom(action);
                        switch (callAction) {
                            case CALL_INVITE: //收到通话邀请
                                int calltype = message.getIntAttribute
                                        (EaseMsgUtils.CALL_TYPE, 0);
                                EaseCallType callkitType =
                                        EaseCallType.getfrom(calltype);
                                if (callState != EaseCallState.CALL_IDLE) {
                                    if (fromCallId.equals(callID) &&
                                            fromUser.equals(fromUserId)
                                            && callkitType == EaseCallType.SINGLE_VOICE_CALL && callType == EaseCallType.SINGLE_VIDEO_CALL) {
                                        InviteEvent inviteEvent = new InviteEvent();
                                        inviteEvent.callId = fromCallId;
                                        inviteEvent.type = callkitType;

                                        //发布消息
                                        EaseLiveDataBus.get().with(EaseCallType.SINGLE_VIDEO_CALL.toString()).postValue(inviteEvent);
                                    } else {
                                        //发送忙碌状态
                                        AnswerEvent callEvent = new AnswerEvent();
                                        callEvent.result = EaseMsgUtils.CALL_ANSWER_BUSY;
                                        callEvent.callerDevId = callerDevId;
                                        callEvent.callId = fromCallId;
                                        sendCmdMsg(callEvent, fromUser);
                                    }
                                } else {
                                    callInfo.setCallerDevId(callerDevId);
                                    callInfo.setCallId(fromCallId);
                                    callInfo.setCallKitType(callkitType);
                                    callInfo.setChannelName(channel);
                                    callInfo.setComming(true);
                                    callInfo.setFromUser(fromUser);
                                    callInfo.setExt(ext);

                                    //邀请信息放入列表中
                                    callInfoMap.put(fromCallId, callInfo);

                                    //发送alert消息
                                    AlertEvent callEvent = new AlertEvent();
                                    callEvent.callerDevId = callerDevId;
                                    callEvent.callId = fromCallId;
                                    sendCmdMsg(callEvent, fromUser);

                                    //启动定时器
                                    timeHandler.startTime();
                                }
                                break;
                            default:
                                break;
                        }

                    }
                }
            }

            @Override
            public void onCmdMessageReceived(List<EMMessage> messages) {
                for(EMMessage message: messages){
                    String messageType = message.getStringAttribute(EaseMsgUtils.CALL_MSG_TYPE, "");
                    EMLog.d(TAG,"Receive cmdmsg:" + message.getMsgId() + " from:" + message.getFrom()  + "  messageType:"+ messageType);
                    //有关通话控制信令
                    if(messageType.equals(EaseMsgUtils.CALL_MSG_INFO) && !(message.getFrom().equals(EMClient.getInstance().getCurrentUser()))){
                        String action = message.getStringAttribute(EaseMsgUtils.CALL_ACTION, "");
                        String callerDevId = message.getStringAttribute(EaseMsgUtils.CALL_DEVICE_ID, "");
                        String fromCallId = message.getStringAttribute(EaseMsgUtils.CLL_ID, "");
                        String fromUser = message.getFrom();
                        String channel = message.getStringAttribute(EaseMsgUtils.CALL_CHANNELNAME, "");
                        EaseCallAction callAction = EaseCallAction.getfrom(action);
                        switch (callAction){
                            case CALL_CANCEL: //取消通话
                                if(callState == EaseCallState.CALL_IDLE){
                                    timeHandler.stopTime();
                                    //取消呼叫
                                    callInfoMap.remove(fromCallId);
                                }else{
                                    CallCancelEvent event = new CallCancelEvent();
                                    event.callerDevId = callerDevId;
                                    event.callId = fromCallId;
                                    event.userId = fromUser;
                                    if(callID.equals(fromCallId)){
                                        callState = EaseCallState.CALL_IDLE;
                                    }
                                    notifier.reset();
                                    //发布消息
                                    EaseLiveDataBus.get().with(EaseCallType.SINGLE_VIDEO_CALL.toString()).postValue(event);
                                }
                                break;
                            case CALL_ALERT:
                                String calleedDeviceId = message.getStringAttribute(EaseMsgUtils.CALLED_DEVICE_ID, "");
                                AlertEvent alertEvent = new AlertEvent();
                                alertEvent.callId = fromCallId;
                                alertEvent.calleeDevId = calleedDeviceId;
                                alertEvent.userId = fromUser;
                                EaseLiveDataBus.get().with(EaseCallType.SINGLE_VIDEO_CALL.toString()).postValue(alertEvent);
                                break;
                            case CALL_CONFIRM_RING: //收到callId 是否有效
                                String calledDvId = message.getStringAttribute(EaseMsgUtils.CALLED_DEVICE_ID, "");
                                boolean vaild = message.getBooleanAttribute(EaseMsgUtils.CALL_STATUS, false);
                                //多端设备时候用于区别哪个DrviceId,
                                // 被叫处理自己设备Id的CALL_CONFIRM_RING
                                if(calledDvId.equals(deviceId)){
                                    timeHandler.stopTime();
                                    if(!vaild){
                                        //通话无效
                                        callInfoMap.remove(fromCallId);
                                    }else{
                                        //收到callId 有效
                                        if(callState == EaseCallState.CALL_IDLE){
                                            callState = EaseCallState.CALL_ALERTING;
                                            //对方主叫的设备信息
                                            clallee_devId = callerDevId;
                                            callID = fromCallId;
                                            EaseCallInfo info = callInfoMap.get(fromCallId);
                                            if(info != null){
                                                channelName = info.getChannelName();
                                                callType = info.getCallKitType();
                                                fromUserId = info.getFromUser();
                                                inviteExt =info.getExt();
                                            }
                                            //收到有效的呼叫map邀请信息
                                            callInfoMap.clear();
                                            timeHandler.startSendEvent();
                                        }else{
                                            //通话无效
                                            callInfoMap.remove(fromCallId);
                                            timeHandler.stopTime();
                                        }
                                    }
                                }

                                break;
                            case CALL_CONFIRM_CALLEE:  //收到仲裁消息
                                String result = message.getStringAttribute(EaseMsgUtils.CALL_RESULT, "");
                                String calledDevId = message.getStringAttribute(EaseMsgUtils.CALLED_DEVICE_ID, "");
                                ConfirmCallEvent event = new ConfirmCallEvent();
                                event.calleeDevId = calledDevId;
                                event.result = result;
                                event.callerDevId = callerDevId;
                                event.callId = fromCallId;
                                event.userId = fromUser;
                                //发布消息
                                EaseLiveDataBus.get().with(EaseCallType.SINGLE_VIDEO_CALL.toString()).postValue(event);
                                break;
                            case CALL_ANSWER:  //收到被叫的回复消息
                                String result1 = message.getStringAttribute(EaseMsgUtils.CALL_RESULT, "");
                                String calledDevId1 = message.getStringAttribute(EaseMsgUtils.CALLED_DEVICE_ID, "");
                                boolean transVoice = message.getBooleanAttribute(EaseMsgUtils.CALLED_TRANSE_VOICE, false);
                                //判断不是被叫另外一台设备的漫游消息
                                //或者是主叫收到的
                                if(callType != EaseCallType.CONFERENCE_CALL){
                                    if((isComingCall && calledDevId1.equals(deviceId)) || !isComingCall) {
                                        AnswerEvent answerEvent = new AnswerEvent();
                                        answerEvent.result = result1;
                                        answerEvent.calleeDevId = calledDevId1;
                                        answerEvent.callerDevId = callerDevId;
                                        answerEvent.callId = fromCallId;
                                        answerEvent.userId = fromUser;
                                        answerEvent.transVoice = transVoice;

                                        //发布消息
                                        EaseLiveDataBus.get().with(EaseCallType.SINGLE_VIDEO_CALL.toString()).postValue(answerEvent);
                                    }
                                }else{
                                        if(!fromUser.equals(EMClient.getInstance().getCurrentUser())){
                                            AnswerEvent answerEvent = new AnswerEvent();
                                            answerEvent.result = result1;
                                            answerEvent.calleeDevId = calledDevId1;
                                            answerEvent.callerDevId = callerDevId;
                                            answerEvent.callId = fromCallId;
                                            answerEvent.userId = fromUser;
                                            answerEvent.transVoice = transVoice;

                                            //发布消息
                                            EaseLiveDataBus.get().with(EaseCallType.SINGLE_VIDEO_CALL.toString()).postValue(answerEvent);

                                        }
                                }
                                break;
                            case CALL_VIDEO_TO_VOICE:
                                if (callState != EaseCallState.CALL_IDLE) {
                                    if (fromCallId.equals(callID) &&
                                            fromUser.equals(fromUserId)) {
                                        InviteEvent inviteEvent = new InviteEvent();
                                        inviteEvent.callId = fromCallId;
                                        inviteEvent.type = EaseCallType.SINGLE_VOICE_CALL;
                                        //发布消息
                                        EaseLiveDataBus.get().with(EaseCallType.SINGLE_VIDEO_CALL.toString()).postValue(inviteEvent);
                                    }
                                }
                                break;
                            default:
                                break;
                        }
                    }
                }
            }

            @Override
            public void onMessageRead(List<EMMessage> messages) {

            }

            @Override
            public void onMessageDelivered(List<EMMessage> messages) {

            }

            @Override
            public void onMessageRecalled(List<EMMessage> messages) {

            }

            @Override
            public void onMessageChanged(EMMessage message, Object change) {

            }
        };
        //增加消息监听
        EMClient.getInstance().chatManager().addMessageListener(this.messageListener);

    }

    /***
     * 设置call kit监听
     * @param listener
     * @return
     */
    public void setCallKitListener(EaseCallKitListener listener){
        this.callListener = listener;
    }


    /***
     * 移除call kit监听
     * @param listener
     * @return
     */
    public void removeCallKitListener(EaseCallKitListener listener){
        this.callListener = null;
    }



    /**
     * 移除消息监听
     */
    private void removeMessageListener() {
        EMClient.getInstance().chatManager().removeMessageListener(messageListener);
        messageListener = null;
    }

    public EaseCallState getCallState() {
        return callState;
    }

    public EaseCallType getCallType() {
        return callType;
    }

    public void setCallType(EaseCallType callType) {
        this.callType = callType;
    }

    public void setCallState(EaseCallState callState) {
        this.callState = callState;
    }

    public String getCallID() { return callID; }

    public void setCallID(String callID) { this.callID = callID; }

    public String  getClallee_devId() {
        return clallee_devId;
    }

    public EaseCallKitListener getCallListener() {
        return callListener;
    }



    private boolean isMainProcess(Context context) {
        int pid = android.os.Process.myPid();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo appProcess : activityManager.getRunningAppProcesses()) {
            if (appProcess.pid == pid) {
                return context.getApplicationInfo().packageName.equals(appProcess.processName);
            }
        }
        return false;
    }


    /**
     * 发送CMD回复信息
     * @param username
     */
    private void sendCmdMsg(BaseEvent event,String username){
        final EMMessage message = EMMessage.createSendMessage(EMMessage.Type.CMD);
        message.setTo(username);
        String action="rtcCall";
        EMCmdMessageBody cmdBody = new EMCmdMessageBody(action);
        message.addBody(cmdBody);

        message.setAttribute(EaseMsgUtils.CALL_ACTION, event.callAction.state);
        message.setAttribute(EaseMsgUtils.CALL_DEVICE_ID, event.callerDevId);
        message.setAttribute(EaseMsgUtils.CLL_ID,event.callId);
        message.setAttribute(EaseMsgUtils.CLL_TIMESTRAMEP, System.currentTimeMillis());
        message.setAttribute(EaseMsgUtils.CALL_MSG_TYPE, EaseMsgUtils.CALL_MSG_INFO);
        if(event.callAction == EaseCallAction.CALL_ANSWER){
            message.setAttribute(EaseMsgUtils.CALLED_DEVICE_ID, deviceId);
            message.setAttribute(EaseMsgUtils.CALL_RESULT,((AnswerEvent)event).result);
        }else if(event.callAction == EaseCallAction.CALL_ALERT){
            message.setAttribute(EaseMsgUtils.CALLED_DEVICE_ID, deviceId);
        }
        final EMConversation conversation = EMClient.getInstance().chatManager().getConversation(username, EMConversation.EMConversationType.Chat, true);
        message.setMessageStatusCallback(new EMCallBack() {
            @Override
            public void onSuccess() {
                EMLog.d(TAG, "Invite call success");
                conversation.removeMessage(message.getMsgId());
            }

            @Override
            public void onError(int code, String error) {
                EMLog.e(TAG, "Invite call error " + code + ", " + error);
                conversation.removeMessage(message.getMsgId());
                if(callListener != null){
                    callListener.onCallError(EaseCallError.IM_ERROR,code,error);
                }
            }

            @Override
            public void onProgress(int progress, String status) {

            }
        });
        EMClient.getInstance().chatManager().sendMessage(message);
    }


    private class TimeHandler extends Handler {
        private final int MSG_TIMER = 0;
        private final int MSG_START_ACTIVITY = 1;
        private DateFormat dateFormat = null;
        private int timePassed = 0;

        public TimeHandler() {
            dateFormat = new SimpleDateFormat("HH:mm:ss");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        public void startTime() {
            timePassed = 0;
            sendEmptyMessageDelayed(MSG_TIMER, 1000);
        }

        public void startSendEvent() {
           sendEmptyMessage(MSG_START_ACTIVITY);
        }

        public void stopTime() {
            removeMessages(MSG_START_ACTIVITY);
            removeMessages(MSG_TIMER);
        }


        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_TIMER) {
                // TODO: update calling time.
                timePassed++;
                String time = dateFormat.format(timePassed * 1000);
                if(timePassed *1000 == EaseMsgUtils.CALL_INVITED_INTERVAL){

                    //呼叫超时
                    timeHandler.stopTime();
                    callState = EaseCallState.CALL_IDLE;
                }
                sendEmptyMessageDelayed(MSG_TIMER, 1000);
            }else if(msg.what == MSG_START_ACTIVITY){
                timeHandler.stopTime();
                String info = "";
                String userName = EaseCallKitUtils.getUserNickName(fromUserId);
                if(callType != EaseCallType.CONFERENCE_CALL){
                    //启动activity
                    EaseVideoCallActivity callActivity = new EaseVideoCallActivity();
                    Intent intent = new Intent(appContext, callActivity.getClass()).addFlags(FLAG_ACTIVITY_NEW_TASK);
                    Bundle bundle = new Bundle();
                    isComingCall = true;
                    bundle.putBoolean("isComingCall", true);
                    bundle.putString("channelName", channelName);
                    bundle.putString("username", fromUserId);
                    intent.putExtras(bundle);
                    appContext.startActivity(intent);
                    if(Build.VERSION.SDK_INT >= 29 && !EasyUtils.isAppRunningForeground(appContext)) {
                        EMLog.e(TAG,"notifier.notify:" + info);
                        if(callType == EaseCallType.SINGLE_VIDEO_CALL){
                            info = appContext.getString(R.string.alert_request_video, userName);
                        }else{
                            info = appContext.getString(R.string.alert_request_voice, userName);
                        }
                        notifier.notify(intent, "环信 ", info);
                    }
                }else {
                    //启动多人通话界面
                    multipleVideoActivity =
                            new EaseMultipleVideoActivity();
                    Intent intent = new Intent(appContext, multipleVideoActivity.getClass()).addFlags(FLAG_ACTIVITY_NEW_TASK);
                    Bundle bundle = new Bundle();
                    isComingCall = true;
                    bundle.putBoolean("isComingCall", true);
                    bundle.putString("channelName", channelName);
                    bundle.putString("username", fromUserId);
                    intent.putExtras(bundle);
                    appContext.startActivity(intent);
                    if (Build.VERSION.SDK_INT >= 29 && isAppRunningForeground(appContext)) {
                        info = appContext.getString(R.string.alert_request_multiple_video, userName);
                        notifier.notify(intent, "Hyphenate", info);
                    }
                }

                //通话邀请回调
                if(callListener != null){
                    callListener.onReceivedCall(callType,fromUserId,inviteExt);
                }
            }
            super.handleMessage(msg);
        }
    }

    /**
     * 判断Activity是否Destroy
     * @param mActivity
     * @return true:已销毁
     */
    private boolean isDestroy(Activity mActivity) {
        if (mActivity == null ||
                mActivity.isFinishing() ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && mActivity.isDestroyed())) {
            return true;
        } else {
            return false;
        }
    }

    public ArrayList<String> getInviteeUsers() {
        return inviteeUsers;
    }

    public void InitInviteeUsers() {
         inviteeUsers.clear();
    }

    public void setMultipleVideoActivity(EaseMultipleVideoActivity activity) {
        this.multipleVideoActivity = null;
    }

    public JSONObject getInviteExt() {
        return inviteExt;
    }

    public Context getAppContext() {
        return appContext;
    }
}
