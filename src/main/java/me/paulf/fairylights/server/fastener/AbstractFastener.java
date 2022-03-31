package me.paulf.fairylights.server.fastener;

import com.google.common.collect.ImmutableList;
import me.paulf.fairylights.FairyLights;
import me.paulf.fairylights.server.capability.CapabilityHandler;
import me.paulf.fairylights.server.fastener.accessor.FastenerAccessor;
import me.paulf.fairylights.util.Catenary;
import me.paulf.fairylights.server.connection.ConnectionType;
import me.paulf.fairylights.server.connection.Connection;
import me.paulf.fairylights.util.AABBBuilder;
import me.paulf.fairylights.util.RegistryObjects;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

public abstract class AbstractFastener<F extends FastenerAccessor> implements Fastener<F> {
    private final Map<UUID, Connection> outgoing = new HashMap<>();

    private final Map<UUID, Incoming> incoming = new HashMap<>();

    protected AxisAlignedBB bounds = TileEntity.INFINITE_EXTENT_AABB;

    @Nullable
    private World world;

    private boolean dirty;

    @Override
    public Optional<Connection> get(final UUID id) {
        return Optional.ofNullable(this.outgoing.get(id));
    }

    @Override
    public List<Connection> getOwnConnections() {
        return ImmutableList.copyOf(this.outgoing.values());
    }

    @Override
    public List<Connection> getAllConnections() {
        final ImmutableList.Builder<Connection> list = new ImmutableList.Builder<>();
        list.addAll(this.outgoing.values());
        if (this.world != null) {
            this.incoming.values().forEach(i -> i.get(this.world).ifPresent(list::add));
        }
        return list.build();
    }

    @Override
    public AxisAlignedBB getBounds() {
        return this.bounds;
    }

    @Override
    public abstract BlockPos getPos();

    @Override
    public void setWorld(final World world) {
        this.world = world;
        this.outgoing.values().forEach(c -> c.setWorld(world));
    }

    @Nullable
    @Override
    public World getWorld() {
        return this.world;
    }

    @Override
    public boolean update() {
        final Iterator<Connection> it = this.outgoing.values().iterator();
        final Vector3d fromOffset = this.getConnectionPoint();
        boolean dirty = this.dirty;
        this.dirty = false;
        while (it.hasNext()) {
            final Connection connection = it.next();
            if (connection.update(fromOffset)) {
                dirty = true;
            }
            if (connection.isRemoved()) {
                dirty = true;
                it.remove();
                this.incoming.remove(connection.getUUID());
                if (this.world != null) {
                    this.drop(this.world, this.getPos(), connection);
                }
            }
        }
        if (this.world != null) {
            this.incoming.values().removeIf(incoming -> incoming.gone(this.world));
        }
        if (dirty) {
            this.calculateBoundingBox();
        }
        return dirty;
    }

    @Override
    public void setDirty() {
        this.dirty = true;
    }

    protected void calculateBoundingBox() {
        if (this.outgoing.isEmpty()) {
            this.bounds = new AxisAlignedBB(this.getPos());
            return;
        }
        final AABBBuilder builder = new AABBBuilder();
        for (final Connection connection : this.outgoing.values()) {
            final Catenary catenary = connection.getCatenary();
            if (catenary == null) {
                continue;
            }
            final Catenary.SegmentIterator it = catenary.iterator();
            while (it.next()) {
                builder.include(it.getX(0.0F), it.getY(0.0F), it.getZ(0.0F));
                if (!it.hasNext()) {
                    builder.include(it.getX(1.0F), it.getY(1.0F), it.getZ(1.0F));
                }
            }
        }
        this.bounds = builder.add(this.getConnectionPoint()).build();
    }

    @Override
    public void dropItems(final World world, final BlockPos pos) {
        for (final Connection connection : this.getAllConnections()) {
            this.drop(world, pos, connection);
        }
    }

    private void drop(final World world, final BlockPos pos, final Connection connection) {
        if (!connection.shouldDrop()) return;
        final float offsetX = world.field_73012_v.nextFloat() * 0.8F + 0.1F;
        final float offsetY = world.field_73012_v.nextFloat() * 0.8F + 0.1F;
        final float offsetZ = world.field_73012_v.nextFloat() * 0.8F + 0.1F;
        final ItemStack stack = connection.getItemStack();
        final ItemEntity entityItem = new ItemEntity(world, pos.func_177958_n() + offsetX, pos.func_177956_o() + offsetY, pos.func_177952_p() + offsetZ, stack);
        final float scale = 0.05F;
        entityItem.func_213293_j(
            world.field_73012_v.nextGaussian() * scale,
            world.field_73012_v.nextGaussian() * scale + 0.2F,
            world.field_73012_v.nextGaussian() * scale
        );
        world.func_217376_c(entityItem);
        connection.noDrop();
    }

    @Override
    public void remove() {
        this.outgoing.values().forEach(Connection::remove);
    }

    @Override
    public boolean hasNoConnections() {
        return this.outgoing.isEmpty() && this.incoming.isEmpty();
    }

    @Override
    public boolean hasConnectionWith(final Fastener<?> fastener) {
        return this.getConnectionTo(fastener.createAccessor()) != null;
    }

    @Nullable
    @Override
    public Connection getConnectionTo(final FastenerAccessor destination) {
        for (final Connection connection : this.outgoing.values()) {
            if (connection.isDestination(destination)) {
                return connection;
            }
        }
        return null;
    }

    @Override
    public boolean removeConnection(final UUID uuid) {
        final Connection connection = this.outgoing.remove(uuid);
        if (connection != null) {
            connection.remove();
            this.setDirty();
            return true;
        } else if (this.incoming.remove(uuid) != null) {
            this.setDirty();
            return true;
        }
        return false;
    }

    @Override
    public boolean removeConnection(final Connection connection) {
        return this.removeConnection(connection.getUUID());
    }

    @Override
    public boolean reconnect(final World world, final Connection connection, final Fastener<?> newDestination) {
        if (this.equals(newDestination) || newDestination.hasConnectionWith(this)) {
            return false;
        }
        final UUID uuid = connection.getUUID();
        if (connection.getDestination().get(world, false).filter(t -> {
            t.removeConnection(uuid);
            return true;
        }).isPresent()) {
            connection.setDestination(newDestination);
            connection.setDrop();
            newDestination.createIncomingConnection(this.world, uuid, this, connection.getType());
            this.setDirty();
            return true;
        }
        return false;
    }

    @Override
    public Connection connect(final World world, final Fastener<?> destination, final ConnectionType<?> type, final CompoundNBT compound, final boolean drop) {
        final UUID uuid = MathHelper.func_188210_a();
        final Connection connection = this.createOutgoingConnection(world, uuid, destination, type, compound, drop);
        destination.createIncomingConnection(world, uuid, this, type);
        return connection;
    }

    @Override
    public Connection createOutgoingConnection(final World world, final UUID uuid, final Fastener<?> destination, final ConnectionType<?> type, final CompoundNBT compound, final boolean drop) {
        final Connection c = type.create(world, this, uuid);
        c.deserialize(destination, compound, drop);
        this.outgoing.put(uuid, c);
        this.setDirty();
        return c;
    }

    @Override
    public void createIncomingConnection(final World world, final UUID uuid, final Fastener<?> destination, final ConnectionType<?> type) {
        this.incoming.put(uuid, new Incoming(destination.createAccessor(), uuid));
        this.setDirty();
    }

    @Override
    public CompoundNBT serializeNBT() {
        final CompoundNBT compound = new CompoundNBT();
        final ListNBT outgoing = new ListNBT();
        for (final Entry<UUID, Connection> connectionEntry : this.outgoing.entrySet()) {
            final UUID uuid = connectionEntry.getKey();
            final Connection connection = connectionEntry.getValue();
            final CompoundNBT connectionCompound = new CompoundNBT();
            connectionCompound.func_218657_a("connection", connection.serialize());
            connectionCompound.func_74778_a("type", RegistryObjects.getName(connection.getType()).toString());
            connectionCompound.func_186854_a("uuid", uuid);
            outgoing.add(connectionCompound);
        }
        compound.func_218657_a("outgoing", outgoing);
        final ListNBT incoming = new ListNBT();
        for (final Entry<UUID, Incoming> e : this.incoming.entrySet()) {
            final CompoundNBT tag = new CompoundNBT();
            tag.func_186854_a("uuid", e.getKey());
            tag.func_218657_a("fastener", FastenerType.serialize(e.getValue().fastener));
            incoming.add(tag);
        }
        compound.func_218657_a("incoming", incoming);
        return compound;
    }

    @Override
    public void deserializeNBT(final CompoundNBT compound) {
        final ListNBT listConnections = compound.func_150295_c("outgoing", NBT.TAG_COMPOUND);
        final List<UUID> nbtUUIDs = new ArrayList<>();
        for (int i = 0; i < listConnections.size(); i++) {
            final CompoundNBT connectionCompound = listConnections.func_150305_b(i);
            final UUID uuid;
            if (connectionCompound.func_186855_b("uuid")) {
                uuid = connectionCompound.func_186857_a("uuid");
            } else {
                uuid = MathHelper.func_188210_a();
            }
            nbtUUIDs.add(uuid);
            if (this.outgoing.containsKey(uuid)) {
                final Connection connection = this.outgoing.get(uuid);
                connection.deserialize(connectionCompound.func_74775_l("connection"));
            } else {
                final ConnectionType<?> type = FairyLights.CONNECTION_TYPES.getValue(ResourceLocation.func_208304_a(connectionCompound.func_74779_i("type")));
                if (type != null) {
                    final Connection connection = type.create(this.world, this, uuid);
                    connection.deserialize(connectionCompound.func_74775_l("connection"));
                    this.outgoing.put(uuid, connection);
                }
            }
        }
        final Iterator<Entry<UUID, Connection>> connectionsIter = this.outgoing.entrySet().iterator();
        while (connectionsIter.hasNext()) {
            final Entry<UUID, Connection> connection = connectionsIter.next();
            if (!nbtUUIDs.contains(connection.getKey())) {
                connectionsIter.remove();
                connection.getValue().remove();
            }
        }
        this.incoming.clear();
        final ListNBT incoming = compound.func_150295_c("incoming", NBT.TAG_COMPOUND);
        for (int i = 0; i < incoming.size(); i++) {
            final CompoundNBT incomingNbt = incoming.func_150305_b(i);
            final UUID uuid = incomingNbt.func_186857_a("uuid");
            final FastenerAccessor fastener = FastenerType.deserialize(incomingNbt.func_74775_l("fastener"));
            this.incoming.put(uuid, new Incoming(fastener, uuid));
        }
        this.setDirty();
    }

    private final LazyOptional<Fastener<?>> lazyOptional = LazyOptional.of(() -> this);

    @Override
    public <T> LazyOptional<T> getCapability(final Capability<T> capability, final Direction facing) {
        return capability == CapabilityHandler.FASTENER_CAP ? this.lazyOptional.cast() : LazyOptional.empty();
    }

    static class Incoming {
        final FastenerAccessor fastener;

        final UUID id;

        Incoming(final FastenerAccessor fastener, final UUID id) {
            this.fastener = fastener;
            this.id = id;
        }

        boolean gone(final World world) {
            return this.fastener.isGone(world);
        }

        Optional<Connection> get(final World world) {
            return this.fastener.get(world, false).map(Optional::of).orElse(Optional.empty()).flatMap(f -> f.get(this.id));
        }
    }
}
