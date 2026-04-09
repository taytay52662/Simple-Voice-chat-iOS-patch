package de.maxhenkel.voicechat.mixin;
import de.maxhenkel.voicechat.ios.AudioQueueMicrophone;
import de.maxhenkel.voicechat.voice.client.microphone.MicrophoneManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

@Mixin(value = MicrophoneManager.class, remap = false)
public class MicrophoneListMixin {
    @Inject(method = "getAllMicrophones", at = @At("HEAD"), cancellable = true, remap = false)
    private void injectIOSMicList(CallbackInfoReturnable<List<String>> cir) {
        if (!AudioQueueMicrophone.isAvailable()) return;
        cir.setReturnValue(AudioQueueMicrophone.getDeviceNames());
    }
}
