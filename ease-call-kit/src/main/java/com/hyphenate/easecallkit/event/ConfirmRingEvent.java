package com.hyphenate.easecallkit.event;

import com.hyphenate.easecallkit.utils.EaseCallAction;

/**
 * author lijian
 * email: Allenlee@easemob.com
 * date: 01/12/2021
 */
public class ConfirmRingEvent extends BaseEvent {
    public ConfirmRingEvent(){
        callAction = EaseCallAction.CALL_CONFIRM_RING;
    }
    public Boolean valid;
}
