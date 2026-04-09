package de.maxhenkel.voicechat.ios;
import com.sun.jna.*;
import com.sun.jna.ptr.PointerByReference;
import de.maxhenkel.voicechat.voice.client.MicrophoneException;
import de.maxhenkel.voicechat.voice.client.microphone.Microphone;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class AudioQueueMicrophone implements Microphone {
    public interface AudioToolbox extends Library {
        AudioToolbox INSTANCE = Native.load("AudioToolbox", AudioToolbox.class);
        int AudioQueueNewInput(Pointer inFormat, Callback inCallbackProc, Pointer inUserData,
            Pointer inCallbackRunLoop, Pointer inCallbackRunLoopMode, int inFlags, PointerByReference outAQ);
        int AudioQueueAllocateBuffer(Pointer inAQ, int inBufferByteSize, PointerByReference outBuffer);
        int AudioQueueEnqueueBuffer(Pointer inAQ, Pointer inBuffer, int inNumPacketDescs, Pointer inPacketDescs);
        int AudioQueueStart(Pointer inAQ, Pointer inStartTime);
        int AudioQueueStop(Pointer inAQ, byte inImmediate);
        int AudioQueueDispose(Pointer inAQ, byte inImmediate);
    }

    public interface AudioQueueInputCallback extends Callback {
        void invoke(Pointer userData, Pointer inAQ, Pointer inBuffer,
            Pointer inStartTime, int inNumPackets, Pointer inPacketDescs);
    }

    private static final int kAudioFormatLinearPCM = 0x6C70636D;
    private static final int PCM_FLAGS = 0x4 | 0x8;
    private static final int AQ_OK = 0;

    private final int sampleRate;
    private final int bufferSize;
    private final String deviceName;
    private Pointer audioQueue;
    private final Pointer[] aqBuffers = new Pointer[3];
    private volatile boolean open = false;
    private volatile boolean started = false;
    private final BlockingQueue<byte[]> pcmQueue = new ArrayBlockingQueue<>(32);
    private AudioQueueInputCallback callback;
    private int bytesPerBuffer;

    public AudioQueueMicrophone(int sampleRate, int bufferSize, String deviceName) {
        this.sampleRate = sampleRate;
        this.bufferSize = bufferSize;
        this.deviceName = deviceName;
        this.bytesPerBuffer = bufferSize * 2;
    }

    @Override
    public void open() {
        if (open) throw new RuntimeException("Microphone already open");
        try {
            AudioToolbox at = AudioToolbox.INSTANCE;
            Memory asbd = new Memory(40);
            asbd.setDouble(0, (double) sampleRate);
            asbd.setInt(8, kAudioFormatLinearPCM);
            asbd.setInt(12, PCM_FLAGS);
            asbd.setInt(16, 2);
            asbd.setInt(20, 1);
            asbd.setInt(24, 2);
            asbd.setInt(28, 1);
            asbd.setInt(32, 16);
            asbd.setInt(36, 0);
            callback = (userData, inAQ, inBuffer, inStartTime, inNumPackets, inPacketDescs) -> {
                try {
                    Pointer audioData = inBuffer.getPointer(8);
                    int byteSize = inBuffer.getInt(16);
                    if (byteSize > 0 && audioData != null) {
                        pcmQueue.offer(audioData.getByteArray(0, byteSize));
                    }
                    if (started) at.AudioQueueEnqueueBuffer(inAQ, inBuffer, 0, null);
                } catch (Exception ignored) {}
            };
            PointerByReference queueRef = new PointerByReference();
            int err = at.AudioQueueNewInput(asbd, callback, null, null, null, 0, queueRef);
            if (err != AQ_OK) throw new RuntimeException("AudioQueueNewInput failed: " + err);
            audioQueue = queueRef.getValue();
            for (int i = 0; i < 3; i++) {
                PointerByReference bufRef = new PointerByReference();
                err = at.AudioQueueAllocateBuffer(audioQueue, bytesPerBuffer, bufRef);
                if (err != AQ_OK) throw new RuntimeException("AudioQueueAllocateBuffer failed: " + err);
                aqBuffers[i] = bufRef.getValue();
                at.AudioQueueEnqueueBuffer(audioQueue, aqBuffers[i], 0, null);
            }
            open = true;
        } catch (Exception e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException("Failed to open iOS microphone: " + t.getMessage());
        }
    }

    @Override
    public void start() {
        if (!open) throw new RuntimeException("Microphone is not open");
        started = true;
        int err = AudioToolbox.INSTANCE.AudioQueueStart(audioQueue, null);
        if (err != AQ_OK) { started = false; throw new RuntimeException("AudioQueueStart failed: " + err); }
    }

    @Override
    public void stop() {
        if (!started) return;
        started = false;
        try { AudioToolbox.INSTANCE.AudioQueueStop(audioQueue, (byte) 0); } catch (Throwable ignored) {}
    }

    @Override
    public void close() {
        stop(); open = false; pcmQueue.clear();
        if (audioQueue != null) {
            try { AudioToolbox.INSTANCE.AudioQueueDispose(audioQueue, (byte) 1); } catch (Throwable ignored) {}
            audioQueue = null;
        }
    }

    @Override public boolean isOpen() { return open; }
    @Override public boolean isStarted() { return started; }
    @Override public int available() { return pcmQueue.size() * bufferSize; }

    @Override
    public short[] read()  {
        if (!open) throw new RuntimeException("Microphone was not opened");
        try {
            byte[] bytes = pcmQueue.poll(100, TimeUnit.MILLISECONDS);
            if (bytes == null) return new short[0];
            short[] shorts = new short[bytes.length / 2];
            for (int i = 0; i < shorts.length; i++)
                shorts[i] = (short) ((bytes[i*2] & 0xFF) | ((bytes[i*2+1] & 0xFF) << 8));
            return shorts;
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); return new short[0]; }
    }

    public static List<String> getDeviceNames() { return Collections.singletonList("Default"); }

    public static boolean isAvailable() {
        try { AudioToolbox.INSTANCE.getClass(); return true; } catch (Throwable t) { return false; }
    }
}
