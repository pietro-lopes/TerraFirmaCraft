package net.dries007.tfc.common.blocks.crop;


import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blocks.TFCBlockStateProperties;

public class DoubleDeadCropBlock extends DeadCropBlock
{
    public static final EnumProperty<DoubleCropBlock.Part> PART = TFCBlockStateProperties.DOUBLE_CROP_PART;

    public DoubleDeadCropBlock(Properties properties)
    {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(PART, DoubleCropBlock.Part.BOTTOM).setValue(MATURE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder.add(PART));
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos)
    {
        final DoubleCropBlock.Part part = state.getValue(PART);
        final BlockState belowState = level.getBlockState(pos.below());
        if (part == DoubleCropBlock.Part.BOTTOM)
        {
            return TFCTags.Blocks.FARMLAND.contains(belowState.getBlock());
        }
        else
        {
            return belowState.is(this) && belowState.getValue(PART) == DoubleCropBlock.Part.BOTTOM;
        }
    }
}