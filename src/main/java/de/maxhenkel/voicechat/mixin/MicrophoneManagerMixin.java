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
    private Microphone redirectMic(int sampleRate, int bufferSize, String device) {
        return new AudioQueueMicrophone(sampleRate, bufferSize, device);
    }
}