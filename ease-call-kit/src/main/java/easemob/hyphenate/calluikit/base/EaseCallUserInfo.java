package easemob.hyphenate.calluikit.base;

/**
 * callkit 用户信息配置
 * niceName 用户昵称
 * headUrl  用户头像（为本地绝对路径或者url)
 */
public class EaseCallUserInfo {
    private String nickName;
    private String headImage;

    public  EaseCallUserInfo(String nickName,String headImage){
        this.nickName = nickName;
        this.headImage = headImage;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getHeadImage() {
        return headImage;
    }

    public void setHeadImage(String headImage) {
        this.headImage = headImage;
    }
}
