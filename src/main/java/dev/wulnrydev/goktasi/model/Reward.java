package dev.wulnrydev.goktasi.model;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Tek bir odul tanimi (config'deki "hit" veya "finish" listesindeki bir eleman).
 * <p>
 * Iki tip vardir:
 * <ul>
 *     <li>{@link Type#COMMAND} - konsoldan komut calistirir.</li>
 *     <li>{@link Type#ITEM} - oyuncuya esya verir.</li>
 * </ul>
 */
public final class Reward {

    public enum Type {
        COMMAND,
        ITEM
    }

    private final double chance;
    private final Type type;

    // COMMAND icin
    private final String command;

    // ITEM icin
    private final Material material;
    private final int amount;
    // Resource pack custom model data. <= 0 ise uygulanmaz.
    private final int customModelData;
    // Ozel gorunen isim (bos ise vanilla isim kullanilir).
    private final String displayName;
    // Ozel lore satirlari (bos ise lore eklenmez).
    private final List<String> lore;

    private Reward(double chance, Type type, String command, Material material, int amount,
                   int customModelData, String displayName, List<String> lore) {
        this.chance = chance;
        this.type = type;
        this.command = command;
        this.material = material;
        this.amount = amount;
        this.customModelData = customModelData;
        this.displayName = displayName;
        this.lore = lore;
    }

    /**
     * Config'deki bir odul listesi elemanindan ({@code Map}) odul olusturur.
     * Oduller YAML'de liste oldugundan her eleman bir {@code Map} olarak gelir.
     *
     * @return gecerli bir odul veya parse edilemezse {@code null}.
     */
    public static Reward fromMap(Map<?, ?> map) {
        if (map == null) {
            return null;
        }
        double chance = toDouble(map.get("chance"), 100.0);
        // Locale.ROOT sart: Turkce locale'de "item".toUpperCase() -> "İTEM" olur
        // ve "ITEM" ile eslesmezdi (Turkce-I sorunu).
        Object typeObj = map.get("type");
        String rawType = (typeObj == null ? "COMMAND" : typeObj.toString().toUpperCase(Locale.ROOT));

        if ("ITEM".equals(rawType)) {
            Object matObj = map.get("material");
            Material mat = matObj == null ? null : Material.matchMaterial(String.valueOf(matObj));
            if (mat == null) {
                return null;
            }
            int amount = Math.max(1, toInt(map.get("amount"), 1));
            // custom-model-data veya custom_model_data anahtarini kabul et.
            Object cmdObj = map.containsKey("custom-model-data") ? map.get("custom-model-data") : map.get("custom_model_data");
            int customModelData = Math.max(0, toInt(cmdObj, 0));
            // Ozel isim ve lore (istege bagli).
            Object nameObj = map.get("name");
            String displayName = nameObj == null ? "" : String.valueOf(nameObj);
            List<String> lore = toStringList(map.get("lore"));
            return new Reward(chance, Type.ITEM, null, mat, amount, customModelData, displayName, lore);
        }

        // Varsayilan: komut
        Object valObj = map.get("value");
        String cmd = valObj == null ? "" : String.valueOf(valObj);
        if (cmd.isBlank()) {
            return null;
        }
        return new Reward(chance, Type.COMMAND, cmd, null, 0, 0, "", Collections.emptyList());
    }

    /** YAML'den gelen bir degeri (List veya tek deger) String listesine cevirir. */
    private static List<String> toStringList(Object o) {
        if (o == null) {
            return Collections.emptyList();
        }
        if (o instanceof List<?> rawList) {
            List<String> out = new ArrayList<>(rawList.size());
            for (Object item : rawList) {
                out.add(item == null ? "" : item.toString());
            }
            return out;
        }
        // Tek satir verilmisse onu da kabul et.
        List<String> single = new ArrayList<>(1);
        single.add(o.toString());
        return single;
    }

    private static double toDouble(Object o, double def) {
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        if (o != null) {
            try {
                return Double.parseDouble(o.toString().trim());
            } catch (NumberFormatException ignored) {
                // Gecersiz deger; varsayilana dus.
            }
        }
        return def;
    }

    private static int toInt(Object o, int def) {
        if (o instanceof Number n) {
            return n.intValue();
        }
        if (o != null) {
            try {
                return Integer.parseInt(o.toString().trim());
            } catch (NumberFormatException ignored) {
                // Gecersiz deger; varsayilana dus.
            }
        }
        return def;
    }

    public double getChance() {
        return chance;
    }

    public Type getType() {
        return type;
    }

    public String getCommand() {
        return command;
    }

    public Material getMaterial() {
        return material;
    }

    public int getAmount() {
        return amount;
    }

    /** Resource pack custom model data; {@code <= 0} ise ayarlanmaz. */
    public int getCustomModelData() {
        return customModelData;
    }

    public boolean hasCustomModelData() {
        return customModelData > 0;
    }

    /** Ozel gorunen isim (bos olabilir). */
    public String getDisplayName() {
        return displayName;
    }

    public boolean hasDisplayName() {
        return displayName != null && !displayName.isEmpty();
    }

    /** Ozel lore satirlari (bos olabilir). */
    public List<String> getLore() {
        return lore;
    }

    public boolean hasLore() {
        return lore != null && !lore.isEmpty();
    }
}
