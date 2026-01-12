package at.htlle.duel;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DamageCredit {

    private final Map<UUID, Credit> credits = new ConcurrentHashMap<>();

    public void credit(UUID victim, UUID attacker, long ttlMillis) {
        credits.put(victim, new Credit(attacker, System.currentTimeMillis() + ttlMillis));
    }

    public UUID getCreditedAttacker(UUID victim) {
        Credit c = credits.get(victim);
        if (c == null) return null;
        if (System.currentTimeMillis() > c.expiresAt) {
            credits.remove(victim);
            return null;
        }
        return c.attacker;
    }

    public void clear(UUID victim) {
        credits.remove(victim);
    }

    private record Credit(UUID attacker, long expiresAt) {}
}