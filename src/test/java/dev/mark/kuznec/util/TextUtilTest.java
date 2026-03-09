package dev.mark.kuznec.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class TextUtilTest {

    @Test
    void colorizeSupportsHexAndLegacyCodes() {
        String value = TextUtil.colorize("&#55FF99Hello &cWorld");
        String expected = net.md_5.bungee.api.ChatColor.of("#55FF99").toString()
                + "Hello "
                + org.bukkit.ChatColor.RED
                + "World";
        assertEquals(expected, value);
    }

    @Test
    void formatPriceUsesSpacesAsGroupingSeparator() {
        assertEquals("40 000", TextUtil.formatPrice(40000));
    }
}
