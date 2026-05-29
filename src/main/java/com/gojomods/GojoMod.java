package com.gojomods;

import com.gojomods.init.ModEntities;
import com.gojomods.init.ModItems;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(GojoMod.MOD_ID)
public class GojoMod {

    public static final String MOD_ID = "gojomods";

    public GojoMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(bus);
        ModEntities.ENTITIES.register(bus);
        MinecraftForge.EVENT_BUS.register(this);
    }
}
