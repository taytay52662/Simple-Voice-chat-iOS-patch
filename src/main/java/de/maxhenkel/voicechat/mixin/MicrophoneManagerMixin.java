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
    private void injectIOSMicrophone(CallbackInfoReturnable<Microphone> cir) throws MicrophoneException {
        if (!AudioQueueMicrophone.isAvailable()) return;
        String deviceName = null;
        try {
            Object config = Class.forName("de.maxhenkel.voicechat.VoicechatClient").getField("CLIENT_CONFIG").get(null);
            Object entry = config.getClass().getField("microphone").get(config);
            Object value = entry.getClass().getMethod("get").invoke(entry);
            if (value instanceof String s && !s.isBlank()) deviceName = s;
        } catch (Throwable ignored) {}
        int sampleRate = 48000;
        int bufferSize = 960;
        try {
            MicrophoneManager self = (MicrophoneManager)(Object) this;
            Field srField = self.getClass().getDeclaredField("sampleRate");
            srField.setAccessible(true);
            sampleRate = srField.getInt(self);
            Field bsField = self.getClass().getDeclaredField("bufferSize");
            bsField.setAccessible(true);
            bufferSize = bsField.getInt(self);
        } catch (Throwable ignored) {}
        AudioQueueMicrophone mic = new AudioQueueMicrophone(sampleRate, bufferSize, deviceName);
        mic.open();
        cir.setReturnValue(mic);
    }
}
