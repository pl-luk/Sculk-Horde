package com.github.sculkhoard.core;


import com.github.sculkhoard.common.DevWand;
import com.github.sculkhoard.core.Main;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ItemRegistry {
    //https://www.mr-pineapple.co.uk/tutorials/items
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Main.MOD_ID);
    
    public static final RegistryObject<Item> SCULK_MATTER = ITEMS.register("sculk_matter", () -> new Item(new Item.Properties().tab(Main.SCULK_GROUP)));

    public static final RegistryObject<DevWand> DEV_WAND = ITEMS.register("dev_wand", 
    		() -> new DevWand(new Item.Properties()
    				.tab(Main.SCULK_GROUP)
    				.durability(5)
    				.rarity(Rarity.EPIC)
    				.fireResistant()));
}