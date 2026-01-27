package at.prx.gamblePluginTest.gui;

import at.prx.gamblePluginTest.executor.SlotActionExecutor;
import at.prx.gamblePluginTest.manager.SlotSymbolManager;
import at.prx.gamblePluginTest.model.SlotSymbol;
import at.prx.gamblePluginTest.states.SpinState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SlotMachineGUI implements Listener {

    private final JavaPlugin plugin;

    private final Map<Player, Inventory> inventories = new HashMap<>();
    private final Set<Player> spinning = new HashSet<>();
    private final Set<Player> respinLocked = new HashSet<>();
    private final Map<Player, Long> cooldowns = new HashMap<>();

    private final SlotSymbolManager symbolManager;
    private final SlotActionExecutor actionExecutor;

    private static final int[] BAND_SLOTS = { 9, 10, 11, 12, 13, 14, 15, 16, 17 };
    private static final int RESULT_SLOT = 13;

    private static final int SPIN_SLOT = 22;
    private static final long COOLDOWN_MS = 1000;

    public SlotMachineGUI(JavaPlugin plugin) {
        this.plugin = plugin;
        this.symbolManager = new SlotSymbolManager(plugin);
        this.actionExecutor = new SlotActionExecutor(plugin);
    }

    /* =========================================================
       GUI ÖFFNEN
       ========================================================= */
    public void open(Player player) {
        Inventory inv = inventories.get(player);

        if (inv == null) {
            inv = Bukkit.createInventory(
                    player,
                    27,
                    Component.text("🎰 Slot Machine", NamedTextColor.RED)
            );

            fillDecor(inv);
            fillInitialBand(inv);
            setSpinButton(inv, SpinState.READY);

            inventories.put(player, inv);
        }

        player.openInventory(inv);
    }

    /* =========================================================
       INVENTORY CLICK
       ========================================================= */
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory inv = inventories.get(player);
        if (inv == null) return;

        // Wichtig: nur reagieren, wenn unser GUI (TopInventory) betroffen ist
        if (!event.getView().getTopInventory().equals(inv)) return;

        event.setCancelled(true);

        if (event.getSlot() != SPIN_SLOT) return;
        if (spinning.contains(player)) return;

        long now = System.currentTimeMillis();
        if (cooldowns.containsKey(player) && cooldowns.get(player) > now) {
            player.sendMessage(Component.text("⏳ Bitte warte kurz...", NamedTextColor.GRAY));
            play(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.6f);
            return;
        }

        startSpin(player, inv);
    }
    /* =========================================================
       INVENTORY CLOSE
       ========================================================= */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!respinLocked.contains(player)) return;

        Inventory inv = inventories.get(player);
        if (inv == null) return;

        // WICHTIG: nicht sofort, sondern 2 Ticks später
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            // Spieler hat evtl. schon ein anderes Inventory offen
            if (player.getOpenInventory().getTopInventory() == inv) return;

            player.openInventory(inv);
        }, 2L);
    }



    /* =========================================================
       SPIN LOGIK
       ========================================================= */
    private void startSpin(Player player, Inventory inv) {
        // Sicherheitscheck: falls Symbols nicht geladen -> keine Animation starten
        if (!symbolManager.hasSymbols()) {
            player.sendMessage(Component.text("❌ Keine gültigen Symbole in der Config gefunden.", NamedTextColor.RED));
            play(player, Sound.ENTITY_VILLAGER_NO, 1f, 0.8f);
            return;
        }

        spinning.add(player);
        play(player, Sound.UI_BUTTON_CLICK, 1f, 1.2f);
        setSpinButton(inv, SpinState.SPINNING);

        runPhase(player, inv, 0);
    }

    private void runPhase(Player player, Inventory inv, int phase) {
        int delay;
        int runs;

        switch (phase) {
            case 0 -> { delay = 2; runs = 30; } // schnell
            case 1 -> { delay = 4; runs = 15; } // langsamer
            case 2 -> { delay = 8; runs = 10; } // sehr langsam
            default -> {
                stopSpin(player, inv);
                return;
            }
        }

        new BukkitRunnable() {
            int count = 0;

            @Override
            public void run() {
                // Spieler könnte währenddessen offline gehen
                if (!player.isOnline()) {
                    spinning.remove(player);
                    cancel();
                    return;
                }

                shiftBand(inv, player);
                count++;

                if (count >= runs) {
                    cancel();
                    runPhase(player, inv, phase + 1);
                }
            }
        }.runTaskTimer(plugin, 0L, delay);
    }

    private void stopSpin(Player player, Inventory inv) {
        spinning.remove(player);
        cooldowns.put(player, System.currentTimeMillis() + COOLDOWN_MS);
        setSpinButton(inv, SpinState.COOLDOWN);

        // 🎯 Endsymbol ziehen & anzeigen (nicht über Material prüfen!)
        SlotSymbol symbol = symbolManager.roll();
        inv.setItem(RESULT_SLOT, symbol.getDisplayItem());

        // 🔊 Stop-Sound
        play(player, Sound.BLOCK_ANVIL_LAND, 0.8f, 1.0f);

        // ✅ Action ausführen (Gamble/BOD Event, Heal, Poison, Shield, etc.)
        boolean forceRespin;
        try {
            forceRespin = actionExecutor.execute(player, symbol);

            if (forceRespin) {
                respinLocked.add(player);
            } else {
                respinLocked.remove(player);
            }
        } catch (Exception ex) {
            plugin.getLogger().severe("[Casino] Fehler beim Ausführen der SlotAction: " + ex.getMessage());
            ex.printStackTrace();
            forceRespin = false;
        }

        // ⏳ Cooldown-Ende → Button wieder aktiv + Sound
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            // Wenn gerade wieder ein Spin läuft, Button nicht überschreiben
            if (!spinning.contains(player)) {
                setSpinButton(inv, SpinState.READY);
            }
            play(player, Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.8f);

        }, Math.max(1L, COOLDOWN_MS / 50));

        // 🔁 FORCE_RESPIN: nach Cooldown automatisch neu starten
        if (forceRespin) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;

                // Wenn Spieler das GUI geschlossen hat, nicht erzwingen
                if (!player.getOpenInventory().getTopInventory().equals(inv)) return;

                // Cooldown respektieren (du wolltest: nicht nötig extra zu ignorieren)
                startSpin(player, inv);
            }, Math.max(1L, COOLDOWN_MS / 50));
        }
    }

    /* =========================================================
       BAND BEWEGUNG
       ========================================================= */
    private void shiftBand(Inventory inv, Player player) {
        // nach rechts schieben
        for (int i = BAND_SLOTS.length - 1; i > 0; i--) {
            inv.setItem(BAND_SLOTS[i], inv.getItem(BAND_SLOTS[i - 1]));
        }

        // links neues Symbol einfügen
        SlotSymbol next = symbolManager.roll();
        inv.setItem(BAND_SLOTS[0], next.getDisplayItem());

        play(player, Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.8f);
    }

    /* =========================================================
       SETUP
       ========================================================= */
    private void fillDecor(Inventory inv) {
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        ItemStack hopper = createItem(Material.HOPPER, "Auswahl");

        for (int i = 0; i < 9; i++) inv.setItem(i, glass);
        for (int i = 18; i < 27; i++) {
            if (i != SPIN_SLOT) inv.setItem(i, glass);
        }
        inv.setItem(4, glowing(hopper));
    }

    private ItemStack glowing(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    private void fillInitialBand(Inventory inv) {
        if (!symbolManager.hasSymbols()) {
            // Fallback, falls Config kaputt ist
            ItemStack fallback = new ItemStack(Material.BARRIER);
            inv.setItem(RESULT_SLOT, fallback);
            for (int slot : BAND_SLOTS) inv.setItem(slot, fallback);
            return;
        }

        for (int slot : BAND_SLOTS) {
            SlotSymbol symbol = symbolManager.roll();
            inv.setItem(slot, symbol.getDisplayItem());
        }
    }

    private void setSpinButton(Inventory inv, SpinState state) {
        Material material;
        Component name;

        switch (state) {
            case READY -> {
                material = Material.LIME_DYE;
                name = Component.text("▶ SPIN", NamedTextColor.GREEN);
            }
            case SPINNING -> {
                material = Material.YELLOW_DYE;
                name = Component.text("⏳ LÄUFT...", NamedTextColor.YELLOW);
            }
            case COOLDOWN -> {
                material = Material.RED_DYE;
                name = Component.text("⌛ WARTEN", NamedTextColor.RED);
            }
            default -> throw new IllegalStateException("Unknown SpinState: " + state);
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            item.setItemMeta(meta);
        }

        inv.setItem(SPIN_SLOT, item);
    }

    /* =========================================================
       HELPERS
       ========================================================= */
    private ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, NamedTextColor.WHITE));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void play(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    /* =========================================================
       CONFIG / RELOAD
       ========================================================= */
    public void reload() {
        symbolManager.reload();
    }

    /* =========================================================
       CLEANUP
       ========================================================= */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        inventories.remove(event.getPlayer());
        spinning.remove(event.getPlayer());
        cooldowns.remove(event.getPlayer());
    }
}
