package com.hyphenate.easecallkit.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hyphenate.EMCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMCmdMessageBody;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.easecallkit.base.EaseCallUserInfo;
import com.hyphenate.easecallkit.base.EaseGetUserAccountCallback;
import com.hyphenate.easecallkit.base.EaseUserAccount;
import com.hyphenate.util.EMLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.hyphenate.easecallkit.EaseCallKit;
import com.hyphenate.easecallkit.R;
import com.hyphenate.easecallkit.base.EaseCallFloatWindow;
import com.hyphenate.easecallkit.base.EaseCallKitConfig;
import com.hyphenate.easecallkit.base.EaseCallKitTokenCallback;
import com.hyphenate.easecallkit.base.EaseCallMemberView;
import com.hyphenate.easecallkit.base.EaseCallMemberViewGroup;
import com.hyphenate.easecallkit.event.AlertEvent;
import com.hyphenate.easecallkit.event.AnswerEvent;
import com.hyphenate.easecallkit.event.BaseEvent;
import com.hyphenate.easecallkit.event.CallCancelEvent;
import com.hyphenate.easecallkit.event.ConfirmCallEvent;
import com.hyphenate.easecallkit.event.ConfirmRingEvent;
import com.hyphenate.easecallkit.livedatas.EaseLiveDataBus;
import com.hyphenate.easecallkit.utils.EaseCallAction;
import com.hyphenate.easecallkit.base.EaseCallEndReason;
import com.hyphenate.easecallkit.base.EaseCallKitListener;
import com.hyphenate.easecallkit.base.EaseCallType;
import com.hyphenate.easecallkit.utils.EaseCallState;
import com.hyphenate.easecallkit.utils.EaseMsgUtils;
import com.hyphenate.easecallkit.utils.EaseCallKitUtils;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.models.UserInfo;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration;

import static com.hyphenate.easecallkit.utils.EaseMsgUtils.CALL_INVITE_EXT;
import static com.hyphenate.easecallkit.utils.EaseMsgUtils.CALL_TIMER_CALL_TIME;
import static com.hyphenate.easecallkit.utils.EaseMsgUtils.CALL_TIMER_TIMEOUT;
import static io.agora.rtc.Constants.*;



/**
 * author lijian
 * email: Allenlee@easemob.com
 * date: 01/15/2021
 */
public class EaseMultipleVideoActivity extends EaseBaseCallActivity implements View.OnClickListener{

    private static final String TAG = EaseMultipleVideoActivity.class.getSimpleName();

    private TimeHandler timehandler;
    private TimeHandler timeUpdataTimer;
    private RtcEngine mRtcEngine;

    private EaseCommingCallView incomingCallView;
    private EaseCallMemberViewGroup callConferenceViewGroup;
    private TextView callTimeView;
    private ImageButton micSwitch;
    private ImageButton cameraSwitch;
    private ImageButton speakerSwitch;
    private ImageButton changeCameraSwitch;
    private ImageButton hangupBtn;;
    private ImageView inviteBtn;
    private ImageView floatBtn;


    //判断是发起者还是被邀请
    protected boolean isInComingCall;
    protected String username;
    protected String channelName;
    protected AudioManager audioManager;
    protected Ringtone ringtone;
    private String ringFile;
    private MediaPlayer mediaPlayer;
    private RelativeLayout viewGroupLayout;


    volatile private boolean mConfirm_ring = false;
    private String tokenUrl;
    private EaseCallType callType;
    private boolean isMuteState = false;
    private boolean isVideoMute = true;
    private boolean isCameraFront = true;
    private EaseCallMemberView localMemberView;
    private Map<String, Long> inViteUserMap = new HashMap<>(); //用户定时map存储
    private String invite_ext;
    private String agoraAppId = null;

    private static final int PERMISSION_REQ_ID = 22;
    private static final String[] REQUESTED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private final Map<Integer, EaseCallMemberView> mUidsList = new HashMap<>();
    // 用于处理joinChannelWithUserAccount后的相关逻辑，修改为joinChannel后，主要使用 uIdMap处理用户信息
    private final Map<Integer, UserInfo> userInfoList = new HashMap<>();
    private final Map<String, Integer> userAccountList = new HashMap<>();
    private final Map<String, EaseCallMemberView> placeholderList = new HashMap<>();
    private List<Integer> uidList = new ArrayList<>();

    //加入频道Uid Map
    private Map<Integer, EaseUserAccount> uIdMap = new HashMap<>();
    EaseCallKitListener listener = EaseCallKit.getInstance().getCallListener();

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onError(int err) {
            super.onError(err);
            EMLog.d(TAG,"IRtcEngineEventHandler onError:" + err);
            if(listener != null){
                listener.onCallError(EaseCallKit.EaseCallError.RTC_ERROR,err,"rtc error");
            }
        }

        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            EMLog.d(TAG,"onJoinChannelSuccess channel:"+ channel + " uid" +uid);
            //加入频道开始计时
            timeUpdataTimer.startTime(CALL_TIMER_CALL_TIME);
            if(!isInComingCall){
                ArrayList<String> userList = EaseCallKit.getInstance().getInviteeUsers();
                if(userList != null && userList.size() > 0){
                    handler.sendEmptyMessage(EaseMsgUtils.MSG_MAKE_CONFERENCE_VIDEO);

                    //邀请人就变为主叫
                    isInComingCall = false;
                }
            }
        }

        @Override
        public void onRejoinChannelSuccess(String channel, int uid, int elapsed) {
            super.onRejoinChannelSuccess(channel, uid, elapsed);
        }


        @Override
        public void onLeaveChannel(RtcStats stats) {
            super.onLeaveChannel(stats);
        }

        @Override
        public void onClientRoleChanged(int oldRole, int newRole) {
            super.onClientRoleChanged(oldRole, newRole);
        }

        @Override
        public void onLocalUserRegistered(int uid, String userAccount) {
            super.onLocalUserRegistered(uid, userAccount);
        }

        /**
         * 此回调在调用{@link RtcEngine#joinChannelWithUserAccount(String, String, String)}时回调，
         * 3.8.1版本中修改为uid，即调用{@link RtcEngine#joinChannel(String, String, String, int)},
         * 下面的方法在3.8.1版本后不再回调。
         * @param uid
         * @param userInfo
         */
        @Override
        public void onUserInfoUpdated(int uid, UserInfo userInfo) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    EMLog.d(TAG,"onUserOffline " + (uid & 0xFFFFFFFFL) + " account:" + userInfo.userAccount);
                    userInfoList.put(uid,userInfo);
                    if(!userAccountList.containsValue(uid)){
                        userAccountList.put(userInfo.userAccount,uid);
                    }

                    //删除占位符
                    EaseCallMemberView placeView = placeholderList.remove(userInfo.userAccount);
                    if(placeView != null){
                        callConferenceViewGroup.removeView(placeView);
                    }
                    if (mUidsList.containsKey(uid)) {
                        EaseCallMemberView memberView = mUidsList.get(uid);
                        if (memberView != null) {
                            memberView.setUserInfo(userInfo);
                        }
                    }else{
                        final EaseCallMemberView memberView = new EaseCallMemberView(getApplicationContext());
                        memberView.setUserInfo(userInfo);
                        //获取有关头像 昵称信息
                        EaseUserAccount account = uIdMap.get(uid);
                        if(account != null){
                            setUserJoinChannelInfo(account.getUserName(),uid);
                        }else{
                            setUserJoinChannelInfo(null,uid);
                        }
                        callConferenceViewGroup.addView(memberView);
                        mUidsList.put(uid, memberView);
                    }
                }
            });
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            super.onUserJoined(uid, elapsed);

            //获取有关信息
            setUserJoinChannelInfo(null,uid);
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    EMLog.d(TAG,"onUserOffline " + (uid & 0xFFFFFFFFL) + " " + reason);
                    if (isFinishing()) {
                        return;
                    }
                    EaseCallMemberView memberView = mUidsList.remove(uid);
                    if (memberView == null) {
                        return;
                    }
                    callConferenceViewGroup.removeView(memberView);
                    if(userAccountList.containsValue(uid)){
                        userAccountList.remove(userInfoList.get(uid).userAccount);
                    }

                    int uid = 0;
                    if (mUidsList.size() > 0) { // 如果会议中有其他成员,则显示第一个成员
                        Set<Integer> uidSet = mUidsList.keySet();
                        for (int id : uidSet) {
                            uid = id;
                        }
                        //更新悬浮窗
                        updateFloatWindow(mUidsList.get(uid));
                    }

                    if(uIdMap != null){
                        uIdMap.remove(uid);
                    }
                }
            });
        }

        @Override
        public void onFirstRemoteVideoDecoded(final int uid, int width, int height, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //增加远端View
                    EMLog.d(TAG, "onFirstRemoteVideoDecoded" +
                            (uid & 0xFFFFFFFFL) + " " + width + " " +
                            height + " " + elapsed);
                    if (isFinishing()) {
                        return;
                    }

                    if (mUidsList.containsKey(uid) ) {
                        EaseCallMemberView memberView = mUidsList.get(uid);
                        if(userInfoList.containsKey(uid)){
                            memberView.setUserInfo(userInfoList.get(uid));
                            if(!userAccountList.containsValue(uid)){
                                userAccountList.put(userInfoList.get(uid).userAccount,uid);
                            }
                        }
                        if(memberView != null){
                            //删除占位符
                            EaseCallMemberView placeView = placeholderList.remove(memberView.getUserAccount());
                            if(placeView != null){
                                callConferenceViewGroup.removeView(placeView);
                            }

                            if(memberView.getSurfaceView() == null){
                                SurfaceView surfaceView =
                                        RtcEngine.CreateRendererView(getApplicationContext());
                                memberView.addSurfaceView(surfaceView);
                                surfaceView.setZOrderOnTop(false);
                                memberView.setVideoOff(false);
                                surfaceView.setZOrderMediaOverlay(false);
                                mRtcEngine.setupRemoteVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid));
                            }else{
                                memberView.setVideoOff(false);
                            }
                        }
                    }else{
                        EaseCallMemberView memberView = createCallMemberView();
                        if(userInfoList.containsKey(uid)){
                            memberView.setUserInfo(userInfoList.get(uid));
                        }

                        //删除占位符
                        EaseCallMemberView placeView = placeholderList.remove(memberView.getUserAccount());
                        if(placeView != null){
                            callConferenceViewGroup.removeView(placeView);
                        }


                        callConferenceViewGroup.addView(memberView);

                        memberView.setVideoOff(false);
                        mUidsList.put(uid, memberView);
                        mRtcEngine.setupRemoteVideo(new VideoCanvas(memberView.getSurfaceView(), VideoCanvas.RENDER_MODE_HIDDEN, uid));

                        //获取有关头像 昵称信息
                        EaseUserAccount account = uIdMap.get(uid);
                        if(account != null){
                            setUserJoinChannelInfo(account.getUserName(),uid);
                        }else{
                            setUserJoinChannelInfo(null,uid);
                        }
                    }
                }
            });
        }

        /** @deprecated */
        @Deprecated
        public void onFirstRemoteAudioFrame(int uid, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //增加远端View
                    EMLog.d(TAG, "onFirstRemoteVideoDecoded" +
                            (uid & 0xFFFFFFFFL) + " "  + elapsed);
                    if (isFinishing()) {
                        return;
                    }
                    if (mUidsList.containsKey(uid)) {
                        EaseCallMemberView memberView = mUidsList.get(uid);
                        if(memberView != null){
                            memberView.setAudioOff(false);
                        }
                        if(userInfoList.containsKey(uid)){
                            memberView.setUserInfo(userInfoList.get(uid));
                        }
                        //删除占位符
                        EaseCallMemberView placeView = placeholderList.remove(memberView.getUserAccount());
                        if(placeView != null){
                            callConferenceViewGroup.removeView(placeView);
                        }
                        if(!userAccountList.containsValue(uid)){
                            if(userInfoList.get(uid) != null && userInfoList.get(uid).userAccount!= null){
                                userAccountList.put(userInfoList.get(uid).userAccount,uid);
                            }
                        }
                    }else {
                        final EaseCallMemberView memberView = new EaseCallMemberView(getApplicationContext());
                        if(userInfoList.containsKey(uid)){
                            memberView.setUserInfo(userInfoList.get(uid));
                        }

                        //删除占位符
                        EaseCallMemberView placeView = placeholderList.remove(memberView.getUserAccount());
                        if(placeView != null){
                            callConferenceViewGroup.removeView(placeView);
                        }

                        memberView.setAudioOff(false);
                        callConferenceViewGroup.addView(memberView);
                        mUidsList.put(uid, memberView);

                        //获取有关头像 昵称信息
                        EaseUserAccount account = uIdMap.get(uid);
                        if(account != null){
                            setUserJoinChannelInfo(account.getUserName(),uid);
                        }else{
                            setUserJoinChannelInfo(null,uid);
                        }
                    }
                }
            });
        }

        @Override
        public void onRemoteVideoStateChanged(int uid, int state, int reason, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    EaseCallMemberView memberView = mUidsList.get(uid);
                    if(memberView != null){
                        if(state == REMOTE_VIDEO_STATE_STOPPED || state == REMOTE_VIDEO_STATE_REASON_REMOTE_MUTED){
                            memberView.setVideoOff(true);
                        }else if(state == REMOTE_VIDEO_STATE_DECODING || state == REMOTE_VIDEO_STATE_REASON_REMOTE_UNMUTED){
                            memberView.setVideoOff(false);
                        }

                        if(state == REMOTE_VIDEO_STATE_STOPPED|| state == REMOTE_VIDEO_STATE_REASON_REMOTE_MUTED || state == REMOTE_VIDEO_STATE_DECODING ||state == REMOTE_VIDEO_STATE_REASON_REMOTE_UNMUTED){
                            //判断视频是当前悬浮窗 更新悬浮窗
                            EaseCallMemberView floatView = EaseCallFloatWindow.getInstance().getCallMemberView();
                            if(floatView != null && floatView.getUserId() == uid){
                                updateFloatWindow(mUidsList.get(uid));
                            }
                        }
                    }
                }
            });
        }

        @Override
        public void onRemoteAudioStateChanged(int uid, int state, int reason, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    EaseCallMemberView memberView = mUidsList.get(uid);
                    if(memberView != null){
                        if(state == REMOTE_AUDIO_REASON_REMOTE_MUTED || state == REMOTE_AUDIO_STATE_STOPPED){
                            memberView.setAudioOff(true);
                        }else if(state == REMOTE_AUDIO_STATE_DECODING || state == REMOTE_AUDIO_REASON_REMOTE_UNMUTED){
                            memberView.setAudioOff(false);
                        }
                    }
                }
            });
        }

        @Override
        public void onAudioVolumeIndication(IRtcEngineEventHandler.AudioVolumeInfo[] speakers, int totalVolume){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (speakers != null && speakers.length > 0) {

                        uidList.clear();
                        uidList.addAll(mUidsList.keySet());
                        for (AudioVolumeInfo info : speakers) {
                            Integer uId = info.uid;
                            int volume = info.volume;
                            EMLog.d(TAG, "onAudioVolumeIndication" +
                                    (uId & 0xFFFFFFFFL) + "  volume: " + volume);
                            if (uidList.contains(uId)) {
                                EaseCallMemberView memberView = mUidsList.get(uId);
                                if (memberView != null && !memberView.getAudioOff()) {
                                    memberView.setSpeak(true, volume);
                                    uidList.remove(uId);
                                }
                            }
                        }
                        if (uidList.size() > 0) {
                            for (int uid : uidList) {
                                EaseCallMemberView memberView = mUidsList.get(uid);
                                if (memberView != null && !memberView.getAudioOff()) {
                                    memberView.setSpeak(false, 0);
                                }
                            }
                        }
                    }
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ease_multiple_video);
        //初始化
        if(savedInstanceState == null){
            initParams(getIntent().getExtras());
        }else{
            initParams(savedInstanceState);
        }

        //Init View
        initView();
        //增加LiveData监听
        addLiveDataObserver();

        //开启设备权限
        if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[2], PERMISSION_REQ_ID)) {
        }
        timehandler = new TimeHandler();
        timeUpdataTimer = new TimeHandler();
        checkConference(true);
        EaseCallKit.getInstance().getNotifier().reset();
    }


    public void initView(){
        incomingCallView = (EaseCommingCallView)findViewById(R.id.incoming_call_view);
        viewGroupLayout = findViewById(R.id.viewGroupLayout);
        callConferenceViewGroup = (EaseCallMemberViewGroup)findViewById(R.id.surface_view_group);
        inviteBtn = (ImageView)findViewById(R.id.btn_invite);
        callTimeView = (TextView)findViewById(R.id.tv_call_time);
        micSwitch = (ImageButton) findViewById(R.id.btn_mic_switch);
        cameraSwitch = (ImageButton) findViewById(R.id.btn_camera_switch);
        speakerSwitch = (ImageButton) findViewById(R.id.btn_speaker_switch);
        changeCameraSwitch = (ImageButton)findViewById(R.id.btn_change_camera_switch);
        hangupBtn = (ImageButton)findViewById(R.id.btn_hangup);
        floatBtn = (ImageView)findViewById(R.id.btn_float);
        incomingCallView.setOnActionListener(onActionListener);
        callConferenceViewGroup.setOnItemClickListener(onItemClickListener);
        callConferenceViewGroup.setOnScreenModeChangeListener(onScreenModeChangeListener);
        inviteBtn.setOnClickListener(this);
        micSwitch.setOnClickListener(this);
        speakerSwitch.setOnClickListener(this);
        cameraSwitch.setOnClickListener(this);
        changeCameraSwitch.setOnClickListener(this);
        hangupBtn.setOnClickListener(this);
        floatBtn.setOnClickListener(this);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        micSwitch.setActivated(false);
        cameraSwitch.setActivated(true);
        speakerSwitch.setActivated(true);
        openSpeakerOn();

        ringFile = EaseCallKitUtils.getRingFile();

        //被邀请的话弹出邀请界面
        if(isInComingCall){
            audioManager = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
            Uri ringUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            audioManager.setMode(AudioManager.MODE_RINGTONE);
            audioManager.setSpeakerphoneOn(true);
            ringtone = RingtoneManager.getRingtone(this, ringUri);
            AudioManager am = (AudioManager)this.getApplication().getSystemService(Context.AUDIO_SERVICE);
            int ringerMode = am.getRingerMode();
            if(ringerMode == AudioManager.RINGER_MODE_NORMAL){
                playRing();
            }

            incomingCallView.setInviteInfo(username);
            //更新昵称头像
            setUserJoinChannelInfo(username,0);
            incomingCallView.setVisibility(View.VISIBLE);

        }else{
            incomingCallView.setVisibility(View.GONE);

            //主叫加入频道
            initEngineAndJoinChannel();
        }
    }

    private void initParams(Bundle bundle){
        if(bundle != null) {
            isInComingCall = bundle.getBoolean("isComingCall", false);
            username = bundle.getString("username");
            channelName = bundle.getString("channelName");
            callType = EaseCallKit.getInstance().getCallType();
           // invite_ext = bundle.getString(CALL_INVITE_EXT);
        }else{
            isInComingCall = EaseCallKit.getInstance().getIsComingCall();
            username = EaseCallKit.getInstance().getFromUserId();
            channelName = EaseCallKit.getInstance().getChannelName();
            callType = EaseCallKit.getInstance().getCallType();
        }
    }

    private void initEngineAndJoinChannel() {
        initializeEngine();
        setupVideoConfig();
        setupLocalVideo();
        joinChannel();
    }

    private void initializeEngine() {
        try {
            EaseCallKitConfig config =  EaseCallKit.getInstance().getCallKitConfig();
            if(config != null){
                agoraAppId = config.getAgoraAppId();
            }
            mRtcEngine = RtcEngine.create(getBaseContext(), agoraAppId, mRtcEventHandler);

            //因为有小程序 设置为直播模式 角色设置为主播
            mRtcEngine.setChannelProfile(CHANNEL_PROFILE_LIVE_BROADCASTING);
            mRtcEngine.setClientRole(CLIENT_ROLE_BROADCASTER);

            EaseCallFloatWindow.getInstance().setRtcEngine(getApplicationContext(), mRtcEngine);
            //设置小窗口悬浮类型
            EaseCallFloatWindow.getInstance().setCallType(EaseCallType.CONFERENCE_CALL);
        } catch (Exception e) {
            EMLog.e(TAG, Log.getStackTraceString(e));
            throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
        }
    }

    private void setupVideoConfig() {
        mRtcEngine.enableVideo();
        mRtcEngine.muteLocalVideoStream(true);
        mRtcEngine.setVideoEncoderConfiguration(new VideoEncoderConfiguration(
                    VideoEncoderConfiguration.VD_1280x720,
                    VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                    VideoEncoderConfiguration.STANDARD_BITRATE,
                    VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT));

        //启动谁在说话检测
        int res = mRtcEngine.enableAudioVolumeIndication(500,3,false);
    }

    /**
     * If float window is showing, use the old view
     */
    private void setupLocalVideo() {
        if(isFloatWindowShowing()) {
            return;
        }
        localMemberView = createCallMemberView();
        UserInfo info = new UserInfo();
        info.userAccount = EMClient.getInstance().getCurrentUser();
        info.uid = 0;
        localMemberView.setUserInfo(info);
        localMemberView.setVideoOff(true);
        localMemberView.setCameraDirectionFront(isCameraFront);
        callConferenceViewGroup.addView(localMemberView);
        setUserJoinChannelInfo(EMClient.getInstance().getCurrentUser(),0);
        mUidsList.put(0, localMemberView);
        mRtcEngine.setupLocalVideo(new VideoCanvas(localMemberView.getSurfaceView(), VideoCanvas.RENDER_MODE_HIDDEN, 0));
    }

    public EaseCallMemberView createCallMemberView() {
        SurfaceView surfaceView = RtcEngine.CreateRendererView(getApplicationContext());
        EaseCallMemberView memberView = new EaseCallMemberView(getApplicationContext());
        surfaceView.setZOrderOnTop(false);
        surfaceView.setZOrderMediaOverlay(false);
        memberView.addSurfaceView(surfaceView);
        return memberView;
    }

    /**
     * 加入频道
     */
    private void joinChannel() {
        EaseCallKitConfig callKitConfig = EaseCallKit.getInstance().getCallKitConfig();
        if(listener != null && callKitConfig != null && callKitConfig.isEnableRTCToken()){
            listener.onGenerateToken(EMClient.getInstance().getCurrentUser(),channelName,  EMClient.getInstance().getOptions().getAppKey(), new EaseCallKitTokenCallback(){
                @Override
                public void onSetToken(String token,int uId) {
                    //获取到Token uid加入频道
                    mRtcEngine.joinChannel(token, channelName,null,uId);
                    //自己信息加入uIdMap
                    uIdMap.put(uId,new EaseUserAccount(uId,EMClient.getInstance().getCurrentUser()));
                }

                @Override
                public void onGetTokenError(int error, String errorMsg) {
                    EMLog.e(TAG,"onGenerateToken error :" + EMClient.getInstance().getAccessToken());
                    //获取Token失败,退出呼叫
                    exitChannel();
                }
            });
        }
//        else{
//            mRtcEngine.joinChannelWithUserAccount(null, channelName,  EMClient.getInstance().getCurrentUser());
//        }
    }

    /**
     * Change whether mute
     * @param isMute
     */
    private void changeMuteState(boolean isMute) {
        localMemberView.setAudioOff(isMute);
        mRtcEngine.muteLocalAudioStream(isMute);
        isMuteState = isMute;
        micSwitch.setBackground(isMute ? getResources().getDrawable(R.drawable.audio_mute) : getResources().getDrawable(R.drawable.audio_unmute));
    }

    private void changeSpeakerState(boolean isActive) {
        localMemberView.setSpeakActivated(isActive);
        speakerSwitch.setActivated(isActive);
        speakerSwitch.setBackground(isActive ? getResources().getDrawable(R.drawable.voice_on) : getResources().getDrawable(R.drawable.voice_off));
        if(isActive) {
            openSpeakerOn();
        }else {
            closeSpeakerOn();
        }
    }

    private void changeVideoState(boolean videoOff) {
        localMemberView.setVideoOff(videoOff);
        mRtcEngine.muteLocalVideoStream(videoOff);
        isVideoMute = videoOff;
        cameraSwitch.setBackground(videoOff ? getResources().getDrawable(R.drawable.video_0ff) : getResources().getDrawable(R.drawable.video_on));
    }

    private void changeCameraDirect(boolean isFront) {
        if(this.isCameraFront != isFront) {
            if(mRtcEngine != null){
                mRtcEngine.switchCamera();
            }
            this.isCameraFront = isFront;
            localMemberView.setCameraDirectionFront(isFront);
        }
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.btn_mic_switch){
            changeMuteState(!isMuteState);
        }else if(view.getId() == R.id.btn_speaker_switch){
            changeSpeakerState(!speakerSwitch.isActivated());
        }else if(view.getId() == R.id.btn_camera_switch){
            changeVideoState(!isVideoMute);
        }else if(view.getId() == R.id.btn_change_camera_switch){
            changeCameraDirect(!isCameraFront);
        }else if(view.getId() == R.id.btn_hangup){
            if(listener != null){
                listener.onEndCallWithReason(callType,channelName, EaseCallEndReason.EaseCallEndReasonHangup,timeUpdataTimer.timePassed*1000);
            }
            exitChannel();
        }else if(view.getId() == R.id.btn_float){
            showFloatWindow();
        }else if(view.getId() == R.id.btn_invite){
            if(listener != null){
                Set<Integer> userset = mUidsList.keySet();
                int size = userset.size();
                JSONObject object = EaseCallKit.getInstance().getInviteExt();
                if(size > 0){
                    String users[] = new String[size];
                    int i = 0;
                    for(Integer user:userset){
                        if(mUidsList.get(user) != null){
                            users[i++] = mUidsList.get(user).getUserAccount();
                        }
                    }
                    listener.onInviteUsers(getApplicationContext(),users,object);
                }else{
                    listener.onInviteUsers(getApplicationContext(),null,object);
                }
            }
        }
    }


    /**
     * 增加LiveData监听
     */
    protected void addLiveDataObserver(){
        EaseLiveDataBus.get().with(EaseCallType.SINGLE_VIDEO_CALL.toString(), BaseEvent.class).observe(this, event -> {
            if(event != null) {
                switch (event.callAction){
                    case CALL_ALERT:
                        AlertEvent alertEvent = (AlertEvent)event;
                        //判断会话是否有效
                        ConfirmRingEvent ringEvent = new ConfirmRingEvent();
                        String user = alertEvent.userId;
                        if(TextUtils.equals(alertEvent.callId, EaseCallKit.getInstance().getCallID())
                                && inViteUserMap.containsKey(user)) {
                            //发送会话有效消息
                            ringEvent.calleeDevId = alertEvent.calleeDevId;
                            ringEvent.valid = true;
                            ringEvent.userId = alertEvent.userId;
                            sendCmdMsg(ringEvent,alertEvent.userId);
                        }else{
                            //发送会话无效消息
                            ringEvent.calleeDevId = alertEvent.calleeDevId;
                            ringEvent.valid = false;
                            sendCmdMsg(ringEvent, alertEvent.userId);
                        }
                        //已经发送过会话确认消息
                        mConfirm_ring = true;
                        break;
                    case CALL_CANCEL:
                        if(!isInComingCall){
                            //停止仲裁定时器
                            timehandler.stopTime();
                        }
                        //取消通话
                        exitChannel();
                        break;
                    case CALL_ANSWER:
                        AnswerEvent answerEvent = (AnswerEvent)event;
                        ConfirmCallEvent callEvent = new ConfirmCallEvent();
                        callEvent.calleeDevId = answerEvent.calleeDevId;
                        callEvent.result = answerEvent.result;

                        //删除超时机制
                        String userId = answerEvent.userId;
                        inViteUserMap.remove(userId);

                        if(TextUtils.equals(answerEvent.result, EaseMsgUtils.CALL_ANSWER_BUSY)) {
                            if(!mConfirm_ring){
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        //提示对方正在忙碌中

                                        //删除占位符
                                        EaseCallMemberView placeView = placeholderList.remove(userId);
                                        if(placeView != null){
                                            callConferenceViewGroup.removeView(placeView);
                                        }

                                        String info = answerEvent.userId;
                                        info +=  getString(R.string.The_other_is_busy);

                                        Toast.makeText(getApplicationContext(),info , Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }else{
                                sendCmdMsg(callEvent,username);
                            }
                        }else if(TextUtils.equals(answerEvent.result, EaseMsgUtils.CALL_ANSWER_ACCEPT)){
                            //设置为接听
                            EaseCallKit.getInstance().setCallState(EaseCallState.CALL_ANSWERED);
                            sendCmdMsg(callEvent,answerEvent.userId);
                        }else if(TextUtils.equals(answerEvent.result, EaseMsgUtils.CALL_ANSWER_REFUSE)){
                            sendCmdMsg(callEvent,answerEvent.userId);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //删除占位符
                                    EaseCallMemberView placeView = placeholderList.remove(userId);
                                    if (placeView != null) {
                                        callConferenceViewGroup.removeView(placeView);
                                    }
                                }
                            });
                        }
                        break;
                    case CALL_CONFIRM_RING:
                        break;
                    case CALL_CONFIRM_CALLEE:
                        ConfirmCallEvent confirmEvent = (ConfirmCallEvent)event;
                        String deviceId = confirmEvent.calleeDevId;
                        String result = confirmEvent.result;
                        timehandler.stopTime();
                        //收到的仲裁为自己设备
                        if(TextUtils.equals(deviceId, EaseCallKit.deviceId)) {
                            //收到的仲裁为接听
                            if(TextUtils.equals(result, EaseMsgUtils.CALL_ANSWER_ACCEPT)) {
                                //加入频道
                                initEngineAndJoinChannel();

                            }else if(TextUtils.equals(result, EaseMsgUtils.CALL_ANSWER_REFUSE)){
                                //退出通话
                                exitChannel();
                                if(listener != null){
                                    listener.onEndCallWithReason(callType,channelName, EaseCallEndReason.EaseCallEndReasonRefuse,0);
                                }
                            }
                        }else{
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //提示已在其他设备处理
                                    String info = null;
                                    if(TextUtils.equals(result, EaseMsgUtils.CALL_ANSWER_ACCEPT)) {
                                        //已经在其他设备接听
                                        info = getString(R.string.The_other_is_recived);

                                    }else if(TextUtils.equals(result, EaseMsgUtils.CALL_ANSWER_REFUSE)){
                                        //已经在其他设备拒绝
                                        info = getString(R.string.The_other_is_refused);
                                    }
                                    Toast.makeText(getApplicationContext(),info , Toast.LENGTH_SHORT).show();
                                    //退出通话
                                    exitChannel();
                                    if(listener != null){
                                        listener.onEndCallWithReason(callType,channelName, EaseCallEndReason.EaseCallEndReasonHandleOnOtherDevice,0);
                                    }
                                }
                            });
                        }
                        break;
                }
            }
        });

        EaseLiveDataBus.get().with(EaseCallKitUtils.UPDATE_USERINFO, EaseCallUserInfo.class).observe(this, userInfo -> {
            if (userInfo != null) {
                //更新本地头像昵称
                EaseCallKit.getInstance().getCallKitConfig().setUserInfo(userInfo.getUserId(),userInfo);
                if(userInfo.getUserId() != null){
                    if(userAccountList.containsKey(userInfo.getUserId())) {
                        int uid = userAccountList.get(userInfo.getUserId());
                        updateUserInfo(uid);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(TextUtils.equals(username, userInfo.getUserId()) && incomingCallView != null) {
                                    incomingCallView.setInviteInfo(username);
                                }
                            }
                        });
                    }
                }
            }
        });
    }


    private EaseCommingCallView.OnActionListener onActionListener = new EaseCommingCallView.OnActionListener() {
        @Override
        public void onPickupClick(View v) {
            //停止震铃
            stopPlayRing();
            incomingCallView.setVisibility(View.GONE);
            if(isInComingCall){
                //发送接听消息
                AnswerEvent event = new AnswerEvent();
                event.result = EaseMsgUtils.CALL_ANSWER_ACCEPT;
                event.callId = EaseCallKit.getInstance().getCallID();
                event.callerDevId = EaseCallKit.getInstance().getClallee_devId();
                event.calleeDevId = EaseCallKit.deviceId;
                sendCmdMsg(event,username);
            }
        }

        @Override
        public void onRejectClick(View v) {
            //停止震铃
            if(isInComingCall){
                stopPlayRing();
                //发送拒绝消息
                AnswerEvent event = new AnswerEvent();
                event.result = EaseMsgUtils.CALL_ANSWER_REFUSE;
                event.callId = EaseCallKit.getInstance().getCallID();
                event.callerDevId = EaseCallKit.getInstance().getClallee_devId();
                event.calleeDevId = EaseCallKit.deviceId;
                sendCmdMsg(event,username);
            }
        }
    };


    private EaseCallMemberViewGroup.OnScreenModeChangeListener onScreenModeChangeListener = new EaseCallMemberViewGroup.OnScreenModeChangeListener() {
        @Override
        public void onScreenModeChange(boolean isFullScreenMode, @Nullable View fullScreenView) {
            if (isFullScreenMode) { // 全屏模式
            } else { // 非全屏模式
            }
        }
    };

    private EaseCallMemberViewGroup.OnItemClickListener onItemClickListener = new EaseCallMemberViewGroup.OnItemClickListener() {
        @Override
        public void onItemClick(View v, int position) {
        }
    };


    /**
     * 开启扬声器
     */
    protected void openSpeakerOn() {
        try {
            if (!audioManager.isSpeakerphoneOn())
                audioManager.setSpeakerphoneOn(true);
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭扬声器
     */
    protected void closeSpeakerOn() {
        try {
            if (audioManager != null) {
                if (audioManager.isSpeakerphoneOn())
                    audioManager.setSpeakerphoneOn(false);
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 离开频道
     */
    private void leaveChannel() {
        // 离开当前频道。
        if(mRtcEngine != null) {
            mRtcEngine.leaveChannel();
        }
    }

    //更新会议时间
    private void updateConferenceTime(String time) {
        Log.e(TAG, "time: "+time);
        callTimeView.setText(time);
    }

    private class TimeHandler extends Handler {
        private DateFormat dateFormat = null;
        private int timePassed = 0;

        public TimeHandler() {
            dateFormat = new SimpleDateFormat("mm:ss");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        public void startTime(int timeType) {
            Log.e(TAG, "start timer");
            timePassed = 0;
            removeMessages(timeType);
            sendEmptyMessageDelayed(timeType, 1000);
        }

        public void stopTime() {
            Log.e(TAG, "stopTime");
            removeMessages(CALL_TIMER_CALL_TIME);
            removeMessages(EaseMsgUtils.CALL_TIMER_TIMEOUT);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == CALL_TIMER_TIMEOUT) {
                // TODO: update calling time.
                timePassed++;
                String time = dateFormat.format(timePassed * 1000);
                if(!isInComingCall){ //如果是主叫
                    long totalMilliSeconds = System.currentTimeMillis();
                    Iterator<String> it_user = inViteUserMap.keySet().iterator();
                    while(it_user.hasNext()){
                        String userName = it_user.next();
                        //判断当前时间是否超时
                        if(totalMilliSeconds >= inViteUserMap.get(userName)){
                            //发送取消事件
                            CallCancelEvent cancelEvent = new CallCancelEvent();
                            sendCmdMsg(cancelEvent,userName);
                            it_user.remove();
                            EaseCallMemberView memberView = placeholderList.remove(userName);
                            if(memberView != null){
                                callConferenceViewGroup.removeView(memberView);
                            }
                      }
                    }
                    if(inViteUserMap.size() == 0){
                        timehandler.stopTime();
                    }else{
                        sendEmptyMessageDelayed(CALL_TIMER_TIMEOUT, 1000);
                    }
                }else{
                    long intervalTime;
                    EaseCallKitConfig callKitConfig = EaseCallKit.getInstance().getCallKitConfig();
                    if(callKitConfig != null){
                        intervalTime = callKitConfig.getCallTimeOut();
                    }else{
                        intervalTime = EaseMsgUtils.CALL_INVITE_INTERVAL;
                    }
                    sendEmptyMessageDelayed(CALL_TIMER_TIMEOUT, 1000);
                    if(timePassed *1000 == intervalTime){
                        timehandler.stopTime();

                        //被叫等待仲裁消息超时
                        exitChannel();
                        if(listener != null){
                            //对方回复超时
                            listener.onEndCallWithReason(callType,channelName, EaseCallEndReason.EaseCallEndReasonRemoteNoResponse,0);
                        }
                    }
                }


            }else if(msg.what == CALL_TIMER_CALL_TIME){
                timePassed++;
                updateTime(this);
            }
            super.handleMessage(msg);
        }
    }

    private void updateTime(TimeHandler handler) {
        String time = handler.dateFormat.format(handler.timePassed * 1000);
        updateConferenceTime(time);
        handler.removeMessages(CALL_TIMER_CALL_TIME);
        handler.sendEmptyMessageDelayed(CALL_TIMER_CALL_TIME, 1000);
    }

    /**
     * 处理异步消息
     */
    HandlerThread callHandlerThread = new HandlerThread("callHandlerThread");
    { callHandlerThread.start(); }
    protected Handler handler = new Handler(callHandlerThread.getLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 100: // 1V1语音通话
                    break;
                case 101: // 1V1视频通话
                    break;
                case 102: // 多人视频通话
                    ArrayList<String> sendInviteeMsg = EaseCallKit.getInstance().getInviteeUsers();
                    sendInviteeMsg(sendInviteeMsg, EaseCallType.CONFERENCE_CALL);
                    break;
                case 301: //停止事件循环线程
                    //防止内存泄漏
                    handler.removeMessages(100);
                    handler.removeMessages(101);
                    handler.removeMessages(102);
                    callHandlerThread.quit();
                    break;
                default:
                    break;
            }
        }
    };


    /**
     * 发送通话邀请信息
     * @param userArray
     * @param callType
     */
    private void sendInviteeMsg(ArrayList<String> userArray, EaseCallType callType){
        //开始定时器
        isInComingCall = false;
        timehandler.startTime(CALL_TIMER_TIMEOUT);
        for(String username:userArray){
            if(!placeholderList.containsKey(username) &&  !userAccountList.containsKey(username)) {

                //更新头像昵称
                setUserJoinChannelInfo(username,0);

                //放入超时时间
                long totalMilliSeconds = System.currentTimeMillis();
                long intervalTime;
                EaseCallKitConfig callKitConfig = EaseCallKit.getInstance().getCallKitConfig();
                if (callKitConfig != null) {
                    intervalTime = callKitConfig.getCallTimeOut();
                } else {
                    intervalTime = EaseMsgUtils.CALL_INVITE_INTERVAL;
                }
                totalMilliSeconds += intervalTime;

                //放进userMap里面
                inViteUserMap.put(username, totalMilliSeconds);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //显示占位符
                        final EaseCallMemberView memberView = new EaseCallMemberView(getApplicationContext());
                        UserInfo userInfo = new UserInfo();
                        userInfo.userAccount = username;
                        memberView.setUserInfo(userInfo);
                        memberView.setLoading(true);
                        callConferenceViewGroup.addView(memberView);
                        placeholderList.put(username, memberView);
                    }
                });

                final EMMessage message = EMMessage.createTxtSendMessage(getApplicationContext().getString(R.string.invited_to_make_multi_party_call), username);
                message.setAttribute(EaseMsgUtils.CALL_ACTION, EaseCallAction.CALL_INVITE.state);
                message.setAttribute(EaseMsgUtils.CALL_CHANNELNAME, channelName);
                message.setAttribute(EaseMsgUtils.CALL_TYPE, callType.code);
                message.setAttribute(EaseMsgUtils.CALL_DEVICE_ID, EaseCallKit.deviceId);
                JSONObject object = EaseCallKit.getInstance().getInviteExt();
                if (object != null) {
                    message.setAttribute(CALL_INVITE_EXT, object);
                } else {
                    try {
                        JSONObject obj = new JSONObject();
                        message.setAttribute(CALL_INVITE_EXT, obj);
                    } catch (Exception e) {
                        e.getStackTrace();
                    }
                }
                if (EaseCallKit.getInstance().getCallID() == null) {
                    EaseCallKit.getInstance().setCallID(EaseCallKitUtils.getRandomString(10));
                }
                message.setAttribute(EaseMsgUtils.CLL_ID, EaseCallKit.getInstance().getCallID());

                message.setAttribute(EaseMsgUtils.CLL_TIMESTRAMEP, System.currentTimeMillis());
                message.setAttribute(EaseMsgUtils.CALL_MSG_TYPE, EaseMsgUtils.CALL_MSG_INFO);

                //增加推送字段
                JSONObject extObject = new JSONObject();
                try {
                    String info = getApplication().getString(R.string.alert_request_multiple_video, EMClient.getInstance().getCurrentUser());
                    extObject.putOpt("em_push_title", info);
                    extObject.putOpt("em_push_content", info);
                    extObject.putOpt("isRtcCall", true);
                    extObject.putOpt("callType", EaseCallType.CONFERENCE_CALL.code);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                message.setAttribute("em_apns_ext", extObject);

                final EMConversation conversation = EMClient.getInstance().chatManager().getConversation(username, EMConversation.EMConversationType.Chat, true);
                message.setMessageStatusCallback(new EMCallBack() {
                    @Override
                    public void onSuccess() {
                        EMLog.d(TAG, "Invite call success username:" + username);
                        if (listener != null) {
                            listener.onInViteCallMessageSent();
                        }
                    }

                    @Override
                    public void onError(int code, String error) {
                        EMLog.e(TAG, "Invite call error " + code + ", " + error + " username:" + username);

                        if (listener != null) {
                            listener.onCallError(EaseCallKit.EaseCallError.IM_ERROR, code, error);
                            listener.onInViteCallMessageSent();
                        }
                    }

                    @Override
                    public void onProgress(int progress, String status) {

                    }
                });
                EMClient.getInstance().chatManager().sendMessage(message);
            }
        }

        //初始化邀请列表
        EaseCallKit.getInstance().InitInviteeUsers();
    }

    /**
     * 发送CMD回复信息
     * @param username
     */
    private void sendCmdMsg(BaseEvent event, String username){
        final EMMessage message = EMMessage.createSendMessage(EMMessage.Type.CMD);

        String action="rtcCall";
        EMCmdMessageBody cmdBody = new EMCmdMessageBody(action);
        message.setTo(username);
        message.addBody(cmdBody);
        if(event.callAction.equals(EaseCallAction.CALL_CANCEL)){
            cmdBody.deliverOnlineOnly(false);
        }else{
            cmdBody.deliverOnlineOnly(true);
        }

        message.setAttribute(EaseMsgUtils.CALL_ACTION, event.callAction.state);
        message.setAttribute(EaseMsgUtils.CALL_DEVICE_ID, EaseCallKit.deviceId);
        message.setAttribute(EaseMsgUtils.CLL_ID, EaseCallKit.getInstance().getCallID());
        message.setAttribute(EaseMsgUtils.CLL_TIMESTRAMEP, System.currentTimeMillis());
        message.setAttribute(EaseMsgUtils.CALL_MSG_TYPE, EaseMsgUtils.CALL_MSG_INFO);
        if(event.callAction == EaseCallAction.CALL_CONFIRM_RING){
            message.setAttribute(EaseMsgUtils.CALL_STATUS, ((ConfirmRingEvent)event).valid);
            message.setAttribute(EaseMsgUtils.CALLED_DEVICE_ID, ((ConfirmRingEvent)event).calleeDevId);
        }else if(event.callAction == EaseCallAction.CALL_CONFIRM_CALLEE){
            message.setAttribute(EaseMsgUtils.CALL_RESULT, ((ConfirmCallEvent)event).result);
            message.setAttribute(EaseMsgUtils.CALLED_DEVICE_ID, ((ConfirmCallEvent)event).calleeDevId);
        }else if(event.callAction == EaseCallAction.CALL_ANSWER){
            message.setAttribute(EaseMsgUtils.CALL_RESULT, ((AnswerEvent)event).result);
            message.setAttribute(EaseMsgUtils.CALLED_DEVICE_ID, ((AnswerEvent) event).calleeDevId);
            message.setAttribute(EaseMsgUtils.CALL_DEVICE_ID, ((AnswerEvent) event).callerDevId);
        }
        final EMConversation conversation = EMClient.getInstance().chatManager().getConversation(username, EMConversation.EMConversationType.Chat, true);
        message.setMessageStatusCallback(new EMCallBack() {
            @Override
            public void onSuccess() {
                EMLog.d(TAG, "Invite call success");
                conversation.removeMessage(message.getMsgId());
                if(event.callAction == EaseCallAction.CALL_CANCEL){
                    //退出频道
                    //exitChannel();
                }else if(event.callAction == EaseCallAction.CALL_ANSWER){
                    //回复以后启动定时器，等待仲裁超时
                    timehandler.startTime(CALL_TIMER_TIMEOUT);
                }
            }

            @Override
            public void onError(int code, String error) {
                EMLog.e(TAG, "Invite call error " + code + ", " + error);
                conversation.removeMessage(message.getMsgId());
                if(listener != null){
                    listener.onCallError(EaseCallKit.EaseCallError.IM_ERROR,code,error);
                }
                if(event.callAction == EaseCallAction.CALL_CANCEL){
                    //退出频道
                    exitChannel();
                }
            }

            @Override
            public void onProgress(int progress, String status) {

            }
        });
        EMClient.getInstance().chatManager().sendMessage(message);
    }

    private boolean checkSelfPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, requestCode);
            return false;
        }
        return true;
    }

    /**
     * 设置用户信息回调
     * @param userName
     * @param uId
     */
    private void setUserJoinChannelInfo(String userName,int uId){
        if (listener != null) {
            listener.onRemoteUserJoinChannel(channelName, userName, uId, new EaseGetUserAccountCallback() {
                @Override
                public void onUserAccount(List<EaseUserAccount> userAccounts) {
                    if (userAccounts != null && userAccounts.size() > 0) {
                        for (EaseUserAccount account : userAccounts) {
                            if(account.getUid() != 0){
                                uIdMap.put(account.getUid(), account);
                            }
                            if(!TextUtils.equals(account.getUserName(), EMClient.getInstance().getCurrentUser())) {
                                updateUserInfo(account.getUid());
                            }else{
                                localMemberView.updateUserInfo();
                            }
                            runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        //删除占位符
                                        EaseCallMemberView placeView = placeholderList.remove(account.getUserName());
                                        if(placeView != null){
                                            callConferenceViewGroup.removeView(placeView);
                                        }
                                        //通知更新昵称头像
                                        if(TextUtils.equals(account.getUserName(), username)) {
                                           if(incomingCallView != null){
                                               incomingCallView.setInviteInfo(userName);
                                           }
                                        }
                                    }
                                });
                        }
                    }
                }

                @Override
                public void onSetUserAccountError(int error, String errorMsg) {
                    EMLog.e(TAG,"onRemoteUserJoinChannel error:" + error + "  errorMsg:" + errorMsg);
                }
            });
        }
    }

    private void resetVideoView() {
        if(mUidsList != null && !mUidsList.isEmpty()) {
            Iterator<Map.Entry<Integer, EaseCallMemberView>> iterator = mUidsList.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Integer, EaseCallMemberView> entry = iterator.next();
                Integer uid = entry.getKey();
                EaseCallMemberView memberView = entry.getValue();
                if(uIdMap.keySet().contains(uid)) {
                    memberView.setUserInfo(uIdMap.get(uid));
                }
                if(uid != 0) {
                    callConferenceViewGroup.addView(memberView);
                    mRtcEngine.setupRemoteVideo(new VideoCanvas(memberView.getSurfaceView(), VideoCanvas.RENDER_MODE_HIDDEN, uid));
                }else {
                    localMemberView = memberView;
                    callConferenceViewGroup.addView(memberView, 0);
                    mRtcEngine.setupLocalVideo(new VideoCanvas(memberView.getSurfaceView(), VideoCanvas.RENDER_MODE_HIDDEN, uid));
                }
            }
        }
        if(localMemberView != null) {
            changeCameraDirect(localMemberView.isCameraDirectionFront());
            changeVideoState(localMemberView.isVideoOff());
            changeSpeakerState(localMemberView.isSpeakActivated());
            changeMuteState(localMemberView.isAudioOff());
        }
    }

    private void updateUserInfo(int uid){
        //更新本地头像昵称
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(mUidsList != null){
                    EaseCallMemberView memberView = mUidsList.get(uid);
                    if(memberView != null){
                        if(memberView.getUserInfo() != null){
                            memberView.updateUserInfo();
                        }else{
                            EaseUserAccount account = uIdMap.get(uid);
                            UserInfo info = new UserInfo();
                            info.userAccount = account.getUserName();
                            info.uid = account.getUid();
                            memberView.setUserInfo(info);
                        }
                    }
                }
            }
        });
    }


    private void playRing(){
        if(ringFile != null){
            mediaPlayer = new MediaPlayer();
            try {
                mediaPlayer.setDataSource(ringFile);
                if (!mediaPlayer.isPlaying()){
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                }
            } catch (IOException e) {
                mediaPlayer = null;
            }
        }else{
            ringtone.play();
        }
    }

    private void stopPlayRing(){
        if(ringFile != null){
            if(mediaPlayer != null){
                mediaPlayer.stop();
                mediaPlayer = null;
            }
        }else{
            if(ringtone != null){
                ringtone.stop();
            }
        }
    }


    /**
     * 退出频道
     */
    void exitChannel(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                EMLog.i(TAG, "exit channel channelName: " + channelName);
                if(isInComingCall){
                    stopPlayRing();
                    EMLog.i(TAG, "exit channel stopPlayRing " + channelName);
                }else{
                    if(inViteUserMap.size() > 0){
                        if(timehandler != null){
                            timehandler.stopTime();
                        }

                        Iterator<String> it_user = inViteUserMap.keySet().iterator();
                        while(it_user.hasNext()){
                            String userName = it_user.next();
                            //发送取消事件
                            CallCancelEvent cancelEvent = new CallCancelEvent();
                            sendCmdMsg(cancelEvent,userName);
                            it_user.remove();
                            EaseCallMemberView memberView = placeholderList.remove(userName);
                            if(memberView != null){
                                callConferenceViewGroup.removeView(memberView);
                            }
                        }
                    }
                }
                if(isFloatWindowShowing()){
                    EaseCallFloatWindow.getInstance().dismiss();
                }

                //重置状态
                EaseCallKit.getInstance().setCallState(EaseCallState.CALL_IDLE);
                EaseCallKit.getInstance().setCallID(null);

                finish();
            }
        });
    }

    /**
     * 显示悬浮窗
     */
    @Override
    public void doShowFloatWindow() {
        super.doShowFloatWindow();
        if(timeUpdataTimer != null) {
            Log.e(TAG, "timeUpdataTimer cost seconds: "+timeUpdataTimer.timePassed);
            EaseCallFloatWindow.getInstance().setCostSeconds(timeUpdataTimer.timePassed);
        }
        EaseCallFloatWindow.getInstance().show();
        setConferenceInfoAfterShowFloat();
        int uid = 0;
        if (mUidsList.size() > 0) { // 如果会议中有其他成员,则显示第一个成员
            Set<Integer> uidSet = mUidsList.keySet();
            for (int id : uidSet) {
                uid = id;
            }
            EaseCallMemberView memberView = mUidsList.get(uid);
            EaseCallFloatWindow.getInstance().update(memberView);
        }
        moveTaskToBack(false);
    }

    private void setConferenceInfoAfterShowFloat() {
        EaseCallFloatWindow.ConferenceInfo info = new EaseCallFloatWindow.ConferenceInfo();
        info.uidToUserAccountMap = uIdMap;
        info.uidToViewList = getViewStateMap();
        info.userAccountToUidMap = userAccountList;
        EaseCallFloatWindow.getInstance().setConferenceInfo(info);
    }

    private Map<Integer, EaseCallFloatWindow.ConferenceInfo.ViewState> getViewStateMap() {
        if(mUidsList == null || mUidsList.isEmpty()) {
            return new HashMap<>();
        }
        Map<Integer, EaseCallFloatWindow.ConferenceInfo.ViewState> viewStateMap = new HashMap<>();
        Iterator<Map.Entry<Integer, EaseCallMemberView>> iterator = mUidsList.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, EaseCallMemberView> entry = iterator.next();
            Integer key = entry.getKey();
            EaseCallMemberView value = entry.getValue();
            EaseCallFloatWindow.ConferenceInfo.ViewState viewState = new EaseCallFloatWindow.ConferenceInfo.ViewState();
            if(value != null) {
                viewState.isAudioOff = value.getAudioOff();
                viewState.isCameraFront = value.isCameraDirectionFront();
                viewState.isFullScreenMode = value.isFullScreen();
                viewState.isVideoOff = value.isVideoOff();
                viewState.speakActivated = value.isSpeakActivated();
            }
            viewStateMap.put(key, viewState);
        }
        return viewStateMap;
    }

    /**
     * 更新悬浮窗
     * @param memberView
     */
    private void updateFloatWindow(EaseCallMemberView memberView) {
        if(memberView != null){
            EaseCallFloatWindow.getInstance().update(memberView);
        }
    }


    @Override
    protected void onNewIntent(Intent intent) {
        EMLog.d(TAG,"TEST onNewIntent");
        super.onNewIntent(intent);
        checkConference(false);
    }

    private void checkConference(boolean isNew) {
        ArrayList<String> users = EaseCallKit.getInstance().getInviteeUsers();
        if(users != null && users.size()> 0){
            handler.sendEmptyMessage(EaseMsgUtils.MSG_MAKE_CONFERENCE_VIDEO);
        }

        if(isFloatWindowShowing()){
            int uId = EaseCallFloatWindow.getInstance().getUid();
            if(uId != -1){
                EaseCallMemberView memberView = mUidsList.get(uId);
                if(memberView != null){
                    if(uId == 0){
                        mRtcEngine.setupLocalVideo(new VideoCanvas(memberView.getSurfaceView(), VideoCanvas.RENDER_MODE_HIDDEN, uId));
                    }else{
                        mRtcEngine.setupRemoteVideo(new VideoCanvas(memberView.getSurfaceView(), VideoCanvas.RENDER_MODE_HIDDEN, uId));
                    }
                }
            }
            if(isNew) {
                EaseCallFloatWindow.ConferenceInfo info = EaseCallFloatWindow.getInstance().getConferenceInfo();
                if(info != null) {
                    resetConferenceData(info);
                    resetVideoView();
                }
            }
            // 防止activity在后台被start至前台导致window还存在
            long costSeconds = EaseCallFloatWindow.getInstance().getTotalCostSeconds();
            Log.e(TAG, "costSeconds: "+costSeconds);
            if(timeUpdataTimer != null) {
                timeUpdataTimer.timePassed = (int) costSeconds;
                updateTime(timeUpdataTimer);
            }
            EaseCallFloatWindow.getInstance().dismiss();
        }
    }

    private void resetConferenceData(EaseCallFloatWindow.ConferenceInfo info) {
        if(info != null) {
            if(info.uidToUserAccountMap != null) {
                this.uIdMap.putAll(info.uidToUserAccountMap);
            }
            if(info.userAccountToUidMap != null) {
                this.userAccountList.putAll(info.userAccountToUidMap);
            }
            if(info.uidToViewList != null) {
                Map<Integer, EaseCallMemberView> callViewMap = createCallViewMap(info.uidToViewList);
                this.mUidsList.putAll(callViewMap);
                info.uidToViewList.clear();
            }
        }
    }
    
    private Map<Integer, EaseCallMemberView> createCallViewMap(Map<Integer, EaseCallFloatWindow.ConferenceInfo.ViewState> viewStateMap) {
        Map<Integer, EaseCallMemberView> memberViewMap = new HashMap<>();
        if(viewStateMap == null || viewStateMap.isEmpty()) {
            return memberViewMap;
        }
        Iterator<Map.Entry<Integer, EaseCallFloatWindow.ConferenceInfo.ViewState>> iterator = viewStateMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, EaseCallFloatWindow.ConferenceInfo.ViewState> entry = iterator.next();
            Integer uid = entry.getKey();
            EaseCallFloatWindow.ConferenceInfo.ViewState state = entry.getValue();
            EaseCallMemberView memberView = createCallMemberView();
            memberView.setCameraDirectionFront(state.isCameraFront);
            memberView.setAudioOff(state.isAudioOff);
            memberView.setVideoOff(state.isVideoOff);
            memberView.setSpeakActivated(state.speakActivated);
            memberView.setFullScreen(state.isFullScreenMode);
            memberViewMap.put(uid, memberView);
        }
        return memberViewMap;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        EMLog.i(TAG, "onActivityResult: " + requestCode + ", result code: " + resultCode);
        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestOverlayPermission = false;
            // Result of window permission request, resultCode = RESULT_CANCELED
            if (Settings.canDrawOverlays(this)) {
                doShowFloatWindow();
            } else {
                Toast.makeText(this, getString(R.string.alert_window_permission_denied), Toast.LENGTH_SHORT).show();
            }
            return;
        }
    }


    /**
     * 停止事件循环
     */
    protected void releaseHandler() {
        handler.sendEmptyMessage(EaseMsgUtils.MSG_RELEASE_HANDLER);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        callConferenceViewGroup.removeAllViews();
        releaseHandler();
        if(timehandler != null){
            timehandler.stopTime();
        }
        if(timeUpdataTimer != null){
            timeUpdataTimer.stopTime();
        }
        if(mUidsList != null){
            mUidsList.clear();
        }
        if(!isFloatWindowShowing()) {
            if(userInfoList != null){
                userInfoList.clear();
            }
            if(userAccountList != null){
                userAccountList.clear();
            }
            if(uIdMap != null){
                uIdMap.clear();
            }
            EaseCallKit.getInstance().releaseCall();
            leaveChannel();
            RtcEngine.destroy();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 是否触发按键为back键
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onBackPressed();
            return true;
        }else{
            // 如果不是back键正常响应
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    protected void onUserLeaveHint() {
        Log.d(TAG,"onUserLeaveHint");
        super.onUserLeaveHint();
    }


    @Override
    public void onBackPressed() {
        exitChannelDisplay();
    }


    /**
     * 是否退出当前通话提示框
     */
    public void exitChannelDisplay() {
        AlertDialog.Builder builder = new AlertDialog.Builder(EaseMultipleVideoActivity.this);
        final AlertDialog dialog = builder.create();
        View dialogView = View.inflate(EaseMultipleVideoActivity.this, R.layout.activity_exit_channel, null);
        dialog.setView(dialogView);

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        WindowManager.LayoutParams wmlp = dialog.getWindow().getAttributes();
        wmlp.gravity = Gravity.CENTER | Gravity.CENTER;
        dialog.show();

        final Button btn_ok = dialogView.findViewById(R.id.btn_ok);
        final Button btn_cancel = dialogView.findViewById(R.id.btn_cancel);
        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                EMLog.e(TAG, "exitChannelDisplay  exit channel:");
                exitChannel();
            }
        });

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                EMLog.e(TAG, "exitChannelDisplay not exit channel");
            }
        });
    }
};