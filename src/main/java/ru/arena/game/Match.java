package ru.arena.game;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** Сервер-авторитетная симуляция матча «Нокаут» (команда на команду). Не зависит от Minecraft. */
public final class Match {
    public static final float DT = 0.05f;
    public static final int WIN_SCORE = 12;
    public static final float MATCH_TIME = 150f;
    public static final float COUNTDOWN = 3.2f;
    public static final float END_TIME = 7f;
    public static final float R = 0.4f;
    public static final int STATE_COUNTDOWN = 0, STATE_PLAY = 1, STATE_END = 2;

    public static final class Slot {
        public final String name; public final int team, brawler; public final boolean bot;
        public Slot(String name, int team, int brawler, boolean bot) {
            this.name = name; this.team = team; this.brawler = brawler; this.bot = bot;
        }
    }

    public final GameMap map;
    public final List<Fighter> fighters = new ArrayList<>();
    public final List<Projectile> projs = new ArrayList<>();
    public final List<Zone> zones = new ArrayList<>();
    public final List<Fx> fx = new ArrayList<>();
    public final int[] score = new int[2];
    public int state = STATE_COUNTDOWN;
    public float stateT = COUNTDOWN;
    public float timeLeft = MATCH_TIME;
    public int winner = -1; // 0/1 команда, 2 ничья
    public long tick;
    public final Random rnd;
    private int nextProj = 1, nextZone = 1;

    public Match(int mapIndex, long seed, List<Slot> slots) {
        map = new GameMap(mapIndex);
        rnd = new Random(seed);
        int[] used = {0, 0};
        for (int i = 0; i < slots.size(); i++) {
            Slot s = slots.get(i);
            Fighter f = new Fighter(i, s.name, s.team, Brawlers.get(s.brawler), s.bot);
            List<float[]> sp = s.team == 0 ? map.spawns0 : map.spawns1;
            float[] p = sp.get(used[s.team]++ % sp.size());
            f.x = p[0]; f.y = p[1];
            f.aim = s.team == 0 ? 0f : (float) Math.PI;
            f.botPhase = rnd.nextFloat() * 6.28f;
            fighters.add(f);
        }
    }

    public boolean finished() { return state == STATE_END && stateT <= 0; }

    // ---------------------------------------------------------------- такт

    public void tick() {
        fx.clear();
        tick++;
        if (state == STATE_COUNTDOWN) {
            stateT -= DT;
            if (stateT <= 0) { state = STATE_PLAY; stateT = 0; }
            return;
        }
        if (state == STATE_END) { stateT -= DT; return; }
        timeLeft -= DT;
        for (Fighter f : fighters) updateFighter(f);
        updateProjectiles();
        updateZones();
        if (score[0] >= WIN_SCORE || score[1] >= WIN_SCORE || timeLeft <= 0) endMatch();
    }

    private void endMatch() {
        timeLeft = Math.max(0, timeLeft);
        state = STATE_END;
        stateT = END_TIME;
        winner = score[0] > score[1] ? 0 : score[1] > score[0] ? 1 : 2;
        for (Fighter f : fighters) { f.attackHeld = false; f.dashing = false; }
    }

    private void updateFighter(Fighter f) {
        BrawlerDef d = f.def;
        if (!f.alive) {
            f.respawnT -= DT;
            if (f.respawnT <= 0) respawn(f);
            return;
        }
        if (f.bot) Bots.think(this, f);

        f.atkCd = Math.max(0, f.atkCd - DT);
        f.dashCd = Math.max(0, f.dashCd - DT);
        f.slowT = Math.max(0, f.slowT - DT);
        f.stunT = Math.max(0, f.stunT - DT);
        f.revealT = Math.max(0, f.revealT - DT);
        f.stealthT = Math.max(0, f.stealthT - DT);
        f.spawnProt = Math.max(0, f.spawnProt - DT);
        if (f.ammo < 3f) f.ammo = Math.min(3f, f.ammo + DT / d.reload);

        if (f.aimDx != 0 || f.aimDy != 0) f.aim = (float) Math.atan2(f.aimDy, f.aimDx);
        boolean stunned = f.stunT > 0;

        if (f.dashing) {
            stepDash(f);
        } else {
            if (!stunned) {
                if (f.dashPressed && f.dashCd <= 0) startDash(f);
                else if (f.superPressed && f.superCharge >= 1f) useSuper(f);
                else if (f.attackHeld && f.ammo >= 1f && f.atkCd <= 0) attack(f);
            }
            if (!f.dashing && f.alive) {
                float len = (float) Math.hypot(f.mx, f.my);
                float mx = f.mx, my = f.my;
                if (len > 1f) { mx /= len; my /= len; }
                float sp = stunned ? 0f : d.speed * (f.slowT > 0 ? 0.6f : 1f);
                move(f, mx * sp * DT, my * sp * DT);
            }
        }
        if (Math.abs(f.kbx) + Math.abs(f.kby) > 0.05f) {
            move(f, f.kbx * DT, f.kby * DT);
            f.kbx *= 0.82f; f.kby *= 0.82f;
        } else { f.kbx = 0; f.kby = 0; }
        f.dashPressed = false;
        f.superPressed = false;
    }

    private void move(Fighter f, float dx, float dy) {
        float nx = f.x + dx;
        if (!map.circleHits(nx, f.y, R)) f.x = nx;
        float ny = f.y + dy;
        if (!map.circleHits(f.x, ny, R)) f.y = ny;
    }

    private void respawn(Fighter f) {
        List<float[]> sp = f.team == 0 ? map.spawns0 : map.spawns1;
        float[] best = sp.get(rnd.nextInt(sp.size()));
        float bestScore = -1;
        for (int i = 0; i < 3; i++) {
            float[] c = sp.get(rnd.nextInt(sp.size()));
            float m = 99;
            for (Fighter e : fighters) if (e.alive && e.team != f.team) m = Math.min(m, dist(e.x, e.y, c[0], c[1]));
            if (m > bestScore) { bestScore = m; best = c; }
        }
        f.x = best[0]; f.y = best[1];
        f.hp = f.def.hp; f.ammo = 3f; f.alive = true; f.spawnProt = 1.6f;
        f.slowT = f.stunT = f.stealthT = 0; f.kbx = f.kby = 0; f.atkCd = 0;
        f.aim = f.team == 0 ? 0f : (float) Math.PI;
        fx.add(new Fx(Fx.SPAWN, f.team, f.x, f.y, 0, 0));
    }

    // ------------------------------------------------------------- урон

    private static float dist(float x1, float y1, float x2, float y2) {
        return (float) Math.hypot(x2 - x1, y2 - y1);
    }

    private void damage(Fighter t, float amount, Fighter src) {
        if (!t.alive || t.invulnerable()) return;
        float real = Math.min(amount, t.hp);
        t.hp -= amount;
        if (src != null) {
            src.dmgDealt += real;
            src.superCharge = Math.min(1f, src.superCharge + real / src.def.supNeed);
            src.revealT = Math.max(src.revealT, 0.9f);
        }
        if (t.hp <= 0) {
            t.hp = 0; t.alive = false; t.deaths++; t.respawnT = 3.5f; t.dashing = false;
            t.kbx = t.kby = 0;
            if (src != null && src.team != t.team) { src.kills++; score[src.team]++; }
            fx.add(new Fx(Fx.DEATH, t.team, t.x, t.y, 0, 0));
        }
    }

    private void healFighter(Fighter t, float amount) {
        if (!t.alive || amount <= 0) return;
        t.hp = Math.min(t.def.hp, t.hp + amount);
        fx.add(new Fx(Fx.HEAL, 0, t.x, t.y, 0, 0));
    }

    private void knockFrom(Fighter e, float x, float y, float power) {
        float dx = e.x - x, dy = e.y - y;
        float len = (float) Math.hypot(dx, dy);
        if (len < 0.01f) { dx = 1; dy = 0; len = 1; }
        e.kbx += dx / len * power; e.kby += dy / len * power;
    }

    private void explode(float x, float y, float radius, float dmg, Fighter owner, int team,
                         float stun, float knock, int style, Fighter skip) {
        fx.add(new Fx(Fx.EXPLOSION, style, x, y, radius, 0));
        for (Fighter e : fighters) {
            if (!e.alive || e.team == team || e == skip) continue;
            if (dist(e.x, e.y, x, y) < radius + R) {
                damage(e, dmg, owner);
                if (!e.alive) continue;
                if (stun > 0) e.stunT = Math.max(e.stunT, stun);
                if (knock > 0) knockFrom(e, x, y, knock);
            }
        }
    }

    // ------------------------------------------------------------ атака

    private Projectile spawnProj(Fighter f, float ang, float speed, float range, float dmg, int style) {
        Projectile p = new Projectile();
        p.id = nextProj++; p.owner = f.id; p.team = f.team; p.style = style;
        p.dx = (float) Math.cos(ang); p.dy = (float) Math.sin(ang);
        p.x = f.x + p.dx * 0.5f; p.y = f.y + p.dy * 0.5f;
        p.speed = speed; p.left = range; p.dmg = dmg;
        projs.add(p);
        return p;
    }

    private void attack(Fighter f) {
        BrawlerDef d = f.def;
        f.ammo -= 1f;
        f.atkCd = d.atkCd;
        f.revealT = Math.max(f.revealT, 1.0f);
        float ang = f.aim;
        switch (d.atk) {
            case SPREAD: {
                int n = d.pellets;
                for (int i = 0; i < n; i++) {
                    float off = n > 1 ? (i - (n - 1) / 2f) * (float) Math.toRadians(d.spread) / (n - 1) : 0;
                    off += (rnd.nextFloat() - 0.5f) * 0.05f;
                    spawnProj(f, ang + off, d.atkSpeed * (0.92f + rnd.nextFloat() * 0.16f), d.atkRange, d.atkDmg, d.projStyle);
                }
                break;
            }
            case BOLT: {
                Projectile p = spawnProj(f, ang, d.atkSpeed, d.atkRange, d.atkDmg, d.projStyle);
                p.splash = d.splash; p.radius = 0.25f;
                break;
            }
            case PIERCE: {
                Projectile p = spawnProj(f, ang, d.atkSpeed, d.atkRange, d.atkDmg, d.projStyle);
                p.pierce = true; p.slowSec = d.slowSec; p.radius = 0.22f;
                break;
            }
            case HEAL: {
                Projectile p = spawnProj(f, ang, d.atkSpeed, d.atkRange, d.atkDmg, d.projStyle);
                p.heal = d.heal; p.radius = 0.28f;
                break;
            }
            case LOB: {
                float dist = (float) Math.hypot(f.aimDx, f.aimDy);
                if (dist < 0.01f) dist = d.atkRange;
                dist = Math.max(2.0f, Math.min(d.atkRange, dist));
                Projectile p = spawnProj(f, ang, d.atkSpeed, dist, d.atkDmg, d.projStyle);
                p.x = f.x; p.y = f.y;
                p.lob = true; p.splash = d.splash; p.total = dist; p.tx = f.x + p.dx * dist; p.ty = f.y + p.dy * dist;
                break;
            }
            case CHAIN: castChain(f); break;
            case MELEE: castMelee(f); break;
            default: break;
        }
    }

    private void castMelee(Fighter f) {
        BrawlerDef d = f.def;
        fx.add(new Fx(Fx.SLASH, d.id, f.x, f.y, f.aim, d.atkRange));
        float half = (float) Math.toRadians(d.arc) / 2f;
        for (Fighter e : fighters) {
            if (!e.alive || e.team == f.team) continue;
            float dx = e.x - f.x, dy = e.y - f.y;
            float dd = (float) Math.hypot(dx, dy);
            if (dd > d.atkRange + R) continue;
            float diff = angleDiff((float) Math.atan2(dy, dx), f.aim);
            if (dd > 0.6f && Math.abs(diff) > half) continue;
            damage(e, d.atkDmg, f);
            fx.add(new Fx(Fx.HIT, d.projStyle, e.x, e.y, 0, 0));
            if (e.alive && d.knock > 0) knockFrom(e, f.x, f.y, d.knock);
        }
    }

    private static float angleDiff(float a, float b) {
        float d = a - b;
        while (d > Math.PI) d -= 2 * Math.PI;
        while (d < -Math.PI) d += 2 * Math.PI;
        return d;
    }

    private void castChain(Fighter f) {
        BrawlerDef d = f.def;
        float dx = (float) Math.cos(f.aim), dy = (float) Math.sin(f.aim);
        float lim = map.rayLimit(f.x, f.y, dx, dy, d.atkRange);
        Fighter first = null;
        float bestT = 1e9f;
        for (Fighter e : fighters) {
            if (!e.alive || e.team == f.team || e.invulnerable()) continue;
            float ex = e.x - f.x, ey = e.y - f.y;
            float t = ex * dx + ey * dy;
            if (t < 0 || t > lim + 0.3f) continue;
            float perp = Math.abs(ex * dy - ey * dx);
            if (perp < 0.8f && t < bestT) { first = e; bestT = t; }
        }
        float sx = f.x + dx * 0.5f, sy = f.y + dy * 0.5f;
        if (first == null) {
            fx.add(new Fx(Fx.BEAM, d.id, sx, sy, f.x + dx * lim, f.y + dy * lim));
            return;
        }
        List<Fighter> seen = new ArrayList<>();
        Fighter cur = first;
        float px = sx, py = sy, dmg = d.atkDmg;
        for (int i = 0; i < 3 && cur != null; i++) {
            fx.add(new Fx(Fx.BEAM, d.id, px, py, cur.x, cur.y));
            damage(cur, dmg, f);
            fx.add(new Fx(Fx.HIT, 9, cur.x, cur.y, 0, 0));
            seen.add(cur);
            px = cur.x; py = cur.y; dmg *= 0.8f;
            Fighter next = null; float nb = 3.4f;
            for (Fighter e : fighters) {
                if (!e.alive || e.team == f.team || seen.contains(e) || e.invulnerable()) continue;
                float dd = dist(px, py, e.x, e.y);
                if (dd < nb && !map.lineBlocked(px, py, e.x, e.y)) { nb = dd; next = e; }
            }
            cur = next;
        }
    }

    // ------------------------------------------------------------ рывок

    private void beginDash(Fighter f, float ex, float ey, float dur, boolean invuln, float dmg, int style, boolean slam) {
        f.dashing = true; f.dashT = 0; f.dashDur = Math.max(0.05f, dur);
        f.dsx = f.x; f.dsy = f.y; f.dex = ex; f.dey = ey;
        f.dashInvuln = invuln; f.dashDmg = dmg; f.dashStyle = style; f.slamOnLand = slam;
        f.dashHit.clear();
        fx.add(new Fx(Fx.DASH, style, f.x, f.y, ex, ey));
    }

    private void startDash(Fighter f) {
        BrawlerDef d = f.def;
        float mx = f.mx, my = f.my;
        float len = (float) Math.hypot(mx, my);
        float dx, dy;
        if (len > 0.1f) { dx = mx / len; dy = my / len; }
        else { dx = (float) Math.cos(f.aim); dy = (float) Math.sin(f.aim); }
        float best = 0;
        if (d.dashThrough) {
            for (float s = d.dashDist; s >= 0.5f; s -= 0.25f) {
                if (!map.circleHits(f.x + dx * s, f.y + dy * s, R)) { best = s; break; }
            }
        } else {
            for (float s = 0.2f; s <= d.dashDist + 0.001f; s += 0.2f) {
                if (map.circleHits(f.x + dx * s, f.y + dy * s, R)) break;
                best = s;
            }
        }
        if (best < 0.4f) return;
        f.dashCd = d.dashCd;
        f.revealT = Math.max(f.revealT, 0.4f);
        float sx = f.x, sy = f.y;
        beginDash(f, f.x + dx * best, f.y + dy * best, d.dashDur, d.dashInvuln, d.dashDmg, d.dashStyle, false);
        switch (d.id) {
            case 5: f.stealthT = 1.8f; break;
            case 6: addZone(f, Zone.HEAL, sx, sy, 1.5f, 1.6f, 0, 380, false, false); healFighter(f, 350); break;
            case 7: explode(sx, sy, 1.6f, d.dashDmg, f, f.team, 0.2f, 5f, 6, null); break;
            default: break;
        }
    }

    private void stepDash(Fighter f) {
        f.dashT += DT;
        float k = Math.min(1f, f.dashT / f.dashDur);
        f.x = f.dsx + (f.dex - f.dsx) * k;
        f.y = f.dsy + (f.dey - f.dsy) * k;
        if (f.dashDmg > 0 && !f.slamOnLand && f.def.id != 7) {
            for (Fighter e : fighters) {
                if (!e.alive || e.team == f.team || f.dashHit.contains(e.id)) continue;
                if (dist(e.x, e.y, f.x, f.y) < 0.95f) {
                    f.dashHit.add(e.id);
                    damage(e, f.dashDmg, f);
                    fx.add(new Fx(Fx.HIT, f.def.dashStyle == 3 ? 9 : 4, e.x, e.y, 0, 0));
                    if (e.alive && f.def.id == 4) { knockFrom(e, f.dsx, f.dsy, 7f); e.stunT = Math.max(e.stunT, 0.25f); }
                }
            }
        }
        if (k >= 1f) {
            f.dashing = false;
            if (f.slamOnLand) {
                f.slamOnLand = false;
                BrawlerDef d = f.def;
                explode(f.x, f.y, d.supRadius, d.supDmg, f, f.team, d.supStun, 8f, 4, null);
                fx.add(new Fx(Fx.SUPER, d.id, f.x, f.y, 0, 0));
            }
            if (f.def.id == 2) addZone(f, Zone.ICE, f.x, f.y, 1.7f, 2.6f, 0, 0, true, false);
        }
    }

    // ------------------------------------------------------------ супер

    private Zone addZone(Fighter f, int kind, float x, float y, float radius, float time,
                         float dps, float heal, boolean slow, boolean follow) {
        Zone z = new Zone();
        z.id = nextZone++; z.kind = kind; z.owner = f.id; z.team = f.team;
        z.x = x; z.y = y; z.radius = radius; z.time = z.total = time;
        z.dps = dps; z.healPerSec = heal; z.slowEnemies = slow; z.follow = follow;
        zones.add(z);
        return z;
    }

    private void useSuper(Fighter f) {
        BrawlerDef d = f.def;
        float dist = (float) Math.hypot(f.aimDx, f.aimDy);
        if (dist < 0.01f) dist = d.supRange;
        float ang = f.aim;
        float ca = (float) Math.cos(ang), sa = (float) Math.sin(ang);
        boolean ok = true;
        switch (d.sup) {
            case BLAST: {
                int n = d.supCount;
                for (int i = 0; i < n; i++) {
                    float off = (i - (n - 1) / 2f) * (float) Math.toRadians(d.supSpread) / (n - 1);
                    Projectile p = spawnProj(f, ang + off, d.supSpeed, d.supRange, d.supDmg, 7);
                    p.knock = d.supKnock; p.radius = 0.28f;
                }
                break;
            }
            case ROCKETS: {
                int n = d.supCount;
                for (int i = 0; i < n; i++) {
                    float off = (i - (n - 1) / 2f) * (float) Math.toRadians(d.supSpread) / (n - 1);
                    off += (rnd.nextFloat() - 0.5f) * 0.06f;
                    Projectile p = spawnProj(f, ang + off, d.supSpeed * (0.9f + rnd.nextFloat() * 0.2f), d.supRange, d.supDmg, 5);
                    p.splash = d.supRadius; p.radius = 0.22f;
                }
                break;
            }
            case METEOR: {
                float s = Math.min(dist, d.supRange);
                addZone(f, Zone.FIRE, f.x + ca * s, f.y + sa * s, d.supRadius, d.supDur, d.supDmg / d.supDur * 1.0f, 0, false, false);
                break;
            }
            case WAVE: {
                Projectile p = spawnProj(f, ang, d.supSpeed, d.supRange, d.supDmg, 8);
                p.pierce = true; p.radius = 1.1f; p.stunSec = d.supStun; p.slowSec = 2f;
                break;
            }
            case STORM:
                addZone(f, Zone.SHOCK, f.x, f.y, d.supRadius, d.supDur, d.supDmg, 0, false, true);
                break;
            case DOME: {
                float s = Math.min(dist, d.supRange);
                addZone(f, Zone.HEAL, f.x + ca * s, f.y + sa * s, d.supRadius, d.supDur, 0, 1000, true, false);
                break;
            }
            case SLAM: {
                float s = Math.max(2f, Math.min(dist, d.supRange));
                float land = 0;
                for (float q = s; q >= 1f; q -= 0.25f) {
                    if (!map.circleHits(f.x + ca * q, f.y + sa * q, R)) { land = q; break; }
                }
                if (land <= 0) { ok = false; break; }
                beginDash(f, f.x + ca * land, f.y + sa * land, 0.55f, true, 0, 8, true);
                break;
            }
            case STRIKE: {
                Fighter tgt = null; float best = d.supRange;
                for (Fighter e : fighters) {
                    if (!e.alive || e.team == f.team || e.invulnerable()) continue;
                    float dd = dist(e.x, e.y, f.x, f.y);
                    if (dd < best) { best = dd; tgt = e; }
                }
                if (tgt == null) { ok = false; break; }
                float vx = (tgt.x - f.x) / Math.max(0.01f, best), vy = (tgt.y - f.y) / Math.max(0.01f, best);
                float px = tgt.x + vx * 0.95f, py = tgt.y + vy * 0.95f;
                if (map.circleHits(px, py, R)) { px = tgt.x - vx * 0.95f; py = tgt.y - vy * 0.95f; }
                if (map.circleHits(px, py, R)) { px = tgt.x; py = tgt.y; }
                fx.add(new Fx(Fx.DASH, 5, f.x, f.y, px, py));
                f.x = px; f.y = py;
                f.aim = (float) Math.atan2(tgt.y - py, tgt.x - px);
                f.stealthT = 3f;
                fx.add(new Fx(Fx.SLASH, 5, f.x, f.y, f.aim, 2.2f));
                damage(tgt, d.supDmg, f);
                if (tgt.alive) tgt.stunT = Math.max(tgt.stunT, d.supStun);
                fx.add(new Fx(Fx.HIT, 10, tgt.x, tgt.y, 0, 0));
                break;
            }
            default: ok = false; break;
        }
        if (!ok) return;
        f.superCharge = 0;
        f.revealT = Math.max(f.revealT, 1.0f);
        fx.add(new Fx(Fx.SUPER, d.id, f.x, f.y, 0, 0));
    }

    // ----------------------------------------------- снаряды и зоны

    private void updateProjectiles() {
        for (Iterator<Projectile> it = projs.iterator(); it.hasNext(); ) {
            Projectile p = it.next();
            Fighter owner = fighters.get(p.owner);
            float step = p.speed * DT;
            int n = Math.max(1, (int) Math.ceil(step / 0.25f));
            float sub = step / n;
            boolean dead = false;
            for (int i = 0; i < n && !dead; i++) {
                p.x += p.dx * sub; p.y += p.dy * sub; p.left -= sub;
                if (p.lob) {
                    if (p.left <= 0) {
                        explode(p.tx, p.ty, p.splash, p.dmg, owner, p.team, 0, 5f, p.style, null);
                        dead = true;
                    }
                    continue;
                }
                if (map.blocksShot((int) Math.floor(p.x), (int) Math.floor(p.y))) {
                    fx.add(new Fx(Fx.HIT, p.style, p.x, p.y, 0, 0));
                    if (p.splash > 0) explode(p.x, p.y, p.splash, p.dmg * 0.6f, owner, p.team, 0, 0, p.style, null);
                    dead = true;
                    break;
                }
                for (Fighter e : fighters) {
                    if (!e.alive || e == owner) continue;
                    if (dist(e.x, e.y, p.x, p.y) >= R + p.radius) continue;
                    if (e.team == p.team) {
                        if (p.heal > 0) { healFighter(e, p.heal); dead = true; break; }
                        continue;
                    }
                    if (e.invulnerable() || p.hit.contains(e.id)) continue;
                    hitFighter(p, e, owner);
                    if (!p.pierce) { dead = true; break; }
                    p.hit.add(e.id);
                }
                if (!dead && p.left <= 0) {
                    if (p.splash > 0) explode(p.x, p.y, p.splash, p.dmg * 0.6f, owner, p.team, 0, 0, p.style, null);
                    dead = true;
                }
            }
            if (dead) it.remove();
        }
    }

    private void hitFighter(Projectile p, Fighter e, Fighter owner) {
        damage(e, p.dmg, owner);
        fx.add(new Fx(Fx.HIT, p.style, p.x, p.y, 0, 0));
        if (e.alive) {
            if (p.slowSec > 0) e.slowT = Math.max(e.slowT, p.slowSec);
            if (p.stunSec > 0) e.stunT = Math.max(e.stunT, p.stunSec);
            if (p.knock > 0) knockFrom(e, p.x - p.dx, p.y - p.dy, p.knock);
        }
        if (p.heal > 0 && owner != null) { /* лечение только союзникам */ }
        if (p.splash > 0) explode(p.x, p.y, p.splash, p.dmg * 0.6f, owner, p.team, 0, 0, p.style, e);
    }

    private void updateZones() {
        for (Iterator<Zone> it = zones.iterator(); it.hasNext(); ) {
            Zone z = it.next();
            Fighter owner = fighters.get(z.owner);
            z.time -= DT;
            if (z.follow) {
                if (!owner.alive) z.time = 0;
                z.x = owner.x; z.y = owner.y;
            }
            if (z.time <= 0) { it.remove(); continue; }
            Fighter beamTarget = null;
            for (Fighter e : fighters) {
                if (!e.alive || dist(e.x, e.y, z.x, z.y) > z.radius + R * 0.5f) continue;
                if (e.team != z.team) {
                    if (z.dps > 0) damage(e, z.dps * DT, owner);
                    if (z.slowEnemies) e.slowT = Math.max(e.slowT, 0.4f);
                    if (z.kind == Zone.SHOCK && e.alive) beamTarget = e;
                } else if (z.healPerSec > 0 && e.hp < e.def.hp) {
                    e.hp = Math.min(e.def.hp, e.hp + z.healPerSec * DT);
                }
            }
            if (z.kind == Zone.SHOCK) {
                z.beamT -= DT;
                if (beamTarget != null && z.beamT <= 0) {
                    z.beamT = 0.3f;
                    fx.add(new Fx(Fx.BEAM, 3, z.x, z.y, beamTarget.x, beamTarget.y));
                }
            }
        }
    }

    // ------------------------------------------------------- видимость

    /** Враг в кусте или в невидимости виден только если рядом есть союзник наблюдателя или он только что атаковал. */
    public boolean visibleTo(Fighter f, int viewerTeam) {
        if (f.team == viewerTeam || !f.alive) return true;
        boolean hidden = (map.bushAt(f.x, f.y) || f.stealthT > 0) && f.revealT <= 0;
        if (!hidden) return true;
        for (Fighter a : fighters) {
            if (a.alive && a.team == viewerTeam && dist(a.x, a.y, f.x, f.y) < 2.2f) return true;
        }
        return false;
    }
}
