package easemob.hyphenate.calluikit.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
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
import com.hyphenate.util.EMLog;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import easemob.hyphenate.calluikit.EaseCallUIKit;
import easemob.hyphenate.calluikit.R;
import easemob.hyphenate.calluikit.base.EaseCallKitConfig;
import easemob.hyphenate.calluikit.base.EaseCallMemberView;
import easemob.hyphenate.calluikit.base.EaseCallMemberViewGroup;
import easemob.hyphenate.calluikit.event.AlertEvent;
import easemob.hyphenate.calluikit.event.AnswerEvent;
import easemob.hyphenate.calluikit.event.BaseEvent;
import easemob.hyphenate.calluikit.event.CallCancelEvent;
import easemob.hyphenate.calluikit.event.ConfirmCallEvent;
import easemob.hyphenate.calluikit.event.ConfirmRingEvent;
import easemob.hyphenate.calluikit.livedatas.EaseLiveDataBus;
import easemob.hyphenate.calluikit.utils.EaseCallAction;
import easemob.hyphenate.calluikit.base.EaseCallEndReason;
import easemob.hyphenate.calluikit.base.EaseCallKitListener;
import easemob.hyphenate.calluikit.base.EaseCallType;
import easemob.hyphenate.calluikit.utils.EaseCallState;
import easemob.hyphenate.calluikit.utils.EaseMsgUtils;
import easemob.hyphenate.calluikit.utils.EaseCallKitUtils;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.models.UserInfo;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration;

import static easemob.hyphenate.calluikit.utils.EaseMsgUtils.CALL_TIMER_CALL_TIME;
import static easemob.hyphenate.calluikit.utils.EaseMsgUtils.CALL_TIMER_TIMEOUT;
import static io.agora.rtc.Constants.*;



/**
 * author lijian
 * email: Allenlee@easemob.com
 * date: 01/15/2021
 */
public class EaseMultipleVideoActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = EaseVideoCallActivity.class.getSimpleName();

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
    private ImageView scaleModeBtn;


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

    private static final int PERMISSION_REQ_ID = 22;
    private static final String[] REQUESTED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private final HashMap<Integer, EaseCallMemberView> mUidsList = new HashMap<>();
    private final HashMap<Integer, UserInfo> userInfoList = new HashMap<>();
    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onError(int err) {
            super.onError(err);
            EMLog.d(TAG,"IRtcEngineEventHandler onError:" + err);
            EaseCallKitListener listener = EaseCallUIKit.getInstance().getCallListener();
            if(listener != null){
                listener.onCallError(EaseCallUIKit.EaseCallError.RTC_ERROR,err,"rtc error");
            }
        }

        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            EMLog.d(TAG,"onJoinChannelSuccess channel:"+ channel + " uid" +uid);
            //加入频道开始计时
            timeUpdataTimer.startTime(CALL_TIMER_CALL_TIME);
            if(!isInComingCall){
                ArrayList<String> userList = EaseCallUIKit.getInstance().getInviteeUsers();
                if(userList != null && userList.size() > 0){
                    handler.sendEmptyMessage(EaseMsgUtils.MSG_MAKE_CONFERENCE_VIDEO);

                    //邀请人就变为主叫
                    isInComingCall = false;
                    //开始定时器
                    timehandler.startTime(CALL_TIMER_TIMEOUT);
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
                    if (mUidsList.containsKey(uid)) {
                        EaseCallMemberView memberView = mUidsList.get(uid);
                        if (memberView != null) {
                            memberView.setUserInfo(userInfo);
                        }
                    }else{
                        final EaseCallMemberView memberView = new EaseCallMemberView(getApplicationContext());
                        memberView.setUserInfo(userInfo);
                        callConferenceViewGroup.addView(memberView);
                        mUidsList.put(uid, memberView);
                    }
                }
            });
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            super.onUserJoined(uid, elapsed);
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
                        }
                        if(memberView != null){
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
                        memberView.addSurfaceView(surfaceView);
                        callConferenceViewGroup.addView(memberView);
                        memberView.setVideoOff(false);
                        mUidsList.put(uid, memberView);
                        surfaceView.setZOrderOnTop(false);
                        surfaceView.setZOrderMediaOverlay(false);
                        mRtcEngine.setupRemoteVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid));
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
                    }else {
                        final EaseCallMemberView memberView = new EaseCallMemberView(getApplicationContext());
                        if(userInfoList.containsKey(uid)){
                            memberView.setUserInfo(userInfoList.get(uid));
                        }
                        memberView.setAudioOff(false);
                        callConferenceViewGroup.addView(memberView);
                        mUidsList.put(uid, memberView);
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
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ease_multiple_video);
        //初始化
        if(savedInstanceState == null){
            initParms(getIntent().getExtras());
        }else{
            initParms(savedInstanceState);
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
    }


    private void initView(){
        incomingCallView = (EaseCommingCallView)findViewById(R.id.incoming_call_view);
        viewGroupLayout = findViewById(R.id.viewGroupLayout);

        int width = viewGroupLayout.getLayoutParams().width;
        int height = viewGroupLayout.getLayoutParams().height;
        callConferenceViewGroup = (EaseCallMemberViewGroup)findViewById(R.id.surface_view_group);
        inviteBtn = (ImageView)findViewById(R.id.btn_invite);
        callTimeView = (TextView)findViewById(R.id.tv_call_time);
        micSwitch = (ImageButton) findViewById(R.id.btn_mic_switch);
        cameraSwitch = (ImageButton) findViewById(R.id.btn_camera_switch);
        speakerSwitch = (ImageButton) findViewById(R.id.btn_speaker_switch);
        changeCameraSwitch = (ImageButton)findViewById(R.id.btn_change_camera_switch);
        hangupBtn = (ImageButton)findViewById(R.id.btn_hangup);
        scaleModeBtn = (ImageView)findViewById(R.id.btn_float);
        incomingCallView.setOnActionListener(onActionListener);
        callConferenceViewGroup.setOnItemClickListener(onItemClickListener);
        callConferenceViewGroup.setOnScreenModeChangeListener(onScreenModeChangeListener);
        inviteBtn.setOnClickListener(this);
        micSwitch.setOnClickListener(this);
        speakerSwitch.setOnClickListener(this);
        cameraSwitch.setOnClickListener(this);
        changeCameraSwitch.setOnClickListener(this);
        hangupBtn.setOnClickListener(this);
        scaleModeBtn.setOnClickListener(this);
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
            incomingCallView.setVisibility(View.VISIBLE);

        }else{
            incomingCallView.setVisibility(View.GONE);

            //主叫加入频道
            initEngineAndJoinChannel();
        }
    }

    private void initParms(Bundle bundle){
        if(bundle != null) {
            isInComingCall = bundle.getBoolean("isComingCall", false);
            username = bundle.getString("username");
            channelName = bundle.getString("channelName");
            callType = EaseCallUIKit.getInstance().getCallType();
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
            String appId = getString(R.string.agora_app_id);
            mRtcEngine = RtcEngine.create(getBaseContext(), appId, mRtcEventHandler);
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
//        for(int i = 0; i < 3;i++){
//            EaseCallMemberView localMemberView = new EaseCallMemberView(getApplicationContext());
//            callConferenceViewGroup.addView(localMemberView);
//        }
        mUidsList.put(0, localMemberView);
        surfaceView.setZOrderOnTop(false);
        surfaceView.setZOrderMediaOverlay(false);
        mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0));
    }


    /**
     * 加入频道
     */
    private void joinChannel() {
        String token = null;
//        if(EMClient.getInstance().getCurrentUser().equals("lijian66")){
//            token = EaseMsgUtils.lijian66_Token;
//        }else if(EMClient.getInstance().getCurrentUser().equals("lijian88")){
//            token = EaseMsgUtils.lijian88_Token;
//        }
//        if (TextUtils.isEmpty(token)) {
//            EMLog.e(TAG,"token is null");
//            token = null;
//            exitChannel();
//        }
        mRtcEngine.joinChannelWithUserAccount(null, channelName,  EMClient.getInstance().getCurrentUser());
        //handler.sendEmptyMessage(EaseMsgUtils.MSG_REQUEST_TOKEN);
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
            exitChannel();

            EaseCallKitListener listener = EaseCallUIKit.getInstance().getCallListener();
            if(listener != null){
                long time = timeUpdataTimer.timePassed;
                listener.onEndCallWithReason(callType,channelName, EaseCallEndReason.EaseCallEndReasonHangup,timeUpdataTimer.timePassed*1000);
            }
        }else if(view.getId() == R.id.btn_invite){

            EaseCallKitListener listener = EaseCallUIKit.getInstance().getCallListener();
            if(listener != null){
                Set<Integer> userset = mUidsList.keySet();
                int size = userset.size();
                if(size > 0){
                    String users[] = new String[size];
                    int i = 0;
                    for(Integer user:userset){
                        if(mUidsList.get(user) != null){
                            users[i++] = mUidsList.get(user).getUserAccount();
                        }
                    }
                    listener.onInviteUsers(getApplicationContext(),users);
                }else{
                    listener.onInviteUsers(getApplicationContext(),null);
                }
            }
        }
    }


    /**
     * 增加LiveData监听
     */
    protected void addLiveDataObserver(){
        EaseLiveDataBus.get().with(EaseCallType.SIGNAL_VIDEO_CALL.toString(), BaseEvent.class).observe(this, event -> {
            if(event != null) {
                switch (event.callAction){
                    case CALL_ALERT:
                        AlertEvent alertEvent = (AlertEvent)event;
                        //判断会话是否有效
                        ConfirmRingEvent ringEvent = new ConfirmRingEvent();
                        if(alertEvent.callId.equals
                                (EaseCallUIKit.getInstance().getCallID())){
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
                            EaseCallUIKit.getInstance().setCallState(EaseCallState.CALL_ANSWERED);
                            sendCmdMsg(callEvent,answerEvent.userId);
                        }else if(answerEvent.result.equals(
                                EaseMsgUtils.CALL_ANSWER_REFUSE)){
                            sendCmdMsg(callEvent,answerEvent.userId);
                        }
                        break;
                    case CALL_CONFIRM_RING:
                        break;
                    case CALL_CONFIRM_CALLEE:
                        ConfirmCallEvent confirmEvent = (ConfirmCallEvent)event;
                        String deviceId = confirmEvent.calleeDevId;
                        timehandler.stopTime();
                        //收到的仲裁为自己设备
                        if(deviceId.equals(EaseCallUIKit.deviceId)){
                            String result = confirmEvent.result;
                            //收到的仲裁为接听
                            if(result.equals(EaseMsgUtils.CALL_ANSWER_ACCEPT)){
                                //加入频道
                                initEngineAndJoinChannel();

                            }else if(result.equals(EaseMsgUtils.CALL_ANSWER_REFUSE)){
                                //退出通话
                                exitChannel();
                                EaseCallKitListener listener = EaseCallUIKit.getInstance().getCallListener();
                                if(listener != null){
                                    listener.onEndCallWithReason(callType,channelName, EaseCallEndReason.EaseCallEndReasonRefuse,0);
                                }
                            }
                        }else{
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //提示已在其他设备处理
                                    String info = getString(R.string.The_other_is_recived);
                                    Toast.makeText(getApplicationContext(),info , Toast.LENGTH_SHORT).show();
                                    //退出通话
                                    exitChannel();
                                    EaseCallKitListener listener = EaseCallUIKit.getInstance().getCallListener();
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
                event.callId = EaseCallUIKit.getInstance().getCallID();
                event.callerDevId = EaseCallUIKit.getInstance().getClallee_devId();
                event.calleeDevId = EaseCallUIKit.deviceId;
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
                event.callId = EaseCallUIKit.getInstance().getCallID();
                event.callerDevId = EaseCallUIKit.getInstance().getClallee_devId();
                event.calleeDevId = EaseCallUIKit.deviceId;
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
                    Set<String> userSet = inViteUserMap.keySet();
                    for(String userName: userSet){
                        //判断当前时间是否超时
                        if(totalMilliSeconds >= inViteUserMap.get(userName)){
                            //发送取消事件
                            CallCancelEvent cancelEvent = new CallCancelEvent();
                            sendCmdMsg(cancelEvent,userName);
                            inViteUserMap.remove(userName);
                        }
                    }
                    if(inViteUserMap.size() == 0){
                        timehandler.stopTime();
                    }
                }else{
                    long intervalTime;
                    EaseCallKitConfig callKitConfig = EaseCallUIKit.getInstance().getCallKitConfig();
                    if(callKitConfig != null){
                        intervalTime = callKitConfig.getCallTimeOut();
                    }else{
                        intervalTime = EaseMsgUtils.CALL_INVITE_INTERVAL;
                    }
                    if(timePassed *1000 == intervalTime){
                        timehandler.stopTime();

                        //被叫等待仲裁消息超时
                        exitChannel();

                        EaseCallKitListener listener = EaseCallUIKit.getInstance().getCallListener();
                        if(listener != null){
                            //对方回复超时
                            listener.onEndCallWithReason(callType,channelName, EaseCallEndReason.EaseCallEndReasonRemoteNoResponse,0);
                        }
                    }
                }

//                if(timePassed *1000 == EaseMsgUtils.CALL_INVITE_INTERVAL){
//                    //呼叫超时
//                    timehandler.stopTime();
//                    if(!isInComingCall){
//                        CallCancelEvent cancelEvent = new CallCancelEvent();
//                        sendCmdMsg(cancelEvent,username);
//                    } else{
//                        //被叫等待仲裁消息超时
//                        exitChannel();
//
//                        //对方回复超时
//                        EaseCallUIKit.getInstance().onEndCallWithReason(callType,channelName, EaseCallEndReason.EaseCallEndReasonRemoteNoResponse,0);
//                    }
//                }
                sendEmptyMessageDelayed(CALL_TIMER_TIMEOUT, 1000);

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
                    ArrayList<String> sendInviteeMsg = EaseCallUIKit.getInstance().getInviteeUsers();
                    sendInviteeMsg(sendInviteeMsg, EaseCallType.CONFERENCE_CALL);
                    break;
                case 301: //停止事件循环线程
                    //防止内存泄漏
                    handler.removeMessages(100);
                    handler.removeMessages(101);
                    handler.removeMessages(102);
                    callHandlerThread.quit();
                    break;
                case 400: //请求token
                    tokenUrl = EaseMsgUtils.TOKEN_SERVER;
                    tokenUrl += EaseMsgUtils.APPCERT;
                    tokenUrl += EaseMsgUtils.ADDAMARK;
                    tokenUrl += getString(R.string.agora_app_cert);
                    tokenUrl += EaseMsgUtils.APPKEY;
                    tokenUrl += EaseMsgUtils.ADDAMARK;
                    tokenUrl += getString(R.string.agora_app_id);
                    tokenUrl += EaseMsgUtils.CHANNEL;
                    tokenUrl += EaseMsgUtils.ADDAMARK;
                    tokenUrl += channelName;
                    tokenUrl += EaseMsgUtils.USERID;
                    tokenUrl += EaseMsgUtils.ADDAMARK;
                    tokenUrl += EMClient.getInstance().getCurrentUser();
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
        for(String username:userArray){

            //放入超时时间
            long totalMilliSeconds = System.currentTimeMillis();
            long intervalTime;
            EaseCallKitConfig callKitConfig = EaseCallUIKit.getInstance().getCallKitConfig();
            if(callKitConfig != null){
                intervalTime = callKitConfig.getCallTimeOut();
            }else{
                intervalTime = EaseMsgUtils.CALL_INVITE_INTERVAL;
            }
            totalMilliSeconds  +=  intervalTime;

            //放进userMap里面
            inViteUserMap.put(username,totalMilliSeconds);

            final EMMessage message = EMMessage.createTxtSendMessage( "邀请您进行多人音视频通话", username);
            message.setAttribute(EaseMsgUtils.CALL_ACTION, EaseCallAction.CALL_INVITE.state);
            message.setAttribute(EaseMsgUtils.CALL_CHANNELNAME, channelName);
            message.setAttribute(EaseMsgUtils.CALL_TYPE,callType.code);
            message.setAttribute(EaseMsgUtils.CALL_DEVICE_ID, EaseCallUIKit.deviceId);

            if(EaseCallUIKit.getInstance().getCallID() == null){
                EaseCallUIKit.getInstance().setCallID(EaseCallKitUtils.getRandomString(10));
            }
            message.setAttribute(EaseMsgUtils.CLL_ID,EaseCallUIKit.getInstance().getCallID());

            message.setAttribute(EaseMsgUtils.CLL_TIMESTRAMEP, System.currentTimeMillis());
            message.setAttribute(EaseMsgUtils.CALL_MSG_TYPE, EaseMsgUtils.CALL_MSG_INFO);

            final EMConversation conversation = EMClient.getInstance().chatManager().getConversation(username, EMConversation.EMConversationType.Chat, true);
            message.setMessageStatusCallback(new EMCallBack() {
                @Override
                public void onSuccess() {
                    EMLog.d(TAG, "Invite call success username:" + username);
                    conversation.removeMessage(message.getMsgId());
                }

                @Override
                public void onError(int code, String error) {
                    EMLog.e(TAG, "Invite call error " + code + ", " + error + " username:" + username);
                    conversation.removeMessage(message.getMsgId());

                    EaseCallKitListener listener = EaseCallUIKit.getInstance().getCallListener();
                    if(listener != null){
                        listener.onCallError(EaseCallUIKit.EaseCallError.IM_ERROR,code,error);
                    }
                }

                @Override
                public void onProgress(int progress, String status) {

                }
            });
            EMClient.getInstance().chatManager().sendMessage(message);
        }

        //初始化邀请列表
        EaseCallUIKit.getInstance().InitInviteeUsers();
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

        message.setAttribute(EaseMsgUtils.CALL_ACTION, event.callAction.state);
        message.setAttribute(EaseMsgUtils.CALL_DEVICE_ID, EaseCallUIKit.deviceId);
        message.setAttribute(EaseMsgUtils.CLL_ID,EaseCallUIKit.getInstance().getCallID());
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

                EaseCallKitListener listener = EaseCallUIKit.getInstance().getCallListener();
                if(listener != null){
                    listener.onCallError(EaseCallUIKit.EaseCallError.IM_ERROR,code,error);
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
                }else{
                    if(inViteUserMap.size() > 0){
                        if(timehandler != null){
                            timehandler.stopTime();
                        }
                        Set<String> userSet = inViteUserMap.keySet();
                        for(String userName: userSet){
                            //发送取消事件
                            CallCancelEvent cancelEvent = new CallCancelEvent();
                            sendCmdMsg(cancelEvent,userName);
                            inViteUserMap.remove(userName);
                        }
                    }
                }
                finish();
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        ArrayList<String> users = EaseCallUIKit.getInstance().getInviteeUsers();
        if(users != null && users.size()> 0){
            handler.sendEmptyMessage(EaseMsgUtils.MSG_MAKE_CONFERENCE_VIDEO);
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
        leaveChannel();
        RtcEngine.destroy();


        //重置状态
        EaseCallUIKit.getInstance().setCallState(EaseCallState.CALL_IDEL);
        EaseCallUIKit.getInstance().setCallID(null);
        EaseCallUIKit.getInstance().setMultipleVideoActivity(null);
    }
};