package ru.arena.game;

/** Описание бойца: характеристики и три способности (атака, рывок, супер). */
public final class BrawlerDef {
    public enum Atk { SPREAD, BOLT, PIERCE, LOB, CHAIN, MELEE, HEAL }
    public enum Sup { BLAST, METEOR, WAVE, STORM, SLAM, STRIKE, DOME, ROCKETS }

    public final int id;
    public final String name, role;
    public String desc = "";
    public int hp;
    public float speed;

    public Atk atk;
    public String atkName = "", atkDesc = "";
    public float atkRange, atkDmg, atkSpeed, atkCd, reload, splash, spread, slowSec, heal, knock, arc;
    public int pellets = 1, projStyle;

    public Sup sup;
    public String supName = "", supDesc = "";
    public float supRange, supDmg, supDur, supRadius, supNeed, supStun, supSpread, supSpeed, supKnock;
    public int supCount = 1;

    public String dashName = "", dashDesc = "";
    public float dashDist, dashDur, dashDmg, dashCd;
    public boolean dashThrough, dashInvuln;
    public int dashStyle;

    public int cMain, cAccent, cGlow;

    public BrawlerDef(int id, String name, String role, int hp, float speed) {
        this.id = id; this.name = name; this.role = role; this.hp = hp; this.speed = speed;
    }

    /** Урон одной полной атаки (для шкал в меню). */
    public float burst() {
        return atkDmg * Math.max(1, pellets);
    }
}
