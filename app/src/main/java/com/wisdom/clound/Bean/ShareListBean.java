package com.wisdom.clound.Bean;

import java.util.List;

public class ShareListBean {
    private int code;
    private int count;
    private String msg;
    private List<DataBean> data;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public List<DataBean> getData() {
        return data;
    }

    public void setData(List<DataBean> data) {
        this.data = data;
    }

    public static class DataBean {
        private int Id;
        private int IsStatus;
        private int IsHotStatus;
        private int IsExist;
        private int IsAdmin;
        private String OpenId;
        private String InviteCode;
        private String PlatformId;
        private String UserLoginName;
        private String UserLoginPwd;
        private String UserPayPwd;
        private String UserAvatar;
        private String UserPhone;
        private String UserName;
        private String UniPlatform;
        private String RegisterDate;
        private String StrRegisterDate;
        private String UserLoginPwdDecrypt;
        private String UserPayPwdDecrypt;
        private String Session;
        public String StrStatus;
        // 新增接口返回字段
        private String ShareUserName;

        public String getShareUserName() {
            return ShareUserName == null ? "" : ShareUserName;
        }

        public void setShareUserName(String shareUserName) {
            ShareUserName = shareUserName;
        }

        public int getId() {
            return Id;
        }

        public void setId(int id) {
            Id = id;
        }

        public int getIsStatus() {
            return IsStatus;
        }

        public void setIsStatus(int isStatus) {
            IsStatus = isStatus;
        }

        public int getIsHotStatus() {
            return IsHotStatus;
        }

        public void setIsHotStatus(int isHotStatus) {
            IsHotStatus = isHotStatus;
        }

        public int getIsExist() {
            return IsExist;
        }

        public void setIsExist(int isExist) {
            IsExist = isExist;
        }

        public int getIsAdmin() {
            return IsAdmin;
        }

        public void setIsAdmin(int isAdmin) {
            IsAdmin = isAdmin;
        }

        public String getOpenId() {
            return OpenId;
        }

        public void setOpenId(String openId) {
            OpenId = openId;
        }

        public String getInviteCode() {
            return InviteCode;
        }

        public void setInviteCode(String inviteCode) {
            InviteCode = inviteCode;
        }

        public String getPlatformId() {
            return PlatformId;
        }

        public void setPlatformId(String platformId) {
            PlatformId = platformId;
        }

        public String getUserLoginName() {
            return UserLoginName;
        }

        public void setUserLoginName(String userLoginName) {
            UserLoginName = userLoginName;
        }

        public String getUserLoginPwd() {
            return UserLoginPwd;
        }

        public void setUserLoginPwd(String userLoginPwd) {
            UserLoginPwd = userLoginPwd;
        }

        public String getUserPayPwd() {
            return UserPayPwd;
        }

        public void setUserPayPwd(String userPayPwd) {
            UserPayPwd = userPayPwd;
        }

        public String getUserAvatar() {
            return UserAvatar;
        }

        public void setUserAvatar(String userAvatar) {
            UserAvatar = userAvatar;
        }

        public String getUserPhone() {
            return UserPhone;
        }

        public void setUserPhone(String userPhone) {
            UserPhone = userPhone;
        }

        public String getUserName() {
            return UserName;
        }

        public void setUserName(String userName) {
            UserName = userName;
        }

        public String getUniPlatform() {
            return UniPlatform;
        }

        public void setUniPlatform(String uniPlatform) {
            UniPlatform = uniPlatform;
        }

        public String getRegisterDate() {
            return RegisterDate;
        }

        public void setRegisterDate(String registerDate) {
            RegisterDate = registerDate;
        }

        public String getStrRegisterDate() {
            return StrRegisterDate;
        }

        public void setStrRegisterDate(String strRegisterDate) {
            StrRegisterDate = strRegisterDate;
        }

        public String getUserLoginPwdDecrypt() {
            return UserLoginPwdDecrypt;
        }

        public void setUserLoginPwdDecrypt(String userLoginPwdDecrypt) {
            UserLoginPwdDecrypt = userLoginPwdDecrypt;
        }

        public String getUserPayPwdDecrypt() {
            return UserPayPwdDecrypt;
        }

        public void setUserPayPwdDecrypt(String userPayPwdDecrypt) {
            UserPayPwdDecrypt = userPayPwdDecrypt;
        }

        public String getSession() {
            return Session;
        }

        public void setSession(String session) {
            Session = session;
        }

        public String getStrStatus() {
            return StrStatus;
        }

        public void setStrStatus(String strStatus) {
            StrStatus = strStatus;
        }
    }
}