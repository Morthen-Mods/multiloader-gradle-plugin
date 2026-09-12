package com.example.example_mod.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import com.example.example_mod.CommonConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class GametestConstants {
    public static final String MOD_ID = CommonConstants.MOD_ID + "_gametest";
    public static final String MOD_NAME = "Example Mod Gametest";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    public static void commonInit() {

    }

    public static void initTests(BiConsumer<String, Consumer<GameTestHelper>> consumer) {
        consumer.accept("dirt_at_origin", ExampleModTest::dirtAtOrigin);
    }
}
