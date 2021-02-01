package easemob.hyphenate.calluikit.base;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import easemob.hyphenate.calluikit.EaseCallUIKit;
import easemob.hyphenate.calluikit.R;
import easemob.hyphenate.calluikit.utils.EaseCallKitUtils;
import io.agora.rtc.models.UserInfo;


import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;


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
    private UserInfo userInfo;

    private boolean isVideoOff = true;
    private boolean isAudioOff = false;
    private boolean isDesktop = false;
    private boolean isFullScreenMode = false;
    private String streamId;
    private Bitmap headBitMap;
    private String headUrl;

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
    }

    public void addSurfaceView(SurfaceView surfaceView) {
        surfaceViewLayout.addView(surfaceView);
        this.surfaceView = surfaceView;
    }

    public void setUserInfo(UserInfo info){
        userInfo = info;
        if(userInfo != null){
            nameView.setText(EaseCallKitUtils.getUserNickName(info.userAccount));
            headUrl = EaseCallKitUtils.getUserHeadImage(info.userAccount);
            if(headUrl != null){
                loadHeadImage();
//                avatarView.setBackgroundResource(R.drawable.call_memberview_background);
            }else{
                avatarView.setBackgroundResource(R.drawable.call_memberview_background);
            }
        }
    }

    public String getUserAccount(){
        if(userInfo != null){
            return userInfo.userAccount;
        }
        return null;
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
            audioOffView.setVisibility(View.VISIBLE);
        } else {
            audioOffView.setVisibility(View.GONE);
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
     * 更新说话状态
     */
    public void setTalking(boolean talking) {
        if (isDesktop) {
            return;
        }

        if (isFullScreenMode) {
            return;
        }

        if (talking) {
            talkingView.setVisibility(VISIBLE);
        } else {
            talkingView.setVisibility(GONE);
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
            avatarView.setImageResource(R.drawable.call_memberview_background);
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
                            avatarView.setImageBitmap(bitmap);
                            avatarView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
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
}

