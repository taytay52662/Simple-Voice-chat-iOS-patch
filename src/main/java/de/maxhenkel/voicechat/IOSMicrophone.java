package your.package;

import de.maxhenkel.voicechat.voice.client.microphone.Microphone;

public class IOSMicrophone implements Microphone {

    public IOSMicrophone() {
        System.out.println("[VoiceChat iOS] IOSMicrophone created");
    }

    @Override
    public void start() {
        System.out.println("[VoiceChat iOS] start()");
    }

    @Override
    public void stop() {
        System.out.println("[VoiceChat iOS] stop()");
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public void close() {
        System.out.println("[VoiceChat iOS] close()");
    }

    @Override
    public int getSampleRate() {
        return 48000;
    }

    @Override
    public int getChannels() {
        return 1;
    }

    @Override
    public void read(short[] buffer, int offset, int length) {
        // Fill with silence for now so it doesn't crash
        for (int i = offset; i < offset + length; i++) {
            buffer[i] = 0;
        }
    }
}