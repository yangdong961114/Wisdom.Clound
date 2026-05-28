package com.wisdom.clound.Bean;

import com.google.gson.annotations.SerializedName;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 收货地址实体类
 */
public class UserAddress {
    @SerializedName("Id")
    private int id;

    @SerializedName("UserId")
    private int userId;

    @SerializedName("UserName")
    private String userName;

    @SerializedName("UserPhone")
    private String userPhone;

    @SerializedName("Province")
    private String province;

    @SerializedName("City")
    private String city;

    @SerializedName("District")
    private String district;

    @SerializedName("CityDesc")
    private String cityDesc;

    @SerializedName("Address")
    private String address;

    @SerializedName("IsDefault")
    private int isDefault;

    @SerializedName("CreateDate")
    private String createDate;

    // 解析/Date(1773315827657)/格式的日期
    public String getFormatCreateDate() {
        if (createDate == null || createDate.isEmpty()) {
            return "";
        }
        try {
            // 提取时间戳
            String timestampStr = createDate.replace("/Date(", "").replace(")/", "");
            long timestamp = Long.parseLong(timestampStr);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
            return sdf.format(new Date(timestamp));
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    // Getter & Setter
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPhone() {
        return userPhone;
    }

    public void setUserPhone(String userPhone) {
        this.userPhone = userPhone;
    }

    public String getProvince(){return province;}

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getCityDesc() {
        return cityDesc;
    }

    public void setCityDesc(String cityDesc) {
        this.cityDesc = cityDesc;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean getIsDefault() {
        return isDefault == 1;
    }

    public void setIsDefault(int isDefault) {
        this.isDefault = isDefault;
    }

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }
}