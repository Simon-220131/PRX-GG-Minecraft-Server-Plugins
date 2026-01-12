package at.htlle.duel;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public sealed interface Stake permits Stake.ItemStake, Stake.BannShieldStake, Stake.DeathStake {

    record ItemStake(Material material, int amount) implements Stake {}
    record BannShieldStake(int amount) implements Stake {}
    record DeathStake() implements Stake {}

    static boolean hasDeath(List<Stake> stakes) {
        return stakes.stream().anyMatch(s -> s instanceof DeathStake);
    }

    static String pretty(List<Stake> stakes) {
        if (stakes == null || stakes.isEmpty()) return "friendly";
        List<String> parts = new ArrayList<>();
        for (Stake s : stakes) {
            if (s instanceof ItemStake it) parts.add(it.material().name() + "x" + it.amount());
            else if (s instanceof BannShieldStake bs) parts.add("shieldx" + bs.amount());
            else if (s instanceof DeathStake) parts.add("death");
        }
        return String.join(", ", parts);
    }
}