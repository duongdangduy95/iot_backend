package com.example.iotbackend.dto;

import lombok.Data;

@Data
public class DeviceRegisterDTO {

    private String deviceId;

    private String type;

    private String ip;

    private Integer rssi;

    private String mac;

    // THÊM
    private String pairToken;
}