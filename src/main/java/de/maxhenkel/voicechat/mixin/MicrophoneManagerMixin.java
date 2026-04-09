package de.maxhenkel.voicechat.mixin;

import de.maxhenkel.voicechat.ios.AudioQueueMicrophone;
import de.maxhenkel.voicechat.voice.client.microphone.Microphone;
import de.maxhenkel.voicechat.voice.client.microphone.MicrophoneManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MicrophoneManager.class)
public class MicrophoneManagerMixin {

    @Inject(method = "createMicrophone", at = @At("HEAD"), cancellable = true)
    private static void injectIOSMicrophone(CallbackInfoReturnable<Microphone> cir) {
        try {

            if (!AudioQueueMicrophone.isAvailable()) {
                return;
            }

            int sampleRate = 48000;
            int bufferSize = 960;

            AudioQueueMicrophone mic =
                    new AudioQueueMicrophone(sampleRate, bufferSize, null);

            mic.open();
            mic.start();

            cir.setReturnValue(mic);
            cir.cancel();

            System.out.println("[VoiceChat iOS] Using AudioQueue microphone");

        } catch (Throwable t) {
            System.err.println("[VoiceChat iOS] Failed to init microphone");
            t.printStackTrace();
        }
    }
}