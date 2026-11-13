package ru.arena.game;

public final class Zone {
    public static final int FIRE = 0, ICE = 1, SHOCK = 2, HEAL = 3;
    public int id, kind, owner, team;
    public float x, y, radius, time, total, dps, healPerSec;
    public boolean slowEnemies, follow;
    public float beamT;
}
