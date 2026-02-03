package at.prx.pRXReprimands.model;

import java.util.UUID;

public record PunishmentRecord(
        PunishmentType type,
        UUID target,
        String targetName,
        String actor,
        String reason,
        long startMillis,
        long endMillis
) {
    public boolean isPermanent() {
        return endMillis <= 0;
    }

    public boolean isExpired() {
        return !isPermanent() && System.currentTimeMillis() > endMillis;
    }
}
