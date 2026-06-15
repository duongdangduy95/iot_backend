package com.example.iotbackend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
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