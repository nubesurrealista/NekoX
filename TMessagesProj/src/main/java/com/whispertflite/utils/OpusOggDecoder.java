package com.whispertflite.utils;

import org.gagravarr.ogg.OggFile;
import org.gagravarr.opus.OpusAudioData;
import org.gagravarr.opus.OpusFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import io.github.jaredmdobson.concentus.OpusDecoder;
import io.github.jaredmdobson.concentus.OpusException;

public class OpusOggDecoder {

    // credit: https://www.codingnote.cc/zh-hk/p/362523/
    public static float[] decodeOpusOggToFloatArray(File file) throws IOException, OpusException {
        List<Byte> allPcm = new ArrayList<>();

        FileInputStream inputStream = new FileInputStream(file);
        OggFile ogg = new OggFile(inputStream);
        OpusFile of = new OpusFile(ogg);
        OpusDecoder decoder = new OpusDecoder(of.getInfo().getSampleRate(), of.getInfo().getNumChannels());

        byte[] data_packet = new byte[of.getInfo().getSampleRate()];
        OpusAudioData ad;

        while ((ad = of.getNextAudioPacket()) != null) {
            // NOTE: samplesDecoded = amount of "short"s decoded; 1 short = 2 byte -> *= 2
            int samplesDecoded =
                    decoder.decode(ad.getData(), 0, ad.getData().length,
                            data_packet, 0, of.getInfo().getSampleRate(),
                            false);

            for (int i = 0; i < samplesDecoded * 2; ++i) {
                allPcm.add(data_packet[i]);
            }
        }

        inputStream.close();

        return convert48kListTo16kFloat(allPcm);
    }

    // Downsamples 48kHz to 16kHz using a very basic 3-tap moving average filter.
    public static byte[] resample48kTo16k(byte[] input) {
        // Convert the byte array into a short array.
        int numSamples = input.length / 2;
        short[] samples = new short[numSamples];
        ByteBuffer.wrap(input).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples);

        // simple moving average filter to smooth out high frequencies
        for (int i = 1; i < numSamples - 1; i++) {
            int sum = samples[i - 1] + samples[i] + samples[i + 1];
            samples[i] = (short) (sum / 3);
        }

        // store processed data
        int newSampleCount = numSamples / 3;
        short[] outSamples = new short[newSampleCount];
        for (int i = 0, j = 0; i < numSamples && j < newSampleCount; i += 3, j++) {
            outSamples[j] = samples[i];
        }

        // stuff in short array back into a byte array (little-endian).
        ByteBuffer outBuffer = ByteBuffer.allocate(newSampleCount * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (short s : outSamples) {
            outBuffer.putShort(s);
        }

        return outBuffer.array();
    }

    // Converts a List<Byte> of 48kHz PCM data into a float[] of 16kHz PCM data.
    public static float[] convert48kListTo16kFloat(List<Byte> inputList) {
        // Convert List<Byte> to byte[]
        byte[] inputData = new byte[inputList.size()];
        for (int i = 0; i < inputList.size(); i++) {
            inputData[i] = inputList.get(i);
        }

        // Downsample from 48kHz to 16kHz
        byte[] downsampledData = resample48kTo16k(inputData);

        // Convert 16kHz PCM (16-bit) into float samples in the range [-1, 1]

        return getSamplesAsNormalizedFloats(downsampledData);
    }

    public static float[] getSamplesAsNormalizedFloats(byte[] input) {

        int numSamples = input.length / 2;
        ByteBuffer byteBuffer = ByteBuffer.wrap(input);
        byteBuffer.order(ByteOrder.nativeOrder());

        // Convert audio data to PCM_FLOAT format
        float[] samples = new float[numSamples];
        float maxAbsValue = 0.0f;

        for (int i = 0; i < numSamples; i++) {
            samples[i] = (float) (byteBuffer.getShort() / 32768.0);
            // Track the maximum absolute value
            if (Math.abs(samples[i]) > maxAbsValue) {
                maxAbsValue = Math.abs(samples[i]);
            }
        }

        // Normalize the samples
        if (maxAbsValue > 0.0f) {
            for (int i = 0; i < numSamples; i++) {
                samples[i] /= maxAbsValue;
            }
        }

        return samples;

    }
}