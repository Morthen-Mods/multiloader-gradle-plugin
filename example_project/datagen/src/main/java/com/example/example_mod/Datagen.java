package com.example.example_mod;

import com.example.example_mod.provider.ExampleModProvider;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@Mod(CommonConstants.MOD_ID)
public class Datagen {
    public Datagen(IEventBus eventBus) {
        eventBus.addListener(Datagen::gatherData);
    }

    public static void gatherData(GatherDataEvent.Client event) {
        event.createProvider(ExampleModProvider.CustomRecipes.Runner::new);
        event.createProvider(ExampleModProvider.CustomItemTags::new);
    }
}
