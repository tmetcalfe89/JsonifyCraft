package us.timinc.jsonifycraft.world;

import java.util.*;

import javax.annotation.*;

import net.minecraft.block.*;
import net.minecraft.block.state.*;
import net.minecraft.entity.item.*;
import net.minecraft.entity.player.*;
import net.minecraft.init.*;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraftforge.fml.relauncher.*;
import us.timinc.jsonifycraft.*;
import us.timinc.jsonifycraft.event.*;
import us.timinc.jsonifycraft.json.world.*;
import us.timinc.mcutil.*;

public class JsonedBlock extends Block {
	protected BlockDescription blockJson = null;
	private AxisAlignedBB boundingBox = null;

	public JsonedBlock(BlockDescription blockJson) {
		super(JsonifyCraft.REGISTRIES.getMaterial(blockJson.material));

		this.blockJson = blockJson;

		setup();
	}

	private void setup() {
		setCreativeTab(JsonifyCraft.REGISTRIES.getCreativeTab(blockJson.creativeTab));
		setDefaultSlipperiness(slipperiness);
		if ((blockJson.hardness < 0) || blockJson.hasFlag("unbreakable")) {
			setBlockUnbreakable();
		} else {
			setHardness(blockJson.hardness);
		}
		if (!blockJson.harvester.isEmpty()) {
			String[] splitHarvester = blockJson.harvester.split(",");
			setHarvestLevel(splitHarvester[0], Integer.parseInt(splitHarvester[1]));
		}
		setLightLevel(blockJson.lightLevel);
		setLightOpacity(blockJson.lightOpacity);
		setResistance(blockJson.resistance);
		setSoundType(JsonifyCraft.REGISTRIES.getSoundType(blockJson.soundType));
		translucent = blockJson.hasFlag("transparent");
		setTickRandomly(blockJson.hasEvent("randomtick"));
	}

	/* Characteristics */

	@Override
	public boolean canPlaceBlockAt(World worldIn, BlockPos pos) {
		if (blockJson == null)
			return super.canPlaceBlockAt(worldIn, pos);

		return super.canPlaceBlockAt(worldIn, pos) && blockJson.hasSoil(worldIn.getBlockState(pos.down()));
	}

	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
		if ((blockJson == null) || blockJson.boundingBox.isEmpty())
			return super.getBoundingBox(state, source, pos);

		// Cache boundingBox.
		if (boundingBox == null) {
			boundingBox = blockJson.createBoundingBox();
		}
		return boundingBox;
	}

	@Override
	@Nullable
	public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn, BlockPos pos) {
		if (blockJson == null)
			return super.getCollisionBoundingBox(blockState, worldIn, pos);

		return blockJson.hasFlag("ghost") ? NULL_AABB : super.getCollisionBoundingBox(blockState, worldIn, pos);
	}

	@Override
	public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
		if (blockJson == null)
			return super.getItem(worldIn, pos, state);

		return blockJson.drop != null ? PlaintextId.createItemStackFrom(blockJson.drop, 1)
				: super.getItem(worldIn, pos, state);
	}

	@Override
	public boolean isBeaconBase(IBlockAccess worldObj, BlockPos pos, BlockPos beacon) {
		if (blockJson == null)
			return super.isBeaconBase(worldObj, pos, beacon);

		return blockJson.hasFlag("beacon");
	}

	@Override
	public boolean isFullCube(IBlockState state) {
		if (blockJson == null)
			return super.isFullCube(state);

		return !blockJson.hasFlag("partial", "ghost");
	}

	@Override
	public boolean isOpaqueCube(IBlockState state) {
		if (blockJson == null)
			return super.isOpaqueCube(state);

		return !blockJson.hasFlag("transparent", "partial");
	}

	@Override
	@SideOnly(Side.CLIENT)
	public BlockRenderLayer getBlockLayer() {
		if (blockJson == null)
			return super.getBlockLayer();

		return blockJson.hasFlag("transparent") ? BlockRenderLayer.CUTOUT : BlockRenderLayer.SOLID;
	}

	/* Events */

	@Override
	public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
		if (blockJson == null) {
			super.neighborChanged(state, worldIn, pos, blockIn, fromPos);
			return;
		}

		checkForFall(worldIn, pos);
		checkForUproot(worldIn, pos, state);

		processEvent(worldIn, null, pos, "neighborchanged");
	}

	@Override
	public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
		if (blockJson == null) {
			super.onBlockAdded(worldIn, pos, state);
			return;
		}

		checkForFall(worldIn, pos);

		processEvent(worldIn, null, pos, "blockadded");
	}

	@Override
	public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
			EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if ((blockJson == null) || (hand == EnumHand.OFF_HAND))
			return super.onBlockActivated(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ);

		processEvent(worldIn, playerIn, pos, "blockclicked");

		return super.onBlockActivated(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ);
	}

	@Override
	public void randomTick(World worldIn, BlockPos pos, IBlockState state, Random random) {
		if (blockJson == null) {
			super.randomTick(worldIn, pos, state, random);
			return;
		}

		processEvent(worldIn, null, pos, "randomtick");

		super.randomTick(worldIn, pos, state, random);
	}

	@Override
	public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
		if (blockJson == null) {
			super.updateTick(worldIn, pos, state, rand);
			return;
		}

		if (blockJson.hasFlag("falls")) {
			if (!worldIn.isRemote) {
				makeBlockFall(worldIn, pos);
			}
		}
	}

	private void processEvent(World world, EntityPlayer player, BlockPos pos, String eventName) {
		EventDescription eventDescription = new EventDescription(world, player, pos);
		EventProcessor.process(eventDescription, blockJson.events, eventName);
	}

	/* Processors */

	@Override
	public void dropBlockAsItemWithChance(World worldIn, BlockPos pos, IBlockState state, float chance, int fortune) {
		if (blockJson == null) {
			super.dropBlockAsItemWithChance(worldIn, pos, state, chance, fortune);
			return;
		}

		if (blockJson.drop != null) {
			spawnAsEntity(worldIn, pos, PlaintextId.createItemStackFrom(blockJson.drop, 1));
		} else {
			super.dropBlockAsItemWithChance(worldIn, pos, state, chance, fortune);
		}
	}

	private void checkForFall(World worldIn, BlockPos pos) {
		if (blockJson.hasFlag("falls")) {
			worldIn.scheduleUpdate(pos, this, tickRate(worldIn));
		}
	}

	private void checkForUproot(World worldIn, BlockPos pos, IBlockState state) {
		if (!blockJson.hasSoil(worldIn.getBlockState(pos.down()))) {
			dropBlockAsItem(worldIn, pos, state, 0);
			worldIn.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
		}
	}

	private void makeBlockFall(World worldIn, BlockPos pos) {
		if ((worldIn.isAirBlock(pos.down()) || worldIn.getBlockState(pos.down()).getMaterial().isReplaceable())
				&& (pos.getY() >= 0)) {
			if (!worldIn.isRemote) {
				EntityFallingBlock entityfallingblock = new EntityFallingBlock(worldIn, pos.getX() + 0.5D, pos.getY(),
						pos.getZ() + 0.5D, worldIn.getBlockState(pos));
				worldIn.spawnEntity(entityfallingblock);
			}
		}
	}
}