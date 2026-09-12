package com.example.example_mod.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import com.example.example_mod.gametest.ExampleModTest;

public class GametestMod {
    @GameTest
    public void dirtAtOrigin(GameTestHelper helper) {
        ExampleModTest.dirtAtOrigin(helper);
    }
}
