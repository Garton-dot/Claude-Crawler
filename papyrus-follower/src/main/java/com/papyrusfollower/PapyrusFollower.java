package com.papyrusfollower;

import com.papyrusfollower.entity.PapyrusEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

public class PapyrusFollower implements ModInitializer {
    public static final String MOD_ID = "papyrus_follower";

    public static final EntityType<PapyrusEntity> PAPYRUS = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            id("papyrus"),
            EntityType.Builder.of(PapyrusEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.9F)
                    .clientTrackingRange(10)
                    .build("papyrus"));

    public static final Item SPAGHETTI = Registry.register(
            BuiltInRegistries.ITEM,
            id("spaghetti"),
            new Item(new Item.Properties()
                    .stacksTo(16)
                    .food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.8F).build())));

    public static final Item PAPYRUS_SPAWN_EGG = Registry.register(
            BuiltInRegistries.ITEM,
            id("papyrus_spawn_egg"),
            new SpawnEggItem(PAPYRUS, 0xFFFFFF, 0xFF4411, new Item.Properties()));

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        FabricDefaultAttributeRegistry.register(PAPYRUS, PapyrusEntity.createAttributes());

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS)
                .register(entries -> entries.accept(PAPYRUS_SPAWN_EGG));
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FOOD_AND_DRINKS)
                .register(entries -> entries.accept(SPAGHETTI));
    }
}
