# TiT 数据字段规范

本文件用于避免同一数据域出现同义字段。TiT 自定义数据域统一使用 `snake_case`；Tetra 原生 `data/tetra/` 文件保持 Tetra 已定义的字段格式，不在跨域复制同名字段。

## TiT 自定义域

- 枪身：`weapon_class`、`fire_modes`、`shots_per_trigger`、`loaded_capacity_multiplier`、`rounds_per_minute`、`dual_wield`、`heat`
- 枪管：`projectile_type`、`pellets_per_round`、`damage`、`velocity`、`gravity`、`friction`、`range`、`armor_ignore`、`pierce`、`knockback`、`ignite`、`explosion`、`max_heat`、`overheat_threshold`、`heat_per_shot`、`cooling_coefficient`、`heat_epsilon`、`min_rpm_multiplier`、`max_rpm_multiplier`、`min_inaccuracy_multiplier`、`max_inaccuracy_multiplier`
- 弹匣：`feed_type`、`resource_base_capacity`、`reload_time_multiplier`
- 材料扩展：`physical_part_items`、`resource_capacity_multiplier`、`thermal_conductivity`、`repair_agent`、`stat_modifiers`
- 副部件：`attachments[].slot`、`aiming_zoom`、`recoil_multiplier`、`handling_multiplier`
- 打磨：`bodies[].base_polish`、`polish.<id>.damage_multiplier`、`rpm_multiplier`、`honing_count_multiplier`、`reload_time_multiplier`
- 弹药：`projectile`、`batch_size`、`cost`
- 修补剂：`item`、`unit_cost`
- 附魔：`id`、`name`、`allowed`、`stats`
- 特殊镶嵌：`id`、`name`、`slot`、`stats`（`stats.capacity` 表示可配置镶嵌孔容量）
- 资源通道：`type`、`external`

## 语义边界

`projectile_type` 表示枪管选择的弹道类型；`projectile` 表示弹药配方产出的 TaCZ 弹道 ID。`resource_base_capacity` 表示弹匣基础容量；`loaded_capacity_multiplier` 表示枪身对装填容量的倍率。两组字段不得互换或并列添加同义别名。

Tetra 原生模块/材料数据只使用 Tetra Schema，例如 `integrityCost`、`integrityGain`、`material.items`；它们由 Tetra loader 读取，不由 TiT 自定义解析器重复解释。
