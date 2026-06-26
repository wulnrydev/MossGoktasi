package dev.wulnrydev.goktasi.util;

import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renk ve metin yardimcisi.
 * <p>
 * Hem klasik {@code &7} renk kodlarini hem de {@code &#RRGGBB} HEX kodlarini
 * destekler. Tum coko-cagrilan regex'ler statik olarak derlenmistir (performans).
 */
public final class TextUtil {

    /** &#RRGGBB formatini yakalayan onceden derlenmis pattern. */
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private TextUtil() {
        // Yardimci sinif; ornek olusturulamaz.
    }

    /**
     * Verilen metindeki HEX ({@code &#RRGGBB}) ve klasik ({@code &a}) renk
     * kodlarini Minecraft'in anlayacagi formata cevirir.
     */
    public static String color(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        // Once HEX kodlarini cevir.
        Matcher matcher = HEX_PATTERN.matcher(input);
        StringBuilder builder = new StringBuilder(input.length() + 32);
        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(builder, Matcher.quoteReplacement(ChatColor.of("#" + hex).toString()));
        }
        matcher.appendTail(builder);

        // Ardindan klasik & kodlarini cevir.
        return ChatColor.translateAlternateColorCodes('&', builder.toString());
    }

    /** Bir liste icindeki tum satirlari renklendirir. */
    public static List<String> color(List<String> input) {
        List<String> out = new ArrayList<>(input.size());
        for (String line : input) {
            out.add(color(line));
        }
        return out;
    }

    /**
     * "150-200" gibi bir araligi parse eder ve dizi {min, max} doner.
     * Tek deger verilirse {deger, deger} doner.
     */
    public static int[] parseRange(String raw, int defMin, int defMax) {
        if (raw == null || raw.isBlank()) {
            return new int[]{defMin, defMax};
        }
        try {
            String[] parts = raw.split("-");
            if (parts.length == 2) {
                int min = Integer.parseInt(parts[0].trim());
                int max = Integer.parseInt(parts[1].trim());
                return min <= max ? new int[]{min, max} : new int[]{max, min};
            }
            int single = Integer.parseInt(raw.trim());
            return new int[]{single, single};
        } catch (NumberFormatException ex) {
            return new int[]{defMin, defMax};
        }
    }
}
