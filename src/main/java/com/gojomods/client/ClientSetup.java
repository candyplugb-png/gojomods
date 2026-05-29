package com.gojomods.client;

import com.gojomods.init.ModEntities;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Registers entity renderers on the CLIENT side only.
 * Without this Forge crashes with "entityrenderer is null".
 * We use NoopRenderer — entities are invisible, no model needed.
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // NoopRenderer = entity exists in world but renders nothing (invisible)
        EntityRenderers.register(ModEntities.CURSED_SPHERE.get(), NoopRenderer::new);
        EntityRenderers.register(ModEntities.HOLLOW_PURPLE.get(), NoopRenderer::new);
    }
}
