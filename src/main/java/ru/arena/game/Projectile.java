package ru.arena.game;

import java.util.HashSet;
import java.util.Set;

public final class Projectile {
    public int id, owner, team, style;
    public float x, y, dx, dy, speed, left, dmg, radius = 0.18f, splash, slowSec, stunSec, knock, heal;
    public boolean pierce, lob;
    public float tx, ty, total;
    public final Set<Integer> hit = new HashSet<>();

    public float angle() { return (float) Math.atan2(dy, dx); }
    public float progress() { return lob && total > 0 ? 1f - Math.max(0, left) / total : -1f; }
}
