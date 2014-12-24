package at.tyron.vintagecraft.WorldGen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Collections;

import at.tyron.vintagecraft.VintageCraftConfig;
import at.tyron.vintagecraft.World.EnumCrustLayer;
import at.tyron.vintagecraft.World.EnumMaterialDeposit;
import at.tyron.vintagecraft.World.EnumRockType;
import at.tyron.vintagecraft.WorldGen.GenLayers.GenLayerVC;
import at.tyron.vintagecraft.block.BlockRock;
import at.tyron.vintagecraft.block.VCBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderGenerate;
import net.minecraft.world.gen.MapGenCaves;
import net.minecraft.world.gen.NoiseGeneratorOctaves;

public class VCChunkProviderGenerate extends ChunkProviderGenerate {

	//public static EnumRockType rocktypes[][] = new EnumRockType[EnumCrustLayer.values().length][];
	private GenLayerVC[] rockLayers = new GenLayerVC[EnumCrustLayer.values().length - 3];
	int[][] rockData;
	private MapGenCavesVC caveGenerator;
	
	public static GenLayerVC depositLayer;
	
	
	public VCChunkProviderGenerate(World worldIn, long seed, boolean mapfeaturesenabled, String customgenjson) {
		super(worldIn, seed, mapfeaturesenabled, customgenjson);
		
		caveGenerator = new MapGenCavesVC();
		
		this.worldObj = worldIn;
		this.rand = new Random(seed);
		this.noiseGen1 = new NoiseGeneratorOctaves(this.rand, 4);
		this.noiseGen2 = new NoiseGeneratorOctaves(this.rand, 16);
		this.noiseGen3 = new NoiseGeneratorOctaves(this.rand, 8);
		this.noiseGen4 = new NoiseGeneratorOctaves(this.rand, 4);
		this.noiseGen5 = new NoiseGeneratorOctaves(this.rand, 2);
		this.noiseGen6 = new NoiseGeneratorOctaves(this.rand, 1);
		this.mobSpawnerNoise = new NoiseGeneratorOctaves(this.rand, 8);
		this.seed = seed;
		
		/*EnumCrustLayer[] crustlayers = EnumCrustLayer.values();
		for (int i = 0; i < crustlayers.length; i++) {
			rocktypes[i] = EnumRockType.getRockTypesForCrustLayer(crustlayers[i]); 
		}*/
		
		
		// The RockLayer generators do not generate rocks evenly, so shuffle them per world so each rock once gets more rare or more common
		for (int i = 0; i < rockLayers.length; i++) {
			List rocktypes = Arrays.asList(EnumRockType.getRockTypesForCrustLayer(EnumCrustLayer.fromDataLayerIndex(i)));
			Collections.shuffle(rocktypes, this.rand);
			rockLayers[i] = GenLayerVC.genRockLayer(seed+i, (EnumRockType[])rocktypes.toArray());
		}
		
		rockData = new int[rockLayers.length][];
		
		
		depositLayer = GenLayerVC.genDeposits(seed+2);
	}
	
	
	long seed;
	
	/** RNG. */
	private Random rand;

	/** A NoiseGeneratorOctaves used in generating terrain */
	private NoiseGeneratorOctaves noiseGen1;

	/** A NoiseGeneratorOctaves used in generating terrain */
	private NoiseGeneratorOctaves noiseGen2;

	/** A NoiseGeneratorOctaves used in generating terrain */
	private NoiseGeneratorOctaves noiseGen3;

	/** A NoiseGeneratorOctaves used in generating terrain */
	private NoiseGeneratorOctaves noiseGen4;

	/** A NoiseGeneratorOctaves used in generating terrain */
	public NoiseGeneratorOctaves noiseGen5;

	/** A NoiseGeneratorOctaves used in generating terrain */
	public NoiseGeneratorOctaves noiseGen6;
	public NoiseGeneratorOctaves mobSpawnerNoise;

	/** Reference to the World object. */
	private World worldObj;

	/** Holds the overall noise array used in chunk generation */
	private double[] noiseArray;
	private double[] stoneNoise = new double[256];

	/** The biomes that are used to generate the chunk */
	private BiomeGenBase[] biomesForGeneration;

	int[] sealevelOffsetMap = new int[256];

	
	

	//GenLayerVC rockDeformationLayer;



	/** A double array that hold terrain noise from noiseGen3 */
	double[] noise3;

	/** A double array that hold terrain noise */
	double[] noise1;

	/** A double array that hold terrain noise from noiseGen2 */
	double[] noise2;

	/** A double array that hold terrain noise from noiseGen5 */
	double[] noise5;

	/** A double array that holds terrain noise from noiseGen6 */
	double[] noise6;

	/**
	 * Used to store the 5x5 parabolic field that is used during terrain generation.
	 */
	float[] parabolicField;

	int[] seaLevelOffsetMap = new int[256];
	int[] chunkGroundLevelMap = new int[256];
	
	
	ChunkPrimer primer;
	
	@Override
	public Chunk provideChunk(int chunkX, int chunkZ) {
		primer = new ChunkPrimer();
		
		//this.rand.setSeed(chunkX * 341873128712L + chunkZ * 132897987541L);
		
		biomesForGeneration = this.worldObj.getWorldChunkManager().loadBlockGeneratorData(biomesForGeneration, chunkX * 16, chunkZ * 16, 16, 16);
		
		//if (chunkX % 5 != 0) { // && (chunkX+1) % 4 != 0) {
		
			generateTerrainHigh(chunkX, chunkZ, primer);
			generateTerrainLow(chunkX, chunkZ, primer);
		
			decorate(chunkX, chunkZ, rand, primer);
			caveGenerator.func_175792_a(this, this.worldObj, chunkX, chunkZ, primer);
			
		//}
		
		
		
		Chunk chunk = new Chunk(this.worldObj, primer, chunkX, chunkZ);
		chunk.generateSkylightMap();
		return chunk;
	}
	
	
	// Get spawnable creatures list
	@Override
	public List func_177458_a(EnumCreatureType p_177458_1_, BlockPos p_177458_2_) {
		BiomeGenBase biomegenbase = this.worldObj.getBiomeGenForCoords(p_177458_2_);
		
		return biomegenbase.getSpawnableList(p_177458_1_);
	}
	
	@Override
	public void recreateStructures(Chunk p_180514_1_, int p_180514_2_, int p_180514_3_) {
				
	}
	
	@Override
	public void populate(IChunkProvider par1IChunkProvider, int chunkX, int chunkZ) {
		BlockPos pos = new BlockPos(chunkX, 0, chunkZ);
		VCBiome biome = (VCBiome) this.worldObj.getBiomeGenForCoords(pos);
		//biome.decorate(this.worldObj, this.rand, pos);
	}
	
	
	@Override
	public boolean func_177460_a(IChunkProvider p_177460_1_, Chunk p_177460_2_, int p_177460_3_, int p_177460_4_) {
		return false;
	}
	
	private BiomeGenBase getBiome(int x, int z)
	{
		//return this.biomesForGeneration[(z+1) + (x+1) * 18];
		return this.biomesForGeneration[z + x * 16];
	}
	
	
	
	
	
	
	
	
	
	
	

	
	
	//int[] rockDeformationData;
	void decorate(int chunkX, int chunkZ, Random rand, ChunkPrimer primer) {
		Arrays.fill(chunkGroundLevelMap, 0);
		
		for (int i = 0; i < rockLayers.length; i++) {
			rockData[i] = rockLayers[i].getInts(chunkZ*16, chunkX*16, 16, 16);
		}
		
		//rockDeformationData = rockDeformationLayer.getInts(chunkZ*16, chunkX*16, 16, 16);
		
		for (int x = 0; x < 16; ++x) {
			for (int z = 0; z < 16; ++z) {
				int arrayIndexChunk = z + x * 16;
				VCBiome biome = (VCBiome)getBiome(x, z);
				int airblocks = 0;
				
				for (int y = 255; y > 0; --y) {
					if (y <= 1) {
						primer.setBlockState(x, y, z, VCBlocks.uppermantle.getDefaultState());
						break;
					}
					if (primer.getBlockState(x, y, z).getBlock() == Blocks.stone) {
						if (chunkGroundLevelMap[arrayIndexChunk] == 0) {
							chunkGroundLevelMap[arrayIndexChunk] = y;
						}
						
						buildCrustLayers(x, y, z, chunkGroundLevelMap[arrayIndexChunk] - y, primer, biome);						
					}
					
					if (chunkGroundLevelMap[arrayIndexChunk] != 0 && primer.getBlockState(x, y, z).getBlock() == Blocks.air) {
						airblocks++;
					}
					
					// Try to exclude floating islands in the ground level map
					if (airblocks > 8) {
						chunkGroundLevelMap[arrayIndexChunk] = 0;
						airblocks = 0;
					}
				}
			}
		}
		
		rockData = new int[rockLayers.length][];
	}
	
	
	
	public void buildCrustLayers(int x, int y, int z, int depth, ChunkPrimer primer, VCBiome biome) {
		int arrayIndexChunk = z + x * 16;
		
		EnumCrustLayer layer = EnumCrustLayer.crustLayerForDepth(depth, primer.getBlockState(x, chunkGroundLevelMap[arrayIndexChunk]+1, z).getBlock() == Blocks.water);
		
		
		if (layer == null) return;
		
		IBlockState blockstate = layer.getFixedBlock(EnumRockType.byId(rockData[0][arrayIndexChunk]), depth);
		
		//int deformationOffset = 0;
		
		if (blockstate == null) {
			try {
				blockstate = EnumRockType.byId(rockData[layer.dataLayerIndex][arrayIndexChunk]).getRockVariantForBlock(VCBlocks.rock);
				
				
			} catch (NullPointerException npe) {
				System.out.println(arrayIndexChunk);
				System.out.println(layer.dataLayerIndex);
				System.out.println(rockData[layer.dataLayerIndex][arrayIndexChunk]);
				throw npe;
			}
		}
		
		// unfinished todo: how to redraw the now shifted stone?
		/*deformationOffset = rockDeformationData[arrayIndexChunk];
		deformationOffset = Math.max(0, deformationOffset / 18 - (10 - depth));
		
		
		//  dataLayerIndex == -1   => not a rock layer
		if (layer.dataLayerIndex == -1) {
			if (deformationOffset > 12) return; 
		} else {
			y += deformationOffset;
		}*/

		
		primer.setBlockState(x, y, z, blockstate);

		if(rand.nextBoolean() && y > 0 && depth > 0) {
			primer.setBlockState(x, y - 1, z, blockstate);
			
			if(rand.nextBoolean() && y > 1 && depth > 5) {
				primer.setBlockState(x, y - 2, z, blockstate);
			}
			
			if(rand.nextBoolean() && y > 2 && depth > 15) {
				primer.setBlockState(x, y - 3, z, blockstate);
			}
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	public void generateTerrainLow(int chunkX, int chunkZ, ChunkPrimer primer) {
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = VintageCraftConfig.terrainGenLevel; y > 0; y--) {
					if (primer.getBlockState(x, y, z).getBlock() == Blocks.air) {
						primer.setBlockState(x, y, z, Blocks.stone.getDefaultState());
					}
				}
			}
		}
	}

	
	public void generateTerrainHigh(int chunkX, int chunkZ, ChunkPrimer primer) {
		byte horizontalPart = 4;
		byte verticalPart = 16;
		int seaLevel = 14;
		
		int xSize = horizontalPart + 1;
		byte ySize = 17;
		int zSize = horizontalPart + 1;
		
		//short arrayYHeight = 128;
		
		this.biomesForGeneration = this.worldObj.getWorldChunkManager().getBiomesForGeneration(this.biomesForGeneration, chunkX * 4 - 2, chunkZ * 4 - 2, xSize + 5, zSize + 5);
		
		this.noiseArray = this.initializeNoiseFieldHigh(this.noiseArray, chunkX * horizontalPart, 0, chunkZ * horizontalPart, xSize, ySize, zSize);

		for (int x = 0; x < horizontalPart; ++x) {
			for (int z = 0; z < horizontalPart; ++z) {
				for (int y = 0; y < verticalPart; ++y) {
					double yLerp = 0.125D;
					double bottom1 = this.noiseArray[((x + 0) * zSize + z + 0) * ySize + y + 0];
					double bottom2 = this.noiseArray[((x + 0) * zSize + z + 1) * ySize + y + 0];
					double bottom3 = this.noiseArray[((x + 1) * zSize + z + 0) * ySize + y + 0];
					double bottom4 = this.noiseArray[((x + 1) * zSize + z + 1) * ySize + y + 0];
					double top1 = (this.noiseArray[((x + 0) * zSize + z + 0) * ySize + y + 1] - bottom1) * yLerp;
					double top2 = (this.noiseArray[((x + 0) * zSize + z + 1) * ySize + y + 1] - bottom2) * yLerp;
					double top3 = (this.noiseArray[((x + 1) * zSize + z + 0) * ySize + y + 1] - bottom3) * yLerp;
					double top4 = (this.noiseArray[((x + 1) * zSize + z + 1) * ySize + y + 1] - bottom4) * yLerp;

					for (int dy = 0; dy < 8; ++dy) {
						double xLerp = 0.25D;
						double bottom1Counting = bottom1;
						double bottom2Counting = bottom2;
						double var38 = (bottom3 - bottom1) * xLerp;
						double var40 = (bottom4 - bottom2) * xLerp;

						for (int dx = 0; dx < 4; ++dx) {	
						//	int index = dx + x * 4 << 12 | 0 + z * 4 << 8 | y * 8 + dy;

						//	index -= arrayYHeight;
							double zLerp = 0.25D;
							double var49 = (bottom2Counting - bottom1Counting) * zLerp;
							double var47 = bottom1Counting - var49;

							for (int dz = 0; dz < 4; ++dz) {
								if ((var47 += var49) > 0.0D) {
									primer.setBlockState(4*x + dx, 8*y + dy + VintageCraftConfig.terrainGenLevel, 4*z + dz, Blocks.stone.getDefaultState());
								} else if (y * 8 + dy < seaLevel) {
									primer.setBlockState(4*x + dx, 8*y + dy + VintageCraftConfig.terrainGenLevel, 4*z + dz, Blocks.water.getDefaultState());
								} else {
									primer.setBlockState(4*x + dx, 8*y + dy + VintageCraftConfig.terrainGenLevel, 4*z + dz, Blocks.air.getDefaultState());
								}
								
							}
							bottom1Counting += var38;
							bottom2Counting += var40;
						}
						bottom1 += top1;
						bottom2 += top2;
						bottom3 += top3;
						bottom4 += top4;
					}
				}
			}
		}
	}













	
	
	
	/***
	 * generates a subset of the level's terrain data. Takes 7 arguments: the [empty] noise array, the position, and the
	 * size.
	 */
	private double[] initializeNoiseFieldHigh(double[] outArray, int xPos, int yPos, int zPos, int xSize, int ySize, int zSize) {
		int smoothingRadius = 3;
		
		if (outArray == null) {
			outArray = new double[xSize * ySize * zSize];
		}

		if (this.parabolicField == null) {
			this.parabolicField = new float[2*smoothingRadius + 5 * 2 * smoothingRadius + 1];
			for (int xR = -smoothingRadius; xR <= smoothingRadius; ++xR) {
				for (int zR = -smoothingRadius; zR <= smoothingRadius; ++zR) {
					float var10 = 10.0F / MathHelper.sqrt_float(xR * xR + zR * zR + 0.2F);
					this.parabolicField[xR + smoothingRadius + (zR + smoothingRadius) * 5] = var10;
				}
			}
		}

		//double var44 = 684.412D;
		//double var45 = 684.412D;
		double var44 = 1000D;
		double var45 = 1000D;
		this.noise5 = this.noiseGen5.generateNoiseOctaves(this.noise5, xPos, zPos, xSize, zSize, 1.121D, 1.121D, 0.5D);
		this.noise6 = this.noiseGen6.generateNoiseOctaves(this.noise6, xPos, zPos, xSize, zSize, 200.0D, 200.0D, 0.5D);
		this.noise3 = this.noiseGen3.generateNoiseOctaves(this.noise3, xPos, yPos, zPos, xSize, ySize, zSize, var44 / 80.0D, var45 / 160.0D, var44 / 80.0D);
		//this.noise3 = this.noiseGen3.generateNoiseOctaves(this.noise3, xPos, yPos, zPos, xSize, ySize, zSize, var44 / 80.0D, 0.5, var44 / 80.0D);
		this.noise1 = this.noiseGen1.generateNoiseOctaves(this.noise1, xPos, yPos, zPos, xSize, ySize, zSize, var44, var45, var44);
		this.noise2 = this.noiseGen2.generateNoiseOctaves(this.noise2, xPos, yPos, zPos, xSize, ySize, zSize, var44, var45, var44);
		boolean var43 = false;
		boolean var42 = false;
		int posIndex = 0;
		int var13 = 0;

		for (int x = 0; x < xSize; ++x) {
			for (int z = 0; z < zSize; ++z) {
				float maxBlendedHeight = 0.0F;
				float minBlendedHeight = 0.0F;
				float blendedHeightSum = 0.0F;
				
				VCBiome baseBiome = (VCBiome)this.biomesForGeneration[x + smoothingRadius + (z + smoothingRadius) * (xSize + 5)];

				for (int xR = -smoothingRadius; xR <= smoothingRadius; ++xR) {
					for (int zR = -smoothingRadius; zR <= smoothingRadius; ++zR) {
						VCBiome blendBiome = (VCBiome)this.biomesForGeneration[x + xR + smoothingRadius + (z + zR + smoothingRadius) * (xSize + 5)];
						float blendedHeight = this.parabolicField[xR + smoothingRadius + (zR + smoothingRadius) * 5] / 2.0F;
						//System.out.println(blendedHeight + " / " + blendBiome.minHeight + " > " + baseBiome.minHeight + " max:" + blendBiome.maxHeight);
						if (blendBiome.minHeight > baseBiome.minHeight) {
							blendedHeight *= 0.5F;
						}

						maxBlendedHeight += blendBiome.maxHeight * blendedHeight;
						minBlendedHeight += blendBiome.minHeight * blendedHeight;
						blendedHeightSum += blendedHeight;
					}
				}

				maxBlendedHeight /= blendedHeightSum;
				minBlendedHeight /= blendedHeightSum;
				maxBlendedHeight = maxBlendedHeight * 0.9F + 0.1F;
				minBlendedHeight = (minBlendedHeight * 4.0F - 1.0F) / 8.0F;
				double var47 = this.noise6[var13] / 8000.0D;

				if (var47 < 0.0D)
					var47 = -var47 * 0.3D;
				var47 = var47 * 3.0D - 2.0D;

				if (var47 < 0.0D)
				{
					var47 /= 2.0D;
					if (var47 < -1.0D)
						var47 = -1.0D;
					var47 /= 1.4D;
					var47 /= 2.0D;
				}
				else
				{
					if (var47 > 1.0D)
						var47 = 1.0D;
					var47 /= 8.0D;
				}

				++var13;
				for (int y = 0; y < ySize; ++y)
				{
					double var48 = minBlendedHeight;
					double var26 = maxBlendedHeight;
					var48 += var47 * 0.2D;
					var48 = var48 * ySize / 16.0D;
					double var28 = ySize / 2.0D + var48 * 4.0D;
					double var30 = 0.0D;
					double var32 = (y - var28) * 12.0D * 256.0D / 256.0D / (2.70 + var26);

					if (var32 < 0.0D)
						var32 *= 4.0D;

					double var34 = this.noise1[posIndex] / 512.0D;
					double var36 = this.noise2[posIndex] / 512.0D;
					double var38 = (this.noise3[posIndex] / 10.0D + 1.0D) / 2.0D;

					if (var38 < 0.0D)
						var30 = var34;
					else if (var38 > 1.0D)
						var30 = var36;
					else
						var30 = var34 + (var36 - var34) * var38;

					var30 -= var32;
					if (y > ySize - 4)
					{
						double var40 = (y - (ySize - 4)) / 3.0F;
						var30 = var30 * (1.0D - var40) + -10.0D * var40;
					}

					outArray[posIndex] = var30;
					++posIndex;
					
					
				}
			}
		}
		return outArray;
	}
	
	
	
	
	
	
	
	

	
	//private double[] layer2Noise = new double[256];
	
	

/*	private void replaceBlocksForBiomeHigh(int chunkX, int chunkZ, Random rand, ChunkPrimer primer) {
		int seaLevel = 16;
		int worldHeight = 256;
		int indexOffset = 128;
		double var6 = 0.03125D;
		stoneNoise = noiseGen4.generateNoiseOctaves(stoneNoise, chunkX * 16, chunkZ * 16, 0, 16, 16, 1, var6 * 4.0D, var6 * 1.0D, var6 * 4.0D);
		boolean[] cliffMap = new boolean[256];
		
		for (int xCoord = 0; xCoord < 16; ++xCoord) {
			for (int zCoord = 0; zCoord < 16; ++zCoord) {
				int arrayIndex = xCoord + zCoord * 16;
				int arrayIndexDL = zCoord + xCoord * 16;
				int arrayIndex2 = xCoord+1 + zCoord+1 * 16;
				VCBiome biome = (VCBiome)getBiome(xCoord,zCoord);
				
				DataLayer rock1 = rockLayer1[arrayIndexDL] == null ? DataLayer.Granite : rockLayer1[arrayIndexDL];
				DataLayer rock2 = rockLayer2[arrayIndexDL] == null ? DataLayer.Granite : rockLayer2[arrayIndexDL];
				DataLayer rock3 = rockLayer3[arrayIndexDL] == null ? DataLayer.Granite : rockLayer3[arrayIndexDL];
				DataLayer evt = evtLayer[arrayIndexDL] == null ? DataLayer.EVT_0_125 : evtLayer[arrayIndexDL];
				
				float rain = rainfallLayer[arrayIndexDL] == null ? DataLayer.Rain_125.floatdata1 : rainfallLayer[arrayIndexDL].floatdata1;
				DataLayer drainage = drainageLayer[arrayIndexDL] == null ? DataLayer.DrainageNormal : drainageLayer[arrayIndexDL];
				int var12 = (int)(stoneNoise[arrayIndex2] / 3.0D + 6.0D);
				int var13 = -1;

				IBlockState surfaceBlock = Blocks.grass.getDefaultState(); // VC_Core.getTypeForGrassWithRain(rock1.data1, rain);
				IBlockState subSurfaceBlock = Blocks.dirt.getDefaultState(); // VC_Core.getTypeForDirtFromGrass(surfaceBlock);

				float _temp = 0.5f; //VC_Climate.getBioTemperature(worldObj, chunkX * 16 + xCoord, chunkZ * 16 + zCoord);
				int h = 0;
				

				if(VCBiome.isShore(getBiome(xCoord-1, zCoord).biomeID) || VCBiome.isShore(getBiome(xCoord+1, zCoord).biomeID) || VCBiome.isShore(getBiome(xCoord, zCoord+1).biomeID) || VCBiome.isShore(getBiome(xCoord, zCoord-1).biomeID)) {
					if(!VCBiome.isShore(getBiome(xCoord, zCoord).biomeID))
						cliffMap[arrayIndex] = true;
				}
				
				
				for (int height = 127; height >= 0; --height) {
					int indexBig = ((arrayIndex) * worldHeight + height + indexOffset);
					int index = ((arrayIndex) * 128 + height);

					float temp = 0.5f; //VC_Climate.adjustHeightToTemp(height, _temp);
					
					if(VCBiome.isShore(biome.biomeID) && height > seaLevel+h && primer.getBlockState(index).getBlock() == Blocks.stone) {
						// idsTop[index] = Blocks.air;
						primer.setBlockState(index, Blocks.stone.getDefaultState());
						
						if (h == 0) {
							h = (height-16)/4;
						}
					}
					
					/*if(idsBig[indexBig] == null) {
						idsBig[indexBig] = idsTop[index];
						if (indexBig+1 < idsBig.length && VC_Core.isSoilOrGravel(idsBig[indexBig+1]) && idsBig[indexBig] == Blocks.air) {
							for (int upCount = 1; VC_Core.isSoilOrGravel(idsBig[indexBig+upCount]); upCount++) {
								idsBig[indexBig+upCount] = Blocks.air;
							}
						}
					}

					if (idsBig[indexBig] == Blocks.stone) {
						if(seaLevelOffsetMap[arrayIndex] == 0 && height-16 >= 0) {
							seaLevelOffsetMap[arrayIndex] = height-16;
						}

						if(chunkHeightMap[arrayIndex] == 0) {
							chunkHeightMap[arrayIndex] = height+indexOffset;
						}

						convertStone(indexOffset+height, arrayIndex, indexBig, idsBig, metaBig, rock1, rock2, rock3);

						//First we check to see if its a cold desert
						if(rain < 125 && temp < 1.5f) {
							surfaceBlock = VC_Core.getTypeForSand(rock1.data1);
							subSurfaceBlock = VC_Core.getTypeForSand(rock1.data1);
						} else {
						
						//Next we check for all other warm deserts
							if (rain < 125 && biome.heightVariation < 0.5f && temp > 20f) {
								surfaceBlock = VC_Core.getTypeForSand(rock1.data1);
								subSurfaceBlock = VC_Core.getTypeForSand(rock1.data1);
							}
						}

						if (biome == VCBiome.beach || biome == VCBiome.ocean || biome == VCBiome.DeepOcean) {
							subSurfaceBlock = surfaceBlock = VC_Core.getTypeForSand(rock1.data1);
						} else if(biome == VCBiome.gravelbeach) {
							subSurfaceBlock = surfaceBlock = VC_Core.getTypeForGravel(rock1.data1);
						}

						if (var13 == -1) {
							//The following makes dirt behave nicer and more smoothly, instead of forming sharp cliffs.
							int arrayIndexx = xCoord > 0? (xCoord - 1) + (zCoord * 16):-1;
							int arrayIndexX = xCoord < 15? (xCoord + 1) + (zCoord * 16):-1;
							int arrayIndexz = zCoord > 0? xCoord + ((zCoord-1) * 16):-1;
							int arrayIndexZ = zCoord < 15? xCoord + ((zCoord+1) * 16):-1;
							int var12Temp = var12;
							for (int counter = 1; counter < var12Temp / 3; counter++) {
								
								if(arrayIndexx >= 0 && seaLevelOffsetMap[arrayIndex]-(3*counter) > seaLevelOffsetMap[arrayIndexx]) {
									seaLevelOffsetMap[arrayIndex]--;
									var12--;
									height--;
									indexBig = ((arrayIndex) * worldHeight + height + indexOffset);
									index = ((arrayIndex) * 128 + height);
								}
								else if(arrayIndexX >= 0 && seaLevelOffsetMap[arrayIndex]-(3*counter) > seaLevelOffsetMap[arrayIndexX]) {
									seaLevelOffsetMap[arrayIndex]--;
									var12--;
									height--;
									indexBig = ((arrayIndex) * worldHeight + height + indexOffset);
									index = ((arrayIndex) * 128 + height);
								}
								else if(arrayIndexz >= 0 && seaLevelOffsetMap[arrayIndex]-(3*counter) > seaLevelOffsetMap[arrayIndexz]) {
									seaLevelOffsetMap[arrayIndex]--;
									var12--;
									height--;
									indexBig = ((arrayIndex) * worldHeight + height + indexOffset);
									index = ((arrayIndex) * 128 + height);
								}
								else if(arrayIndexZ >= 0 && seaLevelOffsetMap[arrayIndex]-(3*counter) > seaLevelOffsetMap[arrayIndexZ]) {
									seaLevelOffsetMap[arrayIndex]--;
									var12--;
									height--;
									indexBig = ((arrayIndex) * worldHeight + height + indexOffset);
									index = ((arrayIndex) * 128 + height);
								}
							}
							var13 = (int)(var12 * (1d-Math.max(Math.min(((height - 16) / 80d), 1), 0)));

							//Set soil below water
							for (int c = 1; c < 3; c++) {
								if (indexBig + c < idsBig.length && (
										(idsBig[indexBig + c] != surfaceBlock) &&
										(idsBig[indexBig + c] != subSurfaceBlock) &&
										(idsBig[indexBig + c] != VCBlocks.SaltWaterStationary) &&
										(idsBig[indexBig + c] != VCBlocks.FreshWaterStationary) &&
										(idsBig[indexBig + c] != VCBlocks.HotWater))) {
									idsBig[indexBig + c] = Blocks.air;
									//metaBig[indexBig + c] = 0;
									if (indexBig + c + 1 < idsBig.length && idsBig[indexBig + c + 1] == VCBlocks.SaltWaterStationary) {
										idsBig[indexBig + c] = subSurfaceBlock;
										metaBig[indexBig + c] = (byte)VC_Core.getSoilMeta(rock1.data1);
									}
								}
							}

							//Determine the soil depth based on world height
							int dirtH = Math.max(8-((height + 96 - Global.SEALEVEL) / 16), 0);

							if(var13 > 0) {
								if (height >= seaLevel - 1 && index+1 < idsTop.length && idsTop[index + 1] != VCBlocks.SaltWaterStationary && dirtH > 0) {
									idsBig[indexBig] = surfaceBlock;
									metaBig[indexBig] = (byte)VC_Core.getSoilMeta(rock1.data1);


									for (int c = 1; c < dirtH && !VC_Core.isMountainBiome(biome.biomeID) && 
											biome != VCBiome.HighHills && biome != VCBiome.HighHillsEdge && !cliffMap[arrayIndex]; c++) {
										int _height = height - c;
										int _indexBig = ((arrayIndex) * worldHeight + _height + indexOffset);
										idsBig[_indexBig] = subSurfaceBlock;
										metaBig[_indexBig] = (byte)VC_Core.getSoilMeta(rock1.data1);

										if (c > 1+(5-drainage.data1)) {
											idsBig[_indexBig] = VC_Core.getTypeForGravel(rock1.data1);
											metaBig[_indexBig] = (byte)VC_Core.getSoilMeta(rock1.data1);
										}
									}
								}
							}
						}
						
						if (!(biome == VCBiome.swampland)) {
							if (((height > seaLevel - 2 && height < seaLevel && idsTop[index + 1] == VCBlocks.SaltWaterStationary)) || (height < seaLevel && idsTop[index + 1] == VCBlocks.SaltWaterStationary)) {
								if (idsBig[indexBig] != VC_Core.getTypeForSand(rock1.data1) && rand.nextInt(5) != 0) {
									idsBig[indexBig] = VC_Core.getTypeForGravel(rock1.data1);
									metaBig[indexBig] = (byte)VC_Core.getSoilMeta(rock1.data1);
								}
							}
						}
					}
					else if (idsTop[index] == VCBlocks.SaltWaterStationary && biome != VCBiome.ocean && biome != VCBiome.DeepOcean && biome != VCBiome.beach && biome != VCBiome.gravelbeach) {
						idsBig[indexBig] = VCBlocks.FreshWaterStationary;
					}
				}
			}
		}
	}
	*/
	
	
	
	
	
	
	
	
	

	@Override
	public boolean unloadQueuedChunks()
	{
		return true;
	}
	
}