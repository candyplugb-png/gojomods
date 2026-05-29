package com.gojomods.event;

import com.gojomods.entity.CursedSphereEntity;
import com.gojomods.entity.HollowPurpleEntity;
import com.gojomods.init.ModEntities;
import com.gojomods.init.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber
public class GojoEventHandler {

    private static final Map<UUID, ChargeState> CHARGING  = new HashMap<>();
    // Tracks which players pressed LMB this tick (set by client->server attack events)
    private static final Set<UUID> ATTACKING = new HashSet<>();

    private static final int PHASE_CHARGING = 0;
    private static final int PHASE_MERGING  = 1;

    private static final int CHARGE_TICKS = 200; // 10 seconds
    private static final int MERGE_TICKS  = 20;  // 1 second merge animation

    // -----------------------------------------------------------------------
    // Capture EVERY attack event (entity, block, empty air via interact)
    // -----------------------------------------------------------------------
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player p = event.getEntity();
        if (!p.level().isClientSide() && holdsVoidBall(p))
            ATTACKING.add(p.getUUID());
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player p = event.getEntity();
        if (!p.level().isClientSide() && holdsVoidBall(p))
            ATTACKING.add(p.getUUID());
    }

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        Player p = event.getEntity();
        if (!p.level().isClientSide() && holdsVoidBall(p))
            ATTACKING.add(p.getUUID());
    }

    // -----------------------------------------------------------------------
    // Server tick — main logic
    // -----------------------------------------------------------------------
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // Process all online players
        for (var serverLevel : event.getServer().getAllLevels()) {
            for (Player player : serverLevel.players()) {
                UUID uuid = player.getUUID();
                boolean holdingVoidBall = holdsVoidBall(player);
                boolean attackedThisTick = ATTACKING.contains(uuid);

                // Start charge on first LMB press
                if (attackedThisTick && holdingVoidBall && !CHARGING.containsKey(uuid)) {
                    startCharge(player, (ServerLevel) serverLevel);
                }

                // Cancel if switched item while charging
                if (CHARGING.containsKey(uuid) && !holdingVoidBall) {
                    cancelCharge(player, (ServerLevel) serverLevel, CHARGING.remove(uuid));
                }
            }
        }
        ATTACKING.clear();

        // Advance charge states
        CHARGING.entrySet().removeIf(entry -> {
            UUID uuid = entry.getKey();
            ChargeState state = entry.getValue();

            // Find player
            ServerLevel level = null;
            Player player = null;
            for (var sl : event.getServer().getAllLevels()) {
                player = sl.getPlayerByUUID(uuid);
                if (player != null) { level = sl; break; }
            }
            if (player == null || level == null) return true;

            state.ticks++;

            // ── PHASE: CHARGING ──────────────────────────────────────────
            if (state.phase == PHASE_CHARGING) {
                // Progress bar every second
                if (state.ticks % 20 == 0) {
                    int sec = state.ticks / 20;
                    String filled = "§5" + "█".repeat(sec);
                    String empty  = "§8" + "█".repeat(10 - sec);
                    player.sendSystemMessage(Component.literal(filled + empty + " §7" + sec + "/10с"));
                }

                if (state.ticks >= CHARGE_TICKS) {
                    // Remove old spheres
                    var blue = level.getEntity(state.blueId);
                    var red  = level.getEntity(state.redId);
                    if (blue != null) blue.discard();
                    if (red  != null) red.discard();

                    level.playSound(null, player.blockPosition(),
                            SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 3.0f, 0.3f);
                    player.sendSystemMessage(Component.literal("§5✦ ПОЛАЯ ПУСТОТА! ✦"));

                    state.phase = PHASE_MERGING;
                    state.ticks = 0;
                }
                return false;
            }

            // ── PHASE: MERGING ────────────────────────────────────────────
            if (state.phase == PHASE_MERGING) {
                if (state.ticks >= MERGE_TICKS) {
                    // Spawn Hollow Purple
                    HollowPurpleEntity purple = ModEntities.HOLLOW_PURPLE.get().create(level);
                    if (purple == null) return true;

                    Vec3 mp = state.mergePoint;
                    purple.moveTo(mp.x, mp.y, mp.z, 0, 0);
                    purple.setVelocity(player.getLookAngle().scale(1.8));
                    level.addFreshEntity(purple);

                    level.playSound(null, player.blockPosition(),
                            SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 3.0f, 0.4f);
                    return true; // done
                }
                return false;
            }

            return true;
        });
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------
    private static boolean holdsVoidBall(Player player) {
        return player.getMainHandItem().getItem() == ModItems.VOID_BALL.get();
    }

    private static void startCharge(Player player, ServerLevel level) {
        Vec3 look  = player.getLookAngle();
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 left  = right.scale(-1);

        Vec3 origin     = player.position().add(0, 1.5, 0);
        Vec3 mergePoint = origin.add(look.scale(8));
        Vec3 blueStart  = origin.add(left.scale(18));
        Vec3 redStart   = origin.add(right.scale(18));

        CursedSphereEntity blue = ModEntities.CURSED_SPHERE.get().create(level);
        CursedSphereEntity red  = ModEntities.CURSED_SPHERE.get().create(level);
        if (blue == null || red == null) return;

        blue.moveTo(blueStart.x, blueStart.y, blueStart.z, 0, 0);
        blue.setRed(false);
        blue.targetX = mergePoint.x; blue.targetY = mergePoint.y; blue.targetZ = mergePoint.z;

        red.moveTo(redStart.x, redStart.y, redStart.z, 0, 0);
        red.setRed(true);
        red.targetX = mergePoint.x; red.targetY = mergePoint.y; red.targetZ = mergePoint.z;

        level.addFreshEntity(blue);
        level.addFreshEntity(red);

        level.playSound(null, player.blockPosition(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0f, 0.5f);

        player.sendSystemMessage(Component.literal("§5▶ Заряжаем Полую Пустоту... (10 секунд)"));
        CHARGING.put(player.getUUID(),
                new ChargeState(blue.getId(), red.getId(), mergePoint, look, PHASE_CHARGING, 0));
    }

    private static void cancelCharge(Player player, ServerLevel level, ChargeState state) {
        var blue = level.getEntity(state.blueId);
        var red  = level.getEntity(state.redId);
        if (blue != null) blue.discard();
        if (red  != null) red.discard();
        player.sendSystemMessage(Component.literal("§cЗаряд прерван."));
    }

    // -----------------------------------------------------------------------
    private static class ChargeState {
        int blueId, redId, phase, ticks;
        Vec3 mergePoint, lookDir;
        ChargeState(int bi, int ri, Vec3 mp, Vec3 ld, int ph, int ti) {
            blueId=bi; redId=ri; mergePoint=mp; lookDir=ld; phase=ph; ticks=ti;
        }
    }
}
