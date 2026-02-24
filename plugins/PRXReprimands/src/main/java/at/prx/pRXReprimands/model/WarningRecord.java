package at.prx.pRXReprimands.model;

import java.util.UUID;

public record WarningRecord(
        long id,
        UUID target,
        String targetName,
        String actor,
        String reason,
        long createdMillis
) {
}
