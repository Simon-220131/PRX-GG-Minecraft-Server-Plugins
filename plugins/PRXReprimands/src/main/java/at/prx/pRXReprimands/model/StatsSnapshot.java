package at.prx.pRXReprimands.model;

import java.util.List;
import java.util.Map;

public record StatsSnapshot(
        int totalPunishments,
        int totalWarnings,
        Map<PunishmentType, Integer> punishmentTotals,
        Map<PunishmentType, Integer> activePunishments,
        List<ReasonCount> topPunishmentReasons,
        List<ReasonCount> topWarningReasons
) {
}
