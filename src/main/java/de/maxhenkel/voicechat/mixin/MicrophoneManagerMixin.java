package de.maxhenkel.voicechat.mixin;

import de.maxhenkel.voicechat.ios.AudioQueueMicrophone;
import de.maxhenkel.voicechat.voice.client.microphone.Microphone;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "de.maxhenkel.voicechat.voice.client.microphone.MicrophoneManager")
public class MicrophoneManagerMixin {

    @Redirect(
        method = "*",
        at = @At(
            value = "NEW",
            target = "de/maxhenkel/voicechat/voice/client/microphone/OpenALMicrophone"
        )
    )
    private Microphone redirectMicrophone(int sampleRate, int bufferSize, String device) {
        try {
            AudioQueueMicrophone mic =
                new AudioQueueMicrophone(48000, 960, null);

            mic.open();
            mic.start();

            return mic;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}