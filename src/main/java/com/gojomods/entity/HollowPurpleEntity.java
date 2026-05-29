package com.gojomods.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The Hollow Purple projectile — merges blue + red into a devastating void orb.
 * Flies straight, then on impact creates a massive explosion.
 */
public class HollowPurpleEntity extends Entity {

    private Vec3 velocity = Vec3.ZERO;
    private int lifeTicks = 0;
    private static final int MAX_LIFE = 200; // 10 seconds max flight

    // Grow-in animation ticks
    private int spawnTicks = 0;
    private static final int SPAWN_ANIM_TICKS = 20;

    public HollowPurpleEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void setVelocity(Vec3 vel) {
        this.velocity = vel;
        this.setDeltaMovement(vel);
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    public void tick() {
        super.tick();
        lifeTicks++;

        if (spawnTicks < SPAWN_ANIM_TICKS) {
            spawnTicks++;
        }

        if (level().isClientSide()) return;

        // Move
        this.setDeltaMovement(velocity);
        this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());

        // Check for block/entity collision
        boolean hitSomething = checkCollision();

        if (hitSomething || lifeTicks > MAX_LIFE) {
            explode();
        }

        // Purple particle trail
        if (lifeTicks % 1 == 0) {
            level().broadcastEntityEvent(this, (byte) 12);
        }
    }

    private boolean checkCollision() {
        // Check if we hit a solid block
        BlockPos pos = this.blockPosition();
        if (!level().getBlockState(pos).isAir()) {
            return true;
        }
        // Check if we hit a living entity (not the owner area)
        AABB hitBox = this.getBoundingBox().inflate(3.0);
        List<LivingEntity> entities = level().getEntitiesOfClass(LivingEntity.class, hitBox);
        return !entities.isEmpty();
    }

    private void explode() {
        if (level().isClientSide()) return;

        ServerLevel serverLevel = (ServerLevel) level();
        double x = this.getX(), y = this.getY(), z = this.getZ();

        // Primary massive explosion
        serverLevel.explode(null, x, y, z, 30.0f,
                Level.ExplosionInteraction.TNT);

        // Secondary ring explosions for full 100-TNT equivalent devastation
        double[][] offsets = {
            {15,0,0},{-15,0,0},{0,0,15},{0,0,-15},
            {10,5,10},{-10,5,-10},{10,5,-10},{-10,5,10},
            {0,10,0},{0,-5,0},
            {8,0,8},{-8,0,-8},{8,0,-8},{-8,0,8},
            {20,0,0},{-20,0,0},{0,0,20},{0,0,-20},
            {12,8,0},{-12,8,0},{0,8,12},{0,8,-12}
        };

        for (double[] off : offsets) {
            serverLevel.explode(null,
                    x + off[0], y + off[1], z + off[2],
                    15.0f, Level.ExplosionInteraction.TNT);
        }

        this.discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        lifeTicks = tag.getInt("lifeTicks");
        double vx = tag.getDouble("vx");
        double vy = tag.getDouble("vy");
        double vz = tag.getDouble("vz");
        velocity = new Vec3(vx, vy, vz);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("lifeTicks", lifeTicks);
        tag.putDouble("vx", velocity.x);
        tag.putDouble("vy", velocity.y);
        tag.putDouble("vz", velocity.z);
    }

    @Override
    public boolean isPickable() { return false; }

    @Override
    public boolean isPushable() { return false; }
}
