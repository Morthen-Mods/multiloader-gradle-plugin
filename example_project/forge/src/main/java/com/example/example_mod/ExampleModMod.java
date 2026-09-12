package com.example.example_mod;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CommonConstants.MOD_ID)
public class ExampleModMod {
    public ExampleModMod(FMLJavaModLoadingContext context) {
        CommonConstants.commonInit();
        CommonConstants.LOGGER.info("Loading ExampleModMod");
    }
}
