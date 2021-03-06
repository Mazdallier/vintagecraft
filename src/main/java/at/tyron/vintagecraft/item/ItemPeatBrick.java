package at.tyron.vintagecraft.item;

import java.util.List;

import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import at.tyron.vintagecraft.World.BlocksVC;
import at.tyron.vintagecraft.WorldProperties.EnumFurnace;
import at.tyron.vintagecraft.block.BlockSaplingVC;
import at.tyron.vintagecraft.interfaces.IFuel;

public class ItemPeatBrick extends ItemVC implements IFuel {

	public ItemPeatBrick() {
		setCreativeTab(CreativeTabs.tabMisc);
	}
	
	@Override
	public int getBurningHeat(ItemStack stack) {
		return 1000;
	}

	@Override
	public float getBurnDurationMultiplier(ItemStack stack) {
		return 0.6f;
	}
	
	
	@Override
	public void addInformation(ItemStack itemstack, EntityPlayer playerIn, List tooltip, boolean advanced) {
		tooltip.add("Heat produced when burned");
		for (EnumFurnace furnace : EnumFurnace.values()) {
			tooltip.add("  " + furnace.name + ": " + (int)(getBurningHeat(itemstack) * furnace.maxHeatModifier()) + " deg.");	
		}
	}
	
	
	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer playerIn, World worldIn, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ) {
		
        if (!playerIn.canPlayerEdit(pos.offset(side), side, stack)) {
            return false;
        }
        
        if (applyFertilizer(stack, worldIn, pos, playerIn)) {
            if (!worldIn.isRemote) {
                worldIn.playAuxSFX(2005, pos, 0);
            }

            return true;
        }
        
		return false;
	}

	private boolean applyFertilizer(ItemStack stack, World worldIn, BlockPos pos, EntityPlayer playerIn) {
		IBlockState state = worldIn.getBlockState(pos);
		
		if (state.getBlock() instanceof BlockSaplingVC) {
			boolean success = ((BlockSaplingVC)state.getBlock()).fertilize(worldIn, worldIn.rand, pos, state);
			if (success) stack.stackSize--;
			return success;
		}
		
		return false;
	}
}
