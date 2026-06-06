package com.wisdom.clound.Bean;

import java.util.List;

public class TabResponse {
    private int code;
    private int count;
    private String msg;
    private DataBean data;

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

    public DataBean getData() {
        return data;
    }

    public void setData(DataBean data) {
        this.data = data;
    }

    public static class DataBean {
        private List<TabBean> IndexTab;
        private List<FullTabBean> FullTab;

        public List<TabBean> getIndexTab() {
            return IndexTab;
        }

        public void setIndexTab(List<TabBean> indexTab) {
            IndexTab = indexTab;
        }

        public List<FullTabBean> getFullTab() {
            return FullTab;
        }

        public void setFullTab(List<FullTabBean> fullTab) {
            FullTab = fullTab;
        }

        public static class FullTabBean implements java.io.Serializable {
            private String FullKeyName;
            private List<TabBean> SubKeyList;

            public String getFullKeyName() {
                return FullKeyName;
            }

            public void setFullKeyName(String fullKeyName) {
                FullKeyName = fullKeyName;
            }

            public List<TabBean> getSubKeyList() {
                return SubKeyList;
            }

            public void setSubKeyList(List<TabBean> subKeyList) {
                SubKeyList = subKeyList;
            }
        }
    }
}