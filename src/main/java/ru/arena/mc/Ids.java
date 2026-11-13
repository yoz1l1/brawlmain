package ru.arena.mc;

import net.minecraft.util.Identifier;
import ru.arena.net.Proto;

/** Идентификаторы сетевых каналов мода. */
public final class Ids {
    public static final Identifier ACTION = new Identifier(Proto.NS, "action");
    public static final Identifier INPUT = new Identifier(Proto.NS, "input");
    public static final Identifier LOBBY = new Identifier(Proto.NS, "lobby");
    public static final Identifier INVITE = new Identifier(Proto.NS, "invite");
    public static final Identifier START = new Identifier(Proto.NS, "start");
    public static final Identifier SNAP = new Identifier(Proto.NS, "snap");
    public static final Identifier END = new Identifier(Proto.NS, "end");
    public static final Identifier TOAST = new Identifier(Proto.NS, "toast");

    private Ids() {}
}
