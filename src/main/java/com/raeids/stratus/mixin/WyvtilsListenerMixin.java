package com.raeids.stratus.mixin;

import com.raeids.stratus.utils.RenderHelper;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.wyvest.wyvtils.core.listener.Listener")
public class WyvtilsListenerMixin {

    @Dynamic("Wyvtils")
    @Inject(method = "onStringRendered", at = @At("HEAD"), cancellable = true)
    private void cancelStringRender(@Coerce Object a, CallbackInfo ci) {
        if (RenderHelper.INSTANCE.getBypassWyvtils()) ci.cancel();
    }
}