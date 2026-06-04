package wcpredictor.entity;

public enum RoundType {
    GROUP_MD1("Group Stage - Matchday 1"),
    GROUP_MD2("Group Stage - Matchday 2"),
    GROUP_MD3("Group Stage - Matchday 3"),
    ROUND_OF_32("Round of 32"),
    ROUND_OF_16("Round of 16"),
    QUARTER_FINAL("Quarter-finals"),
    SEMI_FINAL("Semi-finals"),
    THIRD_PLACE("Third Place"),
    FINAL("Final");

    private final String description;

    RoundType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public int getOrder() {
        return ordinal();
    }
}
