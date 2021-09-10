package com.hyphenate.easecallkit.base;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.hyphenate.easecallkit.R;
import com.hyphenate.easecallkit.utils.EaseCallKitUtils;
import io.agora.rtc.models.UserInfo;


import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * author lijian
 * email: Allenlee@easemob.com
 * date: 01/16/2021
 */
public class EaseCallMemberView extends RelativeLayout {

    private Context context;

    private RelativeLayout surfaceViewLayout;
    private ImageView avatarView;
    private ImageView audioOffView;
    private ImageView talkingView;
    private TextView nameView;
    private SurfaceView surfaceView;
    private ValueAnimator animator;

    private EaseUserAccount userInfo;

    private boolean isVideoOff = true;
    private boolean isAudioOff = false;
    private boolean isDesktop = false;
    private boolean isFullScreenMode = false;
    private String streamId;
    private Bitmap headBitMap;
    private String headUrl;
    private EaseCallMemberView memberView;
    private LinearLayout loading_dialog;
    private boolean speakActivated;
    private boolean isCameraFront;


    public EaseCallMemberView(Context context) {
        this(context, null);
    }

    public EaseCallMemberView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EaseCallMemberView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        LayoutInflater.from(context).inflate(R.layout.avtivity_call_member, this);
        init();
    }

    private void init() {
        surfaceViewLayout = findViewById(R.id.item_surface_layout);
        avatarView = (ImageView) findViewById(R.id.img_call_avatar);
        audioOffView = (ImageView) findViewById(R.id.icon_mute);
        talkingView = (ImageView) findViewById(R.id.icon_talking);
        nameView = (TextView) findViewById(R.id.text_name);
        loading_dialog = findViewById(R.id.member_loading);
    }

    public void setLoading(Boolean loading){
        if(loading){
            loading_dialog.setVisibility(VISIBLE);
        }else {
            loading_dialog.setVisibility(GONE);
        }
    }

    public void addSurfaceView(SurfaceView surfaceView) {
        surfaceViewLayout.addView(surfaceView);
        this.surfaceView = surfaceView;
    }

    public void setUserInfo(UserInfo info){
        if(info == null) {
            return;
        }
        EaseUserAccount account = new EaseUserAccount(info.uid, info.userAccount);
        setUserInfo(account);
    }

    public void setUserInfo(EaseUserAccount info) {
        userInfo = info;
        updateUserInfo();
    }

    public void updateUserInfo(){
        if(userInfo != null){
            nameView.setText(EaseCallKitUtils.getUserNickName(userInfo.getUserName()));
            headUrl = EaseCallKitUtils.getUserHeadImage(userInfo.getUserName());
            if(headUrl != null){
                loadHeadImage();
            }else{
                avatarView.setImageResource(R.drawable.call_memberview_background);
            }
        }
    }

    public EaseUserAccount getUserInfo(){
        return  userInfo;
    }

    public String getUserAccount(){
        if(userInfo != null){
            return userInfo.getUserName();
        }
        return null;
    }

    public int getUserId(){
        if(userInfo != null){
            return userInfo.getUid();
        }
        return 0;
    }

    public SurfaceView getSurfaceView() {
        return this.surfaceView;
    }

    /**
     * 更新静音状态
     */
    public void setAudioOff(boolean state) {
        isAudioOff = state;
        if (isFullScreenMode) {
            return;
        }
        if (isAudioOff) {
            audioOffView.setVisibility(VISIBLE);
            audioOffView.setImageResource(R.drawable.ease_mic_level_off);
        } else {
            audioOffView.setVisibility(GONE);
            audioOffView.setImageResource(R.drawable.ease_mic_level_on);
        }
    }

    public boolean getAudioOff(){
        return  isAudioOff;
    }

    /**
     * 更新正在说话
     */
    public void setSpeak(boolean speak,int volume) {
        if(speak){
              int value = 1;
              value = volume/15  ;
              if(value > 14){
                  value =14;
              }
              audioOffView.setVisibility(VISIBLE);
              if(value == 1){
                  audioOffView.setImageResource(R.drawable.ease_mic_level_01);
              }else if(value == 2){
                  audioOffView.setImageResource(R.drawable.ease_mic_level_02);
              }else if(value == 3){
                  audioOffView.setImageResource(R.drawable.ease_mic_level_03);
              }else if(value ==4){
                  audioOffView.setImageResource(R.drawable.ease_mic_level_04);
              }else if(value ==5){
                  audioOffView.setImageResource(R.drawable.ease_mic_level_05);
              }else if(value ==6){
                  audioOffView.setImageResource(R.drawable.ease_mic_level_06);
              }else if(value ==7){
                  audioOffView.setImageResource(R.drawable.ease_mic_level_07);
              }else if(value ==8){
                  audioOffView.setImageResource(R.drawable.ease_mic_level_08);
              }else if(value ==9){
                  audioOffView.setImageResource(R.drawable.ease_mic_level_09);
              }else if(value ==10){
                  audioOffView.setImageResource(R.drawable.ease_mic_level_10);
              }else if(value ==11){
                  audioOffView.setImageResource(R.drawable.ease_mic_level_11);
              }else if(value ==12){
                  audioOffView.setImageResource(R.drawable.ease_mic_level_12);
              }else if(value ==13){
                  audioOffView.setImageResource(R.drawable.ease_mic_level_13);
              }else if(value == 14){
                  audioOffView.setImageResource(R.drawable.ease_mic_level_14);
              }
        }else{
            audioOffView.setVisibility(GONE);
        }
    }

    public boolean isAudioOff() {
        return isAudioOff;
    }


    /**
     * 更新视频显示状态
     */
    public void setVideoOff(boolean state) {
        isVideoOff = state;
        if (isVideoOff) {
            avatarView.setVisibility(View.VISIBLE);
            surfaceViewLayout.setVisibility(GONE);
        } else {
            avatarView.setVisibility(View.GONE);
            surfaceViewLayout.setVisibility(VISIBLE);
        }
    }

    public boolean isVideoOff() {
        return isVideoOff;
    }

    public void setDesktop(boolean desktop) {
        isDesktop = desktop;
        if (isDesktop) {
            avatarView.setVisibility(View.GONE);
        }
    }


    /**
     * 设置当前 view 对应的 stream 的用户，主要用来语音通话时显示对方头像
     */
    public void setUsername(String username) {
        headUrl = EaseCallKitUtils.getUserHeadImage(username);
        if(headUrl != null){
            avatarView.setImageResource(R.drawable.call_memberview_background);
        }else{
            loadHeadImage();
        }
        nameView.setText(EaseCallKitUtils.getUserNickName(username));
    }

    /**
     * 设置当前控件显示的 Stream Id
     */
    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }

    public String getStreamId() {
        return streamId;
    }

    public void setFullScreen(boolean fullScreen) {
        isFullScreenMode = fullScreen;

        if (fullScreen) {
            talkingView.setVisibility(GONE);
            nameView.setVisibility(GONE);
            audioOffView.setVisibility(GONE);
        } else {
            nameView.setVisibility(VISIBLE);
            if (isAudioOff) {
                audioOffView.setVisibility(VISIBLE);
            }
        }
    }

    public boolean isFullScreen() {
        return isFullScreenMode;
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
                        FutureTarget<Bitmap> futureTarget =
                                Glide.with(getContext())
                                        .asBitmap()
                                        .load(headUrl)
                                        .submit(200, 200);
                        try {
                            bitmap = futureTarget.get();
                        }catch (Exception e){
                            e.getStackTrace();
                        }
                        return  bitmap;
                    }

                    //在doInBackground 执行完成后，onPostExecute 方法将被UI 线程调用，
                    // 后台的计算结果将通过该方法传递到UI线程，并且在界面上展示给用户.
                    @Override
                    protected void onPostExecute(Bitmap bitmap) {
                        if (bitmap != null) {
                            avatarView.setImageBitmap(bitmap);
                            avatarView.setScaleType(ImageView.ScaleType.CENTER);
                        }
                    }
                }.execute(headUrl);
            } else {
                if(headBitMap == null){
                    //该方法直接传文件路径的字符串，即可将指定路径的图片读取到Bitmap对象
                    headBitMap = BitmapFactory.decodeFile(headUrl);
                }
                avatarView.setImageBitmap(headBitMap);
                avatarView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            }
        }
    }

    public void setSpeakActivated(boolean activated) {
        this.speakActivated = activated;
    }

    public boolean isSpeakActivated() {
        return speakActivated;
    }

    public void setCameraDirectionFront(boolean isFront) {
        this.isCameraFront = isFront;
    }

    public boolean isCameraDirectionFront() {
        return this.isCameraFront;
    }
}

