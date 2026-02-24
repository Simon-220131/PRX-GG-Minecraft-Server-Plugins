package at.prx.pRXReprimands.model;

import java.util.UUID;

public record NoteRecord(
        long id,
        UUID target,
        String targetName,
        String actor,
        String note,
        long createdMillis
) {
}
