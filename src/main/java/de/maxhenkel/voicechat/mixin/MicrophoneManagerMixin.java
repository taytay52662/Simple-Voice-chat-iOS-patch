import de.maxhenkel.voicechat.ios.AudioQueueMicrophone;
import de.maxhenkel.voicechat.voice.client.microphone.Microphone;
import de.maxhenkel.voicechat.voice.client.microphone.MicrophoneManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MicrophoneManager.class, remap = false)
public class MicrophoneManagerMixin {

    @Inject(method = "createMicrophone", at = @At("HEAD"), cancellable = true, remap = false)
    private static void injectIOSMicrophone(CallbackInfoReturnable<Microphone> cir) {
        try {
            if (!AudioQueueMicrophone.isAvailable()) {
                return;
            }

            AudioQueueMicrophone mic =
                    new AudioQueueMicrophone(48000, 960, null);

            mic.open();
            mic.start();

            cir.setReturnValue(mic);
            cir.cancel();

        } catch (Throwable ignored) {
        }
    }
}