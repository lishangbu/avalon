-- 基于 https://wiki.52poke.com/wiki/属性 与 https://wiki.52poke.com/wiki/属性相克表 的主线 18 属性基线。
INSERT INTO catalog.type_definition (code, name, description, icon, sorting_order, enabled)
VALUES
    ('NORMAL', '一般', '一般属性', NULL, 10, TRUE),
    ('FIGHTING', '格斗', '格斗属性', NULL, 20, TRUE),
    ('FLYING', '飞行', '飞行属性', NULL, 30, TRUE),
    ('POISON', '毒', '毒属性', NULL, 40, TRUE),
    ('GROUND', '地面', '地面属性', NULL, 50, TRUE),
    ('ROCK', '岩石', '岩石属性', NULL, 60, TRUE),
    ('BUG', '虫', '虫属性', NULL, 70, TRUE),
    ('GHOST', '幽灵', '幽灵属性', NULL, 80, TRUE),
    ('STEEL', '钢', '钢属性', NULL, 90, TRUE),
    ('FIRE', '火', '火属性', NULL, 100, TRUE),
    ('WATER', '水', '水属性', NULL, 110, TRUE),
    ('GRASS', '草', '草属性', NULL, 120, TRUE),
    ('ELECTRIC', '电', '电属性', NULL, 130, TRUE),
    ('PSYCHIC', '超能力', '超能力属性', NULL, 140, TRUE),
    ('ICE', '冰', '冰属性', NULL, 150, TRUE),
    ('DRAGON', '龙', '龙属性', NULL, 160, TRUE),
    ('DARK', '恶', '恶属性', NULL, 170, TRUE),
    ('FAIRY', '妖精', '妖精属性', NULL, 180, TRUE)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    sorting_order = EXCLUDED.sorting_order,
    enabled = EXCLUDED.enabled,
    version = catalog.type_definition.version + 1;

WITH type_id_map AS (
    SELECT id, code
    FROM catalog.type_definition
    WHERE code IN (
        'NORMAL', 'FIGHTING', 'FLYING', 'POISON', 'GROUND', 'ROCK', 'BUG', 'GHOST', 'STEEL',
        'FIRE', 'WATER', 'GRASS', 'ELECTRIC', 'PSYCHIC', 'ICE', 'DRAGON', 'DARK', 'FAIRY'
    )
), seeded_chart(attacking_code, multipliers) AS (
    VALUES
        ('NORMAL', ARRAY[1.00, 1.00, 1.00, 1.00, 1.00, 0.50, 1.00, 0.00, 0.50, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00]::NUMERIC[]),
        ('FIGHTING', ARRAY[2.00, 1.00, 0.50, 0.50, 1.00, 2.00, 0.50, 0.00, 2.00, 1.00, 1.00, 1.00, 1.00, 0.50, 2.00, 1.00, 2.00, 0.50]::NUMERIC[]),
        ('FLYING', ARRAY[1.00, 2.00, 1.00, 1.00, 1.00, 0.50, 2.00, 1.00, 0.50, 1.00, 1.00, 2.00, 0.50, 1.00, 1.00, 1.00, 1.00, 1.00]::NUMERIC[]),
        ('POISON', ARRAY[1.00, 1.00, 1.00, 0.50, 0.50, 0.50, 1.00, 0.50, 0.00, 1.00, 1.00, 2.00, 1.00, 1.00, 1.00, 1.00, 1.00, 2.00]::NUMERIC[]),
        ('GROUND', ARRAY[1.00, 1.00, 0.00, 2.00, 1.00, 2.00, 0.50, 1.00, 2.00, 2.00, 1.00, 0.50, 2.00, 1.00, 1.00, 1.00, 1.00, 1.00]::NUMERIC[]),
        ('ROCK', ARRAY[1.00, 0.50, 2.00, 1.00, 0.50, 1.00, 2.00, 1.00, 0.50, 2.00, 1.00, 1.00, 1.00, 1.00, 2.00, 1.00, 1.00, 1.00]::NUMERIC[]),
        ('BUG', ARRAY[1.00, 0.50, 0.50, 0.50, 1.00, 1.00, 1.00, 0.50, 0.50, 0.50, 1.00, 2.00, 1.00, 2.00, 1.00, 1.00, 2.00, 0.50]::NUMERIC[]),
        ('GHOST', ARRAY[0.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 2.00, 1.00, 1.00, 1.00, 1.00, 1.00, 2.00, 1.00, 1.00, 0.50, 1.00]::NUMERIC[]),
        ('STEEL', ARRAY[1.00, 1.00, 1.00, 1.00, 1.00, 2.00, 1.00, 1.00, 0.50, 0.50, 0.50, 1.00, 0.50, 1.00, 2.00, 1.00, 1.00, 2.00]::NUMERIC[]),
        ('FIRE', ARRAY[1.00, 1.00, 1.00, 1.00, 1.00, 0.50, 2.00, 1.00, 2.00, 0.50, 0.50, 2.00, 1.00, 1.00, 2.00, 0.50, 1.00, 1.00]::NUMERIC[]),
        ('WATER', ARRAY[1.00, 1.00, 1.00, 1.00, 2.00, 2.00, 1.00, 1.00, 1.00, 2.00, 0.50, 0.50, 1.00, 1.00, 1.00, 0.50, 1.00, 1.00]::NUMERIC[]),
        ('GRASS', ARRAY[1.00, 1.00, 0.50, 0.50, 2.00, 2.00, 0.50, 1.00, 0.50, 0.50, 2.00, 0.50, 1.00, 1.00, 1.00, 0.50, 1.00, 1.00]::NUMERIC[]),
        ('ELECTRIC', ARRAY[1.00, 1.00, 2.00, 1.00, 0.00, 1.00, 1.00, 1.00, 1.00, 1.00, 2.00, 0.50, 0.50, 1.00, 1.00, 0.50, 1.00, 1.00]::NUMERIC[]),
        ('PSYCHIC', ARRAY[1.00, 2.00, 1.00, 2.00, 1.00, 1.00, 1.00, 1.00, 0.50, 1.00, 1.00, 1.00, 1.00, 0.50, 1.00, 1.00, 0.00, 1.00]::NUMERIC[]),
        ('ICE', ARRAY[1.00, 1.00, 2.00, 1.00, 2.00, 1.00, 1.00, 1.00, 0.50, 0.50, 0.50, 2.00, 1.00, 1.00, 0.50, 2.00, 1.00, 1.00]::NUMERIC[]),
        ('DRAGON', ARRAY[1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 0.50, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 2.00, 1.00, 0.00]::NUMERIC[]),
        ('DARK', ARRAY[1.00, 0.50, 1.00, 1.00, 1.00, 1.00, 1.00, 2.00, 1.00, 1.00, 1.00, 1.00, 1.00, 2.00, 1.00, 1.00, 0.50, 0.50]::NUMERIC[]),
        ('FAIRY', ARRAY[1.00, 2.00, 1.00, 0.50, 1.00, 1.00, 1.00, 1.00, 0.50, 0.50, 1.00, 1.00, 1.00, 1.00, 1.00, 2.00, 2.00, 1.00]::NUMERIC[])
)
INSERT INTO catalog.type_effectiveness (attacking_type_id, defending_type_id, multiplier)
SELECT attacker.id,
       defender.id,
       multiplier.value
FROM seeded_chart sc
JOIN type_id_map attacker ON attacker.code = sc.attacking_code
CROSS JOIN LATERAL UNNEST(sc.multipliers) WITH ORDINALITY AS multiplier(value, idx)
JOIN (
    SELECT id,
           code,
           ROW_NUMBER() OVER (
               ORDER BY CASE code
                            WHEN 'NORMAL' THEN 10
                            WHEN 'FIGHTING' THEN 20
                            WHEN 'FLYING' THEN 30
                            WHEN 'POISON' THEN 40
                            WHEN 'GROUND' THEN 50
                            WHEN 'ROCK' THEN 60
                            WHEN 'BUG' THEN 70
                            WHEN 'GHOST' THEN 80
                            WHEN 'STEEL' THEN 90
                            WHEN 'FIRE' THEN 100
                            WHEN 'WATER' THEN 110
                            WHEN 'GRASS' THEN 120
                            WHEN 'ELECTRIC' THEN 130
                            WHEN 'PSYCHIC' THEN 140
                            WHEN 'ICE' THEN 150
                            WHEN 'DRAGON' THEN 160
                            WHEN 'DARK' THEN 170
                            WHEN 'FAIRY' THEN 180
                        END
           ) AS idx
    FROM type_id_map
) defender ON defender.idx = multiplier.idx
ON CONFLICT (attacking_type_id, defending_type_id) DO UPDATE
SET multiplier = EXCLUDED.multiplier,
    version = catalog.type_effectiveness.version + 1;
