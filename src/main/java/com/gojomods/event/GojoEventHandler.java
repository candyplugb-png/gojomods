package com.gojomods.event;

import com.gojomods.entity.CursedSphereEntity;
import com.gojomods.entity.HollowPurpleEntity;
import com.gojomods.init.ModEntities;
import com.gojomods.init.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber
public class GojoEventHandler {

    // Tracks charge state per player UUID
    private static final Map<UUID, ChargeState> CHARGING = new HashMap<>();

    // Phases
    private static final int PHASE_CHARGING  = 0;  // Spheres moving toward each other
    private static final int PHASE_MERGING   = 1;  // Merge animation
    private static final int PHASE_FLYING    = 2;  // Hollow Purple projectile in flight

    private static final int CHARGE_TICKS    = 200; // 10 seconds
    private static final int MERGE_TICKS     = 20;  // 1 second merge animation

    // -----------------------------------------------------------------------
    // Left click triggers charging start
    // -----------------------------------------------------------------------
    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        tryStartCharge(event.getEntity());
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        tryStartCharge(event.getEntity());
    }

    private static void tryStartCharge(Player player) {
        if (player.level().isClientSide()) return;
        ItemStack held = player.getMainHandItem();
        if (held.getItem() != ModItems.VOID_BALL.get()) return;
        if (CHARGING.containsKey(player.getUUID())) return; // already charging

        // Spawn blue (left) and red (right) spheres
        Vec3 look = player.getLookAngle();
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 left  = right.scale(-1);

        Vec3 mergePoint = player.position()
                .add(0, 1.5, 0)
                .add(look.scale(8)); // 8 blocks in front

        Vec3 blueStart = player.position().add(0, 1.5, 0).add(left.scale(18));
        Vec3 redStart  = player.position().add(0, 1.5, 0).add(right.scale(18));

        ServerLevel level = (ServerLevel) player.level();

        CursedSphereEntity blue = ModEntities.CURSED_SPHERE.get().create(level);
        CursedSphereEntity red  = ModEntities.CURSED_SPHERE.get().create(level);
        if (blue == null || red == null) return;

        blue.moveTo(blueStart.x, blueStart.y, blueStart.z, 0, 0);
        blue.setRed(false);
        blue.targetX = mergePoint.x;
        blue.targetY = mergePoint.y;
        blue.targetZ = mergePoint.z;

        red.moveTo(redStart.x, redStart.y, redStart.z, 0, 0);
        red.setRed(true);
        red.targetX = mergePoint.x;
        red.targetY = mergePoint.y;
        red.targetZ = mergePoint.z;

        level.addFreshEntity(blue);
        level.addFreshEntity(red);

        // Sound effect
        level.playSound(null, player.blockPosition(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0f, 0.5f);

        player.sendSystemMessage(Component.literal("§5▶ Заряжаем Полую Пустоту... (10 секунд)"));

        CHARGING.put(player.getUUID(), new ChargeState(
                blue.getId(), red.getId(), mergePoint, look,
                PHASE_CHARGING, 0
        ));
    }

    // -----------------------------------------------------------------------
    // Server tick — advance charge states
    // -----------------------------------------------------------------------
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        CHARGING.entrySet().removeIf(entry -> {
            UUID uuid = entry.getKey();
            ChargeState state = entry.getValue();

            // Find the player
            Player player = null;
            for (var serverLevel : event.getServer().getAllLevels()) {
                player = serverLevel.getPlayerByUUID(uuid);
                if (player != null) break;
            }

            if (player == null) return true; // player left, clean up

            // Make sure player still holds the void ball
            if (player.getMainHandItem().getItem() != ModItems.VOID_BALL.get()) {
                // Player switched items — cancel
                cancelCharge(player, state);
                return true;
            }

            state.ticks++;
            ServerLevel level = (ServerLevel) player.level();

            // --- PHASE: CHARGING ---
            if (state.phase == PHASE_CHARGING) {

                // Progress bar feedback
                if (state.ticks % 20 == 0) {
                    int seconds = state.ticks / 20;
                    String bar = "§5" + "█".repeat(seconds) + "§8" + "█".repeat(10 - seconds);
                    player.sendSystemMessage(Component.literal(bar + " §7" + seconds + "/10с"));
                }

                if (state.ticks >= CHARGE_TICKS) {
                    // Transition to MERGING — remove old spheres
                    var blueEntity = level.getEntity(state.blueId);
                    var redEntity  = level.getEntity(state.redId);
                    if (blueEntity != null) blueEntity.discard();
                    if (redEntity  != null) redEntity.discard();

                    // Merge explosion sound
                    level.playSound(null, player.blockPosition(),
                            SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 3.0f, 0.3f);

                    player.sendSystemMessage(Component.literal("§5✦ ПОЛАЯ ПУСТОТА! §7— Летит!"));
                    state.phase = PHASE_MERGING;
                    state.ticks = 0;
                }
                return false;
            }

            // --- PHASE: MERGING ---
            if (state.phase == PHASE_MERGING) {
                if (state.ticks >= MERGE_TICKS) {
                    // Spawn Hollow Purple projectile
                    HollowPurpleEntity purple = ModEntities.HOLLOW_PURPLE.get().create(level);
                    if (purple == null) return true;

                    Vec3 spawnPos = state.mergePoint;
                    purple.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, 0, 0);

                    // Velocity toward player's look direction
                    Vec3 vel = player.getLookAngle().scale(1.8);
                    purple.setVelocity(vel);

                    level.addFreshEntity(purple);

                    level.playSound(null, player.blockPosition(),
                            SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 3.0f, 0.4f);

                    state.phase = PHASE_FLYING;
                    state.ticks = 0;
                    // We can remove charge state — purple handles its own lifecycle
                    return true;
                }
                return false;
            }

            return true;
        });
    }

    private static void cancelCharge(Player player, ChargeState state) {
        ServerLevel level = (ServerLevel) player.level();
        var blueEntity = level.getEntity(state.blueId);
        var redEntity  = level.getEntity(state.redId);
        if (blueEntity != null) blueEntity.discard();
        if (redEntity  != null) redEntity.discard();
        player.sendSystemMessage(Component.literal("§cЗаряд прерван."));
    }

    // -----------------------------------------------------------------------
    // Charge state record
    // -----------------------------------------------------------------------
    private static class ChargeState {
        int blueId, redId;
        Vec3 mergePoint, lookDir;
        int phase, ticks;

        ChargeState(int blueId, int redId, Vec3 mergePoint, Vec3 lookDir, int phase, int ticks) {
            this.blueId = blueId;
            this.redId = redId;
            this.mergePoint = mergePoint;
            this.lookDir = lookDir;
            this.phase = phase;
            this.ticks = ticks;
        }
    }
}
