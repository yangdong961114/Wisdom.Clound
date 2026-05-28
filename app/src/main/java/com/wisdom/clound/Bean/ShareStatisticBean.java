package com.wisdom.clound.Bean;

import java.util.List;

public class ShareStatisticBean {
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
        private String Name;
        private String Content;
        private Object Details;

        public String getName() {
            return Name;
        }

        public void setName(String name) {
            Name = name;
        }

        public String getContent() {
            return Content;
        }

        public void setContent(String content) {
            Content = content;
        }

        public Object getDetails() {
            return Details;
        }

        public void setDetails(Object details) {
            Details = details;
        }
    }
}