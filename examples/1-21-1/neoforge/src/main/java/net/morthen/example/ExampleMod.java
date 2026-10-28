package net.morthen.example;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(ExampleConstants.MOD_ID)
public class ExampleMod {
    public ExampleMod(IEventBus bus) {
        ExampleConstants.LOGGER.warn("Hello from NeoForge");
    }
}
