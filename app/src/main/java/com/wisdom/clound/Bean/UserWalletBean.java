package com.wisdom.clound.Bean;

public class UserWalletBean {
    private int code;
    private int count;
    private String msg;
    private DataBean data;

    public int getCode() { return code; }
    public DataBean getData() { return data; }

    public static class DataBean {
        private int Id;
        private int UserId;
        private double UserWallets;
        private double UserPoint;

        public double getUserWallets() { return UserWallets; }
    }
}
