package dev.mark.kuznec.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class TextUtilTest {

    @Test
    void colorizeSupportsHexAndLegacyCodes() {
        String value = TextUtil.colorize("&#55FF99Hello &cWorld");
        // §x§5§5§F§F§9§9 is the legacy §x format for #55FF99
        String expected = "\u00a7x\u00a75\u00a75\u00a7F\u00a7F\u00a79\u00a79"
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
