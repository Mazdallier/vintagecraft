package at.tyron.vintagecraft.block;

import java.util.List;
import java.util.Random;

import javax.naming.spi.StateFactory;

import com.google.common.collect.Lists;

import at.tyron.vintagecraft.VintageCraft;
import at.tyron.vintagecraft.BlockClass.BlockClass;
import at.tyron.vintagecraft.BlockClass.BlockClassEntry;
import at.tyron.vintagecraft.BlockClass.PropertyBlockClass;
import at.tyron.vintagecraft.BlockClass.TreeClass;
import at.tyron.vintagecraft.World.BlocksVC;
import at.tyron.vintagecraft.World.VCraftWorld;
import at.tyron.vintagecraft.WorldProperties.EnumFlower;
import at.tyron.vintagecraft.block.BlockDoubleFlowerVC.EnumBlockHalf;
import at.tyron.vintagecraft.interfaces.IEnumState;
import at.tyron.vintagecraft.interfaces.IMultiblock;
import at.tyron.vintagecraft.item.ItemLeaves;
import net.minecraft.block.Block;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemSlab;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.ColorizerFoliage;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeColorHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockLeavesVC extends BlockVC implements IMultiblock {
	public int multistateAvailableTypes() {
		return 8;
	}
	
	//public static PropertyBool CHECK_DECAY = PropertyBool.create("checkdecay");
	public PropertyBlockClass TREETYPE;
	
	
//	int[] surroundings;
    
	
	public BlockLeavesVC() {
		super(Material.leaves);
		this.setTickRandomly(true);
		this.setLightOpacity(1);
        this.setCreativeTab(CreativeTabs.tabMaterials);    
	}
	
	
	public void init(BlockClassEntry []subtypes, PropertyBlockClass property) {
		this.subtypes = subtypes;
		setTypeProperty(property);
		
		blockState = this.createBlockState();
		setDefaultState(subtypes[0].getBlockState(blockState.getBaseState()));		
	}
	

    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockAccess worldIn, BlockPos pos, EnumFacing side) {
        return Blocks.leaves.isOpaqueCube() && worldIn.getBlockState(pos).getBlock() == this ? false : super.shouldSideBeRendered(worldIn, pos, side);
    }
    
    
    
    
    public boolean isPassable(IBlockAccess worldIn, BlockPos pos) {
        return true;
    }

    
    
    
    
    @SideOnly(Side.CLIENT)
    public int getBlockColor() {
        //return ColorizerFoliage.getFoliageColor(0.5D, 1.0D);
    	return 16777215;
    }

    @SideOnly(Side.CLIENT)
    public int getRenderColor(IBlockState state) {
        //return ColorizerFoliage.getFoliageColorBasic();
    	return 16777215;
    }

    @SideOnly(Side.CLIENT)
    public int colorMultiplier(IBlockAccess worldIn, BlockPos pos, int renderPass) {
       // return BiomeColorHelper.getFoliageColorAtPos(worldIn, pos);
    	//return VCraftWorld.getGrassColorAtPos(pos);
    	return 16777215;
    }
    
    
    public ItemStack getPickBlock(MovingObjectPosition target, World world, BlockPos pos) {
    	IBlockState state = world.getBlockState(pos);
    	
        return ItemLeaves.withTreeType(
        	new ItemStack(getItem(world,pos)),
        	(BlockClassEntry)state.getValue(getTypeProperty())
        );
    }
    

    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        byte b0 = 1;
        int i = b0 + 1;
        int j = pos.getX();
        int k = pos.getY();
        int l = pos.getZ();

        if (worldIn.isAreaLoaded(new BlockPos(j - i, k - i, l - i), new BlockPos(j + i, k + i, l + i))) {
            for (int i1 = -b0; i1 <= b0; ++i1) {
                for (int j1 = -b0; j1 <= b0; ++j1) {
                    for (int k1 = -b0; k1 <= b0; ++k1) {
                        BlockPos blockpos1 = pos.add(i1, j1, k1);
                        IBlockState iblockstate1 = worldIn.getBlockState(blockpos1);

                        if (iblockstate1.getBlock().isLeaves(worldIn, blockpos1)) {
                            iblockstate1.getBlock().beginLeavesDecay(worldIn, blockpos1);
                        }
                    }
                }
            }
        }
    }

    /*public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
    	if (true) return;
    	
        if (!worldIn.isRemote && ((Boolean)state.getValue(CHECK_DECAY)).booleanValue()) {
            byte b0 = 4;
            int i = b0 + 1;
            int j = pos.getX();
            int k = pos.getY();
            int l = pos.getZ();
            byte b1 = 32;
            int i1 = b1 * b1;
            int j1 = b1 / 2;

            if (this.surroundings == null) {
                this.surroundings = new int[b1 * b1 * b1];
            }

            int k1;

            if (worldIn.isAreaLoaded(new BlockPos(j - i, k - i, l - i), new BlockPos(j + i, k + i, l + i))) {
                int l1;
                int i2;

                for (k1 = -b0; k1 <= b0; ++k1) {
                    for (l1 = -b0; l1 <= b0; ++l1) {
                        for (i2 = -b0; i2 <= b0; ++i2) {
                            BlockPos tmp = new BlockPos(j + k1, k + l1, l + i2);
                            Block block = worldIn.getBlockState(tmp).getBlock();

                            if (!block.canSustainLeaves(worldIn, tmp)) {
                                if (block.isLeaves(worldIn, tmp)) {
                                    this.surroundings[(k1 + j1) * i1 + (l1 + j1) * b1 + i2 + j1] = -2;
                                }
                                else
                                {
                                    this.surroundings[(k1 + j1) * i1 + (l1 + j1) * b1 + i2 + j1] = -1;
                                }
                            }
                            else
                            {
                                this.surroundings[(k1 + j1) * i1 + (l1 + j1) * b1 + i2 + j1] = 0;
                            }
                        }
                    }
                }

                for (k1 = 1; k1 <= 4; ++k1)
                {
                    for (l1 = -b0; l1 <= b0; ++l1)
                    {
                        for (i2 = -b0; i2 <= b0; ++i2)
                        {
                            for (int j2 = -b0; j2 <= b0; ++j2)
                            {
                                if (this.surroundings[(l1 + j1) * i1 + (i2 + j1) * b1 + j2 + j1] == k1 - 1)
                                {
                                    if (this.surroundings[(l1 + j1 - 1) * i1 + (i2 + j1) * b1 + j2 + j1] == -2)
                                    {
                                        this.surroundings[(l1 + j1 - 1) * i1 + (i2 + j1) * b1 + j2 + j1] = k1;
                                    }

                                    if (this.surroundings[(l1 + j1 + 1) * i1 + (i2 + j1) * b1 + j2 + j1] == -2)
                                    {
                                        this.surroundings[(l1 + j1 + 1) * i1 + (i2 + j1) * b1 + j2 + j1] = k1;
                                    }

                                    if (this.surroundings[(l1 + j1) * i1 + (i2 + j1 - 1) * b1 + j2 + j1] == -2)
                                    {
                                        this.surroundings[(l1 + j1) * i1 + (i2 + j1 - 1) * b1 + j2 + j1] = k1;
                                    }

                                    if (this.surroundings[(l1 + j1) * i1 + (i2 + j1 + 1) * b1 + j2 + j1] == -2)
                                    {
                                        this.surroundings[(l1 + j1) * i1 + (i2 + j1 + 1) * b1 + j2 + j1] = k1;
                                    }

                                    if (this.surroundings[(l1 + j1) * i1 + (i2 + j1) * b1 + (j2 + j1 - 1)] == -2)
                                    {
                                        this.surroundings[(l1 + j1) * i1 + (i2 + j1) * b1 + (j2 + j1 - 1)] = k1;
                                    }

                                    if (this.surroundings[(l1 + j1) * i1 + (i2 + j1) * b1 + j2 + j1 + 1] == -2)
                                    {
                                        this.surroundings[(l1 + j1) * i1 + (i2 + j1) * b1 + j2 + j1 + 1] = k1;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            k1 = this.surroundings[j1 * i1 + j1 * b1 + j1];

            if (k1 >= 0)
            {
                worldIn.setBlockState(pos, state.withProperty(CHECK_DECAY, Boolean.valueOf(false)), 4);
            }
            else
            {
                this.destroy(worldIn, pos);
            }
        }
    }*/

    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World worldIn, BlockPos pos, IBlockState state, Random rand)
    {
        if (worldIn.canLightningStrike(pos.up()) && !World.doesBlockHaveSolidTopSurface(worldIn, pos.down()) && rand.nextInt(15) == 1)
        {
            double d0 = (double)((float)pos.getX() + rand.nextFloat());
            double d1 = (double)pos.getY() - 0.05D;
            double d2 = (double)((float)pos.getZ() + rand.nextFloat());
            worldIn.spawnParticle(EnumParticleTypes.DRIP_WATER, d0, d1, d2, 0.0D, 0.0D, 0.0D, new int[0]);
        }
    }

    private void destroy(World worldIn, BlockPos pos)
    {
        this.dropBlockAsItem(worldIn, pos, worldIn.getBlockState(pos), 0);
        worldIn.setBlockToAir(pos);
    }

    public int quantityDropped(Random random)
    {
        return random.nextInt(20) == 0 ? 1 : 0;
    }

    public Item getItemDropped(IBlockState state, Random rand, int fortune)
    {
        return Item.getItemFromBlock(Blocks.sapling);
    }

    public void dropBlockAsItemWithChance(World worldIn, BlockPos pos, IBlockState state, float chance, int fortune)
    {
        super.dropBlockAsItemWithChance(worldIn, pos, state, chance, fortune);
    }

    protected void dropApple(World worldIn, BlockPos pos, IBlockState state, int chance) {}

    protected int getSaplingDropChance(IBlockState state)
    {
        return 20;
    }

    public boolean isOpaqueCube() {
        return Blocks.leaves.isOpaqueCube();
    } 

   
    @SideOnly(Side.CLIENT)
    public EnumWorldBlockLayer getBlockLayer() {
        return Blocks.leaves.isOpaqueCube() ? EnumWorldBlockLayer.SOLID : EnumWorldBlockLayer.CUTOUT_MIPPED;
    }

    public boolean isVisuallyOpaque() {
        return false;
    }

    public boolean isLeaves(IBlockAccess world, BlockPos pos){ return true; }

    
    /*@Override
    public void beginLeavesDecay(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if (!(Boolean)state.getValue(CHECK_DECAY))
        {
            world.setBlockState(pos, state.withProperty(CHECK_DECAY, true), 4);
        }
    }*/

    @Override
    public java.util.List<ItemStack> getDrops(IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        java.util.List<ItemStack> ret = new java.util.ArrayList<ItemStack>();
        return ret;
    }
    
    
    
    @Override
	public void getSubBlocks(Item itemIn, CreativeTabs tab, List list) {
		for (BlockClassEntry tree : getBlockClass().values()) {
			if (tree.block == this) list.add(new ItemStack(itemIn, 1, tree.getMetaData(this)));
		}
		super.getSubBlocks(itemIn, tab, list);
	}
    
    
    @Override
    protected BlockState createBlockState() {
    	if (getTypeProperty() != null) {
    		return new BlockState(this, new IProperty[] {getTypeProperty()});
    	}
    	return new BlockState(this, new IProperty[0]);
    }

    
    @Override
    public int getMetaFromState(IBlockState state) {
    	return getBlockClass().getMetaFromState(state);
    	
    	/*return
    		(Boolean)state.getValue(CHECK_DECAY) ? 1 : 0
    		| getBlockClass().getMetaFromState(state) << 1
    	;*/
    }
    
    @Override
    public IBlockState getStateFromMeta(int meta) {
    	//return getBlockClass().getBlockClassfromMeta(this, meta >> 1).getBlockState().withProperty(CHECK_DECAY, (meta & 1) > 0 ? true : false);
    	
    	return getBlockClass().getBlockClassfromMeta(this, meta).getBlockState();
    }
    

    @Override
    public AxisAlignedBB getCollisionBoundingBox(World worldIn, BlockPos pos, IBlockState state) {
        return null;
    }


	@Override
	public String getSubType(ItemStack stack) {
		ItemBlock itemblock = (ItemBlock)stack.getItem();
		return getBlockClass().getBlockClassfromMeta((BlockVC) itemblock.block, stack.getItemDamage()).getName();	
	}


	@Override
	public IProperty getTypeProperty() {
		return TREETYPE;
	}


	@Override
	public void setTypeProperty(PropertyBlockClass property) {
		TREETYPE = property;
	}


	@Override
	public BlockClass getBlockClass() {
		return BlocksVC.leaves;
	}

}
