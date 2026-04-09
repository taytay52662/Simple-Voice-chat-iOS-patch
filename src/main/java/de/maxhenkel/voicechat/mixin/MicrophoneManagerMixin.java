package de.maxhenkel.voicechat.mixin;

import de.maxhenkel.voicechat.ios.AudioQueueMicrophone;
import de.maxhenkel.voicechat.voice.client.microphone.Microphone;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "de.maxhenkel.voicechat.voice.client.microphone.MicrophoneManager")
public class MicrophoneManagerMixin {

    @Inject(method = "createMicrophone", at = @At("HEAD"), cancellable = true)
    private static void injectIOSMicrophone(CallbackInfoReturnable<Microphone> cir) {
        try {
            if (!AudioQueueMicrophone.isAvailable()) return;

            AudioQueueMicrophone mic =
                    new AudioQueueMicrophone(48000, 960, null);

            mic.open();
            mic.start();

            cir.setReturnValue(mic);
            cir.cancel();

        } catch (Throwable ignored) {}
    }
}