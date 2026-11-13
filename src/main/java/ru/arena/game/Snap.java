package ru.arena.game;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/** Снимок состояния матча, который сервер шлёт клиентам 20 раз в секунду. */
public final class Snap {
    public static final int E_BEAM = 3;
    public static final int F_ALIVE = 1, F_HIDDEN = 2, F_STUN = 4, F_SLOW = 8, F_INVULN = 16, F_DASH = 32, F_STEALTH = 64;

    public static final class F {
        public int id, flags, kills, deaths, maxHp;
        public float x, y, aim, hp, ammo, sup, dashCd, resp;
    }
    public static final class P {
        public int id, style, team;
        public float x, y, ang, prog;
    }
    public static final class Z {
        public int id, kind, team;
        public float x, y, r, frac;
    }
    public static final class E {
        public int type, style;
        public float x, y, x2, y2;
    }

    public int tick, state, winner, score0, score1;
    public float time;
    public final List<F> fighters = new ArrayList<>();
    public final List<P> projs = new ArrayList<>();
    public final List<Z> zones = new ArrayList<>();
    public final List<E> events = new ArrayList<>();

    public static byte[] write(Match m, int viewerTeam) {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream(1024);
            DataOutputStream o = new DataOutputStream(bo);
            o.writeInt((int) m.tick);
            o.writeByte(m.state);
            o.writeByte(m.winner);
            o.writeShort(m.score[0]);
            o.writeShort(m.score[1]);
            o.writeFloat(m.state == Match.STATE_COUNTDOWN ? m.stateT : m.timeLeft);

            o.writeByte(m.fighters.size());
            for (Fighter f : m.fighters) {
                boolean hidden = f.alive && !m.visibleTo(f, viewerTeam);
                int flags = 0;
                if (f.alive) flags |= F_ALIVE;
                if (hidden) flags |= F_HIDDEN;
                if (f.stunT > 0) flags |= F_STUN;
                if (f.slowT > 0) flags |= F_SLOW;
                if (f.invulnerable()) flags |= F_INVULN;
                if (f.dashing) flags |= F_DASH;
                if (f.stealthT > 0) flags |= F_STEALTH;
                o.writeByte(f.id);
                o.writeByte(flags);
                o.writeFloat(hidden ? 0 : f.x);
                o.writeFloat(hidden ? 0 : f.y);
                o.writeFloat(f.aim);
                o.writeShort((int) Math.ceil(f.hp));
                o.writeByte(Math.round(f.ammo * 20));
                o.writeByte(Math.round(f.superCharge * 100));
                o.writeShort(Math.round(f.dashCd * 100));
                o.writeShort(Math.round(f.respawnT * 100));
                o.writeByte(f.kills);
                o.writeByte(f.deaths);
            }
            o.writeShort(m.projs.size());
            for (Projectile p : m.projs) {
                o.writeShort(p.id);
                o.writeByte(p.style);
                o.writeByte(p.team);
                o.writeFloat(p.x);
                o.writeFloat(p.y);
                o.writeFloat(p.angle());
                o.writeFloat(p.progress());
            }
            o.writeByte(m.zones.size());
            for (Zone z : m.zones) {
                o.writeShort(z.id);
                o.writeByte(z.kind);
                o.writeByte(z.team);
                o.writeFloat(z.x);
                o.writeFloat(z.y);
                o.writeFloat(z.radius);
                o.writeFloat(z.time / z.total);
            }
            o.writeShort(m.fx.size());
            for (Fx e : m.fx) {
                o.writeByte(e.type);
                o.writeByte(e.style);
                o.writeFloat(e.x);
                o.writeFloat(e.y);
                o.writeFloat(e.x2);
                o.writeFloat(e.y2);
            }
            return bo.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Snap read(byte[] data) {
        try {
            DataInputStream i = new DataInputStream(new ByteArrayInputStream(data));
            Snap s = new Snap();
            s.tick = i.readInt();
            s.state = i.readUnsignedByte();
            s.winner = i.readByte();
            s.score0 = i.readShort();
            s.score1 = i.readShort();
            s.time = i.readFloat();
            int n = i.readUnsignedByte();
            for (int k = 0; k < n; k++) {
                F f = new F();
                f.id = i.readUnsignedByte();
                f.flags = i.readUnsignedByte();
                f.x = i.readFloat();
                f.y = i.readFloat();
                f.aim = i.readFloat();
                f.hp = i.readShort();
                f.ammo = i.readUnsignedByte() / 20f;
                f.sup = i.readUnsignedByte() / 100f;
                f.dashCd = i.readShort() / 100f;
                f.resp = i.readShort() / 100f;
                f.kills = i.readUnsignedByte();
                f.deaths = i.readUnsignedByte();
                f.maxHp = 0;
                s.fighters.add(f);
            }
            n = i.readUnsignedShort();
            for (int k = 0; k < n; k++) {
                P p = new P();
                p.id = i.readUnsignedShort();
                p.style = i.readUnsignedByte();
                p.team = i.readUnsignedByte();
                p.x = i.readFloat(); p.y = i.readFloat(); p.ang = i.readFloat(); p.prog = i.readFloat();
                s.projs.add(p);
            }
            n = i.readUnsignedByte();
            for (int k = 0; k < n; k++) {
                Z z = new Z();
                z.id = i.readUnsignedShort();
                z.kind = i.readUnsignedByte();
                z.team = i.readUnsignedByte();
                z.x = i.readFloat(); z.y = i.readFloat(); z.r = i.readFloat(); z.frac = i.readFloat();
                s.zones.add(z);
            }
            n = i.readUnsignedShort();
            for (int k = 0; k < n; k++) {
                E e = new E();
                e.type = i.readUnsignedByte();
                e.style = i.readUnsignedByte();
                e.x = i.readFloat(); e.y = i.readFloat(); e.x2 = i.readFloat(); e.y2 = i.readFloat();
                s.events.add(e);
            }
            return s;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
