package at.tyron.vintagecraft.block;

import java.util.List;
import java.util.Random;








import at.tyron.vintagecraft.BlockClass.BlockClass;
import at.tyron.vintagecraft.BlockClass.BlockClassEntry;
import at.tyron.vintagecraft.BlockClass.PropertyBlockClass;
//import at.tyron.vintagecraft.TileEntity.TEOre;
import at.tyron.vintagecraft.World.BlocksVC;
import at.tyron.vintagecraft.World.ItemsVC;
import at.tyron.vintagecraft.WorldProperties.EnumMaterialDeposit;
import at.tyron.vintagecraft.WorldProperties.EnumRockType;
import at.tyron.vintagecraft.interfaces.IMultiblock;
import at.tyron.vintagecraft.item.ItemPlanksVC;
import at.tyron.vintagecraft.item.ItemRock;
import at.tyron.vintagecraft.item.ItemStone;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


//- rock is solid rock - is below bedrock until world bottom - 1
//- hard to mine
//- graphics will be like smooth stone 


public class BlockRock extends BlockVC implements IMultiblock {
	public PropertyBlockClass ROCKTYPE;
	
	// Does the block fall like sand?
	public boolean unstable;
	
	
    public BlockRock() {
        this(Material.rock);
	}
    
	public BlockRock(Material materialIn) {
		super(materialIn);
        this.setCreativeTab(CreativeTabs.tabBlock);
	}

	
	public void init(BlockClassEntry []subtypes, PropertyBlockClass property) {
		this.subtypes = subtypes;
		setTypeProperty(property);
		
		blockState = this.createBlockState();
	
		setDefaultState(subtypes[0].getBlockState(blockState.getBaseState(), getTypeProperty()));
	}



    @Override
    public List<ItemStack> getDrops(IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
    	if (getBlockClass() == BlocksVC.rock) {
	        List<ItemStack> ret = new java.util.ArrayList<ItemStack>();
	
	        Random rand = world instanceof World ? ((World)world).rand : RANDOM;
	
	        EnumRockType rocktype = (EnumRockType) getRockType(state).getKey();
	        
	        ItemStack itemstack = new ItemStack(ItemsVC.stone, 2 + rand.nextInt(3));
	        ItemStone.setRockType(itemstack, rocktype);
	        
	        ret.add(itemstack);
	        
	        if (rocktype == EnumRockType.LIMESTONE && rand.nextInt(8) == 0) {
	        	ret.add(new ItemStack(Items.flint, 1));
	        }
	        
			return ret;
    	}

    	
    	List<ItemStack> ret = new java.util.ArrayList<ItemStack>();
    	
    	ItemStack itemstack = new ItemStack(Item.getItemFromBlock(this));
        ItemPlanksVC.withTreeType(itemstack, getRockType(state));
        ret.add(itemstack);
        
    	return ret;
    }
    
    
	public BlockClassEntry getRockType(IBlockState state) {
		return (BlockClassEntry)state.getValue(getTypeProperty());
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
    }
      
    public IBlockState getStateFromMeta(int meta) {
    	return getBlockClass().getBlockClassfromMeta(this, meta).getBlockState();
    }

    


	
	@Override
	public void getSubBlocks(Item itemIn, CreativeTabs tab, List list) {
		for (BlockClassEntry entry : subtypes) {
			list.add(entry.getItemStack());
		}
		super.getSubBlocks(itemIn, tab, list);
	}

	@Override
	public int multistateAvailableTypes() {
		return 16;
	}

	@Override
	public IProperty getTypeProperty() {
		return ROCKTYPE;
	}

	@Override
	public void setTypeProperty(PropertyBlockClass property) {
		ROCKTYPE = property;
	}

	@Override
	public BlockClass getBlockClass() {
		return BlocksVC.rock;
	}
    
    

    
}
