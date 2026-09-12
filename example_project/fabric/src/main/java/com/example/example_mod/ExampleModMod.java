package com.example.example_mod;

import net.fabricmc.api.ModInitializer;

public class ExampleModMod implements ModInitializer {
    @Override
    public void onInitialize() {
        CommonConstants.commonInit();
        CommonConstants.LOGGER.info("Loading ExampleModMod");
    }
}
