package com.pcpart.assembler.util;

import java.util.LinkedHashMap;
import java.util.Map;

/** 从 AI 回答的 Markdown 表格里解析出 8 类配件型号。 */
public final class BuildSheetParser {

    private BuildSheetParser() {
    }

    /** key 为 cpu / cooler / mb / ram / gpu / psu / ssd / case */
    public static Map<String, String> parse(String answer) {
        Map<String, String> picked = new LinkedHashMap<>();
        if (answer == null || answer.isEmpty()) {
            return picked;
        }
        for (String line : answer.split("\\R")) {
            if (!line.startsWith("|")) {
                continue;
            }
            String[] cells = line.split("\\|");
            if (cells.length < 4) {
                continue;
            }
            String category = normalize(cells[1]);
            String model = cells[2].trim();
            if (category == null || model.isEmpty()) {
                continue;
            }
            if (model.contains("缺失") || "—".equals(model) || "-".equals(model)) {
                continue;
            }
            picked.putIfAbsent(category, model);
        }
        return picked;
    }

    private static String normalize(String cell) {
        String c = cell == null ? "" : cell.trim().toLowerCase();
        if (c.startsWith("cpu")) {
            return "cpu";
        }
        if (c.contains("散热")) {
            return "cooler";
        }
        if (c.contains("显卡") || c.contains("gpu")) {
            return "gpu";
        }
        if (c.contains("主板")) {
            return "mb";
        }
        if (c.contains("内存")) {
            return "ram";
        }
        if (c.contains("电源")) {
            return "psu";
        }
        if (c.contains("硬盘") || c.contains("固态")) {
            return "ssd";
        }
        if (c.contains("机箱")) {
            return "case";
        }
        return null;
    }
}
