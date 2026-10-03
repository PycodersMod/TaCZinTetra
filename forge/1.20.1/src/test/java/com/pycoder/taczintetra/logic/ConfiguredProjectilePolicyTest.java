package com.pycoder.taczintetra.logic;

import com.google.gson.JsonParser;
import com.pycoder.taczintetra.config.ModuleConfig;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguredProjectilePolicyTest {
    @Test
    void resolvesOnlyPositiveFiniteExplosionRadiusFromTheSelectedBarrel() {
        ModuleConfig config = ModuleConfig.from(JsonParser.parseString("""
                {"barrels":[
                  {"id":"40mm","stats":{"explosion":2.0}},
                  {"id":"9mm","stats":{"explosion":0.0}}
                ]}
                """).getAsJsonObject());

        assertEquals(2.0f, ConfiguredProjectilePolicy.explosionRadius(config, "40mm"), 0.0001f);
        assertEquals(0.0f, ConfiguredProjectilePolicy.explosionRadius(config, "9mm"), 0.0001f);
        assertEquals(0.0f, ConfiguredProjectilePolicy.explosionRadius(config, "missing"), 0.0001f);
    }

    @Test
    void configuredBarrelMotionAndIgniteOverridesAreOptionalAndTyped() {
        ModuleConfig config = ModuleConfig.from(JsonParser.parseString("""
                {"barrels":[
                  {"id":"40mm","stats":{"life":1.5,"gravity":0.35,"friction":0.02,"ignite":1}},
                  {"id":"9mm","stats":{"damage":5.0}}
                ]}
                """).getAsJsonObject());

        ConfiguredProjectilePolicy.MotionOverrides overrides =
                ConfiguredProjectilePolicy.motionOverrides(config, "40mm");
        assertEquals(1.5f, overrides.lifeSeconds(), 0.0001f);
        assertEquals(0.35f, overrides.gravity(), 0.0001f);
        assertEquals(0.02f, overrides.friction(), 0.0001f);
        assertTrue(overrides.ignite());

        ConfiguredProjectilePolicy.MotionOverrides missing =
                ConfiguredProjectilePolicy.motionOverrides(config, "9mm");
        assertTrue(Float.isNaN(missing.lifeSeconds()));
        assertTrue(Float.isNaN(missing.gravity()));
        assertTrue(Float.isNaN(missing.friction()));
        assertFalse(missing.ignite());
    }

    @Test
    void configuredBarrelBuildsIndependentNativeBulletDataFromTheTemplate() {
        ModuleConfig config = ModuleConfig.from(JsonParser.parseString("""
                {"barrels":[
                  {"id":"40mm","stats":{"life":1.5,"gravity":0.35,"friction":0.02,
                    "damage":12.0,"pierce":2,"knockback":1.25,"ignite":1}}
                ]}
                """).getAsJsonObject());
        BulletData template = new Gson().fromJson("""
                {"life":0.6,"bullet_amount":1,"damage":5.0,"speed":180.0,
                 "gravity":0.15,"knockback":0.0,"friction":0.025,"pierce":0,
                 "ignite":{"entity":false,"block":false},"ignite_entity_time":0,
                 "tracer_count_interval":0}
                """, BulletData.class);

        BulletData configured = ConfiguredProjectilePolicy.nativeBulletData(config, "40mm", template);

        assertEquals(1.5f, configured.getLifeSecond(), 0.0001f);
        assertEquals(12.0f, configured.getDamageAmount(), 0.0001f);
        assertEquals(0.35f, configured.getGravity(), 0.0001f);
        assertEquals(0.02f, configured.getFriction(), 0.0001f);
        assertEquals(2, configured.getPierce());
        assertEquals(1.25f, configured.getKnockback(), 0.0001f);
        assertTrue(configured.getIgnite().isIgniteEntity());
        assertTrue(configured.getIgnite().isIgniteBlock());
    }
}
