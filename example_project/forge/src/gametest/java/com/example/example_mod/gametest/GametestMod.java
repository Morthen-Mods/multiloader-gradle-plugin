package com.example.example_mod.gametest;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataProvider;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import com.example.example_mod.gametest.GametestConstants;
import com.example.example_mod.gametest.provider.GametestInstanceProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Mod(GametestConstants.MOD_ID)
public class GametestMod {
    public static final DeferredRegister<Consumer<GameTestHelper>> GAMETESTS = DeferredRegister.create(Registries.TEST_FUNCTION, GametestConstants.MOD_ID);
    private static final List<String> testKeys = new ArrayList<>();

    public GametestMod(FMLJavaModLoadingContext context) {
        GametestConstants.LOGGER.info("Loading Gametest Mod");
        GametestConstants.initTests(GametestMod::registerTest);
        GatherDataEvent.getBus(context.getModBusGroup()).addListener(GametestMod::gatherData);
        GAMETESTS.register(context.getModBusGroup());
    }

    private static void registerTest(String name, Consumer<GameTestHelper> consumer) {
        testKeys.add(name);
        GAMETESTS.register(name, () -> consumer);
    }

    public static void gatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(true, (DataProvider.Factory<GametestInstanceProvider>) output -> new GametestInstanceProvider(output, testKeys));
    }
}
