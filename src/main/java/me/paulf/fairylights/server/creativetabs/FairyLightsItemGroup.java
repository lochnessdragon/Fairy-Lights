package me.paulf.fairylights.server.creativetabs;

import me.paulf.fairylights.FairyLights;
import me.paulf.fairylights.server.item.FLItems;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;

public final class FairyLightsItemGroup extends ItemGroup {
    public FairyLightsItemGroup() {
        super(FairyLights.ID);
    }

    @Override
    public ItemStack func_78016_d() {
        return new ItemStack(FLItems.HANGING_LIGHTS.get());
    }
}
