package com.gojomods.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Blue (left) or Red (right) cursed energy sphere.
 * Slowly drifts toward a target merge point over 10 seconds.
 */
public class CursedSphereEntity extends Entity {

    private static final EntityDataAccessor<Boolean> IS_RED =
            SynchedEntityData.defineId(CursedSphereEntity.class, EntityDataSerializers.BOOLEAN);

    // Target position to merge at (set by GojoEventHandler)
    public double targetX, targetY, targetZ;
    public int lifeTicks = 0;
    public static final int MERGE_TICKS = 200; // 10 seconds

    public CursedSphereEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(IS_RED, false);
    }

    public void setRed(boolean red) { this.entityData.set(IS_RED, red); }
    public boolean isRed() { return this.entityData.get(IS_RED); }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;

        lifeTicks++;

        // Drift toward target
        double dx = targetX - this.getX();
        double dy = targetY - this.getY();
        double dz = targetZ - this.getZ();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (dist > 0.3) {
            double speed = 0.08 + (lifeTicks / (double) MERGE_TICKS) * 0.25;
            this.setDeltaMovement(dx / dist * speed, dy / dist * speed, dz / dist * speed);
            this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
        }

        // Particle trail
        spawnParticles();

        if (lifeTicks > MERGE_TICKS + 40) {
            this.discard();
        }
    }

    private void spawnParticles() {
        // Particles sent via custom packet — handled server-side via broadcastEntityEvent
        if (lifeTicks % 2 == 0) {
            this.level().broadcastEntityEvent(this, (byte) (isRed() ? 10 : 11));
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        lifeTicks = tag.getInt("lifeTicks");
        targetX = tag.getDouble("targetX");
        targetY = tag.getDouble("targetY");
        targetZ = tag.getDouble("targetZ");
        this.entityData.set(IS_RED, tag.getBoolean("isRed"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("lifeTicks", lifeTicks);
        tag.putDouble("targetX", targetX);
        tag.putDouble("targetY", targetY);
        tag.putDouble("targetZ", targetZ);
        tag.putBoolean("isRed", isRed());
    }

    @Override
    public boolean isPickable() { return false; }

    @Override
    public boolean isPushable() { return false; }
}
