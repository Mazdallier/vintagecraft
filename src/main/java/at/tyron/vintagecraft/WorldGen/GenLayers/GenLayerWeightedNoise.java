package at.tyron.vintagecraft.WorldGen.GenLayers;

import at.tyron.vintagecraft.WorldProperties.EnumMaterialDeposit;
import at.tyron.vintagecraft.interfaces.IGenLayerSupplier;

public class GenLayerWeightedNoise extends GenLayerNoise {
	IGenLayerSupplier[] genlayersuppliers;
	/*int[] weights;
	int[] colors;*/
	
	int weightsum;
	boolean weightDistributionTriangular = false;
	
	public GenLayerWeightedNoise(long seed, IGenLayerSupplier[] genlayersupplier) {
		this(seed, genlayersupplier, false);
	}

	public GenLayerWeightedNoise(long seed, IGenLayerSupplier[] genlayersupplier, boolean weightDistributionTriangular) {
		super(seed);
		
		this.weightDistributionTriangular = weightDistributionTriangular;
		
		for (int i = 0; i < genlayersupplier.length; i++) {
			weightsum += genlayersupplier[i].getWeight();
		}
		
		this.genlayersuppliers = genlayersupplier;
	}
	
	@Override
	public int[] getInts(int xCoord, int zCoord, int sizeX, int sizeZ) {
		int[] cache = new int[sizeX * sizeZ];
		int rnd, sum=0;
		
		
		for (int z = 0; z < sizeZ; ++z) {
			for (int x = 0; x < sizeX; ++x) {
				this.initChunkSeed(xCoord + x, zCoord + z);
				
				rnd = nextInt(weightsum);
				sum = 0;
				
				//cache[x + z * sizeX] = nextInt(255) << 8;
				
				for (int i = 0; i < genlayersuppliers.length; i++) {
					sum += genlayersuppliers[i].getWeight();
					
					if (rnd < sum) {
						cache[x + z * sizeX] = (getDepth(genlayersuppliers[i]) << 16) + genlayersuppliers[i].getColor(); 
						
						
						if (genlayersuppliers[i].getSize() > 1) {
							cache[Math.max(0, x + z * sizeX - 2)] = cache[x + z * sizeX];
							cache[Math.max(0, x + z * sizeX - 1)] = cache[x + z * sizeX];
							
							cache[Math.max(0, x + (z - 2) * sizeX - 2)] = cache[x + z * sizeX];
							cache[Math.max(0, x + (z - 2) * sizeX - 1)] = cache[x + z * sizeX];
							cache[Math.max(0, x + (z - 2) * sizeX)] = cache[x + z * sizeX];
							
							cache[Math.max(0, x + (z - 1) * sizeX - 2)] = cache[x + z * sizeX];
							cache[Math.max(0, x + (z - 1) * sizeX - 1)] = cache[x + z * sizeX];
							cache[Math.max(0, x + (z - 1) * sizeX)] = cache[x + z * sizeX];
						}
						
						break;
					}
				}
				
				
				
				
			}
		}

		return cache;
	}
	
	
	int getDepth(IGenLayerSupplier genlayersupplier) {
		if (weightDistributionTriangular) {
			return genlayersupplier.getDepthMin() + (nextInt(1 + genlayersupplier.getDepthMax() - genlayersupplier.getDepthMin()) + nextInt(1 + genlayersupplier.getDepthMax() - genlayersupplier.getDepthMin()))/2;
		} else {
			return genlayersupplier.getDepthMin() + nextInt(1 + genlayersupplier.getDepthMax() - genlayersupplier.getDepthMin());
		}
	}
	
}



/*

	int[] depositDepths = new int[deposits.length];

for (int i = 0; i < deposits.length; i++) {
	depositDepths[i] = deposits[i].minDepth + random.nextInt(deposits[i].maxDepth - deposits[i].minDepth);
}

*/
