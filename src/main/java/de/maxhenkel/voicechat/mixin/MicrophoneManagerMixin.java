package de.maxhenkel.voicechat.mixin;
import de.maxhenkel.voicechat.ios.AudioQueueMicrophone;
import de.maxhenkel.voicechat.voice.client.MicrophoneException;
import de.maxhenkel.voicechat.voice.client.microphone.Microphone;
import de.maxhenkel.voicechat.voice.client.microphone.MicrophoneManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.lang.reflect.Field;

@Mixin(value = MicrophoneManager.class, remap = false)
public class MicrophoneManagerMixin {
    @Inject(method = "createMicrophone", at = @At("HEAD"), cancellable = true, remap = false)
    private static void injectIOSMicrophone(CallbackInfoReturnable<Microphone> cir) throws MicrophoneException {
        if (!AudioQueueMicrophone.isAvailable()) return;
        String deviceName = null;
        
        int sampleRate = 48000;
        int bufferSize = 960;
        
        AudioQueueMicrophone mic = new AudioQueueMicrophone(sampleRate, bufferSize, deviceName);
        mic.open();
        cir.setReturnValue(mic);
    }
}
