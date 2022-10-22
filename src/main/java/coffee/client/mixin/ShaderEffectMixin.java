/*
 * Copyright (c) 2022 Coffee Client, 0x150 and contributors.
 * Some rights reserved, refer to LICENSE file.
 */

package coffee.client.mixin;

import coffee.client.mixinUtil.ShaderEffectDuck;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostProcessShader;
import net.minecraft.client.gl.ShaderEffect;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(ShaderEffect.class)
public class ShaderEffectMixin implements ShaderEffectDuck {

    private final List<String> fakedBufferNames = new ArrayList<>();
    @Shadow
    @Final
    private Map<String, Framebuffer> targetsByName;
    @Shadow
    @Final
    private List<PostProcessShader> passes;

    @Override
    public void addFakeTarget(String name, Framebuffer buffer) {
        Framebuffer previousFramebuffer = this.targetsByName.get(name);
        if (previousFramebuffer == buffer) {
            return; // no need to do anything
        }
        if (previousFramebuffer != null) {
            for (PostProcessShader pass : this.passes) {
                // replace input and output of each pass to our new framebuffer, if they reference the one we're replacing
                if (pass.input == previousFramebuffer) {
                    ((PostProcessShaderMixin) pass).renderer_setInput(buffer);
                }
                if (pass.output == previousFramebuffer) {
                    ((PostProcessShaderMixin) pass).renderer_setOutput(buffer);
                }
            }
            this.targetsByName.remove(name);
            this.fakedBufferNames.remove(name);
        }

        this.targetsByName.put(name, buffer);
        this.fakedBufferNames.add(name);
    }

    @Inject(method = "close", at = @At("HEAD"))
    void renderer_deleteFakeBuffers(CallbackInfo ci) {
        for (String fakedBufferName : fakedBufferNames) {
            targetsByName.remove(fakedBufferName); // remove without closing
        }
    }
}
