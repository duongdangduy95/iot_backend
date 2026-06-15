package com.example.iotbackend.mqtt;

import com.example.iotbackend.dto.DeviceRegisterDTO;
import com.example.iotbackend.entity.Device;
import com.example.iotbackend.entity.PairToken;
import com.example.iotbackend.entity.UserDevice;
import com.example.iotbackend.repository.DeviceRepository;
import com.example.iotbackend.repository.PairTokenRepository;
import com.example.iotbackend.repository.UserDeviceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MqttConsumer {

    private final DeviceRepository deviceRepository;
    private final PairTokenRepository pairTokenRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handle(Message<String> message) {

        String topic = message.getHeaders()
                .get("mqtt_receivedTopic", String.class);

        String payload = message.getPayload();

        System.out.println("\n🔥 MQTT RECEIVED");
        System.out.println("TOPIC  : " + topic);
        System.out.println("PAYLOAD: " + payload);

        if (topic == null || payload == null) {
            System.out.println("❌ NULL MQTT DATA");
            return;
        }

        try {

            // =====================================================
            // REGISTER
            // =====================================================
            if (topic.endsWith("/register")) {

                System.out.println("➡ REGISTER HANDLER");

                DeviceRegisterDTO dto =
                        objectMapper.readValue(payload, DeviceRegisterDTO.class);

                if (dto.getDeviceId() == null) {
                    System.out.println("❌ deviceId NULL");
                    return;
                }

                PairToken pairToken = pairTokenRepository
                        .findByToken(dto.getPairToken())
                        .orElse(null);

                if (pairToken == null) {
                    System.out.println("❌ INVALID TOKEN");
                    return;
                }

                if (Boolean.TRUE.equals(pairToken.getUsed())) {
                    System.out.println("❌ TOKEN USED");
                    return;
                }

                if (pairToken.getExpiredAt().isBefore(LocalDateTime.now())) {
                    System.out.println("❌ TOKEN EXPIRED");
                    return;
                }

                Device device = getOrCreate(dto.getDeviceId());

                device.setDeviceCode(dto.getDeviceId());
                device.setType(dto.getType());
                device.setIpAddress(dto.getIp());
                device.setMacAddress(dto.getMac());
                device.setRssi(dto.getRssi());

                device.setOnline(true);
                device.setPaired(true);
                device.setOwner(pairToken.getUser());
                device.setLastSeen(LocalDateTime.now());

                Device saved = deviceRepository.save(device);

                UserDevice ud = new UserDevice();
                ud.setUser(pairToken.getUser());
                ud.setDevice(saved);
                ud.setRole("OWNER");

                userDeviceRepository.save(ud);

                pairToken.setUsed(true);
                pairTokenRepository.save(pairToken);

                System.out.println("✅ DEVICE PAIRED SUCCESS");
            }

            // =====================================================
            // ONLINE
            // =====================================================
            else if (topic.endsWith("/online")) {

                System.out.println("➡ ONLINE HANDLER");

                String deviceCode = extractDeviceId(topic, payload);

                Device device = getOrCreate(deviceCode);

                device.setOnline(true);
                device.setLastSeen(LocalDateTime.now());

                deviceRepository.save(device);

                System.out.println("✅ ONLINE UPDATED");
            }

            // =====================================================
            // STATE
            // =====================================================
            else if (topic.endsWith("/state")) {

                System.out.println("➡ STATE HANDLER");

                JsonNode node = objectMapper.readTree(payload);

                String deviceCode = node.get("deviceId").asText();
                String state = String.valueOf(node.get("state"));

                Device device = getOrCreate(deviceCode);

                device.setStatus(state);
                device.setLastSeen(LocalDateTime.now());

                deviceRepository.save(device);

                System.out.println("✅ STATE UPDATED");
            }

            else {
                System.out.println("⚠ UNKNOWN TOPIC");
            }

        } catch (Exception e) {
            System.out.println("❌ MQTT ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // =====================================================
    // AUTO CREATE DEVICE (CORE FIX)
    // =====================================================
    private Device getOrCreate(String deviceId) {

        return deviceRepository.findByDeviceCode(deviceId)
                .orElseGet(() -> {
                    Device d = new Device();
                    d.setDeviceCode(deviceId);
                    d.setOnline(false);
                    d.setPaired(false);
                    d.setLastSeen(LocalDateTime.now());
                    return deviceRepository.save(d);
                });
    }

    // fallback safe parse
    private String extractDeviceId(String topic, String payload) {

        try {
            JsonNode node = objectMapper.readTree(payload);
            if (node.has("deviceId")) {
                return node.get("deviceId").asText();
            }
        } catch (Exception ignored) {}

        return topic.split("/")[1];
    }
}