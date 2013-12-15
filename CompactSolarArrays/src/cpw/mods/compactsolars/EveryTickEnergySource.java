package cpw.mods.compactsolars;

import ic2.api.energy.prefab.BasicSource;
import net.minecraft.tileentity.TileEntity;

public class EveryTickEnergySource extends BasicSource {

	protected int outputPacketSize;
	
	public EveryTickEnergySource(TileEntity parent, int capacity, int tier, int outputPacketSize) {
		super(parent, capacity, tier);
		this.outputPacketSize = outputPacketSize;
	}
	
	@Override
	public double getOfferedEnergy() {

		if (energyStored >= outputPacketSize) {
			return outputPacketSize;
		} else {
			return 0;
		}
	}
}
