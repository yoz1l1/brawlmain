package ru.arena.game;

import java.util.HashSet;
import java.util.Set;

public final class Fighter {
    public final int id, team;
    public final String name;
    public final BrawlerDef def;
    public boolean bot;

    public float x, y, aim;
    public float hp, ammo = 3f, atkCd, superCharge, dashCd, regenIdle;
    public boolean alive = true;
    public float respawnT, spawnProt, slowT, stunT, revealT, stealthT, kbx, kby;

    public boolean dashing, dashInvuln, slamOnLand;
    public float dashT, dashDur, dsx, dsy, dex, dey, dashDmg;
    public int dashStyle;
    public final Set<Integer> dashHit = new HashSet<>();

    // ввод
    public float mx, my, aimDx, aimDy;
    public boolean attackHeld, superPressed, dashPressed;

    public int kills, deaths;
    public float dmgDealt;

    // ИИ
    public float botPhase, botWpX = -1, botWpY, botWpT, botTurnT;
    public int botTurn = 1;

    public Fighter(int id, String name, int team, BrawlerDef def, boolean bot) {
        this.id = id; this.name = name; this.team = team; this.def = def; this.bot = bot;
        this.hp = def.hp;
    }

    public boolean invulnerable() {
        return spawnProt > 0 || (dashing && dashInvuln);
    }
}
