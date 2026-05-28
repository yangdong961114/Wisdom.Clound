package com.wisdom.clound.Bean;

import com.google.gson.annotations.SerializedName;

public class UserResponse {
    @SerializedName("code")
    private int code;

    @SerializedName("count")
    private int count;

    @SerializedName("msg")
    private String msg;

    @SerializedName("data")
    private UserData data;

    // Getter & Setter
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public UserData getData() {
        return data;
    }

    public void setData(UserData data) {
        this.data = data;
    }

    // 内部类：用户数据
    public static class UserData {
        @SerializedName("UserName")
        private String userName;

        @SerializedName("UserCode")
        private String userCode;

        @SerializedName("UserAvatar")
        private String userAvatar;

        @SerializedName("UserStatus")
        private String userStatus;

        @SerializedName("Fee")
        private String fee;

        @SerializedName("Wallet")
        private String wallet;

        @SerializedName("Amount")
        private String amount;

        @SerializedName("Points")
        private String points;

        @SerializedName("Grade")
        private String grade;

        // Getter & Setter
        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getUserAvatar() {
            return userAvatar;
        }

        public void setUserAvatar(String userAvatar) {
            this.userAvatar = userAvatar;
        }

        public String getUserCode(){
            return userCode;
        }

        public void setUserCode(String userCode) {
            this.userCode = userCode;
        }

        public String getUserStatus(){
            return userStatus;
        }

        public void setUserStatus(String userStatus) {
            this.userStatus = userStatus;
        }

        public String getUserFee(){
            return fee;
        }

        public void setUserFee(String fee) {
            this.fee = fee;
        }

        public String getUserAmount(){
            return amount;
        }

        public void setUserAmount(String amount) {
            this.amount = amount;
        }

        public String getUserWallet(){
            return wallet;
        }

        public void setUserWallet(String wallet) {
            this.wallet = wallet;
        }

        public String getUserPoints(){
            return points;
        }

        public void setUserPoints(String points) {
            this.points = points;
        }

        public String getUserGrade(){
            return grade;
        }

        public void setUserGrade(String grade) {
            this.grade = grade;
        }
    }
}
