package com.hyphenate.easecallkit.base;



/**
 * author lijian
 * email: Allenlee@easemob.com
 * date: 01/27/2021
 */

public enum EaseCallEndReason {
    EaseCallEndReasonHangup(0), //正常挂断
    EaseCallEndReasonCancel(1), //自己取消通话
    EaseCallEndReasonRemoteCancel(2), //对方取消通话
    EaseCallEndReasonRefuse(3),//拒绝接听
    EaseCallEndReasonBusy(4), //忙线中
    EaseCallEndReasonNoResponse(5), //自己无响应
    EaseCallEndReasonRemoteNoResponse(6), //对端无响应
    EaseCallEndReasonHandleOnOtherDevice(7); //在其他设备处理


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
