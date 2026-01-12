package at.htlle.duel;

import java.util.List;
import java.util.UUID;

public record DuelInvite(
        UUID inviter,
        UUID invited,
        List<Stake> stakes,
        long createdAt,
        String worldName
) {}
