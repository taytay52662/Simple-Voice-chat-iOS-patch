package de.maxhenkel.voicechat.mixin;

import de.maxhenkel.voicechat.ios.AudioQueueMicrophone;
import de.maxhenkel.voicechat.voice.client.MicrophoneException;
import de.maxhenkel.voicechat.voice.client.microphone.Microphone;
import de.maxhenkel.voicechat.voice.client.microphone.MicrophoneManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = MicrophoneManager.class, remap = false)
public class MicrophoneManagerMixin {
@Overwrite(remap = false)
public static Microphone createMicrophone() throws MicrophoneException {
    AudioQueueMicrophone mic = new AudioQueueMicrophone(48000, 960, null);
    mic.open();
    return mic;
}
}