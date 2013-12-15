/*******************************************************************************
 * Copyright (c) 2012 cpw.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 *
 * Contributors:
 *     cpw - initial API and implementation
 ******************************************************************************/
package cpw.mods.compactsolars;

import ic2.api.energy.prefab.BasicSource;
import ic2.api.item.IElectricItem;
import ic2.api.tile.IWrenchable;

import java.util.Random;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;

public class TileEntityCompactSolar extends TileEntity implements IInventory, IWrenchable {
    private BasicSource energySource;
    private static Random random = new Random();
    private CompactSolarType type;
    private ItemStack[] inventory;
    private boolean initialized;
    public boolean theSunIsVisible;
    private int tick;
    private boolean canRain;
    private boolean noSunlight;

    public TileEntityCompactSolar() {
        this(CompactSolarType.LV);
    }

    public TileEntityCompactSolar(CompactSolarType type) {
        super();
        this.type = type;
        this.inventory = new ItemStack[1];
        this.tick = random.nextInt(64);
//        this.energySource = new BasicSource(this, type.maxStorage, type.ordinal() + 1);
        this.energySource = new EveryTickEnergySource(this, type.maxStorage, type.ordinal() + 1, this.type.outputPacketSize);
    }

    @Override
    public void updateEntity() {
        energySource.onUpdateEntity();
        if (!initialized && worldObj != null) {
            canRain = worldObj.getWorldChunkManager().getBiomeGenAt(xCoord, zCoord).getIntRainfall() > 0;
            noSunlight = worldObj.provider.hasNoSky;
            initialized = true;
        }
        if (noSunlight) {
            return;
        }
        if (tick-- == 0) {
            updateSunState();
            tick = 64;
        }
        int energyProduction = 0;

        if (theSunIsVisible && (CompactSolars.productionRate == 1 || random.nextInt(CompactSolars.productionRate) == 0)) {
            energyProduction = generateEnergy();
        }
        energySource.addEnergy(energyProduction);
        
        if (inventory[0] != null && (Item.itemsList[inventory[0].itemID] instanceof IElectricItem)) {
            energySource.charge(inventory[0]);
        }
    }

    private void updateSunState() {
        boolean isRaining = canRain && (worldObj.isRaining() || worldObj.isThundering());
        theSunIsVisible = worldObj.isDaytime() && !isRaining && worldObj.canBlockSeeTheSky(xCoord, yCoord + 1, zCoord);
    }

    private int generateEnergy() {
        return type.getOutput();
    }

    public ItemStack[] getContents() {
        return inventory;
    }

    @Override
    public int getSizeInventory() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int i) {
        return inventory[i];
    }

    @Override
    public ItemStack decrStackSize(int i, int j) {
        if (inventory[i] != null) {
            if (inventory[i].stackSize <= j) {
                ItemStack itemstack = inventory[i];
                inventory[i] = null;
                onInventoryChanged();
                return itemstack;
            }
            ItemStack itemstack1 = inventory[i].splitStack(j);
            if (inventory[i].stackSize == 0) {
                inventory[i] = null;
            }
            onInventoryChanged();
            return itemstack1;
        } else {
            return null;
        }
    }

    @Override
    public void setInventorySlotContents(int i, ItemStack itemstack) {
        inventory[i] = itemstack;
        if (itemstack != null && itemstack.stackSize > getInventoryStackLimit()) {
            itemstack.stackSize = getInventoryStackLimit();
        }
        onInventoryChanged();
    }

    @Override
    public String getInvName() {
        return type.name();
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer entityplayer) {
        if (worldObj == null) {
            return true;
        }
        if (worldObj.getBlockTileEntity(xCoord, yCoord, zCoord) != this) {
            return false;
        }
        return entityplayer.getDistanceSq((double) xCoord + 0.5D, (double) yCoord + 0.5D, (double) zCoord + 0.5D) <= 64D;
    }

    @Override
    public void openChest() {
        // NOOP
    }

    @Override
    public void closeChest() {
        // NOOP

    }

    @Override
    public boolean wrenchCanSetFacing(EntityPlayer entityPlayer, int side) {
        return false;
    }

    @Override
    public short getFacing() {
        return 0;
    }

    @Override
    public void setFacing(short facing) {
    }

    @Override
    public boolean wrenchCanRemove(EntityPlayer entityPlayer) {
        return true;
    }

    @Override
    public float getWrenchDropRate() {
        return 1.0F;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbttagcompound) {
        super.writeToNBT(nbttagcompound);
        NBTTagList nbttaglist = new NBTTagList();
        for (int i = 0; i < inventory.length; i++) {
            if (inventory[i] != null) {
                NBTTagCompound nbttagcompound1 = new NBTTagCompound();
                nbttagcompound1.setByte("Slot", (byte) i);
                inventory[i].writeToNBT(nbttagcompound1);
                nbttaglist.appendTag(nbttagcompound1);
            }
        }

        nbttagcompound.setTag("Items", nbttaglist);
        energySource.onWriteToNbt(nbttagcompound);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbttagcompound) {
        super.readFromNBT(nbttagcompound);
        energySource.onReadFromNbt(nbttagcompound);
        NBTTagList nbttaglist = nbttagcompound.getTagList("Items");
        inventory = new ItemStack[getSizeInventory()];
        for (int i = 0; i < nbttaglist.tagCount(); i++) {
            NBTTagCompound nbttagcompound1 = (NBTTagCompound) nbttaglist.tagAt(i);
            int j = nbttagcompound1.getByte("Slot") & 0xff;
            if (j >= 0 && j < inventory.length) {
                inventory[j] = ItemStack.loadItemStackFromNBT(nbttagcompound1);
            }
        }
    }

    public CompactSolarType getType() {
        return type;
    }

    @Override
    public void onChunkUnload() {
        energySource.onChunkUnload();
    }

    @Override
    public void invalidate() {
        energySource.onInvalidate();
        super.invalidate();
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int var1) {
        if (this.inventory[var1] != null) {
            ItemStack var2 = this.inventory[var1];
            this.inventory[var1] = null;
            return var2;
        } else {
            return null;
        }
    }

    @Override
    public ItemStack getWrenchDrop(EntityPlayer entityPlayer) {
        return new ItemStack(CompactSolars.compactSolarBlock, 1, getType().ordinal());
    }

    @Override
    public boolean isInvNameLocalized() {
        return false;
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemstack) {
        return itemstack != null && itemstack.getItem() instanceof IElectricItem;
    }
}
