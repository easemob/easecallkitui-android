package easemob.hyphenate.calluikit.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import easemob.hyphenate.calluikit.EaseCallUIKit;
import easemob.hyphenate.calluikit.R;
import easemob.hyphenate.calluikit.base.EaseCallKitConfig;
import easemob.hyphenate.calluikit.event.*;
import easemob.hyphenate.calluikit.event.BaseEvent;
import easemob.hyphenate.calluikit.livedatas.EaseLiveDataBus;
import easemob.hyphenate.calluikit.utils.EaseCallAction;
import easemob.hyphenate.calluikit.base.EaseCallEndReason;
import easemob.hyphenate.calluikit.base.EaseCallKitListener;
import easemob.hyphenate.calluikit.base.EaseCallType;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hyphenate.EMCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMCmdMessageBody;
import com.hyphenate.chat.EMConferenceStream;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.cloud.HttpClientManager;
import com.hyphenate.exceptions.HyphenateException;
import com.hyphenate.util.EMLog;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import easemob.hyphenate.calluikit.utils.EaseCallState;
import easemob.hyphenate.calluikit.utils.EaseMsgUtils;
import easemob.hyphenate.calluikit.utils.EaseCallKitUtils;
import easemob.hyphenate.calluikit.widget.EaseImageView;
import easemob.hyphenate.calluikit.widget.MyChronometer;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.models.UserInfo;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration;

import static com.hyphenate.cloud.HttpClientManager.Method_GET;
import static io.agora.rtc.Constants.REMOTE_VIDEO_STATE_REASON_REMOTE_MUTED;
import static io.agora.rtc.Constants.REMOTE_VIDEO_STATE_STOPPED;


/**
 * author lijian
 * email: Allenlee@easemob.com
 * date: 01/11/2021
 */
public class EaseVideoCallActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = EaseVideoCallActivity.class.getSimpleName();

    private static final int PERMISSION_REQ_ID = 22;

    private static final String[] REQUESTED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private View rootView;
    private Group comingBtnContainer;
    private ImageButton refuseBtn;
    private ImageButton answerBtn;
    private ImageButton hangupBtn;
    private Group voiceContronlLayout;

    private Group groupHangUp;
    private Group groupUseInfo;
    private Group groupOngoingSettings;
    private TextView nickTextView;
    private EMConferenceStream oppositeStream;
    private boolean isMuteState = false;
    private boolean isHandsfreeState;
    private ImageView muteImage;
    private ImageView handsFreeImage;
    private ImageButton switchCameraBtn;
    private MyChronometer chronometer;
    private boolean surfaceStateChange = false;
    private EaseImageView avatarView;
    private TextView call_stateView;

    private Group videoCallingGroup;
    private Group voiceCallingGroup;
    private TextView tv_nick_voice;

    private Group videoCalledGroup;
    private Group voiceCalledGroup;

    private RelativeLayout video_transe_layout;
    private RelativeLayout video_transe_comming_layout;
    private ImageButton btn_voice_trans;
    private TextView tv_call_state_voice;
    private EaseImageView iv_avatar_voice;

    //判断是发起者还是被邀请
    protected boolean isInComingCall;
    protected String username;
    protected String channelName;

    protected AudioManager audioManager;
    protected Ringtone ringtone;

    private boolean mMuted = false;
    private boolean mCallEnd = false;
    volatile private boolean mConfirm_ring = false;
    private String tokenUrl;
    private int remoteUId = 0;
    private boolean changeFlag = true;
    boolean transVoice = false;
    private String headUrl = null;
    private Bitmap headBitMap;
    private String ringFile;
    private MediaPlayer mediaPlayer;


    // 视频通话画面显示控件，这里在新版中使用同一类型的控件，方便本地和远端视图切换
    protected RelativeLayout localSurface_layout;
    protected RelativeLayout oppositeSurface_layout;
    private VideoCanvas mLocalVideo;
    private VideoCanvas mRemoteVideo;
    private EaseCallType callType;
    private View Voice_View;
    private TimeHandler timehandler;

    private RtcEngine mRtcEngine;
    private boolean isMuteVideo = false;

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

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(!isInComingCall){
                        //发送邀请信息
                        if(EaseCallUIKit.getInstance().getCallType() == EaseCallType.SIGNAL_VIDEO_CALL){
                            handler.sendEmptyMessage(EaseMsgUtils.MSG_MAKE_SIGNAL_VIDEO);
                        }else{
                            handler.sendEmptyMessage(EaseMsgUtils.MSG_MAKE_SIGNAL_VOICE);
                        }

                        isHandsfreeState = true;
                        openSpeakerOn();
                        handsFreeImage.setImageResource(R.drawable.em_icon_speaker_on);

                        //开始定时器
                        timehandler.startTime();
                    }
                }
            });


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
            super.onUserInfoUpdated(uid, userInfo);
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            super.onUserJoined(uid, elapsed);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //检测到对方进来
                    makeOngoingStatus();
                }
            });
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //检测到对方退出 自己退出
                    exitChannel();

                    EaseCallKitListener listener = EaseCallUIKit.getInstance().getCallListener();
                    if(listener != null){
                        //对方挂断
                        long time = getChronometerSeconds(chronometer);
                        listener.onEndCallWithReason(callType,channelName, EaseCallEndReason.EaseCallEndReasonHangup,time * 1000);
                    }
                }
            });
        }


        @Override
        public void onFirstRemoteVideoDecoded(final int uid, int width, int height, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    remoteUId = uid;
                    if(callType == EaseCallType.SIGNAL_VIDEO_CALL){
                        setupRemoteVideo(uid);
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
                        remoteUId = uid;
                        chronometer.start();
                        handsFreeImage.setImageResource(R.drawable.em_icon_speaker_on);
                }
            });
        }

        @Override
        public void onRemoteVideoStateChanged(int uid, int state, int reason, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //对端停止视频
                    if(uid == remoteUId){
                        //远端转换为视频流
                        if(state == REMOTE_VIDEO_STATE_STOPPED || state == REMOTE_VIDEO_STATE_REASON_REMOTE_MUTED){
                            callType = EaseCallType.SIGNAL_VOICE_CALL;
                            EaseCallUIKit.getInstance().setCallType(EaseCallType.SIGNAL_VOICE_CALL);
                            changeVideoVoiceState();
                            if(mRtcEngine != null){
                                mRtcEngine.muteLocalVideoStream(true);
                                mRtcEngine.enableVideo();
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
        setContentView(R.layout.activity_ease_video_call);

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
        //EMLog.e(TAG,"token:" + EMClient.getInstance().getAccessToken());
    }

    private void initParms(Bundle bundle){
        if(bundle != null) {
            isInComingCall = bundle.getBoolean("isComingCall", false);
            username = bundle.getString("username");
            channelName = bundle.getString("channelName");
            callType = EaseCallUIKit.getInstance().getCallType();
        }
    }

    private void initView(){
        refuseBtn = findViewById(R.id.btn_refuse_call);
        answerBtn = findViewById(R.id.btn_answer_call);
        hangupBtn = findViewById(R.id.btn_hangup_call);
        voiceContronlLayout = findViewById(R.id.ll_voice_control);
        comingBtnContainer = findViewById(R.id.ll_coming_call);
        avatarView = findViewById(R.id.iv_avatar);
        iv_avatar_voice = findViewById(R.id.iv_avatar_voice);

        muteImage = (ImageView) findViewById(R.id.iv_mute);
        handsFreeImage = (ImageView) findViewById(R.id.iv_handsfree);
        switchCameraBtn = (ImageButton) findViewById(R.id.btn_switch_camera);

        //呼叫中页面
        videoCallingGroup = findViewById(R.id.ll_video_calling);
        voiceCallingGroup = findViewById(R.id.ll_voice_calling);

        video_transe_layout = findViewById(R.id.bnt_video_transe);
        video_transe_comming_layout = findViewById(R.id.bnt_video_transe_comming);
        tv_nick_voice = findViewById(R.id.tv_nick_voice);
        tv_call_state_voice = findViewById(R.id.tv_call_state_voice);

        headUrl = EaseCallKitUtils.getUserHeadImage(username);
        ringFile = EaseCallKitUtils.getRingFile();
        if(ringFile != null){

        }

        //加载头像图片
        loadHeadImage();

        if(callType == EaseCallType.SIGNAL_VIDEO_CALL){
            videoCallingGroup.setVisibility(View.VISIBLE);
            voiceCallingGroup.setVisibility(View.GONE);
            if(isInComingCall){
                video_transe_layout.setVisibility(View.GONE);
                video_transe_comming_layout.setVisibility(View.VISIBLE);
            }else{
                video_transe_layout.setVisibility(View.VISIBLE);
                video_transe_comming_layout.setVisibility(View.GONE);
            }
        }else{
            if(!isInComingCall){
                voiceContronlLayout.setVisibility(View.VISIBLE);
            }
            videoCallingGroup.setVisibility(View.GONE);
            video_transe_layout.setVisibility(View.GONE);
            video_transe_comming_layout.setVisibility(View.GONE);
            voiceCallingGroup.setVisibility(View.VISIBLE);
            hangupBtn.setVisibility(View.GONE);
            tv_nick_voice.setText(EaseCallKitUtils.getUserNickName(username));
        }

        video_transe_layout.setOnClickListener(this);
        video_transe_comming_layout.setOnClickListener(this);

        //通话中页面
        videoCalledGroup = findViewById(R.id.ll_video_called);
        voiceCalledGroup =findViewById(R.id.ll_voice_control);

        btn_voice_trans = findViewById(R.id.btn_voice_trans);
        btn_voice_trans.setOnClickListener(this);

        refuseBtn.setOnClickListener(this);
        answerBtn.setOnClickListener(this);
        hangupBtn.setOnClickListener(this);

        muteImage.setOnClickListener(this);
        handsFreeImage.setOnClickListener(this);
        switchCameraBtn.setOnClickListener(this);

        // local surfaceview
        localSurface_layout = (RelativeLayout) findViewById(R.id.local_surface_layout);
        // remote surfaceview
        oppositeSurface_layout = (RelativeLayout) findViewById(R.id.opposite_surface_layout);
        groupHangUp = findViewById(R.id.group_hang_up);
        groupUseInfo = findViewById(R.id.group_use_info);
        groupOngoingSettings = findViewById(R.id.group_ongoing_settings);
        nickTextView = (TextView) findViewById(R.id.tv_nick);
        chronometer = (MyChronometer) findViewById(R.id.chronometer);
        call_stateView = (TextView)findViewById(R.id.tv_call_state) ;

        nickTextView.setText(EaseCallKitUtils.getUserNickName(username));
        localSurface_layout.setOnClickListener(this);

        Voice_View = findViewById(R.id.view_ring);

        rootView = ((ViewGroup)getWindow().getDecorView().findViewById(android.R.id.content)).getChildAt(0);

        if(isInComingCall){
            call_stateView.setText("邀请你进行音视频通话");
            tv_call_state_voice.setText("邀请你进行音视频通话");
        }else{
            call_stateView.setText("正在等待对方接受邀请");
            tv_call_state_voice.setText("正在等待对方接受邀请");
        }

        //如果是语音通话
        if(callType == EaseCallType.SIGNAL_VOICE_CALL){
            rootView.setBackground(getResources().getDrawable(R.drawable.call_bg_voice));
            //sufaceview不可见
            localSurface_layout.setVisibility(View.GONE);
            oppositeSurface_layout.setVisibility(View.GONE);

            //语音通话UI可见
            Voice_View.setVisibility(View.VISIBLE);
            avatarView.setVisibility(View.VISIBLE);
        }else{
            avatarView.setVisibility(View.GONE);
        }

        audioManager = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
        if(!isInComingCall){
            //拨打电话状态
            makeCallStatus();

            //主叫加入频道
            initEngineAndJoinChannel();
        }else{
            //被呼叫状态
            makeComingStatus();

            //开始振铃
            Uri ringUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            audioManager.setMode(AudioManager.MODE_RINGTONE);
            audioManager.setSpeakerphoneOn(true);
            ringtone = RingtoneManager.getRingtone(this, ringUri);
            AudioManager am = (AudioManager)this.getApplication().getSystemService(Context.AUDIO_SERVICE);
            int ringerMode = am.getRingerMode();
            if(ringerMode == AudioManager.RINGER_MODE_NORMAL){
                playRing();
            }
        }
    }

    /**
     * 来电话的状态
     */
    private void makeComingStatus() {
        voiceContronlLayout.setVisibility(View.INVISIBLE);
        comingBtnContainer.setVisibility(View.VISIBLE);
        groupUseInfo.setVisibility(View.VISIBLE);
        if(callType == EaseCallType.SIGNAL_VIDEO_CALL){
            groupOngoingSettings.setVisibility(View.INVISIBLE);
            localSurface_layout.setVisibility(View.INVISIBLE);
        }else{
            avatarView.setVisibility(View.VISIBLE);
            nickTextView.setVisibility(View.VISIBLE);
        }
        groupHangUp.setVisibility(View.INVISIBLE);
        groupRequestLayout();
    }


    /**
     * 通话中的状态
     */
    private void makeOngoingStatus() {
        voiceContronlLayout.setVisibility(View.VISIBLE);
        comingBtnContainer.setVisibility(View.INVISIBLE);
        groupUseInfo.setVisibility(View.INVISIBLE);
        groupHangUp.setVisibility(View.VISIBLE);

        if(callType == EaseCallType.SIGNAL_VIDEO_CALL){
            groupOngoingSettings.setVisibility(View.VISIBLE);
            localSurface_layout.setVisibility(View.VISIBLE);
            videoCalledGroup.setVisibility(View.VISIBLE);
            voiceCalledGroup.setVisibility(View.INVISIBLE);
            hangupBtn.setVisibility(View.VISIBLE);
            videoCallingGroup.setVisibility(View.GONE);
            voiceCallingGroup.setVisibility(View.GONE);
        }else{
            groupOngoingSettings.setVisibility(View.VISIBLE);
            avatarView.setVisibility(View.VISIBLE);
            localSurface_layout.setVisibility(View.GONE);
            oppositeSurface_layout.setVisibility(View.GONE);
            nickTextView.setVisibility(View.VISIBLE);
            videoCalledGroup.setVisibility(View.INVISIBLE);
            voiceCalledGroup.setVisibility(View.VISIBLE);
            hangupBtn.setVisibility(View.VISIBLE);

            videoCallingGroup.setVisibility(View.GONE);
            voiceCallingGroup.setVisibility(View.VISIBLE);
            tv_nick_voice.setText(EaseCallKitUtils.getUserNickName(username));
            tv_call_state_voice.setText("通话中");
        }

        video_transe_layout.setVisibility(View.GONE);
        video_transe_comming_layout.setVisibility(View.GONE);
        groupRequestLayout();
    }

    /**
     * 拨打电话的状态
     */
    public void makeCallStatus() {
        if(!isInComingCall && callType == EaseCallType.SIGNAL_VOICE_CALL){
            voiceContronlLayout.setVisibility(View.VISIBLE);
        }else{
            voiceContronlLayout.setVisibility(View.INVISIBLE);
        }
        comingBtnContainer.setVisibility(View.INVISIBLE);
        groupUseInfo.setVisibility(View.VISIBLE);
        groupOngoingSettings.setVisibility(View.INVISIBLE);
        localSurface_layout.setVisibility(View.INVISIBLE);
        groupHangUp.setVisibility(View.VISIBLE);
        groupRequestLayout();
    }

    public void groupRequestLayout() {
        comingBtnContainer.requestLayout();
        //voiceContronlLayout.requestLayout();
        groupHangUp.requestLayout();
        groupUseInfo.requestLayout();
        groupOngoingSettings.requestLayout();
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
        if(EaseCallUIKit.getInstance().getCallType() == EaseCallType.SIGNAL_VIDEO_CALL){
            mRtcEngine.enableVideo();
            mRtcEngine.setVideoEncoderConfiguration(new VideoEncoderConfiguration(
                    VideoEncoderConfiguration.VD_1280x720,
                    VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                    VideoEncoderConfiguration.STANDARD_BITRATE,
                    VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT));
        }else{
            mRtcEngine.disableVideo();
        }
    }

    private void setupLocalVideo() {
        SurfaceView view = RtcEngine.CreateRendererView(getBaseContext());
        view.setZOrderMediaOverlay(true);
        localSurface_layout.addView(view);
        mLocalVideo = new VideoCanvas(view, VideoCanvas.RENDER_MODE_HIDDEN, 0);
        mRtcEngine.setupLocalVideo(mLocalVideo);
    }


    private void setupRemoteVideo(int uid) {
        SurfaceView view = RtcEngine.CreateRendererView(getBaseContext());
        oppositeSurface_layout.addView(view);
        mRemoteVideo = new VideoCanvas(view, VideoCanvas.RENDER_MODE_HIDDEN,uid);
        mRtcEngine.setupRemoteVideo(mRemoteVideo);

        //设置在通话中的状态
        // makeOngoingStatus();
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
        if(id == R.id.btn_refuse_call) {
            stopPlayRing();
            if(isInComingCall){
                chronometer.stop();

                //发送拒绝消息
                AnswerEvent event = new AnswerEvent();
                event.result = EaseMsgUtils.CALL_ANSWER_REFUSE;
                event.callId = EaseCallUIKit.getInstance().getCallID();
                event.callerDevId = EaseCallUIKit.getInstance().getClallee_devId();
                event.calleeDevId = EaseCallUIKit.deviceId;
                sendCmdMsg(event,username);
            }

        } else if (id == R.id.btn_answer_call) {
            if(isInComingCall){
                stopPlayRing();
                //发送接听消息
                AnswerEvent event = new AnswerEvent();
                event.result = EaseMsgUtils.CALL_ANSWER_ACCEPT;
                event.callId = EaseCallUIKit.getInstance().getCallID();
                event.callerDevId = EaseCallUIKit.getInstance().getClallee_devId();
                event.calleeDevId = EaseCallUIKit.deviceId;
                sendCmdMsg(event,username);
            }
        } else if (id == R.id.btn_hangup_call) {
            chronometer.stop();
            if(remoteUId == 0){
                CallCancelEvent cancelEvent = new CallCancelEvent();
                sendCmdMsg(cancelEvent,username);
            }else{
                exitChannel();

                EaseCallKitListener listener = EaseCallUIKit.getInstance().getCallListener();
                if(listener != null){
                    //通话结束原因挂断
                    long time = getChronometerSeconds(chronometer);
                    listener.onEndCallWithReason(callType,channelName, EaseCallEndReason.EaseCallEndReasonHangup,time *1000);
                }
            }
        } else if(id == R.id.local_surface_layout){
            changeSurface();
        } else if (id == R.id.iv_mute) { // mute
            if (isMuteState) {
                // resume voice transfer
                muteImage.setImageResource(R.drawable.call_mute_normal);
                mRtcEngine.muteLocalAudioStream(false);
                isMuteState = false;
            } else {
                // pause voice transfer
                muteImage.setImageResource(R.drawable.call_mute_on);
                mRtcEngine.muteLocalAudioStream(true);
                isMuteState = true;
            }
        } else if (id == R.id.iv_handsfree) { // handsfree
            if (isHandsfreeState) {
                handsFreeImage.setImageResource(R.drawable.em_icon_speaker_normal);
                closeSpeakerOn();
                isHandsfreeState = false;
            } else {
                handsFreeImage.setImageResource(R.drawable.em_icon_speaker_on);
                openSpeakerOn();
                isHandsfreeState = true;
            }
        }else if(id == R.id.btn_switch_camera){
            if(mRtcEngine != null){
                mRtcEngine.switchCamera();
            }
        }else if(id == R.id.btn_voice_trans){
            if(callType == EaseCallType.SIGNAL_VOICE_CALL){
                callType = EaseCallType.SIGNAL_VIDEO_CALL;
                EaseCallUIKit.getInstance().setCallType(EaseCallType.SIGNAL_VIDEO_CALL);
                changeVideoVoiceState();
                if(mRtcEngine != null){
                    mRtcEngine.muteLocalVideoStream(false);
                }
            }else{
                callType = EaseCallType.SIGNAL_VOICE_CALL;
                EaseCallUIKit.getInstance().setCallType(EaseCallType.SIGNAL_VOICE_CALL);
                changeVideoVoiceState();
                if(mRtcEngine != null){
                    mRtcEngine.muteLocalVideoStream(true);
                }
            }
        }else if(id == R.id.bnt_video_transe_comming || id == R.id.bnt_video_transe){
            //进入通话之前转音频
            callType = EaseCallType.SIGNAL_VOICE_CALL;
            EaseCallUIKit.getInstance().setCallType(EaseCallType.SIGNAL_VOICE_CALL);
            if(mRtcEngine != null){
                mRtcEngine.disableVideo();
                mRtcEngine.muteLocalVideoStream(true);
            }
            localSurface_layout.setVisibility(View.GONE);
            oppositeSurface_layout.setVisibility(View.GONE);
            rootView.setBackground(getResources().getDrawable(R.drawable.call_bg_voice));

            loadHeadImage();

            videoCallingGroup.setVisibility(View.GONE);
            video_transe_layout.setVisibility(View.GONE);
            video_transe_comming_layout.setVisibility(View.GONE);
            voiceCallingGroup.setVisibility(View.VISIBLE);
            tv_nick_voice.setText(EaseCallKitUtils.getUserNickName(username));
            if(!isInComingCall){
                voiceContronlLayout.setVisibility(View.VISIBLE);
            }
            if(isInComingCall){
                stopPlayRing();
                //发送接听消息
                AnswerEvent event = new AnswerEvent();
                event.result = EaseMsgUtils.CALL_ANSWER_ACCEPT;
                event.callId = EaseCallUIKit.getInstance().getCallID();
                event.callerDevId = EaseCallUIKit.getInstance().getClallee_devId();
                event.calleeDevId = EaseCallUIKit.deviceId;
                event.transVoice = true;
                sendCmdMsg(event,username);
            }else{
                //发送转音频信息
                VideoToVoiceeEvent event = new VideoToVoiceeEvent();
                sendCmdMsg(event,username);
            }
        }
    }

    private void changeSurface(){
        if(changeFlag){
            SurfaceView remoteview = RtcEngine.CreateRendererView(getBaseContext());
            localSurface_layout.removeAllViews();
            localSurface_layout.addView(remoteview);
            remoteview.setZOrderMediaOverlay(true);
            mRemoteVideo = new VideoCanvas(remoteview, VideoCanvas.RENDER_MODE_HIDDEN,remoteUId);
            mRtcEngine.setupRemoteVideo(mRemoteVideo);


            SurfaceView localview = RtcEngine.CreateRendererView(getBaseContext());
            oppositeSurface_layout.removeAllViews();
            oppositeSurface_layout.addView(localview);
            mLocalVideo = new VideoCanvas(localview, VideoCanvas.RENDER_MODE_HIDDEN, 0);
            mRtcEngine.setupLocalVideo(mLocalVideo);

            changeFlag = !changeFlag;

        }else{
            SurfaceView localview = RtcEngine.CreateRendererView(getBaseContext());
            localview.setZOrderMediaOverlay(true);
            localSurface_layout.removeAllViews();
            localSurface_layout.addView(localview);
            mLocalVideo = new VideoCanvas(localview, VideoCanvas.RENDER_MODE_HIDDEN, 0);
            mRtcEngine.setupLocalVideo(mLocalVideo);

            SurfaceView remoteview = RtcEngine.CreateRendererView(getBaseContext());
            oppositeSurface_layout.removeAllViews();
            oppositeSurface_layout.addView(remoteview);
            mRemoteVideo = new VideoCanvas(remoteview, VideoCanvas.RENDER_MODE_HIDDEN,remoteUId);
            mRtcEngine.setupRemoteVideo(mRemoteVideo);
            changeFlag = !changeFlag;
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

    private boolean checkSelfPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, requestCode);
            return false;
        }
        return true;
    }


    void changeVideoVoiceState(){
        if(callType == EaseCallType.SIGNAL_VIDEO_CALL){//切换到视频通话UI
            //语音通话UI可见
            Voice_View.setVisibility(View.GONE);
            avatarView.setVisibility(View.GONE);

            //sufaceview不可见
            localSurface_layout.setVisibility(View.VISIBLE);
            oppositeSurface_layout.setVisibility(View.VISIBLE);

            makeOngoingStatus();
        }else{ // 切换到音频通话UI
            localSurface_layout.setVisibility(View.GONE);
            oppositeSurface_layout.setVisibility(View.GONE);
            rootView.setBackground(getResources().getDrawable(R.drawable.call_bg_voice));

            //已经在通话中
            if(videoCalledGroup.getVisibility() == View.VISIBLE){
                //语音通话UI可见
                Voice_View.setVisibility(View.VISIBLE);
                avatarView.setVisibility(View.VISIBLE);
                tv_call_state_voice.setText("通话中");
                makeOngoingStatus();

            }else{
                localSurface_layout.setVisibility(View.GONE);
                oppositeSurface_layout.setVisibility(View.GONE);
                rootView.setBackground(getResources().getDrawable(R.drawable.call_bg_voice));

                if(isInComingCall){
                    tv_call_state_voice.setText("邀请你进行音视频通话");
                }else{
                    tv_call_state_voice.setText("正在等待对方接受邀请");
                    if(!isInComingCall){
                        voiceContronlLayout.setVisibility(View.VISIBLE);
                    }
                }
                videoCallingGroup.setVisibility(View.GONE);
                video_transe_layout.setVisibility(View.GONE);
                video_transe_comming_layout.setVisibility(View.GONE);
                voiceCallingGroup.setVisibility(View.VISIBLE);
                tv_nick_voice.setText(EaseCallKitUtils.getUserNickName(username));
            }

            loadHeadImage();
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
                                 (EaseCallUIKit.getInstance().getCallID()) && EaseCallUIKit.getInstance().getCallState() != EaseCallState.CALL_ANSWERED){
                             //发送会话有效消息
                             ringEvent.calleeDevId = alertEvent.calleeDevId;
                             ringEvent.valid = true;
                             sendCmdMsg(ringEvent,username);
                         }else{
                             //发送会话无效消息
                             ringEvent.calleeDevId = alertEvent.calleeDevId;
                             ringEvent.valid = false;
                             sendCmdMsg(ringEvent, username);
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

                        EaseCallKitListener listener = EaseCallUIKit.getInstance().getCallListener();
                        if(listener != null){
                            //对方取消
                            listener.onEndCallWithReason(callType,channelName, EaseCallEndReason.EaseCallEndReasonRemoteCancel,0);
                        }
                        break;
                    case CALL_ANSWER:
                        AnswerEvent answerEvent = (AnswerEvent)event;
                        ConfirmCallEvent callEvent = new ConfirmCallEvent();
                        boolean transVoice = answerEvent.transVoice;
                        callEvent.calleeDevId = answerEvent.calleeDevId;
                        callEvent.callerDevId = answerEvent.callerDevId;
                        callEvent.result = answerEvent.result;
                         if(answerEvent.result.equals(
                                 EaseMsgUtils.CALL_ANSWER_BUSY)){
                             if(!mConfirm_ring){
                                 //退出频道
                                timehandler.stopTime();
                                runOnUiThread(new Runnable() {
                                     @Override
                                     public void run() {
                                         //提示对方正在忙碌中
                                         String info = getString(R.string.The_other_is_busy);
                                         Toast.makeText(getApplicationContext(),info , Toast.LENGTH_SHORT).show();
                                         //退出通话
                                         exitChannel();

                                         EaseCallKitListener listener = EaseCallUIKit.getInstance().getCallListener();
                                         if(listener != null){
                                             //对方正在忙碌中
                                             listener.onEndCallWithReason(callType,channelName, EaseCallEndReason.EaseCallEndReasonBusy,0); }
                                         }
                                 });
                             }else{
                                 timehandler.stopTime();
                                 sendCmdMsg(callEvent,username);
                             }
                         }else if(answerEvent.result.equals(
                                 EaseMsgUtils.CALL_ANSWER_ACCEPT)){
                             //设置为接听
                             EaseCallUIKit.getInstance().setCallState(EaseCallState.CALL_ANSWERED);
                             timehandler.stopTime();
                             sendCmdMsg(callEvent,username);
                             if(transVoice){
                                 runOnUiThread(new Runnable() {
                                     @Override
                                     public void run() {
                                         callType = EaseCallType.SIGNAL_VOICE_CALL;
                                         EaseCallUIKit.getInstance().setCallType(EaseCallType.SIGNAL_VOICE_CALL);
                                         changeVideoVoiceState();
                                     }

                                 });
                             }
                         }else if(answerEvent.result.equals(
                                 EaseMsgUtils.CALL_ANSWER_REFUSE)){
                             timehandler.stopTime();
                             sendCmdMsg(callEvent,username);
                         }
                         break;
                    case CALL_INVITE:
                         //收到转音频事件
                         InviteEvent inviteEvent = (InviteEvent)event;
                         if(inviteEvent.type == EaseCallType.SIGNAL_VOICE_CALL){
                             callType = EaseCallType.SIGNAL_VOICE_CALL;
                             EaseCallUIKit.getInstance().setCallType(EaseCallType.SIGNAL_VOICE_CALL);
                             if(mRtcEngine != null){
                                 mRtcEngine.disableVideo();
                             }
                             changeVideoVoiceState();
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
                         if(deviceId.equals(EaseCallUIKit.deviceId)){

                             //收到的仲裁为接听
                             if(result.equals(EaseMsgUtils.CALL_ANSWER_ACCEPT)){
                                 //加入频道
                                 initEngineAndJoinChannel();
                                 makeOngoingStatus();

                             }else if(result.equals(EaseMsgUtils.CALL_ANSWER_REFUSE)){
                                 //退出通话
                                 exitChannel();
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

                                     EaseCallKitListener listener = EaseCallUIKit.getInstance().getCallListener();
                                     if(listener != null){
                                         //已经在其他设备处理
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
                    sendInviteeMsg(username, EaseCallType.SIGNAL_VOICE_CALL);
                    break;
                case 101: // 1V1视频通话
                    sendInviteeMsg(username, EaseCallType.SIGNAL_VIDEO_CALL);
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
                    try {
                        Pair<Integer,String> reponse = HttpClientManager.sendRequest(tokenUrl,null,null,Method_GET);
                     EMLog.e(TAG,"reponse: " + reponse.toString());
                     String token = null;
                     if (TextUtils.isEmpty(token)) {
                         EMLog.e(TAG,"token: " + token);
                         //退出通话
                         exitChannel();
                     }
                     mRtcEngine.joinChannel(token, channelName, null, 0);

                    }catch (IOException exception){
                        EMLog.e(TAG,"IOException errorCode: " +
                                exception.getMessage());

                        //退出通话
                        exitChannel();
                    }catch (HyphenateException exception){
                        EMLog.e(TAG,"errorCode: " + exception.getErrorCode()
                                + " Description:" + exception.getDescription());

                        //退出通话
                        exitChannel();
                    }

                default:
                    break;
            }
        }
    };

    /**
     * 发送通话邀请信息
     * @param username
     * @param callType
     */
    private void sendInviteeMsg(String username, EaseCallType callType){
        mConfirm_ring = false;
        final EMMessage message;
        if(callType == EaseCallType.SIGNAL_VIDEO_CALL){
            message = EMMessage.createTxtSendMessage( "邀请您进行视频通话", username);
        }else{
            message = EMMessage.createTxtSendMessage( "邀请您进行语音通话", username);
        }
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
                EMLog.d(TAG, "Invite call success");
                conversation.removeMessage(message.getMsgId());
            }

            @Override
            public void onError(int code, String error) {
                EMLog.e(TAG, "Invite call error " + code + ", " + error);
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


    /**
     * 发送CMD回复信息
     * @param username
     */
    private void sendCmdMsg(BaseEvent event,String username){
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
            message.setAttribute(EaseMsgUtils.CALLED_TRANSE_VOICE, ((AnswerEvent) event).transVoice);
        }
        final EMConversation conversation = EMClient.getInstance().chatManager().getConversation(username, EMConversation.EMConversationType.Chat, true);
        message.setMessageStatusCallback(new EMCallBack() {
            @Override
            public void onSuccess() {
                EMLog.d(TAG, "Invite call success");
                conversation.removeMessage(message.getMsgId());
                if(event.callAction == EaseCallAction.CALL_CANCEL){
                    //退出频道
                    exitChannel();

                    boolean cancel = ((CallCancelEvent)event).cancel;
                    if(cancel){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                EaseCallKitListener listener = EaseCallUIKit.getInstance().getCallListener();
                                if(listener != null){
                                    //取消通话
                                    listener.onEndCallWithReason(callType,channelName, EaseCallEndReason.EaseCallEndReasonCancel,0);
                                }
                            }
                        });
                    }else{
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                EaseCallKitListener listener = EaseCallUIKit.getInstance().getCallListener();
                                if(listener != null){
                                    //对方无响应
                                    listener.onEndCallWithReason(callType,channelName, EaseCallEndReason.EaseCallEndReasonRemoteNoResponse,0);
                                }
                            }
                        });
                    }
                }else if(event.callAction == EaseCallAction.CALL_CONFIRM_CALLEE){
                    //不为接通状态 退出频道
                    if(!(((ConfirmCallEvent)event).result).equals(EaseMsgUtils.CALL_ANSWER_ACCEPT)){
                        exitChannel();
                        String result = ((ConfirmCallEvent)event).result;

                        //对方拒绝通话
                        if(result.equals(EaseMsgUtils.CALL_ANSWER_REFUSE)){

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    EaseCallKitListener listener = EaseCallUIKit.getInstance().getCallListener();
                                    if(listener != null){
                                        listener.onEndCallWithReason(callType,channelName, EaseCallEndReason.EaseCallEndReasonRefuse,0);
                                    }
                                }
                            });
                        }
                    }
                }else if(event.callAction == EaseCallAction.CALL_ANSWER){
                    //回复以后启动定时器，等待仲裁超时
                    timehandler.startTime();
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
                }else if(event.callAction == EaseCallAction.CALL_CONFIRM_CALLEE){
                    //不为接通状态 退出频道
                    if(!(((ConfirmCallEvent)event).result).equals(EaseMsgUtils.CALL_ANSWER_ACCEPT)){
                        exitChannel();
                    }
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

        public void stopTime() {
            removeMessages(MSG_TIMER);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_TIMER) {
                // TODO: update calling time.
                timePassed++;
                String time = dateFormat.format(timePassed * 1000);

                long intervalTime;
                EaseCallKitConfig callKitConfig = EaseCallUIKit.getInstance().getCallKitConfig();
                if(callKitConfig != null){
                    intervalTime = callKitConfig.getCallTimeOut();
                }else{
                    intervalTime = EaseMsgUtils.CALL_INVITE_INTERVAL;
                }
                if(timePassed *1000 == intervalTime){

                    //呼叫超时
                    timehandler.stopTime();
                    if(!isInComingCall){
                        CallCancelEvent cancelEvent = new CallCancelEvent();
                        cancelEvent.cancel  = false;
                        cancelEvent.remoteTimeout =true;

                        //对方超时未接通,发送取消
                        sendCmdMsg(cancelEvent,username);
                    }else{
                        //被叫等待仲裁消息超时
                        exitChannel();

                        EaseCallKitListener listener = EaseCallUIKit.getInstance().getCallListener();
                        if(listener != null){
                            //对方接通超时
                            listener.onEndCallWithReason(callType,channelName, EaseCallEndReason.EaseCallEndReasonRemoteNoResponse,0);
                        }
                    }
                }
                sendEmptyMessageDelayed(MSG_TIMER, 1000);
                return;
            }
            super.handleMessage(msg);
        }
    }

    public long getChronometerSeconds(Chronometer cmt) {
        long totalss = 0;
        String string = cmt.getText().toString();
        if(string.length()==7){

            String[] split = string.split(":");
            String string2 = split[0];
            int hour = Integer.parseInt(string2);
            int Hours =hour*3600;
            String string3 = split[1];
            int min = Integer.parseInt(string3);
            int Mins =min*60;
            int  SS =Integer.parseInt(split[2]);
            totalss = Hours+Mins+SS;
            return totalss;
        }

        else if(string.length()==5){

            String[] split = string.split(":");
            String string3 = split[0];
            int min = Integer.parseInt(string3);
            int Mins =min*60;
            int  SS =Integer.parseInt(split[1]);

            totalss =Mins+SS;
            return totalss;
        }
        return totalss;
    }


    /**
     * 加载用户配置头像
     * @return
     */
    private void loadHeadImage() {
        if(headUrl != null) {
            if (headUrl.startsWith("http://") || headUrl.startsWith("https://")) {
                new AsyncTask<String, Void, Bitmap>() {
                    //该方法运行在后台线程中，因此不能在该线程中更新UI，UI线程为主线程
                    @Override
                    protected Bitmap doInBackground(String... params) {
                        Bitmap bitmap = null;
                        try {
                            String url = params[0];
                            URL HttpURL = new URL(url);
                            HttpURLConnection conn = (HttpURLConnection) HttpURL.openConnection();
                            conn.setDoInput(true);
                            conn.connect();
                            InputStream is = conn.getInputStream();
                            bitmap = BitmapFactory.decodeStream(is);
                            is.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return bitmap;
                    }

                    //在doInBackground 执行完成后，onPostExecute 方法将被UI 线程调用，
                    // 后台的计算结果将通过该方法传递到UI线程，并且在界面上展示给用户.
                    @Override
                    protected void onPostExecute(Bitmap bitmap) {
                        if (bitmap != null) {
                            if (EaseCallUIKit.getInstance().getCallType() == EaseCallType.SIGNAL_VIDEO_CALL) {
                                avatarView.setImageBitmap(bitmap);
                            } else {
                                iv_avatar_voice.setImageBitmap(bitmap);
                            }
                        }
                    }
                }.execute(headUrl);
            } else {
                if(headBitMap == null){
                    //该方法直接传文件路径的字符串，即可将指定路径的图片读取到Bitmap对象
                    headBitMap = BitmapFactory.decodeFile(headUrl);
                }
                if (EaseCallUIKit.getInstance().getCallType() == EaseCallType.SIGNAL_VIDEO_CALL) {
                        avatarView.setImageBitmap(headBitMap);
                    } else {
                    iv_avatar_voice.setImageBitmap(headBitMap);
                }
            }
        }
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
     * 退出频道
     */
    void exitChannel(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                EMLog.i(TAG, "exit channel channelName: " + channelName);
                if(isInComingCall){
                   stopPlayRing();
                }
                finish();
            }
        });
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
        leaveChannel();
        RtcEngine.destroy();
        if(headBitMap != null){
            headBitMap.recycle();
        }

        //重置状态
        EaseCallUIKit.getInstance().setCallState(EaseCallState.CALL_IDEL);
        EaseCallUIKit.getInstance().setCallID(null);
    }
}