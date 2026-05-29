package com.gojomods.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The Void Ball — holding LMB with this triggers the Hollow Purple sequence.
 * The actual logic is in GojoEventHandler (attack events).
 */
public class VoidBallItem extends Item {

    public VoidBallItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("§5✦ Пустой шар ✦"));
        lines.add(Component.literal("§7Удерживай ЛКМ — зарядка Пустоты"));
        lines.add(Component.literal("§9Синий + §cКрасный §5= Полая Пустота"));
        lines.add(Component.literal("§8Техника: Годзё Сатору"));
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    // Not a usable item — activation via attack event
    @Override
    public boolean isEnchantable(ItemStack stack) { return false; }
}
