# Local Chat (NeoForge 1.21.1)
Обычное сообщение видят все игроки твоей команды (scoreboard team), независимо от расстояния.
Сообщение с `!` в начале видят все игроки.
Настройки: config/localchat-common.toml. Мод нужен только на сервере.

Команды чата: /team add red ; /team join red Ник

Мут (нужен уровень оператора 2+):
  /mute <игрок> [минуты, 0 = навсегда] [причина]
  /unmute <игрок>
  /mutelist
Муты сохраняются в мире: localchat_mutes.json. Замученный не может писать в чат, /msg, /me, /say, /teammsg.

Сборка через GitHub: Actions -> Build mod -> Artifacts.
