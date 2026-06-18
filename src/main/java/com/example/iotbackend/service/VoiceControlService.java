package com.example.iotbackend.service;

import com.example.iotbackend.dto.ControlDeviceRequest;
import com.example.iotbackend.dto.SpeechToTextResponse;
import com.example.iotbackend.dto.VoiceControlResponse;
import com.example.iotbackend.entity.Device;
import com.example.iotbackend.entity.User;
import com.example.iotbackend.entity.UserDevice;
import com.example.iotbackend.repository.UserDeviceRepository;
import com.example.iotbackend.security.SecurityUtils;
import com.example.iotbackend.util.DeviceVocabulary;
import com.example.iotbackend.util.TextNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VoiceControlService {

    private final SecurityUtils securityUtils;
    private final UserDeviceRepository userDeviceRepository;
    private final DeviceService controlDeviceService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final RestTemplate restTemplate =
            new RestTemplate();

    public VoiceControlResponse processVoice(MultipartFile file) {

        try {

            String raw = speechToText(file);

            JsonNode node = objectMapper.readTree(raw);

            String text = node.has("text")
                    ? node.get("text").asText()
                    : raw;

            System.out.println("VOICE RAW = " + text);

            return executeCommand(text);

        } catch (Exception e) {
            return new VoiceControlResponse(false, e.getMessage());
        }
    }

    private String speechToText(MultipartFile file) throws Exception {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        body.add("file", new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        });

        HttpEntity<MultiValueMap<String, Object>> request =
                new HttpEntity<>(body, headers);

        ResponseEntity<SpeechToTextResponse> response =
                restTemplate.exchange(
                        "https://voicemodel-production.up.railway.app/speech-to-text",
                        HttpMethod.POST,
                        request,
                        SpeechToTextResponse.class
                );

        SpeechToTextResponse result = response.getBody();

        if (result == null) {
            throw new RuntimeException("STT null response");
        }

        return result.getText(); // 👈 CHỈ LẤY TEXT
    }
    private VoiceControlResponse executeCommand(
            String text
    ) {

        String command =
                TextNormalizer.normalize(text);

        System.out.println(
                "VOICE NORMALIZED = "
                        + command
        );

        boolean state;

        if (command.contains("bat")) {

            state = true;

        } else if (command.contains("tat")) {

            state = false;

        } else {

            return new VoiceControlResponse(
                    false,
                    "Không nhận diện được lệnh bật hoặc tắt"
            );
        }

        String deviceName =
                DeviceVocabulary.normalizeDeviceName(
                        command
                                .replace("bat", "")
                                .replace("tat", "")
                                .trim()
                );

        System.out.println(
                "DEVICE NAME = "
                        + deviceName
        );

        return controlDeviceByName(
                deviceName,
                state
        );
    }

    private VoiceControlResponse controlDeviceByName(
            String deviceName,
            boolean state
    ) {

        User user =
                securityUtils.getCurrentUser();

        List<UserDevice> userDevices =
                userDeviceRepository.findByUserId(
                        user.getId()
                );

        Device device =
                userDevices.stream()
                        .map(UserDevice::getDevice)
                        .filter(d ->
                                DeviceVocabulary
                                        .normalizeDeviceName(
                                                d.getName()
                                        )
                                        .equals(deviceName)
                        )
                        .findFirst()
                        .orElse(null);

        if (device == null) {

            return new VoiceControlResponse(
                    false,
                    "Không tìm thấy thiết bị '"
                            + deviceName
                            + "', hãy thử lại"
            );
        }

        ControlDeviceRequest req =
                new ControlDeviceRequest();

        req.setDeviceId(
                device.getId()
        );

        req.setState(
                state
        );

        controlDeviceService.controlDevice(
                req
        );

        return new VoiceControlResponse(
                true,
                "Đã "
                        + (state ? "bật " : "tắt ")
                        + device.getName()
        );
    }
}