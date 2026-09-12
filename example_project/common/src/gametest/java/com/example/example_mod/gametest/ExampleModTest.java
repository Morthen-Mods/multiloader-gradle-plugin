package com.example.example_mod.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

public class ExampleModTest {
    public static void dirtAtOrigin(GameTestHelper helper) {
        helper.setBlock(BlockPos.ZERO, Blocks.DIRT);
        helper.assertBlockPresent(Blocks.DIRT, BlockPos.ZERO);
        helper.succeed();
    }
}
