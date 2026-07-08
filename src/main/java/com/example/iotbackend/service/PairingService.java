package com.example.iotbackend.service;

import com.example.iotbackend.entity.*;
import com.example.iotbackend.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PairingService {

    private final ObjectMapper objectMapper;

    private final PairTokenRepository pairTokenRepository;

    private final DeviceRepository deviceRepository;

    private final UserDeviceRepository userDeviceRepository;

    @Transactional
    public void handlePairing(String payload) {
        try {
            JsonNode json = objectMapper.readTree(payload);

            String deviceCode = json.get("deviceCode").asText();
            String pairToken = json.get("pairToken").asText();

            PairToken token = pairTokenRepository.findByToken(pairToken)
                    .orElseThrow(() -> new RuntimeException("Pair token không tồn tại"));

            // Token hết hạn -> xóa khỏi DB
            if (token.getExpiredAt().isBefore(LocalDateTime.now())) {
                pairTokenRepository.delete(token);
                throw new RuntimeException("Pair token đã hết hạn");
            }

            User user = token.getUser();

            Device device = deviceRepository.findByDeviceCode(deviceCode)
                    .orElse(null);

            // Nếu thiết bị chưa tồn tại thì tạo mới
            if (device == null) {
                device = new Device();
                device.setDeviceCode(deviceCode);
            }

            // Thiết bị đã có owner thì không cho pair
            if (device.getOwner() != null) {
                pairTokenRepository.delete(token);
                throw new RuntimeException("Thiết bị đã được ghép với tài khoản khác");
            }

            device.setOnline(true);
            device.setOwner(user);

            device = deviceRepository.save(device);

            UserDevice userDevice = new UserDevice();
            userDevice.setUser(user);
            userDevice.setDevice(device);
            userDevice.setRole("OWNER");

            userDeviceRepository.save(userDevice);

            // Pair thành công -> xóa token
            pairTokenRepository.delete(token);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}