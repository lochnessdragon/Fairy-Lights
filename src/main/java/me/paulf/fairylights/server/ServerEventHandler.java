package me.paulf.fairylights.server;

import me.paulf.fairylights.FairyLights;
import me.paulf.fairylights.server.block.FLBlocks;
import me.paulf.fairylights.server.block.FastenerBlock;
import me.paulf.fairylights.server.block.entity.FastenerBlockEntity;
import me.paulf.fairylights.server.capability.CapabilityHandler;
import me.paulf.fairylights.server.config.FLConfig;
import me.paulf.fairylights.server.connection.Connection;
import me.paulf.fairylights.server.connection.HangingLightsConnection;
import me.paulf.fairylights.server.entity.FenceFastenerEntity;
import me.paulf.fairylights.server.fastener.BlockFastener;
import me.paulf.fairylights.server.fastener.Fastener;
import me.paulf.fairylights.server.fastener.FenceFastener;
import me.paulf.fairylights.server.fastener.PlayerFastener;
import me.paulf.fairylights.server.feature.light.Light;
import me.paulf.fairylights.server.item.ConnectionItem;
import me.paulf.fairylights.server.jingle.Jingle;
import me.paulf.fairylights.server.jingle.JingleLibrary;
import me.paulf.fairylights.server.jingle.JingleManager;
import me.paulf.fairylights.server.net.clientbound.JingleMessage;
import me.paulf.fairylights.server.net.clientbound.UpdateEntityFastenerMessage;
import me.paulf.fairylights.server.sound.FLSounds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FenceBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.item.HangingEntity;
import net.minecraft.entity.item.LeashKnotEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.server.SBlockActionPacket;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.server.management.PlayerList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.NoteBlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class ServerEventHandler {
    private final Random rng = new Random();

    private boolean eventOccurring = false;

    @SubscribeEvent
    public void onEntityJoinWorld(final EntityJoinWorldEvent event) {
        final Entity entity = event.getEntity();
        if (entity instanceof PlayerEntity || entity instanceof FenceFastenerEntity) {
            entity.getCapability(CapabilityHandler.FASTENER_CAP).ifPresent(f -> f.setWorld(event.getWorld()));
        }
    }

    @SubscribeEvent
    public void onAttachEntityCapability(final AttachCapabilitiesEvent<Entity> event) {
        final Entity entity = event.getObject();
        if (entity instanceof PlayerEntity) {
            event.addCapability(CapabilityHandler.FASTENER_ID, new PlayerFastener((PlayerEntity) entity));
        } else if (entity instanceof FenceFastenerEntity) {
            event.addCapability(CapabilityHandler.FASTENER_ID, new FenceFastener((FenceFastenerEntity) entity));
        }
    }

    @SubscribeEvent
    public void onAttachBlockEntityCapability(final AttachCapabilitiesEvent<TileEntity> event) {
        final TileEntity entity = event.getObject();
        if (entity instanceof FastenerBlockEntity) {
            event.addCapability(CapabilityHandler.FASTENER_ID, new BlockFastener((FastenerBlockEntity) entity, ServerProxy.buildBlockView()));
        }
    }

    @SubscribeEvent
    public void onTick(final TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            event.player.getCapability(CapabilityHandler.FASTENER_CAP).ifPresent(fastener -> {
                if (fastener.update() && !event.player.field_70170_p.field_72995_K) {
                    ServerProxy.sendToPlayersWatchingEntity(new UpdateEntityFastenerMessage(event.player, fastener.serializeNBT()), event.player);
                }
            });
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onNoteBlockPlay(final NoteBlockEvent.Play event) {
        final World world = (World) event.getWorld();
        final BlockPos pos = event.getPos();
        final Block noteBlock = world.func_180495_p(pos).func_177230_c();
        final BlockState below = world.func_180495_p(pos.func_177977_b());
        if (below.func_177230_c() == FLBlocks.FASTENER.get() && below.func_177229_b(FastenerBlock.field_176387_N) == Direction.DOWN) {
            final int note = event.getVanillaNoteId();
            final float pitch = (float) Math.pow(2, (note - 12) / 12D);
            world.func_184133_a(null, pos, FLSounds.JINGLE_BELL.get(), SoundCategory.RECORDS, 3, pitch);
            world.func_195594_a(ParticleTypes.field_197597_H, pos.func_177958_n() + 0.5, pos.func_177956_o() + 1.2, pos.func_177952_p() + 0.5, note / 24D, 0, 0);
            if (!world.field_72995_K) {
                final IPacket<?> pkt = new SBlockActionPacket(pos, noteBlock, event.getInstrument().ordinal(), note);
                final PlayerList players = world.func_73046_m().func_184103_al();
                players.func_148543_a(null, pos.func_177958_n(), pos.func_177956_o(), pos.func_177952_p(), 64, world.func_234923_W_(), pkt);
            }
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(final PlayerInteractEvent.RightClickBlock event) {
        final World world = event.getWorld();
        final BlockPos pos = event.getPos();
        if (!(world.func_180495_p(pos).func_177230_c() instanceof FenceBlock)) {
            return;
        }
        final ItemStack stack = event.getItemStack();
        boolean checkHanging = stack.func_77973_b() == Items.field_151058_ca;
        final PlayerEntity player = event.getPlayer();
        if (event.getHand() == Hand.MAIN_HAND) {
            final ItemStack offhandStack = player.func_184592_cb();
            if (offhandStack.func_77973_b() instanceof ConnectionItem) {
                if (checkHanging) {
                    event.setCanceled(true);
                    return;
                } else {
                    event.setUseBlock(Event.Result.DENY);
                }
            }
        }
        if (!checkHanging && !world.field_72995_K) {
            final double range = 7;
            final int x = pos.func_177958_n();
            final int y = pos.func_177956_o();
            final int z = pos.func_177952_p();
            final AxisAlignedBB area = new AxisAlignedBB(x - range, y - range, z - range, x + range, y + range, z + range);
            for (final MobEntity entity : world.func_217357_a(MobEntity.class, area)) {
                if (entity.func_110167_bD() && entity.func_110166_bE() == player) {
                    checkHanging = true;
                    break;
                }
            }
        }
        if (checkHanging) {
            final HangingEntity entity = FenceFastenerEntity.findHanging(world, pos);
            if (entity != null && !(entity instanceof LeashKnotEntity)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onWorldTick(final TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.START || event.side == LogicalSide.CLIENT || !FLConfig.isJingleEnabled()) {
            return;
        }
        if (event.world.func_82737_E() % (5 * 60 * 20) == 0) {
            this.eventOccurring = FairyLights.CHRISTMAS.isOccurringNow() || FairyLights.HALLOWEEN.isOccurringNow();
        }
        if (this.eventOccurring && this.rng.nextFloat() < 1.0F / (5 * 60 * 20)) {
            List<TileEntity> tileEntities = Collections.emptyList();
            try {
                tileEntities = new ArrayList<>(event.world.field_175730_i);
            } catch (ConcurrentModificationException ignored) {
            }
            final List<Vector3d> playingSources = new ArrayList<>();
            final Map<Fastener<?>, List<HangingLightsConnection>> feasibleConnections = new HashMap<>();
            for (final TileEntity tileEntity : tileEntities) {
                tileEntity.getCapability(CapabilityHandler.FASTENER_CAP).ifPresent(fastener -> {
                    final List<Vector3d> newPlayingSources = this.getPlayingLightSources(event.world, feasibleConnections, fastener);
                    if (!newPlayingSources.isEmpty()) {
                        playingSources.addAll(newPlayingSources);
                    }
                });
            }
            ((ServerWorld) event.world).getEntities().forEach(entity -> {
                entity.getCapability(CapabilityHandler.FASTENER_CAP).ifPresent(fastener -> {
                    final List<Vector3d> newPlayingSources = this.getPlayingLightSources(event.world, feasibleConnections, fastener);
                    if (!newPlayingSources.isEmpty()) {
                        playingSources.addAll(newPlayingSources);
                    }
                });
            });
            final Iterator<Fastener<?>> feasibleFasteners = feasibleConnections.keySet().iterator();
            while (feasibleFasteners.hasNext()) {
                final Fastener<?> fastener = feasibleFasteners.next();
                final List<HangingLightsConnection> connections = feasibleConnections.get(fastener);
                connections.removeIf(connection -> this.isTooCloseTo(fastener, connection.getFeatures(), playingSources));
                if (connections.size() == 0) {
                    feasibleFasteners.remove();
                }
            }
            if (feasibleConnections.size() == 0) {
                return;
            }
            final Fastener<?> fastener = feasibleConnections.keySet().toArray(new Fastener[0])[this.rng.nextInt(feasibleConnections.size())];
            final List<HangingLightsConnection> connections = feasibleConnections.get(fastener);
            final HangingLightsConnection connection = connections.get(this.rng.nextInt(connections.size()));
            tryJingle(event.world, connection, FairyLights.CHRISTMAS.isOccurringNow() ? JingleLibrary.CHRISTMAS : JingleLibrary.HALLOWEEN);
        }
    }

    private List<Vector3d> getPlayingLightSources(final World world, final Map<Fastener<?>, List<HangingLightsConnection>> feasibleConnections, final Fastener<?> fastener) {
        final List<Vector3d> points = new ArrayList<>();
        final double expandAmount = FLConfig.getJingleAmplitude();
        final AxisAlignedBB listenerRegion = fastener.getBounds().func_72321_a(expandAmount, expandAmount, expandAmount);
        final List<PlayerEntity> nearPlayers = world.func_217357_a(PlayerEntity.class, listenerRegion);
        final boolean arePlayersNear = nearPlayers.size() > 0;
        for (final Connection connection : fastener.getOwnConnections()) {
            if (connection.getDestination().get(world, false).isPresent() && connection instanceof HangingLightsConnection) {
                final HangingLightsConnection connectionLogic = (HangingLightsConnection) connection;
                final Light<?>[] lightPoints = connectionLogic.getFeatures();
                if (connectionLogic.canCurrentlyPlayAJingle()) {
                    if (arePlayersNear) {
                        if (feasibleConnections.containsKey(fastener)) {
                            feasibleConnections.get(fastener).add((HangingLightsConnection) connection);
                        } else {
                            final List<HangingLightsConnection> connections = new ArrayList<>();
                            connections.add((HangingLightsConnection) connection);
                            feasibleConnections.put(fastener, connections);
                        }
                    }
                } else {
                    for (final Light<?> light : lightPoints) {
                        points.add(light.getAbsolutePoint(fastener));
                    }
                }
            }
        }
        return points;
    }

    public boolean isTooCloseTo(final Fastener<?> fastener, final Light<?>[] lights, final List<Vector3d> playingSources) {
        for (final Light<?> light : lights) {
            for (final Vector3d point : playingSources) {
                if (light.getAbsolutePoint(fastener).func_72438_d(point) <= FLConfig.getJingleAmplitude()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean tryJingle(final World world, final HangingLightsConnection hangingLights, final String lib) {
        if (world.field_72995_K) return false;
        final Light<?>[] lights = hangingLights.getFeatures();
        final Jingle jingle = JingleManager.INSTANCE.get(lib).getRandom(world.field_73012_v, lights.length);
        if (jingle != null) {
            final int lightOffset = lights.length / 2 - jingle.getRange() / 2;
            hangingLights.play(jingle, lightOffset);
            ServerProxy.sendToPlayersWatchingChunk(new JingleMessage(hangingLights, lightOffset, jingle), world, hangingLights.getFastener().getPos());
            return true;
        }
        return false;
    }
}
