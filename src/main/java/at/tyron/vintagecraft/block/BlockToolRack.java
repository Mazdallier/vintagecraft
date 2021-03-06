package at.tyron.vintagecraft.block;


import java.util.ArrayList;
import java.util.List;

import at.tyron.vintagecraft.TileEntity.TEToolRack;
import at.tyron.vintagecraft.WorldProperties.EnumTree;
import at.tyron.vintagecraft.interfaces.IEnumState;
import at.tyron.vintagecraft.interfaces.IRackable;
import at.tyron.vintagecraft.item.ItemToolRack;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockToolRack extends BlockContainerVC {
	//public static PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
	//public static PropertyEnum TREETYPE = PropertyEnum.create("treetype", EnumTree.class);
	
	
	public BlockToolRack() {
		super(Material.wood);
		//this.setCreativeTab(CreativeTabs.tabDecorations);
		this.setDefaultState(this.blockState.getBaseState()); //.withProperty(FACING, EnumFacing.NORTH));
	}
	
	@Override
	public void getSubBlocks(Item item, CreativeTabs tabs, List list) {
		for (EnumTree tree : EnumTree.values()) {
			list.add(getItemStackFor(tree));
		}
	}

	
	/*@Override
	public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
		return state.withProperty(TREETYPE, getTreeType(worldIn, pos));
	}*/
	
	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TEToolRack();
	}
	

	@Override
	public boolean isOpaqueCube() {
		return false;
	}
	
	@Override
	public boolean isPassable(IBlockAccess worldIn, BlockPos pos) {
		return true;
	}
	
	@Override
	public AxisAlignedBB getCollisionBoundingBox(World worldIn, BlockPos pos, IBlockState state) {
		return null;
	}
	

	@Override
	public EnumWorldBlockLayer getBlockLayer() {
		return EnumWorldBlockLayer.CUTOUT;
	}
	
	
	public EnumFacing getFacing(World world, BlockPos pos) {
		TileEntity te = world.getTileEntity(pos);
		if(te != null && te instanceof TEToolRack) {
			return ((TEToolRack) te).facing == null ? EnumFacing.NORTH : ((TEToolRack) te).facing;
		}
		return EnumFacing.NORTH;
	}
	
	
	// Called when user places a ItemToolRack
	public void initTileEntity(World world, BlockPos pos, EnumFacing placedontoside, EnumTree tree) {
		//System.out.println(world.isRemote + " / " + placedontoside.getOpposite());
		
		TileEntity te = world.getTileEntity(pos);
		if(te != null && te instanceof TEToolRack) {
			((TEToolRack) te).facing = placedontoside;
			((TEToolRack) te).woodtype = tree;
		}
	}
	
	
	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer entityplayer, EnumFacing side, float hitX, float hitY, float hitZ) {
		if(!world.isRemote) {
			TileEntity te = world.getTileEntity(pos);
			if(te != null && te instanceof TEToolRack) {
				TEToolRack tet = (TEToolRack) te;
				EnumFacing facing = tet.facing; // (EnumFacing) world.getBlockState(pos).getValue(FACING);
				
				int slot = 0 + (hitY < 0.5 ? 2 : 0);
				
				switch (facing) {
					case NORTH: if (hitX > 0.5) slot++; break;
					case SOUTH: if (hitX < 0.5) slot++; break;
					case WEST: if (hitZ > 0.5) slot++; break;
					case EAST: if (hitZ < 0.5) slot++; break;
					default: break;
				}
				
				if (handleArea(world, pos, entityplayer, tet, slot, facing)) { 
					world.markBlockForUpdate(pos);
					return true;
				}
			}
		}
		return true;
	}

	private boolean handleArea(World world, BlockPos pos, EntityPlayer entityplayer, TEToolRack te, int slot, EnumFacing facing) {
		ItemStack currentitem = entityplayer.getCurrentEquippedItem();
		boolean hasToolInHand = currentitem != null && currentitem.getItem() instanceof IRackable;

		if(te.storage[slot] == null && hasToolInHand) {
			te.storage[slot] = entityplayer.getCurrentEquippedItem().copy();
			entityplayer.inventory.mainInventory[entityplayer.inventory.currentItem] = null;
			return true;
		}
		else if(te.storage[slot] != null && entityplayer.getCurrentEquippedItem() == null) {
			te.grabItem(slot, facing, entityplayer);
			te.storage[slot] = null;
			return true;
		}
		
		return false;
	}

	
	
	@Override
	public boolean removedByPlayer(World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
		if(!world.isRemote) {
			TileEntity te = world.getTileEntity(pos);
			
			if((te != null) && (te instanceof TEToolRack)) {
				TEToolRack rack = (TEToolRack) te;
				
				rack.ejectContents();
				spawnAsEntity(world, pos, ItemToolRack.getItemStack(rack.woodtype));
			}
		}
		return world.setBlockToAir(pos);
	}

	@Override
	public void harvestBlock(World worldIn, EntityPlayer player, BlockPos pos, IBlockState state, TileEntity te) {
		// don't drop here, we dropped in removedByPlayer instead
	}
	

	public EnumTree getTreeType(IBlockAccess worldIn, BlockPos pos) {
		EnumTree treetype = EnumTree.ASH;
		
		TileEntity te = worldIn.getTileEntity(pos);
		if((te != null) && (te instanceof TEToolRack)) {
			if (((TEToolRack)te).woodtype != null) {
				treetype = ((TEToolRack)te).woodtype;
			}
		}
		return treetype;
	}
	
	
	@Override
	public List<ItemStack> getDrops(IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
		ArrayList<ItemStack> ret = new ArrayList<ItemStack>();
		ret.add(getItemStackFor(getTreeType(world, pos)));		
		return ret;
	}


	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess worldIn, BlockPos pos) {
		EnumFacing facing = getFacing((World)worldIn, pos);
		
		switch (facing) {
			case NORTH: this.setBlockBounds(0.0F, 0F, 0.85F, 1F, 1F, 1F); break;
			case EAST: this.setBlockBounds(0.0F, 0F, 0.0F, 0.15F, 1F, 1F); break;
			case SOUTH: this.setBlockBounds(0.0F, 0F, 0.00F, 1F, 1F, 0.15F); break;
			case WEST: this.setBlockBounds(0.85F, 0F, 0.0F, 1F, 1F, 1F); break;
		default:
			break;
		}
	}

	@Override
	public AxisAlignedBB getSelectedBoundingBox(World worldIn, BlockPos pos) {
		EnumFacing facing = getFacing(worldIn, pos);
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		
		switch (facing) {
			case NORTH: return AxisAlignedBB.fromBounds(x + 0.0F, y + 0F, z + 0.85F, x + 1F, y + 1F, z + 1F);
			case EAST: return AxisAlignedBB.fromBounds(x + 0.0F, y + 0F, z + 0.0F, x + 0.15F, y + 1F, z + 1F);
			case SOUTH: return AxisAlignedBB.fromBounds(x + 0.0F, y + 0F, z + 0.00F, x + 1F, y + 1F, z + 0.15F);
			case WEST: return AxisAlignedBB.fromBounds(x + 0.85F, y + 0F, z + 0.0F, x + 1F, y + 1F, z + 1F);
			default: return AxisAlignedBB.fromBounds(x, y, z, x + 1, y + 1, z + 1);
		
		}		
	}
	
	
	public boolean suitableWall(World world, BlockPos wallpos, EnumFacing side) {
		return world.isSideSolid(wallpos, side);
	}

	
	@Override
	public void onNeighborBlockChange(World world, BlockPos pos, IBlockState state, Block neighborBlock) {
		EnumFacing facing = getFacing(world, pos);

		if (!suitableWall(world, pos.offset(facing.getOpposite()), facing)) {
			removedByPlayer(world, pos, null, true);
		}
	}

	

	@Override
	public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side) {
		return this.canPlaceBlockAt(world, pos) && suitableWall(world, pos.offset(side.getOpposite()), side);
	}

	

	
	@Override
	public boolean isSideSolid(IBlockAccess world, BlockPos pos, EnumFacing side) {
		return false;
	}
	
	@Override
	public boolean shouldSideBeRendered(IBlockAccess worldIn, BlockPos pos, EnumFacing side) {
		return true;
	}
	
	
	

	
	public ItemStack getItemStackFor(IEnumState key) {
		return ItemToolRack.getItemStack((EnumTree)key);
	}

	@Override
	public String getSubType(ItemStack stack) {
		// TODO Auto-generated method stub
		return null;
	}

    public boolean isFullCube() {
    	return false;
    }

}