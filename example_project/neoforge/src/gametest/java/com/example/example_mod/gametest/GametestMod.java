package com.example.example_mod.gametest;

import com.google.common.collect.Maps;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import com.example.example_mod.gametest.GametestConstants;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Map;
import java.util.function.Consumer;

@Mod(GametestConstants.MOD_ID)
public class GametestMod {
    public static final DeferredRegister<Consumer<GameTestHelper>> GAMETEST = DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, GametestConstants.MOD_ID);
    private static final Map<String, ResourceKey<Consumer<GameTestHelper>>> TEST_FUNCTION_MAP = Maps.newHashMap();

    public GametestMod(IEventBus eventBus) {
        GAMETEST.register(eventBus);
        GametestConstants.initTests(GametestMod::registerTest);
        eventBus.addListener(GametestMod::registerTests);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(Identifier.fromNamespaceAndPath(GametestConstants.MOD_ID, "default"));

        for (Map.Entry<String, ResourceKey<Consumer<GameTestHelper>>> entry : TEST_FUNCTION_MAP.entrySet()) {
            event.registerTest(
                    Identifier.fromNamespaceAndPath(GametestConstants.MOD_ID, entry.getKey()),
                    new FunctionGameTestInstance(entry.getValue(),
                            new TestData<>(env, Identifier.withDefaultNamespace("empty"), 100, 0, true)));
        }
    }

    public static void registerTest(String testId, Consumer<GameTestHelper> consumer) {
        DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> test = GAMETEST.register(testId, () -> consumer);
        TEST_FUNCTION_MAP.putIfAbsent(testId, test.getKey());
    }
}
