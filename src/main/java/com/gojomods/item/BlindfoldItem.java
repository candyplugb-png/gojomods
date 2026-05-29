package com.gojomods.item;

import com.gojomods.init.ModItems;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.List;

public class BlindfoldItem extends ArmorItem {

    private static final ArmorMaterial MATERIAL = ArmorMaterials.LEATHER;

    public BlindfoldItem() {
        super(MATERIAL, Type.HELMET, new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC));
    }

    /**
     * Called every tick while equipped — give VoidBall if player doesn't have one.
     */
    @Override
    public void onArmorTick(ItemStack stack, Level level, Player player) {
        if (level.isClientSide()) return;

        // Check if player already has a VoidBall anywhere in inventory
        boolean hasVoidBall = false;
        for (ItemStack inv : player.getInventory().items) {
            if (inv.getItem() == ModItems.VOID_BALL.get()) {
                hasVoidBall = true;
                break;
            }
        }

        if (!hasVoidBall) {
            ItemStack voidBall = new ItemStack(ModItems.VOID_BALL.get());
            player.getInventory().add(voidBall);
            player.sendSystemMessage(Component.literal("§5✦ Техника Годзё активирована — Пустота"));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("§7Повязка на глаза Сатору Годзё"));
        lines.add(Component.literal("§5При надевании — выдаёт Пустой шар"));
        lines.add(Component.literal("§8«Бесконечность»"));
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }
}
