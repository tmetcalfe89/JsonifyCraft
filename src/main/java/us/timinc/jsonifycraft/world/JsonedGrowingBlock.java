package us.timinc.jsonifycraft.world;

import java.util.Random;

import net.minecraft.block.IGrowable;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import us.timinc.jsonifycraft.event.EventContext;
import us.timinc.jsonifycraft.event.EventProcessor;
import us.timinc.jsonifycraft.json.world.BlockDescription;
import us.timinc.jsonifycraft.json.world.GrowingBlockDescription;

public class JsonedGrowingBlock extends JsonedBlock implements IGrowable {
	public static final PropertyInteger AGE = PropertyInteger.create("age", 0, 15);

	public JsonedGrowingBlock(BlockDescription blockJson) {
		super(blockJson);
		setDefaultState(blockState.getBaseState().withProperty(AGE, Integer.valueOf(0)));
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, new IProperty[] { AGE });
	}

	/**
	 * Convert the given metadata into a BlockState for this Block
	 */
	@Override
	public IBlockState getStateFromMeta(int meta) {
		return withAge(meta);
	}

	/**
	 * Convert the BlockState into the correct metadata value
	 */
	@Override
	public int getMetaFromState(IBlockState state) {
		return getAge(state);
	}

	public IBlockState withAge(int age) {
		return getDefaultState().withProperty(getAgeProperty(), Integer.valueOf(age));
	}

	protected PropertyInteger getAgeProperty() {
		return AGE;
	}

	public int getAge(IBlockState state) {
		return state.getValue(getAgeProperty()).intValue();
	}

	protected int getMaxAge() {
		return getJson().stages - 1;
	}

	protected GrowingBlockDescription getJson() {
		return (GrowingBlockDescription) blockJson;
	}

	@Override
	public boolean canGrow(World worldIn, BlockPos pos, IBlockState state, boolean isClient) {
		int currentAge = state.getValue(JsonedGrowingBlock.AGE).intValue();
		return currentAge < (getMaxAge());
	}

	@Override
	public boolean canUseBonemeal(World worldIn, Random rand, BlockPos pos, IBlockState state) {
		return blockJson.hasFlag("bonemealable");
	}

	@Override
	public void grow(World world, Random rand, BlockPos pos, IBlockState state) {
		int currentAge = state.getValue(JsonedGrowingBlock.AGE).intValue();
		if (currentAge != getMaxAge()) {
			world.setBlockState(pos, state.withProperty(JsonedGrowingBlock.AGE, Integer.valueOf(currentAge + 1)), 2);
		}

		EventContext eventContext = new EventContext(world);
		eventContext.addPosition("block", pos);

		EventProcessor.process(eventContext, blockJson.events, "growblock");
	}
}
