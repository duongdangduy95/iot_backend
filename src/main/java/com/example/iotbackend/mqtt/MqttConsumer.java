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
import java.util.Optional;

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

        String topic = message.getHeaders().get("mqtt_receivedTopic", String.class);
        String payload = message.getPayload();

        if (topic == null || payload == null) return;

        System.out.println("\n📩 MQTT TOPIC: " + topic);
        System.out.println("PAYLOAD: " + payload);

        try {

            String deviceId = extractDeviceId(topic, payload);

            // =====================================================
            // REGISTER (ONLY PLACE ALLOW CREATE)
            // =====================================================
            if (topic.endsWith("/register")) {

                DeviceRegisterDTO dto = objectMapper.readValue(payload, DeviceRegisterDTO.class);

                if (dto.getDeviceId() == null) return;

                Device device = deviceRepository.findByDeviceCode(dto.getDeviceId())
                        .orElseGet(() -> {
                            Device d = new Device();
                            d.setDeviceCode(dto.getDeviceId());
                            d.setOnline(false);
                            d.setPaired(false);
                            return d;
                        });

                device.setType(dto.getType());
                device.setIpAddress(dto.getIp());
                device.setMacAddress(dto.getMac());
                device.setRssi(dto.getRssi());
                device.setLastSeen(LocalDateTime.now());
                device.setOnline(true);

                // ============================
                // PAIR LOGIC
                // ============================
                if (dto.getPairToken() != null && !dto.getPairToken().isBlank()) {

                    PairToken token = pairTokenRepository.findByToken(dto.getPairToken())
                            .orElse(null);

                    if (token != null
                            && Boolean.FALSE.equals(token.getUsed())
                            && token.getExpiredAt().isAfter(LocalDateTime.now())) {

                        device.setOwner(token.getUser());
                        device.setPaired(true);

                        token.setUsed(true);
                        pairTokenRepository.save(token);

                        System.out.println("✅ DEVICE PAIRED SUCCESS");
                    } else {
                        System.out.println("⚠ INVALID PAIR TOKEN");
                    }
                }

                deviceRepository.save(device);
            }

            // =====================================================
            // ONLINE (NO AUTO CREATE)
            // =====================================================
            if (topic.endsWith("/online")) {

                Optional<Device> opt = deviceRepository.findByDeviceCode(deviceId);

                if (opt.isEmpty()) {
                    System.out.println("⚠ IGNORE ONLINE - UNKNOWN DEVICE: " + deviceId);
                    return;
                }

                JsonNode node = objectMapper.readTree(payload);
                boolean online = node.has("online") && node.get("online").asBoolean();

                Device device = opt.get();
                device.setOnline(online);
                device.setLastSeen(LocalDateTime.now());

                deviceRepository.save(device);
                return;
            }

            // =====================================================
            // STATE (NO AUTO CREATE)
            // =====================================================
            if (topic.endsWith("/state")) {

                Optional<Device> opt = deviceRepository.findByDeviceCode(deviceId);

                if (opt.isEmpty()) {
                    System.out.println("⚠ IGNORE STATE - UNKNOWN DEVICE: " + deviceId);
                    return;
                }

                JsonNode node = objectMapper.readTree(payload);
                boolean state = node.has("state") && node.get("state").asBoolean();

                Device device = opt.get();
                device.setStatus(String.valueOf(state));
                device.setLastSeen(LocalDateTime.now());

                deviceRepository.save(device);
            }

        } catch (Exception e) {
            System.out.println("❌ MQTT ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String extractDeviceId(String topic, String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            if (node.has("deviceId")) return node.get("deviceId").asText();
        } catch (Exception ignored) {}

        String[] parts = topic.split("/");
        return parts.length > 1 ? parts[1] : topic;
    }
}