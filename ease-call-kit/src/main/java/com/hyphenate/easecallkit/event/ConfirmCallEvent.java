package com.hyphenate.easecallkit.event;

import com.hyphenate.easecallkit.utils.EaseCallAction;

/**
 * author lijian
 * email: Allenlee@easemob.com
 * date: 01/12/2021
 */
public class ConfirmCallEvent extends BaseEvent {
    public ConfirmCallEvent(){
        callAction = EaseCallAction.CALL_CONFIRM_CALLEE;
    }
    public String result;
}
