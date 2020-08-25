package hu.jard.luminor.mixin;

import hu.jard.luminor.Luminor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkToNibbleArrayMap;
import net.minecraft.world.chunk.light.ChunkLightProvider;
import net.minecraft.world.chunk.light.ChunkLightingView;
import net.minecraft.world.chunk.light.LevelPropagator;
import net.minecraft.world.chunk.light.LightStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin (ChunkLightProvider.class)
public abstract class ChunkLightProviderMixin<M extends ChunkToNibbleArrayMap<M>, S extends LightStorage<M>> extends LevelPropagator implements ChunkLightingView {
    protected ChunkLightProviderMixin(int levelCount, int expectedLevelSize, int expectedTotalSize) {
        super (levelCount, expectedLevelSize, expectedTotalSize);
    }

    /**
     * Increases the block light level count parameter in the constructor of ChunkLightProvider to 128.
     *
     * @reason Set up the lighting engine for proper light attenuation
     */
    @ModifyArg (method="<init>", at=@At (value="INVOKE", target="Lnet/minecraft/world/chunk/light/LevelPropagator;<init>(III)V"), index=0)
    private static int increaseSubLightLevels (int levels) {
        if (Luminor.MAX_LIGHT_LEVEL > 254)
            throw new IllegalArgumentException ("Sublevel count must be < 254. This is usually because the subdivision count is > 15.");

        return Luminor.MAX_LIGHT_LEVEL;
    }

    /**
     * Scale the outputted mutable int when determining the block state for internal lighting.
     *
     * @reason Set up the lighting engine for proper light attenuation
     */
    @ModifyArg (method="getStateForLighting", at=@At (value="INVOKE", target="Lorg/apache/commons/lang3/mutable/MutableInt;setValue(I)V"), index=0)
    private int increaseMutableIntReturn (int in) {
        System.out.println (in * Luminor.LIGHT_SUBDIVS);
        // Scale the default block light level by the number of sublevels
        return in * Luminor.LIGHT_SUBDIVS;
    }

    /**
     * Change ChunkLightProvider#getLevel() to subtract from the highest sub-level of 127 instead of the hard-coded value of 15.
     *
     * @reason Set up the lighting engine for proper light attenuation
     */
    @Inject (at = @At("HEAD"), method = "getLevel(J)I", cancellable = true)
    protected void getLevel (long id, CallbackInfoReturnable<Integer> info) {
        info.cancel ();
        info.setReturnValue (id == Long.MAX_VALUE ? 0 : Luminor.MAX_LIGHT_LEVEL - this.lightStorage.get (id));
    }

    /**
     * Change ChunkLightProvider#setLevel() to use the highest sub-level of 127 instead of the hard-coded value of 15.
     *
     * @reason Set up the lighting engine for proper light attenuation
     */
    @Inject (at = @At("HEAD"), method = "setLevel(JI)V", cancellable = true)
    protected void setLevel (long id, int level, CallbackInfo info) {
        info.cancel ();

        this.lightStorage.set(id, Math.min(Luminor.MAX_LIGHT_LEVEL, Luminor.MAX_LIGHT_LEVEL - level));
    }

    /**
     * Change ChunkLightProvider#getCurrentLevelFromArray() to use the highest sub-level of 127 instead of the hard-coded value of 15.
     *
     * @reason Set up the lighting engine for proper light attenuation
     */
    @Inject (at = @At("HEAD"), method = "getCurrentLevelFromArray(Lnet/minecraft/world/chunk/ChunkNibbleArray;J)I", cancellable = true)
    protected void getCurrentLevelFromArray(ChunkNibbleArray array, long blockPos, CallbackInfoReturnable <Integer> info) {
        info.cancel ();

        int ret = Luminor.MAX_LIGHT_LEVEL - array.get (ChunkSectionPos.getLocalCoord (BlockPos.unpackLongX (blockPos)),
                                                       ChunkSectionPos.getLocalCoord (BlockPos.unpackLongY (blockPos)),
                                                       ChunkSectionPos.getLocalCoord (BlockPos.unpackLongZ (blockPos)));

        info.setReturnValue (ret);
    }


    /**
     * ChunkLightingView is the outwards-facing interface of the lighting engine, which is used by the lighting provider
     * to actually determine the light level at specific blocks for rendering or gameplay purposes.
     *
     * Since our internal lighting levels are on a 0-127 scale, we need to scale it back down to 0-15 through integral
     * division by 8.
     *
     * @reason Scale internal lighting back down to 0-15 scale
     */
    @Inject (at = @At("HEAD"), method = "getLightLevel(Lnet/minecraft/util/math/BlockPos;)I", cancellable = true)
    public void getLightLevel(BlockPos blockPos, CallbackInfoReturnable<Integer> info) {
        info.cancel ();

        int internalLightLevel = this.lightStorage.getLight (blockPos.asLong());

        info.setReturnValue (internalLightLevel / Luminor.LIGHT_SUBDIVS);
    }

    @Shadow
    @Final
    protected S lightStorage;
}
