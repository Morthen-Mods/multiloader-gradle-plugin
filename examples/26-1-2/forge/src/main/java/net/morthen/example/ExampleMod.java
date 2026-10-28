package net.morthen.example;

import net.minecraftforge.fml.common.Mod;

@Mod(ExampleConstants.MOD_ID)
public class ExampleMod {
    public ExampleMod() {
        ExampleConstants.LOGGER.warn("Hello from Forge");
    }
}
