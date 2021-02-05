/*
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package net.dries007.tfc.objects.blocks.stone;

import java.util.Random;
import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import mcp.MethodsReturnNonnullByDefault;
import net.dries007.tfc.api.registries.TFCRegistries;
import net.dries007.tfc.api.types.Plant;
import net.dries007.tfc.api.types.Rock;
import net.dries007.tfc.objects.blocks.BlockPeat;
import net.dries007.tfc.objects.blocks.BlocksTFC;
import net.dries007.tfc.objects.blocks.plants.BlockShortGrassTFC;
import net.dries007.tfc.objects.entity.EntityFallingBlockTFC;
import net.dries007.tfc.util.IFallingBlock;
import net.dries007.tfc.util.climate.ClimateTFC;
import net.dries007.tfc.world.classic.chunkdata.ChunkDataTFC;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class BlockRockVariantConnected extends BlockRockVariantFallable
{
    // Used for connected textures only.
    public static final PropertyBool NORTH = PropertyBool.create("north");
    public static final PropertyBool EAST = PropertyBool.create("east");
    public static final PropertyBool SOUTH = PropertyBool.create("south");
    public static final PropertyBool WEST = PropertyBool.create("west");

    public static void spreadGrass(World world, BlockPos pos, IBlockState us, Random rand) {
        BlockPos upPos = pos.up();
        IBlockState up = world.getBlockState(upPos);
        int neighborLight;
        Block usBlock;
        if (up.getMaterial().isLiquid() || ((neighborLight = world.getLightFromNeighbors(upPos)) < 4 && up.getLightOpacity(world, upPos) > 2))
        {
            usBlock = us.getBlock();
            if (usBlock instanceof BlockPeat)
            {
                world.setBlockState(pos, BlocksTFC.PEAT.getDefaultState());
            }
            else if (usBlock instanceof BlockRockVariant)
            {
                BlockRockVariant rock = ((BlockRockVariant) usBlock);
                world.setBlockState(pos, rock.getVariant(rock.getType().getNonGrassVersion()).getDefaultState());
            }
        }
        else if (neighborLight >= 9)
        {
            for (int i = 0; i < 4; ++i)
            {
                BlockPos target = pos.add(rand.nextInt(3) - 1, rand.nextInt(5) - 3, rand.nextInt(3) - 1);
                if (world.isOutsideBuildHeight(target) || !world.isBlockLoaded(target))
                {
                    return;
                }
                IBlockState current = world.getBlockState(target);
                if (!BlocksTFC.isSoil(current) || BlocksTFC.isGrass(current))
                {
                    continue;
                }
                BlockPos targetUp = target.up();
                IBlockState targetUpState;
                if (world.getLightFromNeighbors(targetUp) < 4 || (targetUpState = world.getBlockState(targetUp)).getMaterial().isLiquid() || targetUpState.getLightOpacity(world, targetUp) > 3)
                {
                    continue;
                }
                Block currentBlock = current.getBlock();
                if (currentBlock instanceof BlockPeat)
                {
                    world.setBlockState(target, BlocksTFC.PEAT_GRASS.getDefaultState());
                }
                else if (currentBlock instanceof BlockRockVariant)
                {
                    Rock.Type spreader = Rock.Type.GRASS;
                    usBlock = us.getBlock();
                    if ((usBlock instanceof BlockRockVariant) && ((BlockRockVariant) usBlock).getType() == Rock.Type.DRY_GRASS)
                    {
                        spreader = Rock.Type.DRY_GRASS;
                    }
                    BlockRockVariant block = ((BlockRockVariant) current.getBlock());
                    world.setBlockState(target, block.getVariant(block.getType().getGrassVersion(spreader)).getDefaultState());
                }
            }
            for (Plant plant : TFCRegistries.PLANTS.getValuesCollection())
            {
                if (plant.getPlantType() == Plant.PlantType.SHORT_GRASS && rand.nextFloat() < 0.5f)
                {
                    float temp = ClimateTFC.getActualTemp(world, upPos);
                    BlockShortGrassTFC plantBlock = BlockShortGrassTFC.get(plant);

                    if (world.isAirBlock(upPos) &&
                        plant.isValidLocation(temp, ChunkDataTFC.getRainfall(world, upPos), Math.subtractExact(world.getLightFor(EnumSkyBlock.SKY, upPos), world.getSkylightSubtracted())) &&
                        plant.isValidGrowthTemp(temp) &&
                        rand.nextDouble() < plantBlock.getGrowthRate(world, upPos))
                    {
                        world.setBlockState(upPos, plantBlock.getDefaultState());
                    }
                }
            }
        }
    }

    public BlockRockVariantConnected(Rock.Type type, Rock rock)
    {
        super(type, rock);
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return 0;
    }

    @SuppressWarnings("deprecation")
    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        pos = pos.add(0, -1, 0);
        return state.withProperty(NORTH, BlocksTFC.isGrass(world.getBlockState(pos.offset(EnumFacing.NORTH))))
            .withProperty(EAST, BlocksTFC.isGrass(world.getBlockState(pos.offset(EnumFacing.EAST))))
            .withProperty(SOUTH, BlocksTFC.isGrass(world.getBlockState(pos.offset(EnumFacing.SOUTH))))
            .withProperty(WEST, BlocksTFC.isGrass(world.getBlockState(pos.offset(EnumFacing.WEST))));
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, NORTH, EAST, WEST, SOUTH);
    }

    @Override
    public boolean checkFalling(World worldIn, BlockPos pos, IBlockState state)
    {
        BlockPos pos1 = getFallablePos(worldIn, pos);
        if (pos1 != null)
        {
            if (!BlockFalling.fallInstantly && worldIn.isAreaLoaded(pos.add(-32, -32, -32), pos.add(32, 32, 32)))
            {
                if (!pos1.equals(pos))
                {
                    worldIn.setBlockToAir(pos);
                }
                // Replace grass with dirt
                if (type.getNonGrassVersion() != type)
                {
                    worldIn.setBlockState(pos1, BlockRockVariant.get(rock, type.getNonGrassVersion()).getDefaultState());
                }
                worldIn.spawnEntity(new EntityFallingBlockTFC(worldIn, pos1, this, worldIn.getBlockState(pos1)));
            }
            else
            {
                worldIn.setBlockToAir(pos);
                pos1 = pos1.down();
                while (IFallingBlock.canFallThrough(worldIn, pos1, state.getMaterial()) && pos1.getY() > 0)
                {
                    pos1 = pos1.down();
                }
                if (pos1.getY() > 0)
                {
                    worldIn.setBlockState(pos1.up(), state); // Includes Forge's fix for data loss.
                }
            }
            return true;
        }
        return false;
    }
}
