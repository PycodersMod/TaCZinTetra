package com.pycoder.taczintetra.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/** Extensible runtime hook for addon behavior without adding addon hard dependencies. */
@Cancelable
public final class GunRuntimeEvent extends Event {
    public enum Type {
        BULLET_FIRE,
        BULLET_HIT,
        BULLET_HEADSHOT,
        BULLET_KILL,
        RELOAD,
        EXPLOSION_HIT
    }

    private final Type type;
    private final LivingEntity actor;
    private final ItemStack stack;

    public GunRuntimeEvent(Type type, LivingEntity actor, ItemStack stack) {
        this.type = type;
        this.actor = actor;
        this.stack = stack;
    }

    public Type type() { return type; }
    public LivingEntity actor() { return actor; }
    public ItemStack stack() { return stack; }
}
