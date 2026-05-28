package com.wisdom.clound.Bean;

import java.util.List;

public class CartResponse {
    private int code;
    private int count;
    private String msg;
    private CartData data;

    // Getter & Setter
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
    public CartData getData() { return data; }
    public void setData(CartData data) { this.data = data; }

    // 内部类：购物车数据体
    public static class CartData {
        private UserInfo User;
        private AddressInfo UserMap;
        private List<CartItem> UserCarList;

        // Getter & Setter
        public UserInfo getUser() { return User; }
        public void setUser(UserInfo user) { User = user; }
        public AddressInfo getUserMap() { return UserMap; }
        public void setUserMap(AddressInfo userMap) { UserMap = userMap; }
        public List<CartItem> getUserCarList() { return UserCarList; }
        public void setUserCarList(List<CartItem> userCarList) { UserCarList = userCarList; }
    }

    // 内部类：用户信息（暂用，可简化）
    public static class UserInfo {
        private int Id;
        // 其他字段可根据需要添加Getter/Setter
        public int getId() { return Id; }
        public void setId(int id) { Id = id; }
    }

    // 内部类：地址信息
    public static class AddressInfo {
        private int Id;
        private int UserId;
        private String UserName;
        private String UserPhone;
        private String Province;
        private String City;
        private String District;
        private String CityDesc;
        private String Address;
        private int IsDefault;

        // Getter & Setter
        public int getId() { return Id; }
        public void setId(int id) { Id = id; }
        public int getUserId() { return UserId; }
        public void setUserId(int userId) { UserId = userId; }
        public String getUserName() { return UserName; }
        public void setUserName(String userName) { UserName = userName; }
        public String getUserPhone() { return UserPhone; }
        public void setUserPhone(String userPhone) { UserPhone = userPhone; }
        public String getProvince() { return Province; }
        public void setProvince(String province) { Province = province; }
        public String getCity() { return City; }
        public void setCity(String city) { City = city; }
        public String getDistrict() { return District; }
        public void setDistrict(String district) { District = district; }
        public String getCityDesc() { return CityDesc; }
        public void setCityDesc(String cityDesc) { CityDesc = cityDesc; }
        public String getAddress() { return Address; }
        public void setAddress(String address) { Address = address; }
        public int getIsDefault() { return IsDefault; }
        public void setIsDefault(int isDefault) { IsDefault = isDefault; }
    }

    // 内部类：购物车商品项
    public static class CartItem {
        private int IsHot;
        private int GoodsId;
        private int UserCarId;
        private int GoodsNums;
        private String GoodsName;
        private String GoodsAvatar;
        private double GoodsWallet;
        private double GoodsPv;
        private double GoodsPointsPv;
        private double GoodsFee;
        private boolean IsChecket;

        // Getter & Setter
        public int getIsHot() { return IsHot; }
        public void setIsHot(int isHot) { IsHot = isHot; }
        public int getGoodsId() { return GoodsId; }
        public void setGoodsId(int goodsId) { GoodsId = goodsId; }
        public int getUserCarId() { return UserCarId; }
        public void setUserCarId(int userCarId) { UserCarId = userCarId; }
        public int getGoodsNums() { return GoodsNums; }
        public void setGoodsNums(int goodsNums) { GoodsNums = goodsNums; }
        public String getGoodsName() { return GoodsName; }
        public void setGoodsName(String goodsName) { GoodsName = goodsName; }
        public String getGoodsAvatar() { return GoodsAvatar; }
        public void setGoodsAvatar(String goodsAvatar) { GoodsAvatar = goodsAvatar; }
        public double getGoodsWallet() { return GoodsWallet; }
        public void setGoodsWallet(double goodsWallet) { GoodsWallet = goodsWallet; }
        public double getGoodsPv() { return GoodsPv; }
        public void setGoodsPv(double goodsPv) { GoodsPv = goodsPv; }
        public double getGoodsPointsPv() { return GoodsPointsPv; }
        public void setGoodsPointsPv(double goodsPointsPv) { GoodsPointsPv = goodsPointsPv; }
        public double getGoodsFee() { return GoodsFee; }
        public void setGoodsFee(double goodsFee) { GoodsFee = goodsFee; }
        public boolean isIsChecket() { return IsChecket; }
        public void setIsChecket(boolean isChecket) { IsChecket = isChecket; }
    }
}