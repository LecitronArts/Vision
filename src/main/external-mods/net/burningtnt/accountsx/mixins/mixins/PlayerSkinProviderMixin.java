package net.burningtnt.accountsx.mixins.mixins;

import com.mojang.authlib.minecraft.MinecraftSessionService;
import net.burningtnt.accountsx.utils.PlayerSkinProviderAccessor;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.client.texture.PlayerSkinProvider;
import net.minecraft.client.texture.TextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.concurrent.Executor;

@Mixin(SkinManager.class)
public class PlayerSkinProviderMixin implements PlayerSkinProviderAccessor {
    @Unique
    private Path accountsx$directory;

    @Unique
    private Executor accountsx$executor;

    @Inject(
            method = "<init>",
            at = @At("TAIL")
    )
    private void accountsx$init(TextureManager textureManager, Path directory, final MinecraftSessionService sessionService, final Executor executor, CallbackInfo ci) {
        accountsx$directory = directory;
        accountsx$executor = executor;
    }

    @Unique
    public Path accountx$getDirectory() {
        return accountsx$directory;
    }

    public Executor accountx$getExecutor() {
        return accountsx$executor;
    }
}
