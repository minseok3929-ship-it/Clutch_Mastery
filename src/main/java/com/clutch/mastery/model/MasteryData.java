package com.clutch.mastery.model;

public class MasteryData {
    private int level;
    private int exp;

    public MasteryData() {
        this(1, 0);
    }

    public MasteryData(int level, int exp) {
        this.level = Math.max(1, level);
        this.exp = Math.max(0, exp);
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(1, level);
    }

    public int getExp() {
        return exp;
    }

    public void setExp(int exp) {
        this.exp = Math.max(0, exp);
    }

    public MasteryData copy() {
        return new MasteryData(level, exp);
    }
}
