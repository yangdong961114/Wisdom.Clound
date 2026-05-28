package com.wisdom.clound.Bean;

public class ConfigBean {
    private int code;
    private int count;
    private String msg;
    private DataBean data;

    public int getCode() { return code; }
    public DataBean getData() { return data; }

    public static class DataBean {
        private double TranSale;

        public double getTranSale() { return TranSale; }
    }
}
