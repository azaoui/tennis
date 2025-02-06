package com.kata.tennisscore.domain;

public enum TennisPoint {
    ZERO(0),
    FIFTEEN(15),
    THIRTY(30),
    FORTY(40);

    private final int value;

    TennisPoint(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    // Advances the score to the next level.
    public TennisPoint next() {
        switch (this) {
            case ZERO:
                return FIFTEEN;
            case FIFTEEN:
                return THIRTY;
            case THIRTY:
                return FORTY;
            default:
                throw new IllegalStateException("Cannot advance score beyond FORTY");
        }
    }
}
