package at.prx.pRXReprimands.model;

import java.util.UUID;

public record PunishmentHistoryRecord(
        long id,
        PunishmentType type,
        UUID target,
        String targetName,
        String actor,
        String reason,
        long startMillis,
        long endMillis
) {
    public boolean isPermanent() {
        return endMillis == 0L;
    }

    public boolean isActive() {
        return isPermanent() || endMillis > System.currentTimeMillis();
    }
}
