package me.paulf.fairylights.server.fastener;

import me.paulf.fairylights.server.capability.CapabilityHandler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.eventbus.api.Event;

import java.util.ConcurrentModificationException;
import java.util.Set;

public class CollectFastenersEvent extends Event {
    private final World world;

    private final AxisAlignedBB region;

    private final Set<Fastener<?>> fasteners;

    public CollectFastenersEvent(final World world, final AxisAlignedBB region, final Set<Fastener<?>> fasteners) {
        this.world = world;
        this.region = region;
        this.fasteners = fasteners;
    }

    public World getWorld() {
        return this.world;
    }

    public AxisAlignedBB getRegion() {
        return this.region;
    }

    public void accept(final Chunk chunk) {
        try {
            for (final TileEntity entity : chunk.func_177434_r().values()) {
                this.accept(entity);
            }
        } catch (final ConcurrentModificationException e) {
            // RenderChunk's may find an invalid block entity while building and trigger a remove not on main thread
        }
    }

    public void accept(final ICapabilityProvider provider) {
        provider.getCapability(CapabilityHandler.FASTENER_CAP).ifPresent(this::accept);
    }

    public void accept(final Fastener<?> fastener) {
        if (this.region.func_72318_a(fastener.getConnectionPoint())) {
            this.fasteners.add(fastener);
        }
    }
}
