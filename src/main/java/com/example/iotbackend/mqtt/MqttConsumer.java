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
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MqttConsumer {

    private final DeviceRepository deviceRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final PairTokenRepository pairTokenRepository;

    private final UserDeviceRepository userDeviceRepository;

    @Transactional
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handle(Message<String> message) {

        String topic = message.getHeaders()
                .get("mqtt_receivedTopic", String.class);

        String payload = message.getPayload();

        System.out.println("\n🔥 MQTT RECEIVED");
        System.out.println("TOPIC  : " + topic);
        System.out.println("PAYLOAD: " + payload);

        try {

            // ================= REGISTER =================
            if (topic != null && topic.endsWith("/register")) {

                System.out.println("➡ REGISTER HANDLER");

                DeviceRegisterDTO dto =
                        objectMapper.readValue(payload, DeviceRegisterDTO.class);

                if (dto.getDeviceId() == null) {
                    System.out.println("❌ deviceId NULL");
                    return;
                }

                // ================= TOKEN =================

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

                // ================= DEVICE =================

                Device device = deviceRepository
                        .findByDeviceCode(dto.getDeviceId())
                        .orElseGet(Device::new);

                device.setDeviceCode(dto.getDeviceId());
                device.setType(dto.getType());
                device.setIpAddress(dto.getIp());
                device.setRssi(dto.getRssi());
                device.setMacAddress(dto.getMac());

                device.setOnline(true);
                device.setPaired(true);

                device.setOwner(pairToken.getUser());

                device.setLastSeen(LocalDateTime.now());

                Device savedDevice = deviceRepository.save(device);

                // ================= USER DEVICE =================

                UserDevice ud = new UserDevice();

                ud.setUser(pairToken.getUser());
                ud.setDevice(savedDevice);

                ud.setRole("OWNER");

                userDeviceRepository.save(ud);

                // ================= TOKEN USED =================

                pairToken.setUsed(true);

                pairTokenRepository.save(pairToken);

                System.out.println("✅ DEVICE PAIRED SUCCESS");
            }

            // ================= ONLINE =================
            else if (topic != null && topic.endsWith("/online")) {

                System.out.println("➡ ONLINE HANDLER");

                String deviceCode = topic.split("/")[1];

                deviceRepository.findByDeviceCode(deviceCode)
                        .ifPresentOrElse(d -> {
                            d.setOnline(true);
                            d.setLastSeen(LocalDateTime.now());
                            deviceRepository.save(d);
                            System.out.println("✅ ONLINE UPDATED");
                        }, () -> {
                            System.out.println("❌ DEVICE NOT FOUND ONLINE: " + deviceCode);
                        });
            }

            // ================= STATE =================
            else if (topic != null && topic.endsWith("/state")) {

                System.out.println("➡ STATE HANDLER");

                JsonNode node = objectMapper.readTree(payload);

                String deviceCode = node.get("deviceId").asText();
                String state = node.get("state").asText();

                deviceRepository.findByDeviceCode(deviceCode)
                        .ifPresentOrElse(d -> {
                            d.setStatus(state);
                            d.setLastSeen(LocalDateTime.now());
                            deviceRepository.save(d);
                            System.out.println("✅ STATE UPDATED");
                        }, () -> {
                            System.out.println("❌ DEVICE NOT FOUND STATE: " + deviceCode);
                        });
            }

            else {
                System.out.println("⚠ UNKNOWN TOPIC");
            }

        } catch (Exception e) {
            System.out.println("❌ MQTT ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}