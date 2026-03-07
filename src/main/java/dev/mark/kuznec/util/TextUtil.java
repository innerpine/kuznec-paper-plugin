package dev.mark.kuznec.util;

import org.bukkit.ChatColor;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TextUtil {

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
        return ChatColor.translateAlternateColorCodes('&', value);
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
}
