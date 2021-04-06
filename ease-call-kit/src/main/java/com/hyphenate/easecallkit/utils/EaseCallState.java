package com.hyphenate.easecallkit.utils;

/**
 * author lijian
 * email: Allenlee@easemob.com
 * date: 01/11/2021
 */
public enum EaseCallState {
    CALL_IDLE(0), //初始状态
    CALL_OUTGOING(1), //拨打电话状态
    CALL_ALERTING(2),   //振铃状态
    CALL_ANSWERED(3); //接通同话状态

    public int code;

    EaseCallState(int code) {
        this.code = code;
    }

    public static EaseCallState getfrom(int code) {
        switch (code) {
            case 0:
                return CALL_IDLE;
            case 1:
                return CALL_OUTGOING;
            case 2:
                return CALL_ALERTING;
            case 3:
                return CALL_ANSWERED;
            default:
                return CALL_IDLE;
        }
    }
}
