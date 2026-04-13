package dev.mark.kuznec.util;

import org.bukkit.ChatColor;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextUtil {

    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("(?i)&?#([0-9A-F]{6})");
    private static final DecimalFormat PRICE_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(' ');
        PRICE_FORMAT = new DecimalFormat("#,###.##", symbols);
    }

    private TextUtil() {
    }

    public static String colorize(String value) {
        if (value == null) {
            return "";
        }

        String translated = applyHexColors(value);
        return ChatColor.translateAlternateColorCodes('&', translated);
    }

    public static List<String> colorize(List<String> values) {
        List<String> result = new ArrayList<String>();
        if (values == null) {
            return result;
        }
        for (String value : values) {
            result.add(colorize(value));
        }
        return result;
    }

    public static String replace(String value, String target, String replacement) {
        if (value == null) {
            return "";
        }
        return value.replace(target, replacement);
    }

    public static String formatPrice(double price) {
        return PRICE_FORMAT.format(price);
    }

    private static String applyHexColors(String value) {
        Matcher matcher = HEX_COLOR_PATTERN.matcher(value);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(hexToLegacyColor(matcher.group(1))));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    /**
     * Converts a 6-character hex color string (without '#') to Minecraft's legacy
     * §x§R§R§G§G§B§B format understood by all 1.16+ clients.
     */
    private static String hexToLegacyColor(String hex) {
        StringBuilder sb = new StringBuilder("\u00a7x");
        for (char c : hex.toCharArray()) {
            sb.append('\u00a7').append(c);
        }
        return sb.toString();
    }
}
