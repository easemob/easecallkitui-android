package com.hyphenate.easecallkit.base;

import java.util.List;


/**
 * 用户返回EaseUserAccount(加入频道的uId及对应的环信ID)
 * onUserAccount          为返回channel里面存在用户的EaseUserAccount
 * onSetUserAccountError 为返回channel信息错误的回调(错误码和错误描述)
 */
public interface EaseGetUserAccountCallback {
    /**
     *  \~chinese
     * 获取到channel里面的用户信息
     *
     * @param userAccounts 用于加入channel用户Id信息
     */
    void onUserAccount(List<EaseUserAccount> userAccounts);

    /**
     * \~chinese
     * 获取uid失败的错误回调
     *
     * @param error   获取uid失败的错误码
     * @param errorMsg  获取uid失败的错误信息描述
     */
    void onSetUserAccountError(int error, final String errorMsg);
}
