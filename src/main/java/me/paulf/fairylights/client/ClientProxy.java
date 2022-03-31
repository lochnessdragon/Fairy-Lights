package me.paulf.fairylights.client;

import com.google.common.collect.ImmutableList;
import me.paulf.fairylights.FairyLights;
import me.paulf.fairylights.client.command.ClientCommandProvider;
import me.paulf.fairylights.client.command.JinglerCommand;
import me.paulf.fairylights.client.renderer.block.entity.FastenerBlockEntityRenderer;
import me.paulf.fairylights.client.renderer.block.entity.LetterBuntingRenderer;
import me.paulf.fairylights.client.renderer.block.entity.LightBlockEntityRenderer;
import me.paulf.fairylights.client.renderer.block.entity.PennantBuntingRenderer;
import me.paulf.fairylights.client.renderer.entity.FenceFastenerRenderer;
import me.paulf.fairylights.client.tutorial.ClippyController;
import me.paulf.fairylights.server.ServerProxy;
import me.paulf.fairylights.server.block.FLBlocks;
import me.paulf.fairylights.server.block.entity.FLBlockEntities;
import me.paulf.fairylights.server.entity.FLEntities;
import me.paulf.fairylights.server.feature.light.ColorChangingBehavior;
import me.paulf.fairylights.server.item.DyeableItem;
import me.paulf.fairylights.server.item.FLItems;
import me.paulf.fairylights.server.item.HangingLightsConnectionItem;
import me.paulf.fairylights.server.string.StringTypes;
import me.paulf.fairylights.util.styledstring.StyledString;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.color.ItemColors;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.RenderMaterial;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.Random;

public final class ClientProxy extends ServerProxy {
    @SuppressWarnings("deprecation")
    public static final RenderMaterial SOLID_TEXTURE = new RenderMaterial(AtlasTexture.field_110575_b, new ResourceLocation(FairyLights.ID, "entity/connections"));

    @SuppressWarnings("deprecation")
    public static final RenderMaterial TRANSLUCENT_TEXTURE = new RenderMaterial(AtlasTexture.field_110575_b, new ResourceLocation(FairyLights.ID, "entity/connections"));

    private final ImmutableList<ResourceLocation> entityModels = new ImmutableList.Builder<ResourceLocation>()
        .addAll(PennantBuntingRenderer.MODELS)
        .addAll(LetterBuntingRenderer.MODELS.values())
        .build();

    @Override
    public void init(final IEventBus modBus) {
        super.init(modBus);
        new ClippyController().init(modBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, FLClientConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(new ClientEventHandler());
        new ClientCommandProvider.Builder()
            .add(JinglerCommand::register)
            .build()
            .register(MinecraftForge.EVENT_BUS);
        JinglerCommand.register(MinecraftForge.EVENT_BUS);
        modBus.<TextureStitchEvent.Pre>addListener(e -> {
            if (SOLID_TEXTURE.func_229310_a_().equals(e.getMap().func_229223_g_())) {
                e.addSprite(SOLID_TEXTURE.func_229313_b_());
            }
        });
        // Undo sprite uv contraction
        modBus.<ModelBakeEvent>addListener(e -> {
            this.entityModels.forEach(path -> {
                final IBakedModel model = Minecraft.func_71410_x().func_209506_al().getModel(path);
                if (model == Minecraft.func_71410_x().func_209506_al().func_174951_a()) {
                    return;
                }
                final TextureAtlasSprite sprite = model.getParticleTexture(EmptyModelData.INSTANCE);
                final int w = (int) (sprite.func_94211_a() / (sprite.func_94212_f() - sprite.func_94209_e()));
                final int h = (int) (sprite.func_94216_b() / (sprite.func_94210_h() - sprite.func_94206_g()));
                for (final BakedQuad quad : model.getQuads(null, null, new Random(42L), EmptyModelData.INSTANCE)) {
                    final int[] data = quad.func_178209_a();
                    for (int n = 0; n < 4; n++) {
                        data[n * 8 + 4] = Float.floatToIntBits((float) Math.round(Float.intBitsToFloat(data[n * 8 + 4]) * w) / w);
                        data[n * 8 + 5] = Float.floatToIntBits((float) Math.round(Float.intBitsToFloat(data[n * 8 + 5]) * h) / h);
                    }
                }
            });
        });
        modBus.addListener(this::setup);
        modBus.addListener(this::setupColors);
        modBus.addListener(this::setupModels);
    }

    private void setup(final FMLClientSetupEvent event) {
        ClientRegistry.bindTileEntityRenderer(FLBlockEntities.FASTENER.get(), dispatcher -> new FastenerBlockEntityRenderer(dispatcher, ServerProxy.buildBlockView()));
        ClientRegistry.bindTileEntityRenderer(FLBlockEntities.LIGHT.get(), LightBlockEntityRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(FLEntities.FASTENER.get(), FenceFastenerRenderer::new);
        RenderTypeLookup.setRenderLayer(FLBlocks.FASTENER.get(), RenderType.func_228641_d_());
        /*final LightRenderer r = new LightRenderer();
        final StringBuilder bob = new StringBuilder();
        FLItems.lights().forEach(l -> {
            final LightModel<?> model = r.getModel(l.getBlock().getVariant(), -1);
            final AxisAlignedBB bb = model.getBounds();
            bob.append(String.format("%n%s new AxisAlignedBB(%.3fD, %.3fD, %.3fD, %.3fD, %.3fD, %.3fD), %.3fD", l.getRegistryName(), bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ, model.getFloorOffset()));
        });
        LogManager.getLogger().debug("waldo {}", bob);*/
    }

    private void setupModels(final ModelRegistryEvent event) {
        ModelLoader.addSpecialModel(FenceFastenerRenderer.MODEL);
        this.entityModels.forEach(ModelLoader::addSpecialModel);
    }

    private void setupColors(final ColorHandlerEvent.Item event) {
        final ItemColors colors = event.getItemColors();
        colors.func_199877_a((stack, index) -> {
            if (index == 1) {
                if (ColorChangingBehavior.exists(stack)) {
                    return ColorChangingBehavior.animate(stack);
                }
                return DyeableItem.getColor(stack);
            }
            return 0xFFFFFF;
        },
            FLItems.FAIRY_LIGHT.get(),
            FLItems.PAPER_LANTERN.get(),
            FLItems.ORB_LANTERN.get(),
            FLItems.FLOWER_LIGHT.get(),
            FLItems.CANDLE_LANTERN_LIGHT.get(),
            FLItems.OIL_LANTERN_LIGHT.get(),
            FLItems.JACK_O_LANTERN.get(),
            FLItems.SKULL_LIGHT.get(),
            FLItems.GHOST_LIGHT.get(),
            FLItems.SPIDER_LIGHT.get(),
            FLItems.WITCH_LIGHT.get(),
            FLItems.SNOWFLAKE_LIGHT.get(),
            FLItems.HEART_LIGHT.get(),
            FLItems.MOON_LIGHT.get(),
            FLItems.STAR_LIGHT.get(),
            FLItems.ICICLE_LIGHTS.get(),
            FLItems.METEOR_LIGHT.get()
        );
        colors.func_199877_a((stack, index) -> {
            final CompoundNBT tag = stack.func_77978_p();
            if (index == 0) {
                if (tag != null) {
                    return HangingLightsConnectionItem.getString(tag).getColor();
                }
                return StringTypes.BLACK_STRING.get().getColor();
            }
            if (tag != null) {
                final ListNBT tagList = tag.func_150295_c("pattern", NBT.TAG_COMPOUND);
                if (tagList.size() > 0) {
                    final ItemStack item = ItemStack.func_199557_a(tagList.func_150305_b((index - 1) % tagList.size()));
                    if (ColorChangingBehavior.exists(item)) {
                        return ColorChangingBehavior.animate(item);
                    }
                    return DyeableItem.getColor(item);
                }
            }
            if (FairyLights.CHRISTMAS.isOccurringNow()) {
                return (index + Util.func_211177_b() / 2000) % 2 == 0 ? 0x993333 : 0x7FCC19;
            }
            if (FairyLights.HALLOWEEN.isOccurringNow()) {
                return index % 2 == 0 ? 0xf9801d : 0x8932b8;
            }
            return 0xFFD584;
        }, FLItems.HANGING_LIGHTS.get());
        colors.func_199877_a((stack, index) -> index == 0 ? DyeableItem.getColor(stack) : 0xFFFFFFFF, FLItems.TINSEL.get());
        colors.func_199877_a((stack, index) -> {
            if (index == 0) {
                return 0xFFFFFFFF;
            }
            final CompoundNBT tag = stack.func_77978_p();
            if (tag != null) {
                final ListNBT tagList = tag.func_150295_c("pattern", NBT.TAG_COMPOUND);
                if (tagList.size() > 0) {
                    final ItemStack light = ItemStack.func_199557_a(tagList.func_150305_b((index - 1) % tagList.size()));
                    return DyeableItem.getColor(light);
                }
            }
            return 0xFFFFFFFF;
        }, FLItems.PENNANT_BUNTING.get());
        colors.func_199877_a(ClientProxy::secondLayerColor, FLItems.TRIANGLE_PENNANT.get());
        colors.func_199877_a(ClientProxy::secondLayerColor, FLItems.SPEARHEAD_PENNANT.get());
        colors.func_199877_a(ClientProxy::secondLayerColor, FLItems.SWALLOWTAIL_PENNANT.get());
        colors.func_199877_a(ClientProxy::secondLayerColor, FLItems.SQUARE_PENNANT.get());
        colors.func_199877_a((stack, index) -> {
            final CompoundNBT tag = stack.func_77978_p();
            if (index > 0 && tag != null) {
                final StyledString str = StyledString.deserialize(tag.func_74775_l("text"));
                if (str.length() > 0) {
                    TextFormatting lastColor = null, color = null;
                    int n = (index - 1) % str.length();
                    for (int i = 0; i < str.length(); lastColor = color, i++) {
                        color = str.styleAt(i).getColor();
                        if (lastColor != color && (n-- == 0)) {
                            break;
                        }
                    }
                    return StyledString.getColor(color) | 0xFF000000;
                }
            }
            return 0xFFFFFFFF;
        }, FLItems.LETTER_BUNTING.get());
    }

    private static int secondLayerColor(final ItemStack stack, final int index) {
        return index == 0 ? 0xFFFFFF : DyeableItem.getColor(stack);
    }
}
