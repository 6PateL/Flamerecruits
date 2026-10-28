# Flame Recruits

Сабмод для Recruits + From Another World (Forge 1.20.1). Рекрути тримають і застосовують **Flamethrower**.

## Як користуватись
1. Відкрийте інвентар рекрута.
2. У слот правої руки покладіть Flamethrower (`fromanotherworld:flamethrower_mk_1`). Якщо він лежить у рюкзаку, рекрут візьме його сам.
3. Рекрут палить ціль на відстані до 9 блоків, короткими чергами (60 тіків вогню, 20 пауза).

Палива не потрібно (як у гравця). Конфіг `flamerecruits-common.toml`: range, fireIntervalTicks, burstTicks, pauseTicks, avoidFriendlyFire.

Увага: полум'я підпалює блоки там, де влучає (так працює сам мод From Another World), тож обережно біля дерев'яних будівель.

## Збірка
Завантажити в GitHub, Actions збере jar. Для збірки потрібен лише Recruits (лежить у `libs/`).
