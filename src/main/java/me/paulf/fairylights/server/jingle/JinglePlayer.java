package me.paulf.fairylights.server.jingle;

import me.paulf.fairylights.server.feature.light.Light;
import me.paulf.fairylights.server.sound.FLSounds;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class JinglePlayer {
    private State<?> state = new NotPlayingState();

    @Nullable
    public Jingle getJingle() {
        return this.state.getJingle();
    }

    public boolean isPlaying() {
        return this.state.isPlaying();
    }

    public float getProgress() {
        return this.state.getProgress();
    }

    public void play(final Jingle jingle, final int lightOffset) {
        this.state = new PlayingState(jingle, lightOffset);
    }

    public void tick(final World world, final Vector3d origin, final Light<?>[] lights, final boolean isClient) {
        this.state = this.state.tick(world, origin, lights, isClient);
    }

    public CompoundNBT serialize() {
        return StateType.serialize(this.state);
    }

    public void deserialize(final CompoundNBT compound) {
        this.state = StateType.deserialize(compound);
    }

    private enum StateType {
        NOT_PLAYING(NotPlayingState.FACTORY),
        PLAYING(PlayingState.FACTORY);

        private static final Map<String, StateType> MAP = Stream.of(values())
            .collect(Collectors.toMap(StateType::getId, Function.identity()));

        private final StateFactory<?> factory;

        StateType(final StateFactory<?> factory) {
            this.factory = factory;
        }

        private String getId() {
            return this.factory.getId();
        }

        private StateFactory<?> getFactory() {
            return this.factory;
        }

        public static <S extends State<S>> CompoundNBT serialize(final State<S> state) {
            final StateFactory<S> factory = state.getFactory();
            final CompoundNBT compound = new CompoundNBT();
            compound.func_74778_a("state", factory.getId());
            compound.func_218657_a("data", factory.serialize(state.resolve()));
            return compound;
        }

        public static State<?> deserialize(final CompoundNBT compound) {
            return MAP.getOrDefault(compound.func_74779_i("state"), NOT_PLAYING)
                .getFactory()
                .deserialize(compound.func_74775_l("data"));
        }
    }

    private abstract static class StateFactory<S extends State<S>> {
        public abstract String getId();

        public abstract CompoundNBT serialize(S state);

        public abstract State<?> deserialize(CompoundNBT compound);
    }

    private abstract static class State<S extends State<S>> {
        public abstract Jingle getJingle();

        public abstract boolean isPlaying();

        public abstract float getProgress();

        public abstract State<?> tick(World world, Vector3d origin, Light<?>[] lights, boolean isClient);

        public abstract StateFactory<S> getFactory();

        public abstract S resolve();
    }

    private static final class NotPlayingState extends State<NotPlayingState> {
        public static final StateFactory<NotPlayingState> FACTORY = newFactory();

        @Override
        public Jingle getJingle() {
            return null;
        }

        @Override
        public boolean isPlaying() {
            return false;
        }

        @Override
        public float getProgress() {
            return 0;
        }

        @Override
        public State<?> tick(final World world, final Vector3d origin, final Light<?>[] lights, final boolean isClient) {
            return this;
        }

        @Override
        public StateFactory<NotPlayingState> getFactory() {
            return FACTORY;
        }

        @Override
        public NotPlayingState resolve() {
            return this;
        }

        private static StateFactory<NotPlayingState> newFactory() {
            return new StateFactory<NotPlayingState>() {
                @Override
                public String getId() {
                    return "not_playing";
                }

                @Override
                public CompoundNBT serialize(final NotPlayingState state) {
                    return new CompoundNBT();
                }

                @Override
                public NotPlayingState deserialize(final CompoundNBT compound) {
                    return new NotPlayingState();
                }
            };
        }
    }

    private static final class PlayingState extends State<PlayingState> {
        public static final StateFactory<PlayingState> FACTORY = newFactory();

        private final Jingle jingle;

        private final int lightOffset;

        private final List<Jingle.PlayTick> playTicks;

        private final int length;

        private final BasicParticleType[] noteParticle;

        private int index;

        private int rest;

        private int time;

        private PlayingState(final Jingle jingle, final int lightOffset) {
            this(jingle, lightOffset, jingle.getPlayTicks(), jingle.getLength(), getParticles(jingle));
        }

        private PlayingState(final Jingle jingle, final int lightOffset, final List<Jingle.PlayTick> playTicks, final int length, final BasicParticleType[] noteParticle) {
            this.jingle = jingle;
            this.lightOffset = lightOffset;
            this.playTicks = playTicks;
            this.length = length;
            this.noteParticle = noteParticle;
        }

        @Override
        public Jingle getJingle() {
            return this.jingle;
        }

        @Override
        public boolean isPlaying() {
            return true;
        }

        @Override
        public float getProgress() {
            return this.time / (float) this.length;
        }

        @Override
        public State<?> tick(final World world, final Vector3d origin, final Light<?>[] lights, final boolean isClient) {
            this.time++;
            if (this.rest <= 0) {
                if (this.index >= this.playTicks.size()) {
                    return new NotPlayingState();
                }
                final Jingle.PlayTick playTick = this.playTicks.get(this.index++);
                this.rest = playTick.getDuration() - 1;
                if (isClient) {
                    this.play(world, origin, lights, playTick);
                }
            } else {
                this.rest--;
            }
            return this;
        }

        private void play(final World world, final Vector3d origin, final Light<?>[] lights, final Jingle.PlayTick playTick) {
            for (final int note : playTick.getNotes()) {
                final int idx = note - this.jingle.getLowestNote() + this.lightOffset;
                if (idx >= 0 && idx < lights.length) {
                    lights[idx].jingle(world, origin, note, FLSounds.JINGLE_BELL.get(), this.noteParticle);
                }
            }
        }

        @Override
        public StateFactory<PlayingState> getFactory() {
            return FACTORY;
        }

        @Override
        public PlayingState resolve() {
            return this;
        }

        private static StateFactory<PlayingState> newFactory() {
            return new StateFactory<PlayingState>() {
                @Override
                public String getId() {
                    return "playing";
                }

                @Override
                public CompoundNBT serialize(final PlayingState state) {
                    final CompoundNBT compound = new CompoundNBT();
                    Jingle.CODEC.encodeStart(NBTDynamicOps.field_210820_a, state.jingle).result()
                        .ifPresent(jingle -> {
                            compound.func_218657_a("jingle", jingle);
                            compound.func_74768_a("lightOffset", state.lightOffset);
                            compound.func_74768_a("index", state.index);
                            compound.func_74768_a("rest", state.rest);
                            compound.func_74768_a("time", state.time);
                        });
                    return compound;
                }

                @Override
                public State<?> deserialize(final CompoundNBT compound) {
                    return Jingle.CODEC.parse(NBTDynamicOps.field_210820_a, compound.func_74775_l("jingle"))
                        .result().<State<?>>map(jingle -> {
                            final int lightOffset = compound.func_74762_e("lightOffset");
                            final PlayingState state = new PlayingState(jingle, lightOffset);
                            state.index = compound.func_74762_e("index");
                            state.rest = compound.func_74762_e("rest");
                            state.time = compound.func_74762_e("time");
                            return state;
                        }).orElseGet(NotPlayingState::new);
                }
            };
        }

        private static BasicParticleType[] getParticles(final Jingle jingle) {
            if (jingle.getTitle().hashCode() == 0xf2b587de) {
                return new BasicParticleType[]{ParticleTypes.field_197597_H, ParticleTypes.field_197595_F};
            }
            if (jingle.getTitle().hashCode() == 0xc3fa68bd || jingle.getTitle().hashCode() == 0x70f77bf4) {
                return new BasicParticleType[]{ParticleTypes.field_197597_H, ParticleTypes.field_197633_z};
            }
            return new BasicParticleType[]{ParticleTypes.field_197597_H};
        }
    }
}
