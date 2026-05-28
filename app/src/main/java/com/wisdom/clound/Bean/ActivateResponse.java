package com.wisdom.clound.Bean;

import java.util.List;

public class ActivateResponse {
    private int code;
    private int count;
    private String msg;
    private DataBean data;

    public int getCode() { return code; }
    public int getCount() { return count; }
    public String getMsg() { return msg; }
    public DataBean getData() { return data; }

    public static class DataBean {
        private int CodeCounts;
        private int InCodeCounts;
        private int UnCodeCounts;
        private List<UsersBean> Users;

        public int getCodeCounts() { return CodeCounts; }
        public int getInCodeCounts() { return InCodeCounts; }
        public int getUnCodeCounts() { return UnCodeCounts; }
        public List<UsersBean> getUsers() { return Users; }
    }

    public static class UsersBean {
        private int Id;
        private int IsStatus;
        private String UserName;
        private String UserAvatar;
        private String UserLoginName;
        private String ActivateCode;
        private String CreateDate;

        public int getId() { return Id; }
        public int getIsStatus() { return IsStatus; }
        public String getUserName() { return UserName; }
        public String getUserLoginName() { return UserLoginName; }
        public String getActivateCode() { return ActivateCode; }
        public String getCreateDate() { return CreateDate; }
    }
}
