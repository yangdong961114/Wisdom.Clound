package com.wisdom.clound.Bean;

import java.util.List;

public class VideoListResponse {
    private int code;
    private int count;
    private String msg;
    private List<VideoItemBean> data;

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

    public List<VideoItemBean> getData() {
        return data;
    }

    public void setData(List<VideoItemBean> data) {
        this.data = data;
    }
}