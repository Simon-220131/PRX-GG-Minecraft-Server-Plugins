package at.prx.gamblePluginTest.manager;

import at.prx.gamblePluginTest.model.SlotSymbol;
import at.prx.gamblePluginTest.states.Outcome;
import at.prx.gamblePluginTest.states.SlotAction;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class SlotSymbolManager {

    private final JavaPlugin plugin;
    private final List<SlotSymbol> symbols = new ArrayList<>();
    private int totalWeight = 0;
    private final Random random = new Random();

    public SlotSymbolManager(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        symbols.clear();
        totalWeight = 0;

        plugin.reloadConfig();

        ConfigurationSection section =
                plugin.getConfig().getConfigurationSection("slots.symbols");

        if (section == null) {
            plugin.getLogger().severe("[Casino] slots.symbols fehlt in config.yml!");
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(key);
            if (s == null) continue;

            String outcomeRaw = s.getString("outcome", "NEUTRAL"); // oder "outcome"

            int weight = s.getInt("weight", 0);
            if (weight <= 0) {
                plugin.getLogger().warning("[Casino] Symbol " + key + " hat ungültiges weight");
                continue;
            }

            Outcome outcome;
            try {
                outcome = Outcome.valueOf(outcomeRaw.toUpperCase(Locale.ROOT));
            } catch (Exception ex) {
                plugin.getLogger().warning("[Casino] Ungültiger status '" + outcomeRaw + "' bei Symbol " + key + " -> NEUTRAL");
                outcome = Outcome.NEUTRAL;
            }

            // ✅ Action sicher lesen
            String actionRaw = s.getString("action");
            SlotAction action;
            try {
                action = SlotAction.valueOf(actionRaw);
            } catch (Exception ex) {
                plugin.getLogger().warning("[Casino] Ungültige action '" + actionRaw + "' bei Symbol " + key);
                continue;
            }

            String message = s.getString("message", "");
            int poisonSeconds = s.getInt("poison-seconds", 0);

            boolean creeperPlayerDamage =
                    s.getBoolean("creeper.player-damage", false);


            ItemStack displayItem = createDisplayItem(s, key);
            if (displayItem == null) continue;

            symbols.add(new SlotSymbol(
                    key,
                    displayItem,
                    weight,
                    action,
                    outcome,
                    message,
                    poisonSeconds,
                    creeperPlayerDamage
            ));


            totalWeight += weight;

            plugin.getLogger().info("[Casino] Symbol geladen: " + key +
                    " (Action=" + action + ", Weight=" + weight + ")");
        }

        if (symbols.isEmpty()) {
            plugin.getLogger().severe("[Casino] KEINE gültigen Slot-Symbole geladen!");
        }
    }

    public SlotSymbol roll() {
        if (symbols.isEmpty() || totalWeight <= 0) {
            throw new IllegalStateException("Keine Slot-Symbole verfügbar!");
        }

        int rnd = random.nextInt(totalWeight);
        int current = 0;

        for (SlotSymbol symbol : symbols) {
            current += symbol.getWeight();
            if (rnd < current) {
                return symbol;
            }
        }

        // mathematisch unmöglich – aber sicher ist sicher
        return symbols.get(symbols.size() - 1);
    }

    public boolean hasSymbols() {
        return !symbols.isEmpty() && totalWeight > 0;
    }


    /* ================= DISPLAY ITEM ================= */

    private ItemStack createDisplayItem(ConfigurationSection s, String key) {

        ConfigurationSection display = s.getConfigurationSection("display");
        if (display == null) {
            plugin.getLogger().warning("[Casino] display fehlt bei Symbol " + key);
            return null;
        }

        String type = display.getString("type");
        if (type == null) {
            plugin.getLogger().warning("[Casino] display.type fehlt bei Symbol " + key);
            return null;
        }

        ItemStack item;

        // ================= HEAD =================
        if (type.equalsIgnoreCase("HEAD")) {

            String texture = display.getString("texture");
            if (texture == null || texture.isBlank()) {
                plugin.getLogger().warning("[Casino] display.texture fehlt bei HEAD-Symbol " + key);
                return null;
            }

            item = createCustomHead(texture);

            // ================= MATERIAL =================
        } else if (type.equalsIgnoreCase("MATERIAL")) {

            String matRaw = display.getString("material");
            if (matRaw == null || matRaw.isBlank()) {
                plugin.getLogger().warning("[Casino] display.material fehlt bei Symbol " + key);
                return null;
            }

            Material mat = Material.matchMaterial(matRaw);
            if (mat == null) {
                plugin.getLogger().warning("[Casino] Ungültiges Material '" + matRaw + "' bei Symbol " + key);
                return null;
            }

            item = new ItemStack(mat);

        } else {
            plugin.getLogger().warning("[Casino] Unbekannter display.type '" + type + "' bei Symbol " + key);
            return null;
        }

        // ================= NAME =================
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = display.getString("name", " ");
            meta.displayName(Component.text(name, NamedTextColor.WHITE));
            item.setItemMeta(meta);
        }

        return item;
    }



    private ItemStack createCustomHead(String base64) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta == null) return head;

        PlayerProfile profile = plugin.getServer()
                .createProfile(UUID.randomUUID());

        profile.getProperties().add(
                new ProfileProperty("textures", base64)
        );

        meta.setPlayerProfile(profile);
        head.setItemMeta(meta);
        return head;
    }
}
