package com.example.iotbackend.util;

import java.util.HashMap;
import java.util.Map;

public class DeviceVocabulary {

    private static final Map<String, String> DEVICE_ALIASES =
            new HashMap<>();

    static {

        // ĐÈN

        DEVICE_ALIASES.put("den", "den");
        DEVICE_ALIASES.put("đèn", "den");
        DEVICE_ALIASES.put("bong den", "den");
        DEVICE_ALIASES.put("bóng đèn", "den");

        // QUẠT

        DEVICE_ALIASES.put("quat", "quat");
        DEVICE_ALIASES.put("quạt", "quat");
        DEVICE_ALIASES.put("quatj", "quat");
        DEVICE_ALIASES.put("quac", "quat");

        // TIVI

        DEVICE_ALIASES.put("tivi", "tivi");
        DEVICE_ALIASES.put("ti vi", "tivi");
        DEVICE_ALIASES.put("tv", "tivi");
    }

    public static String normalizeDeviceName(
            String input
    ) {

        input =
                TextNormalizer.normalize(input);

        return DEVICE_ALIASES.getOrDefault(
                input,
                input
        );
    }
}