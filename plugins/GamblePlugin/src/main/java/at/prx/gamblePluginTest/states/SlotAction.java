package at.prx.gamblePluginTest.states;

public enum SlotAction {
    // Gewinne
    BAN_COUNTER_MINUS,
    BAN_SHIELD,
    FULL_HEAL,

    // Verluste
    INSTANT_BAN,
    BAN_COUNTER_PLUS,
    POISON,
    SPAWN_CREEPER,

    // Neutral
    FORCE_RESPIN
}
