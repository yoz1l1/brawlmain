package ru.arena.game;

/** Простой ИИ ботов: сближается на удобную дистанцию, стреляет, использует рывок и супер. */
final class Bots {
    private Bots() {}

    static void think(Match m, Fighter f) {
        BrawlerDef d = f.def;
        f.attackHeld = false;
        float tick = m.tick;
        Fighter t = null;
        float best = 99;
        for (Fighter e : m.fighters) {
            if (!e.alive || e.team == f.team || !m.visibleTo(e, f.team)) continue;
            float dd = (float) Math.hypot(e.x - f.x, e.y - f.y);
            if (dd < best) { best = dd; t = e; }
        }
        float ux = 0, uy = 0;
        if (t != null) {
            float dx = t.x - f.x, dy = t.y - f.y;
            float dist = best;
            float nx = dx / Math.max(0.01f, dist), ny = dy / Math.max(0.01f, dist);
            float err = (m.rnd.nextFloat() - 0.5f) * 0.25f;
            float ca = (float) Math.cos(err), sa = (float) Math.sin(err);
            f.aimDx = (nx * ca - ny * sa) * dist; f.aimDy = (nx * sa + ny * ca) * dist;
            boolean melee = d.atk == BrawlerDef.Atk.MELEE;
            float ideal = melee ? 1.3f : d.atkRange * 0.62f;
            float strafe = (float) Math.sin(f.botPhase + tick * 0.06f);
            boolean low = f.hp < d.hp * 0.3f;
            if (low && dist < d.atkRange + 2) {
                ux = -nx; uy = -ny;
                if (f.dashCd <= 0 && dist < 4 && m.rnd.nextFloat() < 0.15f) { f.mx = ux; f.my = uy; f.dashPressed = true; }
            } else if (dist > ideal + 0.8f) {
                ux = nx - ny * strafe * 0.3f; uy = ny + nx * strafe * 0.3f;
            } else if (dist < ideal - 0.8f && !melee) {
                ux = -nx; uy = -ny;
            } else {
                ux = -ny * strafe; uy = nx * strafe;
            }
            boolean clear = d.atk == BrawlerDef.Atk.LOB || !m.map.lineBlocked(f.x, f.y, t.x, t.y);
            if (dist <= d.atkRange * 0.95f && clear && f.ammo >= 1f) f.attackHeld = true;
            if (f.superCharge >= 1f && dist < superReach(d) && clear) f.superPressed = true;
            if (f.dashCd <= 0 && !low && dist > d.atkRange && dist < d.atkRange + d.dashDist + 2 && m.rnd.nextFloat() < 0.04f) {
                f.mx = nx; f.my = ny; f.dashPressed = true;
            }
        } else {
            f.botWpT -= Match.DT;
            if (f.botWpX < 0 || f.botWpT <= 0 || Math.hypot(f.botWpX - f.x, f.botWpY - f.y) < 1.5) {
                f.botWpX = 4 + m.rnd.nextFloat() * (GameMap.W - 8);
                f.botWpY = 3 + m.rnd.nextFloat() * (GameMap.H - 6);
                f.botWpT = 6f;
            }
            float dx = f.botWpX - f.x, dy = f.botWpY - f.y;
            float len = (float) Math.max(0.01, Math.hypot(dx, dy));
            ux = dx / len; uy = dy / len;
            f.aimDx = ux; f.aimDy = uy;
        }
        steer(m, f, ux, uy);
    }

    private static float superReach(BrawlerDef d) {
        switch (d.sup) {
            case STORM: return 3.0f;
            case SLAM: return 5.5f;
            case STRIKE: return 8f;
            default: return d.supRange * 0.85f;
        }
    }

    private static void steer(Match m, Fighter f, float ux, float uy) {
        float len = (float) Math.hypot(ux, uy);
        if (len < 0.01f) { f.mx = 0; f.my = 0; return; }
        ux /= len; uy /= len;
        f.botTurnT -= Match.DT;
        float[] offs = {0f, 0.7f, -0.7f, 1.4f, -1.4f, 2.1f, -2.1f};
        for (float o : offs) {
            float a = o * f.botTurn;
            float ca = (float) Math.cos(a), sa = (float) Math.sin(a);
            float vx = ux * ca - uy * sa, vy = ux * sa + uy * ca;
            if (!m.map.circleHits(f.x + vx * 0.7f, f.y + vy * 0.7f, Match.R)) {
                if (o != 0f && f.botTurnT <= 0) f.botTurnT = 1f;
                f.mx = vx; f.my = vy;
                return;
            }
        }
        f.botTurn = -f.botTurn;
        f.mx = 0; f.my = 0;
    }
}
