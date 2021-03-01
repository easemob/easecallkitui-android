package com.hyphenate.easecallkit.utils;


/**
 * author lijian
 * email: Allenlee@easemob.com
 * date: 01/11/2021
 */
public enum EaseCallAction {
    CALL_INVITE("invite"),
    CALL_ALERT("alert"),
    CALL_CONFIRM_RING("confirmRing"),
    CALL_CANCEL("cancelCall"),
    CALL_ANSWER("answerCall"),
    CALL_CONFIRM_CALLEE("confirmCallee"),
    CALL_VIDEO_TO_VOICE("videoToVoice");

    public String state;

    EaseCallAction(String state) {
        this.state = state;
    }

    public static EaseCallAction getfrom(String state) {
        switch (state) {
            case "invite":
                return CALL_INVITE;
            case "alert":
                return CALL_ALERT;
            case "confirmRing":
                return CALL_CONFIRM_RING;
            case "cancelCall":
                return CALL_CANCEL;
            case "answerCall":
                return CALL_ANSWER;
            case "confirmCallee":
                return CALL_CONFIRM_CALLEE;
            case "videoToVoice":
                return CALL_VIDEO_TO_VOICE;
            default:
                return CALL_INVITE;
        }
    }
}
