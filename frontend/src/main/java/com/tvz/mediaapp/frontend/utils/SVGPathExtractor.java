package com.tvz.mediaapp.frontend.utils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SVGPathExtractor {
    public static String extractSVGPath(String filePath) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            Pattern pattern = Pattern.compile("d=\"([^\"]*)\"");
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
