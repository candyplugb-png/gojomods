package com.gojomods.init;

import com.gojomods.GojoMod;
import com.gojomods.entity.CursedSphereEntity;
import com.gojomods.entity.HollowPurpleEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, GojoMod.MOD_ID);

    public static final RegistryObject<EntityType<CursedSphereEntity>> CURSED_SPHERE =
            ENTITIES.register("cursed_sphere", () ->
                    EntityType.Builder.<CursedSphereEntity>of(CursedSphereEntity::new, MobCategory.MISC)
                            .sized(3.0f, 3.0f)
                            .clientTrackingRange(64)
                            .noSummon()
                            .build("cursed_sphere"));

    public static final RegistryObject<EntityType<HollowPurpleEntity>> HOLLOW_PURPLE =
            ENTITIES.register("hollow_purple", () ->
                    EntityType.Builder.<HollowPurpleEntity>of(HollowPurpleEntity::new, MobCategory.MISC)
                            .sized(4.0f, 4.0f)
                            .clientTrackingRange(64)
                            .noSummon()
                            .build("hollow_purple"));
}
