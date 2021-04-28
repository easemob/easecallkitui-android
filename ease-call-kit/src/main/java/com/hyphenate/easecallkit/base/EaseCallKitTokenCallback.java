package com.hyphenate.easecallkit.base;

/**
 * 用户返回Token的回调
 * onGetToken       为返回正确的Token
 * onGetTokenError 为生成ToKen错误的回调(错误码和错误描述)
 */
public interface EaseCallKitTokenCallback {
    /**
     *  \~chinese
     * 获取到正确Token的值
     *
     * @param token  token的值
     */
    void onSetToken(String token,int uId);

    /**
     * \~chinese
     * 获取Token失败的错误回调
     *
     * @param error   获取token失败的错误码
     * @param errorMsg  获取token失败的错误信息描述
     */
    void onGetTokenError(int error, final String errorMsg);
}
