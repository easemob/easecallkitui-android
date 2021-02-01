package easemob.hyphenate.calluikit.event;

import easemob.hyphenate.calluikit.utils.EaseCallAction;
import easemob.hyphenate.calluikit.base.EaseCallType;

/**
 * author lijian
 * email: Allenlee@easemob.com
 * date: 01/16/2021
 */
public class InviteEvent extends BaseEvent {
    public InviteEvent(){
        callAction = EaseCallAction.CALL_INVITE;
    }
    public EaseCallType type;
}
