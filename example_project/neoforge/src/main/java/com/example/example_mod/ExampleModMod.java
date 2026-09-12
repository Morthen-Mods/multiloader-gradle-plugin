package com.example.example_mod;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(CommonConstants.MOD_ID)
public class ExampleModMod {
    public ExampleModMod(IEventBus eventBus) {
        CommonConstants.commonInit();
        CommonConstants.LOGGER.info("ExampleModMod Loaded");
    }
}
