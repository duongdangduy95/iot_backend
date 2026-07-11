package com.example.iotbackend.util;

import java.util.HashMap;
import java.util.Map;

public class CommandVocabulary {

    private static final Map<String, Boolean> COMMAND_MAP = new HashMap<>();

    static {

        // ===== BẬT =====
        COMMAND_MAP.put("bat", true);
        COMMAND_MAP.put("bật", true);
        COMMAND_MAP.put("mo", true);
        COMMAND_MAP.put("mở", true);
        COMMAND_MAP.put("on", true);
        COMMAND_MAP.put("len", true);
        COMMAND_MAP.put("lên", true);

        // ===== TẮT =====
        COMMAND_MAP.put("tat", false);
        COMMAND_MAP.put("tắt", false);
        COMMAND_MAP.put("dong", false);
        COMMAND_MAP.put("đóng", false);
        COMMAND_MAP.put("off", false);
        COMMAND_MAP.put("ngung", false);
        COMMAND_MAP.put("ngừng", false);
    }

    public static Boolean normalizeCommand(String input) {

        input = TextNormalizer.normalize(input);

        // match nguyên câu
        if (COMMAND_MAP.containsKey(input)) {
            return COMMAND_MAP.get(input);
        }

        // match từng từ
        String[] words = input.split("\\s+");

        for (String word : words) {
            if (COMMAND_MAP.containsKey(word)) {
                return COMMAND_MAP.get(word);
            }
        }

        // fuzzy
        for (String word : words) {
            for (String key : COMMAND_MAP.keySet()) {
                if (levenshtein(word, key) <= 1) {
                    return COMMAND_MAP.get(key);
                }
            }
        }

        return null;
    }

    private static int levenshtein(String a, String b) {

        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) {
            for (int j = 0; j <= b.length(); j++) {

                if (i == 0) dp[i][j] = j;
                else if (j == 0) dp[i][j] = i;
                else {
                    dp[i][j] = Math.min(
                            Math.min(
                                    dp[i - 1][j] + 1,
                                    dp[i][j - 1] + 1
                            ),
                            dp[i - 1][j - 1] +
                                    (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1)
                    );
                }
            }
        }

        return dp[a.length()][b.length()];
    }
}
