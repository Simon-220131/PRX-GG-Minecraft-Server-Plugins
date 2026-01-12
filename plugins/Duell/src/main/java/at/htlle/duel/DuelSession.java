package at.htlle.duel;

import java.util.List;
import java.util.UUID;

public record DuelSession(UUID p1, UUID p2, UUID dummy, List<Stake> stakes, Escrow escrow) {

    public static DuelSession players(UUID p1, UUID p2, List<Stake> stakes, Escrow escrow) {
        return new DuelSession(p1, p2, null, stakes, escrow);
    }

    public static DuelSession test(UUID p1, UUID dummy, List<Stake> stakes, Escrow escrow) {
        return new DuelSession(p1, null, dummy, stakes, escrow);
    }

    public boolean isTestDuel() {
        return dummy != null;
    }

    public boolean isDeathMode() {
        return Stake.hasDeath(stakes);
    }

    public boolean isDuelist(UUID u) {
        return p1.equals(u) || (p2 != null && p2.equals(u));
    }

    public UUID other(UUID u) {
        if (p2 == null) return null;
        if (p1.equals(u)) return p2;
        if (p2.equals(u)) return p1;
        return null;
    }
}