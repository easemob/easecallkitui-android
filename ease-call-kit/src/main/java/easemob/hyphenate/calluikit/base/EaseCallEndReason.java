package easemob.hyphenate.calluikit.base;



/**
 * author lijian
 * email: Allenlee@easemob.com
 * date: 01/27/2021
 */

public enum EaseCallEndReason {
    EaseCallEndReasonHangup(0),
    EaseCallEndReasonCancel(1),
    EaseCallEndReasonRemoteCancel(2),
    EaseCallEndReasonRefuse(3),
    EaseCallEndReasonBusy(4),
    EaseCallEndReasonNoResponse(5),
    EaseCallEndReasonRemoteNoResponse(6),
    EaseCallEndReasonHandleOnOtherDevice(7);


    public int code;

    EaseCallEndReason(int code) {
        this.code = code;
    }

    public static EaseCallEndReason getfrom(int code) {
        switch (code) {
            case 0:
                return EaseCallEndReasonHangup;
            case 1:
                return EaseCallEndReasonCancel;
            case 2:
                return EaseCallEndReasonRemoteCancel;
            case 3:
                return EaseCallEndReasonRefuse;
            case 4:
                return EaseCallEndReasonBusy;
            case 5:
                return EaseCallEndReasonNoResponse;
            case 6:
                return EaseCallEndReasonRemoteNoResponse;
            case 7:
                return EaseCallEndReasonHandleOnOtherDevice;
            default:
                return EaseCallEndReasonHangup;
        }
    }
}
