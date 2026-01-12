package at.htlle.duel;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class StakeParser {

    public static List<Stake> parse(String[] args, int fromIndex) {
        List<Stake> stakes = new ArrayList<>();
        for (int i = fromIndex; i < args.length; i++) {
            String token = args[i];

            if (token.equalsIgnoreCase("death")) {
                stakes.add(new Stake.DeathStake());
                continue;
            }

            // shieldxN
            if (token.toLowerCase().startsWith("shieldx")) {
                int n = parseIntSafe(token.substring("shieldx".length()));
                if (n <= 0) throw new IllegalArgumentException("shieldx braucht eine Anzahl > 0");
                stakes.add(new Stake.BannShieldStake(n));
                continue;
            }

            // item:MATERIALxN  (xN optional -> default 1)
            // item:MATERIALxN  (xN optional -> default 1)
            if (token.toLowerCase().startsWith("item:") || true) {
                String use = token;
                if (token.toLowerCase().startsWith("item:")) use = token.substring("item:".length());
                // allow forms like DIAMONDx3 or diamondx3 or diamond
                String[] parts = use.split("x", 2);
                Material mat = Material.matchMaterial(parts[0].toUpperCase());
                if (mat == null) {
                    // not an item, continue to other checks or error
                } else {
                    int amount = 1;
                    if (parts.length == 2) amount = parseIntSafe(parts[1]);
                    if (amount <= 0) throw new IllegalArgumentException("Item amount muss > 0 sein.");
                    stakes.add(new Stake.ItemStake(mat, amount));
                    continue;
                }
            }

            throw new IllegalArgumentException("Unbekannter stake: " + token);
        }
        return stakes;
    }

    private static int parseIntSafe(String s) {
        try { return Integer.parseInt(s.trim()); }
        catch (Exception e) { return -1; }
    }
}