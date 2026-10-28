package net.morthen.example;

import net.fabricmc.api.ModInitializer;

public class ExampleMod implements ModInitializer {
    @Override
    public void onInitialize() {
        ExampleConstants.LOGGER.warn("Hello from Fabric");
    }
}
