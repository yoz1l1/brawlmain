# Настройка облачного профиля Boevaya Arena

Облачный проект: https://feyxldpkwomajbykwysz.supabase.co

## 1. Создать таблицы
1. Открой Supabase Dashboard → SQL Editor → New query.
2. Вставь целиком файл `supabase/migrations/202611130001_arena_profiles.sql`.
3. Нажми Run. В таблицах появятся `arena_profiles`, `arena_brawler_progress`, `arena_match_results`.

RLS включён. Клиентам запрещено напрямую читать/изменять таблицы и начислять себе трофеи.

## 2. Развернуть API
Установи Supabase CLI и выполни из корня репозитория:
```sh
supabase login
supabase link --project-ref feyxldpkwomajbykwysz
supabase secrets set ARENA_SERVER_KEY="СОЗДАЙ_ДЛИННЫЙ_СЛУЧАЙНЫЙ_СЕКРЕТ"
supabase functions deploy arena-api
```
У Edge Functions уже есть SUPABASE_URL и SUPABASE_SERVICE_ROLE_KEY в окружении Supabase. Не добавляй service-role key в Minecraft-мод или GitHub.

## 3. API (только для доверенных серверных интеграций)
Заголовок: `x-arena-server-key: <ARENA_SERVER_KEY>`

Профиль:
`GET https://feyxldpkwomajbykwysz.supabase.co/functions/v1/arena-api?player_uuid=<UUID>`

Завершение матча:
`POST https://feyxldpkwomajbykwysz.supabase.co/functions/v1/arena-api`
`Content-Type: application/json`

Тело POST содержит match_id (UUID) и players[]. Повторная отправка того же match_id + player_uuid не начислит награду повторно.

## Важные ограничения безопасности
- Publishable key не является секретом, но сам по себе не подтверждает личность игрока.
- ARENA_SERVER_KEY — серверный секрет. Не встраивай его в клиентский JAR и не отправляй в чат.
- Результат матча можно отправлять только с доверенного серверного мода/плагина. Для чужих серверов владелец должен установить серверную интеграцию и настроить её секрет.
- Текущий клиентский мод пока не подключён к этому API: сначала нужно развернуть SQL и функцию, затем добавить серверную интеграцию и клиентскую загрузку профиля.
