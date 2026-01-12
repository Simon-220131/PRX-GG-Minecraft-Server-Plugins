package at.htlle.duel;

import at.htlle.util.ItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class Escrow {

    private final Map<UUID, List<ItemStack>> withdrawnItems = new HashMap<>();
    private final Map<UUID, Integer> withdrawnShields = new HashMap<>();

    public String withdrawAll(Player p1, Player p2, List<Stake> stakes) {
        withdrawnItems.put(p1.getUniqueId(), new ArrayList<>());
        withdrawnItems.put(p2.getUniqueId(), new ArrayList<>());
        withdrawnShields.put(p1.getUniqueId(), 0);
        withdrawnShields.put(p2.getUniqueId(), 0);

        for (Stake s : stakes) {
            if (s instanceof Stake.ItemStake it) {
                // both must pay same stakes (symmetrical)
                if (!ItemUtil.hasEnough(p1, it.material(), it.amount())) return p1.getName() + " hat nicht genug " + it.material();
                if (!ItemUtil.hasEnough(p2, it.material(), it.amount())) return p2.getName() + " hat nicht genug " + it.material();

                withdrawnItems.get(p1.getUniqueId()).addAll(ItemUtil.removeMaterial(p1, it.material(), it.amount()));
                withdrawnItems.get(p2.getUniqueId()).addAll(ItemUtil.removeMaterial(p2, it.material(), it.amount()));
            }
            else if (s instanceof Stake.BannShieldStake bs) {
                // both pay shields
                if (!ItemUtil.hasBannShield(p1, bs.amount())) return p1.getName() + " hat nicht genug Bannshields";
                if (!ItemUtil.hasBannShield(p2, bs.amount())) return p2.getName() + " hat nicht genug Bannshields";

                ItemUtil.removeBannShield(p1, bs.amount());
                ItemUtil.removeBannShield(p2, bs.amount());

                withdrawnShields.put(p1.getUniqueId(), withdrawnShields.get(p1.getUniqueId()) + bs.amount());
                withdrawnShields.put(p2.getUniqueId(), withdrawnShields.get(p2.getUniqueId()) + bs.amount());
            }
            else if (s instanceof Stake.DeathStake) {
                // no escrow needed
            }
        }

        return null;
    }

    public void refundAll(Player p1, Player p2) {
        if (p1 != null) refund(p1);
        if (p2 != null) refund(p2);
        withdrawnItems.clear();
        withdrawnShields.clear();
    }

    private void refund(Player p) {
        List<ItemStack> items = withdrawnItems.getOrDefault(p.getUniqueId(), List.of());
        for (ItemStack it : items) p.getInventory().addItem(it);

        int shields = withdrawnShields.getOrDefault(p.getUniqueId(), 0);
        if (shields > 0) ItemUtil.giveBannShield(p, shields);
    }

    public void payoutWinner(Player winner) {
        if (winner == null) return;

        // give all items to winner
        for (List<ItemStack> list : withdrawnItems.values()) {
            for (ItemStack it : list) winner.getInventory().addItem(it);
        }
        // give all shields
        int totalShields = withdrawnShields.values().stream().mapToInt(Integer::intValue).sum();
        if (totalShields > 0) ItemUtil.giveBannShield(winner, totalShields);

        withdrawnItems.clear();
        withdrawnShields.clear();
    }

    public String withdrawFromOne(Player p, List<Stake> stakes) {
        withdrawnItems.put(p.getUniqueId(), new ArrayList<>());
        withdrawnShields.put(p.getUniqueId(), 0);

        for (Stake s : stakes) {
            if (s instanceof Stake.ItemStake it) {
                if (!ItemUtil.hasEnough(p, it.material(), it.amount()))
                    return p.getName() + " hat nicht genug " + it.material();

                withdrawnItems.get(p.getUniqueId()).addAll(ItemUtil.removeMaterial(p, it.material(), it.amount()));
            }
            else if (s instanceof Stake.BannShieldStake bs) {
                if (!ItemUtil.hasBannShield(p, bs.amount()))
                    return p.getName() + " hat nicht genug Bannshields";

                ItemUtil.removeBannShield(p, bs.amount());
                withdrawnShields.put(p.getUniqueId(), withdrawnShields.get(p.getUniqueId()) + bs.amount());
            }
            else if (s instanceof Stake.DeathStake) {
                // no escrow
            }
        }
        return null;
    }

    public void refundToOne(Player p) {
        if (p == null) return;

        List<ItemStack> items = withdrawnItems.getOrDefault(p.getUniqueId(), List.of());
        for (ItemStack it : items) p.getInventory().addItem(it);

        int shields = withdrawnShields.getOrDefault(p.getUniqueId(), 0);
        if (shields > 0) ItemUtil.giveBannShield(p, shields);
    }

    public void payoutToOne(Player winner) {
        if (winner == null) return;

        for (List<ItemStack> list : withdrawnItems.values()) {
            for (ItemStack it : list) winner.getInventory().addItem(it);
        }

        int totalShields = withdrawnShields.values().stream().mapToInt(Integer::intValue).sum();
        if (totalShields > 0) ItemUtil.giveBannShield(winner, totalShields);
    }
}