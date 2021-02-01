package easemob.hyphenate.calluikit.utils;



/**
 * author lijian
 * email: Allenlee@easemob.com
 * date: 01/11/2021
 */
public class EaseMsgUtils {
    public static String TOKEN_SERVER = "http://172.17.2.159:8080/token?";

    public static String lijian66_Token = "006d2d1af30f5fb483bbef2c9f7ace22f4aIABjFQlCXSNxc8LnTG7G8cKvYFbaJiAB5X3cxFMn1n/6NCLLri0LKHBGIgAAAAAAoVgBYAQAAQAxFQBgAgAxFQBgAwAxFQBgBAAxFQBg";

    public static String lijian88_Token = " 006d2d1af30f5fb483bbef2c9f7ace22f4aIAAaBzfiLA1VwZItxfhkytztzVs/h9NIpvVA23gqH6NXjpiap7SCKEs/IgAAAAAAHXcBYAQAAQCtMwBgAgCtMwBgAwCtMwBgBACtMwBg";


    public static int MSG_MAKE_SIGNAL_VOICE = 100;
    public static int MSG_MAKE_SIGNAL_VIDEO = 101;
    public static int MSG_MAKE_CONFERENCE_VIDEO = 102;
    public static int MSG_RELEASE_HANDLER = 301;
    public static int MSG_REQUEST_TOKEN = 400;

    public static String CALL_ACTION = "action";
    public static String CALL_CHANNELNAME = "channelName";
    public static String CALL_TYPE = "type";
    public static String CALL_DEVICE_ID = "callerDevId";
    public static String CALLED_DEVICE_ID = "calleeDevId";
    public static String CALLED_TRANSE_VOICE = "videoToVoice";

    public static String CLL_ID = "callId";
    public static String CLL_TIMESTRAMEP = "ts";
    public static String CALL_MSG_TYPE = "msgType";
    public static String CALL_STATUS = "status";
    public static String CALL_RESULT = "result";
    public static String CALL_MSG_INFO = "rtcCallWithAgora";

    public static String CALL_ANSWER_BUSY = "busy";
    public static String CALL_ANSWER_ACCEPT = "accept";
    public static String CALL_ANSWER_REFUSE = "refuse";

    public static String APPCERT = "appCert";
    public static String APPKEY = "appKey";
    public static String CHANNEL = "channel";
    public static String USERID = "userId";
    public static String ADDAMARK = "+";

    final public static int CALL_TIMER_TIMEOUT = 0;
    final public static int CALL_TIMER_CALL_TIME = 1;

    final public static long CALL_INVITE_INTERVAL = 30 *1000;  //主叫超时时间
    final public static int CALL_INVITED_INTERVAL = 5 *1000; //被叫超时时间
}
