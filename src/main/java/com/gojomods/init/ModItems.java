package com.gojomods.init;

import com.gojomods.GojoMod;
import com.gojomods.item.BlindfoldItem;
import com.gojomods.item.VoidBallItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, GojoMod.MOD_ID);

    public static final RegistryObject<Item> BLINDFOLD =
            ITEMS.register("blindfold", BlindfoldItem::new);

    public static final RegistryObject<Item> VOID_BALL =
            ITEMS.register("void_ball", VoidBallItem::new);
}
