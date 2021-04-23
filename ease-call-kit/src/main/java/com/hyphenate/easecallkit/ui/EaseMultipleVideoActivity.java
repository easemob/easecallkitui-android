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
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
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
public class EaseMultipleVideoActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = EaseVideoCallActivity.class.getSimpleName();

    private final int REQUEST_CODE_OVERLAY_PERMISSION = 1002;

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
    private EaseCallMemberView localMemberView;
    private Map<String, Long> inViteUserMap = new HashMap<>(); //用户定时map存储
    private List<Integer> uidList = new ArrayList<>();
    private String invite_ext;
    //用于防止多次打开请求悬浮框页面
    private boolean requestOverlayPermission;
    private String agoraAppId = null;

    private static final int PERMISSION_REQ_ID = 22;
    private static final String[] REQUESTED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private final HashMap<Integer, EaseCallMemberView> mUidsList = new HashMap<>();
    private final HashMap<Integer, UserInfo> userInfoList = new HashMap<>();
    private final HashMap<String, Integer> userAccountList = new HashMap<>();
    private final HashMap<String, EaseCallMemberView> placeholderList = new HashMap<>();

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
                        SurfaceView surfaceView = RtcEngine.CreateRendererView(getApplicationContext());
                        final EaseCallMemberView memberView = new EaseCallMemberView(getApplicationContext());
                        if(userInfoList.containsKey(uid)){
                            memberView.setUserInfo(userInfoList.get(uid));
                        }

                        //删除占位符
                        EaseCallMemberView placeView = placeholderList.remove(memberView.getUserAccount());
                        if(placeView != null){
                            callConferenceViewGroup.removeView(placeView);
                        }

                        memberView.addSurfaceView(surfaceView);

                        callConferenceViewGroup.addView(memberView);

                        memberView.setVideoOff(false);
                        mUidsList.put(uid, memberView);
                        surfaceView.setZOrderOnTop(false);
                        surfaceView.setZOrderMediaOverlay(false);
                        mRtcEngine.setupRemoteVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid));

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
                            EaseCallMemberView floatView = EaseCallFloatWindow.getInstance(getApplicationContext()).getCallMemberView();
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

                        Set<Integer> uidSet = mUidsList.keySet();
                        uidList.clear();
                        for (Integer uId : uidSet) {
                            uidList.add(new Integer(uId.intValue()));
                        }
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

        //设置小窗口悬浮类型
        EaseCallFloatWindow.getInstance(getApplicationContext()).setCallType(EaseCallType.CONFERENCE_CALL);
    }


    private void initView(){
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

            EaseCallFloatWindow.getInstance(getApplicationContext()).setRtcEngine(mRtcEngine);
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

    private void setupLocalVideo() {
        SurfaceView surfaceView = RtcEngine.CreateRendererView(getApplicationContext());
        localMemberView = new EaseCallMemberView(getApplicationContext());
        localMemberView.addSurfaceView(surfaceView);
        localMemberView.setVideoOff(true);
        UserInfo info = new UserInfo();
        info.userAccount = EMClient.getInstance().getCurrentUser();
        info.uid = 0;
        localMemberView.setUserInfo(info);
        callConferenceViewGroup.addView(localMemberView);
        setUserJoinChannelInfo(EMClient.getInstance().getCurrentUser(),0);
        mUidsList.put(0, localMemberView);
        surfaceView.setZOrderOnTop(false);
        surfaceView.setZOrderMediaOverlay(false);
        mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0));
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


    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(view.getId() == R.id.btn_mic_switch){
            if (isMuteState) {
                // resume voice transfer
                localMemberView.setAudioOff(false);
                mRtcEngine.muteLocalAudioStream(false);
                micSwitch.setBackground(getResources().getDrawable(R.drawable.audio_unmute));
                isMuteState = false;
            } else {
                // pause voice transfer
                localMemberView.setAudioOff(true);
                mRtcEngine.muteLocalAudioStream(true);
                micSwitch.setBackground(getResources().getDrawable(R.drawable.audio_mute));
                isMuteState = true;
            }
        }else if(view.getId() == R.id.btn_speaker_switch){
            if (speakerSwitch.isActivated()) {
                speakerSwitch.setActivated(false);
                speakerSwitch.setBackground(getResources().getDrawable(R.drawable.voice_off));
                closeSpeakerOn();
            }else{
                speakerSwitch.setActivated(true);
                speakerSwitch.setBackground(getResources().getDrawable(R.drawable.voice_on));
                openSpeakerOn();
            }
        }else if(view.getId() == R.id.btn_camera_switch){
            if (isVideoMute) {
                localMemberView.setVideoOff(false);
                cameraSwitch.setBackground(getResources().getDrawable(R.drawable.video_on));
                mRtcEngine.muteLocalVideoStream(false);
                isVideoMute = false;
            } else {
                localMemberView.setVideoOff(true);
                mRtcEngine.muteLocalVideoStream(true);
                cameraSwitch.setBackground(getResources().getDrawable(R.drawable.video_0ff));
                isVideoMute = true;
            }
        }else if(view.getId() == R.id.btn_change_camera_switch){
            if(mRtcEngine != null){
                mRtcEngine.switchCamera();
            }
        }else if(view.getId() == R.id.btn_hangup){
            if(listener != null){
                long time = timeUpdataTimer.timePassed;
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
                        if(alertEvent.callId.equals
                                (EaseCallKit.getInstance().getCallID())
                                && inViteUserMap.containsKey(user)){
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

                        if(answerEvent.result.equals(
                                EaseMsgUtils.CALL_ANSWER_BUSY)){
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
                        }else if(answerEvent.result.equals(
                                EaseMsgUtils.CALL_ANSWER_ACCEPT)){
                            //设置为接听
                            EaseCallKit.getInstance().setCallState(EaseCallState.CALL_ANSWERED);
                            sendCmdMsg(callEvent,answerEvent.userId);
                        }else if(answerEvent.result.equals(
                                EaseMsgUtils.CALL_ANSWER_REFUSE)){
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
                        if(deviceId.equals(EaseCallKit.deviceId)){
                            //收到的仲裁为接听
                            if(result.equals(EaseMsgUtils.CALL_ANSWER_ACCEPT)){
                                //加入频道
                                initEngineAndJoinChannel();

                            }else if(result.equals(EaseMsgUtils.CALL_ANSWER_REFUSE)){
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
                                    if(result.equals(EaseMsgUtils.CALL_ANSWER_ACCEPT)){
                                        //已经在其他设备接听
                                        info = getString(R.string.The_other_is_recived);

                                    }else if(result.equals(EaseMsgUtils.CALL_ANSWER_REFUSE)){
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
                    updateUserInfo(userAccountList.get(userInfo.getUserId()));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(username.equals(userInfo.getUserId()) && incomingCallView != null){
                                incomingCallView.setInviteInfo(username);
                            }
                        }
                    });
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
            timePassed = 0;
            sendEmptyMessageDelayed(timeType, 1000);
        }

        public void stopTime() {
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
                String time = dateFormat.format(timePassed * 1000);
                updateConferenceTime(time);
                sendEmptyMessageDelayed(CALL_TIMER_CALL_TIME, 1000);
            }
            super.handleMessage(msg);
        }
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

                final EMMessage message = EMMessage.createTxtSendMessage("邀请您进行多人音视频通话", username);
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
                            if(!account.getUserName().equals(EMClient.getInstance().getCurrentUser())){
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
                                        if(account.getUserName().equals(username)){
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
                if(EaseCallFloatWindow.getInstance(getApplicationContext()).isShowing()){
                    EaseCallFloatWindow.getInstance(getApplicationContext()).dismiss();
                }

                //重置状态
                EaseCallKit.getInstance().setCallState(EaseCallState.CALL_IDLE);
                EaseCallKit.getInstance().setCallID(null);
                EaseCallKit.getInstance().setMultipleVideoActivity(null);

                finish();
            }
        });
    }


    private void showFloatWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                doShowFloatWindow();
            } else { // To reqire the window permission.
                if(!requestOverlayPermission) {
                    try {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                        // Add this to open the management GUI specific to this app.
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION);
                        requestOverlayPermission = true;
                        // Handle the permission require result in #onActivityResult();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            doShowFloatWindow();
        }
    }

    /**
     * 显示悬浮窗
     */
    private void doShowFloatWindow() {
            EaseCallFloatWindow.getInstance(getApplicationContext()).show();
            int uid = 0;
            if (mUidsList.size() > 0) { // 如果会议中有其他成员,则显示第一个成员
                Set<Integer> uidSet = mUidsList.keySet();
                for (int id : uidSet) {
                    uid = id;
                }
                EaseCallMemberView memberView = mUidsList.get(uid);
                EaseCallFloatWindow.getInstance(getApplicationContext()).update(memberView);
            }
            EaseMultipleVideoActivity.this.moveTaskToBack(false);
    }

    /**
     * 更新悬浮窗
     * @param memberView
     */
    private void updateFloatWindow(EaseCallMemberView memberView) {
        if(memberView != null){
            EaseCallFloatWindow.getInstance(getApplicationContext()).update(memberView);
        }
    }


    @Override
    protected void onNewIntent(Intent intent) {
        EMLog.d(TAG,"TEST onNewIntent");
        super.onNewIntent(intent);
        ArrayList<String> users = EaseCallKit.getInstance().getInviteeUsers();
        if(users != null && users.size()> 0){
            handler.sendEmptyMessage(EaseMsgUtils.MSG_MAKE_CONFERENCE_VIDEO);
        }

        if(EaseCallFloatWindow.getInstance(getApplicationContext()).isShowing()){
            int uId = EaseCallFloatWindow.getInstance(getApplicationContext()).getUid();
            if(uId != -1){
                EaseCallMemberView memberView = mUidsList.get(uId);
                if(memberView != null){
                    SurfaceView surfaceView = RtcEngine.CreateRendererView(getApplicationContext());
                    memberView.addSurfaceView(surfaceView);
                    surfaceView.setZOrderOnTop(false);
                    surfaceView.setZOrderMediaOverlay(false);
                    if(uId == 0){
                        mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uId));
                    }else{
                        mRtcEngine.setupRemoteVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uId));
                    }
                }
                // 防止activity在后台被start至前台导致window还存在
                EaseCallFloatWindow.getInstance(getApplicationContext()).dismiss();
            }
        }
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
        if(userInfoList != null){
            userInfoList.clear();
        }
        if(userAccountList != null){
            userAccountList.clear();
        }
        if(uIdMap != null){
            uIdMap.clear();
        }
        leaveChannel();
        RtcEngine.destroy();
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