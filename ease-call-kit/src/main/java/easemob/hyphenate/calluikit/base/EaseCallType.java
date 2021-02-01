package easemob.hyphenate.calluikit.base;



/**
 * author lijian
 * email: Allenlee@easemob.com
 * date: 01/11/2021
 */
public enum EaseCallType {
    SIGNAL_VOICE_CALL(0), //1v1语音通话
    SIGNAL_VIDEO_CALL(1), //1v1视频通话
    CONFERENCE_CALL(2);   //多人音视频

    public int code;

    EaseCallType(int code) {
        this.code = code;
    }

    public static EaseCallType getfrom(int code) {
        switch (code) {
            case 0:
                return SIGNAL_VOICE_CALL;
            case 1:
                return SIGNAL_VIDEO_CALL;
            case 2:
                return CONFERENCE_CALL;
            default:
                return SIGNAL_VIDEO_CALL;
        }
    }
};