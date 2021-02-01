package easemob.hyphenate.calluikit.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import easemob.hyphenate.calluikit.EaseCallUIKit;
import easemob.hyphenate.calluikit.base.EaseCallKitConfig;
import easemob.hyphenate.calluikit.base.EaseCallUserInfo;

import static android.content.Context.MODE_PRIVATE;


/**
 * author lijian
 * email: Allenlee@easemob.com
 * date: 01/11/2021
 */
public class EaseCallKitUtils {

    /**
     * length用户要求产生字符串的长度，随机生成会议密码
     * @param length
     * @return
     */
    static public String getRandomString(int length){
        String str="abcdefghijklmnopqrstuvwxyz";
        Random random=new Random();
        StringBuffer sb=new StringBuffer();
        for(int i=0;i<length;i++){
            int number=random.nextInt(26);
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }


    /**
     * 获取手机唯一标识符
     * @return
     */
    public static String getPhoneSign(){
        StringBuilder deviceId = new StringBuilder();
        // 渠道标志
        deviceId.append("a");
        try {
            //IMEI（imei）
            TelephonyManager tm = (TelephonyManager)EaseCallUIKit.getInstance().getAppContext().getSystemService(Context.TELEPHONY_SERVICE);
            String imei = tm.getDeviceId();
            if(!TextUtils.isEmpty(imei)){
                deviceId.append("imei");
                deviceId.append(imei);
                return deviceId.toString();
            }
            //序列号（sn）
            String sn = tm.getSimSerialNumber();
            if(!TextUtils.isEmpty(sn)){
                deviceId.append("sn");
                deviceId.append(sn);
                return deviceId.toString();
            }
            //如果上面都没有， 则生成一个id：随机码
            String uuid = getUUID();
            if(!TextUtils.isEmpty(uuid)){
                deviceId.append("id");
                deviceId.append(uuid);
                return deviceId.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
            deviceId.append("id").append(getUUID());
        }
        return deviceId.toString();
    }

    private static String uuid;
    public static String getUUID(){
          SharedPreferences mShare = EaseCallUIKit.getInstance().getAppContext().getSharedPreferences("uuid",MODE_PRIVATE);
          if(mShare != null){
              uuid = mShare.getString("uuid", "");
          }
          if(TextUtils.isEmpty(uuid)){
              uuid = UUID.randomUUID().toString();
              mShare.edit().putString("uuid",uuid).commit();
          }
          return uuid;
    }

    /**
     * 获取用户头像
     * @param uersId
     * @return
     */
    public static String getUserHeadImage(String uersId){
        EaseCallKitConfig callKitConfig = EaseCallUIKit.getInstance().getCallKitConfig();
        if(callKitConfig != null){
            Map<String, EaseCallUserInfo> userInfoMap = callKitConfig.getUserInfoMap();
            if(userInfoMap != null){
                EaseCallUserInfo userInfo = userInfoMap.get(uersId);
                if(userInfo != null){
                    if(userInfo.getHeadImage() != null){
                        return userInfo.getHeadImage();
                    }
                }
            }
            return callKitConfig.getDefaultHeadImage();
        }
        return  null;
    }

    /**
     * 获取用户昵称
     * @param uersId
     * @return
     */
    public static String  getUserNickName(String uersId){
        EaseCallKitConfig callKitConfig = EaseCallUIKit.getInstance().getCallKitConfig();
        if(callKitConfig != null){
            Map<String, EaseCallUserInfo> userInfoMap = callKitConfig.getUserInfoMap();
            if(userInfoMap != null){
                EaseCallUserInfo userInfo = userInfoMap.get(uersId);
                if(userInfo != null){
                    if(userInfo.getNickName() != null){
                        return userInfo.getNickName();
                    }
                }
            }
            return uersId;
        }
        return  uersId;
    }


    /**
     * 获取用户振铃文件
     * @return
     */
    public static String getRingFile(){
        EaseCallKitConfig callKitConfig = EaseCallUIKit.getInstance().getCallKitConfig();
        if(callKitConfig != null){
            if(callKitConfig.getRingFile() != null){
                return callKitConfig.getRingFile();
            }
        }
        return  null;
    }
}
