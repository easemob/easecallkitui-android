package com.hyphenate.easecallkit.event;

import com.hyphenate.easecallkit.utils.EaseCallAction;
import com.hyphenate.easecallkit.utils.EaseCallState;

/**
 * author lijian
 * email: Allenlee@easemob.com
 * date: 01/12/2021
 */

public class BaseEvent {
    public BaseEvent(){}

    public EaseCallAction callAction;
    public String callerDevId;
    public String calleeDevId;
    public long timeStramp;
    public String callId;
    public String msgType;
    public String userId;
}
