package at.tyron.vintagecraft.World;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import com.sun.security.ntlm.Client;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.UseHoeEvent;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.ChunkWatchEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import at.tyron.vintagecraft.VintageCraft;
import at.tyron.vintagecraft.Network.ChunkPutNbt;
import at.tyron.vintagecraft.Network.ChunkRemoveNbt;
import at.tyron.vintagecraft.WorldGen.ChunkProviderGenerateVC;
import at.tyron.vintagecraft.WorldGen.WorldChunkManagerVC;
import at.tyron.vintagecraft.WorldGen.GenLayers.PseudoNumberGen;
import at.tyron.vintagecraft.WorldProperties.EnumFertility;
import at.tyron.vintagecraft.WorldProperties.EnumOrganicLayer;
import at.tyron.vintagecraft.WorldProperties.EnumRockType;
import at.tyron.vintagecraft.block.BlockGravelVC;
import at.tyron.vintagecraft.block.BlockSandVC;
import at.tyron.vintagecraft.block.BlockTopSoil;
import at.tyron.vintagecraft.block.BlockVC;
import at.tyron.vintagecraft.interfaces.ClimateGenWorldChunkManager;

public class VCraftWorld {
	public static boolean chunkdataprofiling = false;
	
	
	public static VCraftWorld instance;
	
	
	public static int seaLevel = 128;
	public int terrainGenHiLevel = 73;
 	
	public static final ResourceLocation grassColormap = new ResourceLocation("vintagecraft:textures/colormap/grass.png");
	
	
	public ArrayList<BlockPos> unpopulatedChunks = new ArrayList<BlockPos>();
	private boolean printingProfiling = false;
	private static int[] grassBuffer = new int[65536];
	private long seed;
	private HashMap<Long, HashMap<String, String>> profiling = new HashMap<Long, HashMap<String,String>>();
	
	ClimateGenWorldChunkManager wcm;
	PseudoNumberGen grassspeckle;  
	
	
	public VCraftWorld(long seed, WorldChunkManager wcm) {
		this.seed = seed;
		this.wcm = (ClimateGenWorldChunkManager)wcm;
		
		grassspeckle = new PseudoNumberGen(1);
		grassspeckle.initWorldGenSeed(seed);
	}
	
	
	void mark(int chunkX, int chunkZ, String key) {
		long index = ChunkPos2Index(chunkX, chunkZ);
		
		if (!chunkdataprofiling) return;
		
		
		if (printingProfiling) return;
		
		if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT ) {
			//System.out.println("called by client");
			return;
		}
		
		HashMap<String, String> chunk = profiling.get(index);
		if (chunk == null) {
			chunk = new HashMap<String, String>();
			chunk.put("chunkX", "" + chunkX);
			chunk.put("chunkZ", "" + chunkZ);
			chunk.put("list", "");
			chunk.put("counter", "0");
		}
		
		String str = chunk.get(key);
		int num;
		if (str == null) {
			num = 1;
		} else {
			num = Integer.parseInt(str);
			num++;
		}
		chunk.put(key, ""+num);
		
		int counter = Integer.parseInt(chunk.get("counter")) + 1;
		chunk.put("counter", "" + counter);
		chunk.put("list", chunk.get("list") + "\r\n" + counter + " "  + key);
		
		profiling.put(index, chunk);
	}
	
	
	void printProfiling(String reason) {
		System.out.println("writing chunknbt.txt");
		printingProfiling = true;
		
		Writer writer;
		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("chunknbt.txt"), "utf-8"));

			writer.write("Crash at " + FMLCommonHandler.instance().getEffectiveSide());
			writer.write(reason + "\r\n");
			
			Set<Long> keys = profiling.keySet();
			
			for (Long index : keys) {
				HashMap<String, String> chunk = profiling.get(index);
				
				writer.write("=======================\r\n");
				writer.write("chunk @ " + chunk.get("chunkX") + "/" + chunk.get("chunkZ") + "\r\n");
				writer.write("index = " + index + "\r\n");
				
				Set<String> chunkkeys = chunk.keySet();
				for (String key : chunkkeys) {
					if (!key.equals("chunkX") && !key.equals("chunkZ") && !key.equals("list") && !key.equals("counter")) {
						writer.write(key + ": " + chunk.get(key) + "\r\n");
					}
				}
				
				writer.write("order:\r\n");
				writer.write(chunk.get("list") + "\r\n");
			}
			

			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		printingProfiling = false;
		System.out.println("chunknbt.txt written.");
		
	}
	

	


	NBTTagCompound getChunkNBT(BlockPos blockpos) {
		return VintageCraft.proxy.getChunkNbt(BlockPos2Index(blockpos));
	}
	
	
	public void setChunkNBT(int chunkX, int chunkZ, String key, int[] data) {
		long index = ChunkPos2Index(chunkX, chunkZ);
		
		int x = 2;
		
		NBTTagCompound nbt = VintageCraft.proxy.getChunkNbt(index);
		
		if (nbt == null) {
			nbt = new NBTTagCompound();
			x = 0;
		}
		
		nbt.setIntArray(key, data);
		VintageCraft.proxy.putChunkNbt(index, nbt);
		
		mark(chunkX, chunkZ, "setchunknbt-" + key + " x-" + x);
	}
	
	
	  
	

	public void setChunkNBT(int chunkX, int chunkZ, String key, boolean value) {
		long index = ChunkPos2Index(chunkX, chunkZ);
		
		NBTTagCompound nbt = VintageCraft.proxy.getChunkNbt(index);
		
		if (nbt == null) {
			nbt = new NBTTagCompound();
		}
		
		nbt.setBoolean(key, value);
		
		VintageCraft.proxy.putChunkNbt(index, nbt);
		
		mark(chunkX, chunkZ, "setchunknbt-" + key + " (" + value +")");
	}
	
	
	

	
	@SubscribeEvent
	public void loadChunk(ChunkDataEvent.Load event) {
		NBTTagCompound nbt = event.getData().getCompoundTag("vintagecraft");
		
		VintageCraft.proxy.putChunkNbt(Chunk2Index(event.getChunk()), nbt);
		
		if (nbt.hasKey("vcraftpopulated") && !nbt.getBoolean("vcraftpopulated")) {
			unpopulatedChunks.add(new BlockPos(event.getChunk().xPosition, 0, event.getChunk().zPosition));
		}
		
		mark(event.getChunk().xPosition, event.getChunk().zPosition, "load " + nbt.hasKey("climate"));		
	}
	

	@SubscribeEvent
	public void saveChunk(ChunkDataEvent.Save event) {	
		long index = Chunk2Index(event.getChunk());
		NBTTagCompound nbt = VintageCraft.proxy.getChunkNbt(index); // chunkextranbt_savequeue.get(index);
		
		
		if (nbt != null) {
			event.getData().setTag("vintagecraft", nbt);
			mark(event.getChunk().xPosition, event.getChunk().zPosition, "save " + nbt.hasKey("climate"));
		} else {
			mark(event.getChunk().xPosition, event.getChunk().zPosition, "save-no nbt?");
		}
		
		
		
		if (!event.getChunk().isLoaded()) {
			mark(event.getChunk().xPosition, event.getChunk().zPosition, "removed from list");
			// TODO
			//VintageCraft.proxy.removeChunkNbt(Chunk2Index(event.getChunk()));
		}
	}
	
	
	
    @SubscribeEvent
    public void onChunkWatch(ChunkWatchEvent.Watch event) {
    	long index = ChunkPos2Index(event.chunk.chunkXPos, event.chunk.chunkZPos);
    	VintageCraft.packetPipeline.sendTo(new ChunkPutNbt(index, VintageCraft.proxy.getChunkNbt(index)), event.player);
    }
    
    @SubscribeEvent
    public void onChunkUnWatch(ChunkWatchEvent.UnWatch event) {
    	long index = ChunkPos2Index(event.chunk.chunkXPos, event.chunk.chunkZPos);
    	VintageCraft.packetPipeline.sendTo(new ChunkRemoveNbt(index), event.player);
    }
    
    
    
    
    
    public NBTTagCompound recreateClimateNBT(BlockPos pos) {
    	int[] climate = wcm.getClimateInts(pos.getX() >> 4, pos.getZ() >> 4, 16, 16);
    	
    	setChunkNBT(pos.getX() >> 4, pos.getZ() >> 4, "climate", climate);
    	return getChunkNBT(pos);
    }
    


    private int _getClimate(BlockPos pos) {
    	NBTTagCompound nbt = getChunkNBT(pos);
    	mark(pos.getX() >> 4, pos.getZ() >> 4, "getnbt-_climate " + (nbt == null));
    	
    	if (nbt == null || !nbt.hasKey("climate")) {
    		nbt = recreateClimateNBT(pos);
    		/*printProfiling*/System.out.println("_climate array for chunk " + (pos.getX()>>4) + "/" + + (pos.getZ()>>4) + " at coord " + pos + " missing - recreated!" + " (@index " + BlockPos2Index(pos) + ")");
    	}
    	
    	int climate = nbt.getIntArray("climate")[((pos.getZ() & 15) << 4) + (pos.getX() & 15)];
    	
    	//int sealevelheight = pos.getY() - seaLevel;
    	
    	return climate;
    }

    

    // Returns climate = int[temp, fertility, rain] 
    public int[] getClimate(BlockPos pos) {
    	NBTTagCompound nbt = getChunkNBT(pos);
    	mark(pos.getX() >> 4, pos.getZ() >> 4, "getnbt-climate");
    	
    	if (!nbt.hasKey("climate")) {
    		nbt = recreateClimateNBT(pos);
    		/*printProfiling*/System.out.println("climate array for chunk " + (pos.getX()>>4) + "/" + + (pos.getZ()>>4) + " missing - recreated!");	
    	}
    	
    	int climate = nbt.getIntArray("climate")[((pos.getZ() & 15) << 4) + (pos.getX() & 15)];
    	
    	int rain = normalizeRainFall(climate & 0xff, pos);
    	int temp = normalizeTemperature((climate >> 16) & 0xff, pos);
    	
    	return new int[]{temp, normalizeFertility((climate >> 8) & 0xff, rain, deScaleTemperature(temp), pos), rain};
    }
    
    public int getTemperature(BlockPos pos) {
    	return normalizeTemperature((_getClimate(pos) >> 16) & 0xff, pos);
    }
    
    public int getRainfall(BlockPos pos) {
    	return normalizeRainFall((_getClimate(pos) >> 0) & 0xff, pos);
    }
    
    public int getFertily(int rain, int temperature, BlockPos pos) {
    	return normalizeFertility((_getClimate(pos) >> 8) & 0xff, rain, temperature, pos);
    }
    
    
    

	
    public int normalizeRainFall(int rainfall, BlockPos pos) {
    	return (int) Math.min(255, rainfall + (pos.getY() - seaLevel)/2 + 9 * Math.min(8, Math.max(0, 137 - pos.getY())));
    }

    
    // Temperature range between -30 and +30 degree
    public int normalizeTemperature(int temperature, BlockPos pos) {
    	return Math.min(30, Math.max(-30, (int) ((temperature - (pos.getY() - seaLevel)/2) / 4.25f) - 30));
    }
    
    public int deScaleTemperature(int temperature) {
    	return (int) ((temperature + 30) * 4.25f);
    }
    
    
    
    public int normalizeFertility(int fertility, int rain, int temp, BlockPos pos) {
    	return Math.max(0, Math.min(255, rain + Math.max(0, (rain-128)*(temp-128)/256) - (pos.getY() - seaLevel)));
    }
    
    
    public int getForest(BlockPos pos) {
    	int forest = getChunkNBT(pos).getIntArray("forest")[((pos.getZ() & 15) << 4) + (pos.getX() & 15)];
    	mark(pos.getX() >> 4, pos.getZ() >> 4, "getnbt-forest");
    	
    	return 255 - (forest & 0xff);
    }
    

    
    
    
    
    
    
    public IBlockState getTopLayerAtPos(int x, int y, int z, EnumRockType rocktype, int steepness) {
    	BlockPos pos = new BlockPos(x, y, z);
    	int []climate = getClimate(pos);
    	
		/*int temperature = getTemperature(pos);
		int rainfall = getRainfall(pos);
		int fertilityvalue = getFertily(pos);*/
		
		EnumFertility fertility = EnumFertility.fromFertilityValue(climate[1]);

		if (climate[0] <= -18) {
			return Blocks.snow.getDefaultState();
		}
		
		if (fertility != null) {
			if (steepness > 3 && climate[2] < 180) return null;
			EnumOrganicLayer layer = EnumOrganicLayer.fromClimate(climate[2], climate[0]);
			return BlocksVC.topsoil.getDefaultState().withProperty(BlockTopSoil.organicLayer, layer).withProperty(BlockTopSoil.fertility, fertility);
		} else {
			if (steepness > 2) return null;
			
			if (climate[0] < 10) {
				return BlocksVC.gravel.getFromKey(rocktype).getBlockState();
			} else {
				return BlocksVC.sand.getFromKey(rocktype).getBlockState();
			}
		}
	}

	public IBlockState getSubLayerAtPos(int x, int y, int z, EnumRockType rocktype, int steepness) {
		int []climate = getClimate(new BlockPos(x, y, z));
		
		//int fertilityvalue = getFertily(new BlockPos(x, y, z));
		EnumFertility fertility = EnumFertility.fromFertilityValue(climate[1]);
		//int temperature = getTemperature(new BlockPos(x, y, z));
		
		if (climate[0] <= -23) {
			return Blocks.ice.getDefaultState();
		}

		if (fertility != null) {
			if (steepness > 2 || y > 200) {
				return BlocksVC.regolith.getFromKey(rocktype).getBlockState();
			}
			return BlocksVC.subsoil.getFromKey(rocktype).getBlockState();
		} else {
			if (climate[0] < 10) {
				return BlocksVC.gravel.getFromKey(rocktype).getBlockState();
			} else {
				return BlocksVC.sand.getFromKey(rocktype).getBlockState();
			}
		}
	}

	
	public IBlockState getReoglithLayerAtPos(int x, int y, int z, EnumRockType rocktype, int steepness) {
		int temperature = getTemperature(new BlockPos(x, y, z));
		
		if (temperature <= -28) return Blocks.packed_ice.getDefaultState();
		
		// Above y 200 the sublayer turns into regolith, so no need to generate it here again
		if (steepness < 1 && y <= 200) return BlocksVC.regolith.getFromKey(rocktype).getBlockState();
		return null;
	}


    
    
    
	
	@SideOnly(Side.CLIENT)
	public static void loadGrassColors(IResourceManager resourceManager) {
		try {
			grassBuffer = TextureUtil.readImageData(resourceManager, grassColormap);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
    @SideOnly(Side.CLIENT)
    public int getGrassColorAtPos(BlockPos pos) {
    	return getGrassColorAtPos(pos, 0);
    }
    
    @SideOnly(Side.CLIENT)
    public int getGrassColorAtPos(BlockPos pos, int rainfallmodifier) {
    	int climate = _getClimate(pos);
    	
    	grassspeckle.initPosSeed(pos.getX(), pos.getZ());
    	
    	int temperature = (climate >> 16) & 0xff  - (pos.getY() - seaLevel)/2;
    	int rainfall = normalizeRainFall(climate & 0xff, pos);
    	//System.out.println(temperature + "/" + (255-rainfall));
    	return grassBuffer[Math.min(255, Math.max(0, temperature + grassspeckle.nextInt(15))) + 256 * Math.min(255, (255-rainfall+rainfallmodifier))];
    }
    
    
    
    
    
    
    
    
    
    
      
    long Chunk2Index(Chunk chunk) {
    	return ChunkPos2Index(chunk.xPosition, chunk.zPosition);
    }
	long BlockPos2Index(BlockPos pos) {
		return ChunkPos2Index(pos.getX() >> 4, pos.getZ() >> 4);
	}
	long ChunkPos2Index(int chunkX, int chunkZ) {
		return ChunkCoordIntPair.chunkXZ2Int(chunkX, chunkZ);
		//return ((long)chunkX + (long)Integer.MAX_VALUE) + (((long)chunkZ + (long)Integer.MAX_VALUE) << 32); 
	}
    
	
	
	
	
	@SubscribeEvent
	public void onEvent(UseHoeEvent event) {
		Block block = event.world.getBlockState(event.pos).getBlock();
		if (block instanceof BlockVC) {
			((BlockVC)block).hoeUsed(event);
		}
	}



}
