package ru.arena.game;

import ru.arena.game.BrawlerDef.Atk;
import ru.arena.game.BrawlerDef.Sup;

/** Все бойцы мода. Имена, внешность и способности придуманы с нуля. */
public final class Brawlers {
    public static final BrawlerDef[] ALL = new BrawlerDef[8];

    public static BrawlerDef get(int id) {
        return ALL[((id % ALL.length) + ALL.length) % ALL.length];
    }

    static {
        BrawlerDef b;

        // 0 — Бумер
        b = new BrawlerDef(0, "Бумер", "Стрелок", 5200, 3.4f);
        b.desc = "Бывший старатель с дробовиком. Страшен вблизи, но любит держать врага на расстоянии залпа.";
        b.atk = Atk.SPREAD; b.atkName = "Дробь"; b.atkDesc = "Веер из 5 картечин. Чем ближе враг, тем больнее.";
        b.atkRange = 6.0f; b.atkDmg = 420; b.atkSpeed = 18; b.atkCd = 0.30f; b.reload = 1.6f; b.pellets = 5; b.spread = 30; b.projStyle = 0;
        b.sup = Sup.BLAST; b.supName = "Громовой залп"; b.supDesc = "Девять тяжёлых зарядов с мощным отбрасыванием.";
        b.supRange = 7.5f; b.supDmg = 520; b.supCount = 9; b.supSpread = 52; b.supSpeed = 17; b.supKnock = 7; b.supNeed = 6000;
        b.dashName = "Реактивный рывок"; b.dashDesc = "Короткий огненный рывок вперёд.";
        b.dashDist = 3.6f; b.dashDur = 0.22f; b.dashCd = 6f; b.dashStyle = 0;
        b.cMain = 0xFFE8742A; b.cAccent = 0xFFC8321F; b.cGlow = 0xFFFFB13B;
        ALL[0] = b;

        // 1 — Искра
        b = new BrawlerDef(1, "Искра", "Огненный маг", 3600, 3.5f);
        b.desc = "Юная чародейка, у которой огонь живёт даже в волосах. Любит эффектные выходы.";
        b.atk = Atk.BOLT; b.atkName = "Огненный шар"; b.atkDesc = "Шар взрывается при попадании и задевает всех рядом.";
        b.atkRange = 8.0f; b.atkDmg = 1000; b.atkSpeed = 12; b.atkCd = 0.35f; b.reload = 1.7f; b.splash = 0.8f; b.projStyle = 1;
        b.sup = Sup.METEOR; b.supName = "Метеоритный дождь"; b.supDesc = "Огненный дождь на 3 секунды выжигает выбранную область.";
        b.supRange = 7.5f; b.supDmg = 1000; b.supDur = 3.2f; b.supRadius = 2.4f; b.supNeed = 5500;
        b.dashName = "Огненный телепорт"; b.dashDesc = "Мгновенно перемещается сквозь стены и неуязвима.";
        b.dashDist = 4.5f; b.dashDur = 0.10f; b.dashCd = 7f; b.dashThrough = true; b.dashInvuln = true; b.dashStyle = 1;
        b.cMain = 0xFFD43B2A; b.cAccent = 0xFFFFC233; b.cGlow = 0xFFFF7A1F;
        ALL[1] = b;

        // 2 — Лёдик
        b = new BrawlerDef(2, "Лёдик", "Ледяной снайпер", 3300, 3.3f);
        b.desc = "Молчаливый охотник с севера. Один выстрел издалека — и враг уже мёрзнет.";
        b.atk = Atk.PIERCE; b.atkName = "Ледяной осколок"; b.atkDesc = "Пробивает врагов насквозь и замедляет их.";
        b.atkRange = 10.5f; b.atkDmg = 1250; b.atkSpeed = 22; b.atkCd = 0.50f; b.reload = 2.2f; b.slowSec = 1.8f; b.projStyle = 2;
        b.sup = Sup.WAVE; b.supName = "Ледяная волна"; b.supDesc = "Широкая волна замораживает врагов на месте.";
        b.supRange = 8.5f; b.supDmg = 1500; b.supSpeed = 13; b.supStun = 1.3f; b.supNeed = 6500;
        b.dashName = "Ледяное скольжение"; b.dashDesc = "Скользит вперёд и оставляет лёд, который замедляет врагов.";
        b.dashDist = 4.2f; b.dashDur = 0.30f; b.dashCd = 7f; b.dashStyle = 2;
        b.cMain = 0xFF3B7FD0; b.cAccent = 0xFFEAF6FF; b.cGlow = 0xFF8FE3FF;
        ALL[2] = b;

        // 3 — Тесла
        b = new BrawlerDef(3, "Тесла", "Электрик", 3900, 3.6f);
        b.desc = "Изобретатель, который слишком часто проверял свои катушки на себе.";
        b.atk = Atk.CHAIN; b.atkName = "Цепная молния"; b.atkDesc = "Бьёт ближайшего врага и перескакивает ещё на двоих.";
        b.atkRange = 6.8f; b.atkDmg = 750; b.atkCd = 0.40f; b.reload = 1.4f; b.projStyle = 3;
        b.sup = Sup.STORM; b.supName = "Грозовой шторм"; b.supDesc = "Электрическое поле вокруг Теслы 4 секунды бьёт всех рядом.";
        b.supRange = 3.3f; b.supDmg = 700; b.supDur = 4.0f; b.supRadius = 3.3f; b.supNeed = 6000;
        b.dashName = "Разряд"; b.dashDesc = "Рывок, который бьёт током всех на пути.";
        b.dashDist = 3.6f; b.dashDur = 0.20f; b.dashDmg = 650; b.dashCd = 6.5f; b.dashStyle = 3;
        b.cMain = 0xFFF2C21B; b.cAccent = 0xFF2B2B33; b.cGlow = 0xFF6FD3FF;
        ALL[3] = b;

        // 4 — Танкуша
        b = new BrawlerDef(4, "Танкуша", "Тяжеловес", 9000, 2.8f);
        b.desc = "Закованный в сталь здоровяк. Не любит бегать, зато любит, когда бегут от него.";
        b.atk = Atk.MELEE; b.atkName = "Ударная волна"; b.atkDesc = "Мощный удар кулаками по дуге перед собой.";
        b.atkRange = 2.7f; b.atkDmg = 1400; b.atkCd = 0.55f; b.reload = 1.3f; b.arc = 110; b.knock = 4; b.projStyle = 4;
        b.sup = Sup.SLAM; b.supName = "Сокрушающий прыжок"; b.supDesc = "Прыгает через стены и оглушает всех в месте приземления.";
        b.supRange = 6.5f; b.supDmg = 2500; b.supRadius = 2.6f; b.supStun = 1.0f; b.supNeed = 5000;
        b.dashName = "Таран"; b.dashDesc = "Несётся вперёд и отбрасывает врагов на пути.";
        b.dashDist = 3.4f; b.dashDur = 0.30f; b.dashDmg = 600; b.dashCd = 7f; b.dashStyle = 4;
        b.cMain = 0xFF8895A3; b.cAccent = 0xFFE07A1F; b.cGlow = 0xFFFFA24D;
        ALL[4] = b;

        // 5 — Тень
        b = new BrawlerDef(5, "Тень", "Ассасин", 4200, 4.3f);
        b.desc = "Появляется из кустов, исчезает в них же. Предпочитает работать молча.";
        b.atk = Atk.MELEE; b.atkName = "Клинки"; b.atkDesc = "Быстрые взмахи кинжалами вблизи.";
        b.atkRange = 2.0f; b.atkDmg = 1100; b.atkCd = 0.28f; b.reload = 1.0f; b.arc = 140; b.knock = 0; b.projStyle = 10;
        b.sup = Sup.STRIKE; b.supName = "Удар из тени"; b.supDesc = "Появляется за спиной ближайшего врага и наносит огромный урон.";
        b.supRange = 9f; b.supDmg = 2800; b.supNeed = 5500; b.supStun = 0.3f;
        b.dashName = "Теневой шаг"; b.dashDesc = "Сквозь стены, неуязвим и невидим на короткое время.";
        b.dashDist = 4.0f; b.dashDur = 0.12f; b.dashCd = 6f; b.dashThrough = true; b.dashInvuln = true; b.dashStyle = 5;
        b.cMain = 0xFF4B2A7A; b.cAccent = 0xFFC73BD6; b.cGlow = 0xFFB65CFF;
        ALL[5] = b;

        // 6 — Мята
        b = new BrawlerDef(6, "Мята", "Лекарь", 4000, 3.4f);
        b.desc = "Полевой врач с посохом-аптечкой. Лечит друзей и больно воспитывает врагов.";
        b.atk = Atk.HEAL; b.atkName = "Целебный шар"; b.atkDesc = "Ранит врагов и лечит союзников.";
        b.atkRange = 7.5f; b.atkDmg = 850; b.atkSpeed = 14; b.atkCd = 0.40f; b.reload = 1.5f; b.heal = 750; b.projStyle = 3;
        b.sup = Sup.DOME; b.supName = "Купол исцеления"; b.supDesc = "Лечит союзников внутри и замедляет врагов.";
        b.supRange = 6.5f; b.supDur = 4.5f; b.supRadius = 3.0f; b.supNeed = 5500; b.supDmg = 1000;
        b.dashName = "Целебный шаг"; b.dashDesc = "Рывок, который лечит и оставляет зелёное облако.";
        b.dashDist = 3.3f; b.dashDur = 0.22f; b.dashCd = 6.5f; b.dashStyle = 6;
        b.cMain = 0xFF3FB86B; b.cAccent = 0xFFF4FFF7; b.cGlow = 0xFF7DFFA6;
        ALL[6] = b;

        // 7 — Бахарь
        b = new BrawlerDef(7, "Бахарь", "Подрывник", 3800, 3.3f);
        b.desc = "Шахтёр-подрывник. Бомбы бросает из-за стен и никогда не извиняется за шум.";
        b.atk = Atk.LOB; b.atkName = "Динамитная шашка"; b.atkDesc = "Летит по дуге через стены и взрывается в точке прицела.";
        b.atkRange = 8.5f; b.atkDmg = 1050; b.atkSpeed = 10; b.atkCd = 0.60f; b.reload = 1.8f; b.splash = 1.5f; b.projStyle = 6;
        b.sup = Sup.ROCKETS; b.supName = "Ракетный обстрел"; b.supDesc = "Шесть ракет веером с взрывом при попадании.";
        b.supRange = 9f; b.supDmg = 850; b.supCount = 6; b.supSpread = 24; b.supSpeed = 12; b.supRadius = 1.3f; b.supNeed = 6000;
        b.dashName = "Прыжок на ракете"; b.dashDesc = "Взлетает над стенами, оставляя взрыв на старте.";
        b.dashDist = 4.0f; b.dashDur = 0.30f; b.dashDmg = 400; b.dashCd = 7f; b.dashThrough = true; b.dashInvuln = true; b.dashStyle = 7;
        b.cMain = 0xFFB5651D; b.cAccent = 0xFFF5C518; b.cGlow = 0xFFFF9A3C;
        ALL[7] = b;
    }

    private Brawlers() {}
}
