package com.hyphenate.easecallkit.event;
import com.hyphenate.easecallkit.utils.EaseCallAction;


/**
 * author lijian
 * email: Allenlee@easemob.com
 * date: 01/12/2021
 */
public class AlertEvent extends BaseEvent {
   public AlertEvent(){
        callAction = EaseCallAction.CALL_ALERT;
    }
}
