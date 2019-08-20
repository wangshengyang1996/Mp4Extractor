package com.wsy.mp4extractor;

import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

public class Mp4Decoder {

    private static final String TAG = "Mp4Decoder";

    private static final long DEFAULT_TIMEOUT_US = 10000;
    private static final int decodeColorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;

    private MediaExtractor extractor = null;
    private MediaCodec decoder = null;
    private MediaFormat mediaFormat = null;

    public void init(String mp4Path) throws IOException {


        extractor = new MediaExtractor();
        extractor.setDataSource(mp4Path);
        int trackIndex = selectTrack(extractor);
        if (trackIndex < 0) {
            throw new RuntimeException("decode failed for file " + mp4Path);
        }
        extractor.selectTrack(trackIndex);
        mediaFormat = extractor.getTrackFormat(trackIndex);
        String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
        decoder = MediaCodec.createDecoderByType(mime);
        showSupportedColorFormat(decoder.getCodecInfo().getCapabilitiesForType(mime));
        if (isColorFormatSupported(decodeColorFormat, decoder.getCodecInfo().getCapabilitiesForType(mime))) {
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, decodeColorFormat);
        } else {
            throw new IllegalArgumentException("unable to set decode color format");
        }

    }

    public void videoDecode() {
        decodeFramesToYUV(decoder, extractor, mediaFormat);
    }

    public void release() {
        if (decoder != null) {
            decoder.stop();
            decoder.release();
            decoder = null;
        }
        if (extractor != null) {
            extractor.release();
            extractor = null;
        }
    }

    private void showSupportedColorFormat(MediaCodecInfo.CodecCapabilities caps) {
        Log.i(TAG, "showSupportedColorFormat: ");
        for (int c : caps.colorFormats) {
            Log.i(TAG, "showSupportedColorFormat: " + c);
        }
    }

    private boolean isColorFormatSupported(int colorFormat, MediaCodecInfo.CodecCapabilities caps) {
        for (int c : caps.colorFormats) {
            if (c == colorFormat) {
                return true;
            }
        }
        return false;
    }

    private void decodeFramesToYUV(MediaCodec decoder, MediaExtractor extractor, MediaFormat mediaFormat) {
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean inputFinished = false;
        boolean outputFinished = false;
        decoder.configure(mediaFormat, null, null, 0);
        decoder.start();
        while (!outputFinished) {
            if (!inputFinished) {
                int inputBufferId = decoder.dequeueInputBuffer(DEFAULT_TIMEOUT_US);
                if (inputBufferId >= 0) {
                    ByteBuffer inputBuffer = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        inputBuffer = decoder.getInputBuffer(inputBufferId);
                    } else {
                        inputBuffer = decoder.getInputBuffers()[inputBufferId];
                    }
                    int sampleSize = extractor.readSampleData(inputBuffer, 0);
                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(inputBufferId, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputFinished = true;
                    } else {
                        long presentationTimeUs = extractor.getSampleTime();
                        decoder.queueInputBuffer(inputBufferId, 0, sampleSize, presentationTimeUs, 0);
                        extractor.advance();
                    }
                }
            }
            int outputBufferId = decoder.dequeueOutputBuffer(info, DEFAULT_TIMEOUT_US);
            if (outputBufferId >= 0) {
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    outputFinished = true;
                }
                if (info.size > 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        Image image = decoder.getOutputImage(outputBufferId);
                        Log.i(TAG, "decodeFramesToYUV: " + image.getWidth() + "x" + image.getHeight() + "   " + image.getFormat() + "  " + image.getPlanes().length + "  " + image.getPlanes()[0].getBuffer().capacity() + "  " + image.getPlanes()[1].getBuffer().capacity() + "  " + image.getPlanes()[2].getBuffer().capacity());
                        image.close();
                    } else {
                        ByteBuffer outputBuffer = decoder.getOutputBuffers()[outputBufferId];
                        Log.i(TAG, "decodeFramesToYUV: " + outputBuffer);
                    }
                    decoder.releaseOutputBuffer(outputBufferId, false);
                }
            }
        }
    }

    private int selectTrack(MediaExtractor extractor) {
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                return i;
            }
        }
        return -1;
    }

}
