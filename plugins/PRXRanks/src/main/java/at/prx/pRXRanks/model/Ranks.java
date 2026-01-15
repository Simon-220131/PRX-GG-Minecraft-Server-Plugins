package at.prx.pRXRanks.model;

public enum Ranks {
    ADMIN("\uE001", "§0"),
    SPIELER("\uE002", "§1");

    private final String glyph;
    private final String sortPrefix;

    Ranks(String glyph, String sortPrefix) {
        this.glyph = glyph;
        this.sortPrefix = sortPrefix;
    }

    public String getGlyph() {
        return glyph;
    }

    public String getDisplayName() {
        return name();
    }

    public String getSortPrefix() {
        return sortPrefix;
    }
}
