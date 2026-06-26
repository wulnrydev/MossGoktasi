package dev.wulnrydev.goktasi.model;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

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

    private Reward(double chance, Type type, String command, Material material, int amount) {
        this.chance = chance;
        this.type = type;
        this.command = command;
        this.material = material;
        this.amount = amount;
    }

    /**
     * Bir konfigurasyon bolumunden odul olusturur.
     *
     * @return gecerli bir odul veya parse edilemezse {@code null}.
     */
    public static Reward fromSection(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        double chance = section.getDouble("chance", 100.0);
        String rawType = section.getString("type", "command").toUpperCase();

        if ("ITEM".equals(rawType)) {
            Material mat = Material.matchMaterial(section.getString("material", ""));
            if (mat == null) {
                return null;
            }
            int amount = Math.max(1, section.getInt("amount", 1));
            return new Reward(chance, Type.ITEM, null, mat, amount);
        }

        // Varsayilan: komut
        String cmd = section.getString("value", "");
        if (cmd.isBlank()) {
            return null;
        }
        return new Reward(chance, Type.COMMAND, cmd, null, 0);
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
}
