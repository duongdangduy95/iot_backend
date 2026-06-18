package com.example.iotbackend.util;

import java.text.Normalizer;

public class TextNormalizer {

    private TextNormalizer() {
    }

    public static String normalize(String text) {

        if (text == null) {
            return "";
        }

        String normalized =
                Normalizer.normalize(
                        text,
                        Normalizer.Form.NFD
                );

        normalized =
                normalized.replaceAll(
                        "\\p{M}",
                        ""
                );

        return normalized
                .toLowerCase()
                .trim()
                .replaceAll("\\s+", " ");
    }
}