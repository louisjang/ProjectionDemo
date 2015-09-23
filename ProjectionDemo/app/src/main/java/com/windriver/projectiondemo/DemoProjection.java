/*
 * Copyright (c) 2015 Wind River Systems, Inc.
 *
 * The right to copy, distribute, modify, or otherwise make use
 * of this software may be licensed only pursuant to the terms
 * of an applicable Wind River license agreement.
 */

package com.windriver.projectiondemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.Surface;

import java.io.BufferedOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

class DemoProjection {
    private static final String TAG = "PresentationTest";

    private Context mContext;

    // AOA
    private UsbManager mUsbManager;
    private InputStream mAccIn;
    private OutputStream mAccOut;
    private Thread mAccReaderThread;
    private ParcelFileDescriptor mParcelFd;
    volatile boolean mAccReaderThreadRunning;

    //
    // Presentation
    //
    private DemoPresentation mPresentation;
    private MediaCodec mEncoder;

    final int WIDTH = 800;
    final int HEIGHT = 480;
    final int DENSITY = 150;

    private boolean mRunning;

    Thread mEncodingThread = new Thread(new Runnable() {
        @Override
        public void run() {
            boolean formatAcquired = false;
            MediaCodec.BufferInfo bufInfo = new MediaCodec.BufferInfo();
            while (mRunning) {
                int index = mEncoder.dequeueOutputBuffer(bufInfo, 100*1000);
                if (index >= 0) {
                    if (formatAcquired) {
                        ByteBuffer outputBuffer = mEncoder.getOutputBuffer(index);
                        byte[] outData = new byte[bufInfo.size];
                        outputBuffer.get(outData);
                        //Log.v(TAG, "encoded byte = " + Util.dump(outData, -1));
                        try {
                            mAccOut.write(((outData.length) >> 8) & 0xFF);
                            mAccOut.write(((outData.length)) & 0xFF);
                            mAccOut.write(outData);
                            mAccOut.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    mEncoder.releaseOutputBuffer(index, true);
                } else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    formatAcquired = true;
                    MediaFormat newFormat = mEncoder.getOutputFormat();
                    Log.d(TAG, "encoder output format changed: " + newFormat);
                }
            }
        }
    });

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.v(TAG, "onReceive: " + action);
            if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                closeAccessory();
            }
        }
    };

    public DemoProjection(Context ctx) {
        mContext = ctx;

        mUsbManager = (UsbManager) ctx.getSystemService(Context.USB_SERVICE);
        IntentFilter filter = new IntentFilter();
        //filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        ctx.registerReceiver(mUsbReceiver, filter);

        UsbAccessory[] accessories = mUsbManager.getAccessoryList();
        UsbAccessory accessory = (accessories != null) ? accessories[0] : null;
        if (accessory != null) {
            if (mUsbManager.hasPermission(accessory)) {
                openAccessory(accessory);
            }
        }
    }

    private void openAccessory(UsbAccessory accessory) {
        mParcelFd = mUsbManager.openAccessory(accessory);
        FileDescriptor fd = mParcelFd.getFileDescriptor();
        mAccIn = new FileInputStream(fd);
        mAccOut = new BufferedOutputStream(new FileOutputStream(fd));

        /* not needed in the demo
        mAccReaderThread = new Thread() {
            @Override
            public void run() {
                byte[] buf = new byte[4096];
                while (mAccReaderThreadRunning) {
                    try {
                        int n = mAccIn.read(buf);
                        Log.v(TAG, "Read: " + Util.dump(buf, n));
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        };
        mAccReaderThreadRunning = true;
        mAccReaderThread.start();
        */
        startPresentation();
    }

    private void closeAccessory() {
        stopPresentation();

        try {
            mAccIn.close();
            mAccOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mAccIn = null;
        mAccOut = null;
        mAccReaderThreadRunning = false;
        mAccReaderThread = null;
    }

    void startPresentation() {
        mRunning = true;
        mEncoder = null;

        try {
            mEncoder = MediaCodec.createEncoderByType("video/avc");
        } catch (IOException e) {
            e.printStackTrace();
        }
        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", WIDTH, HEIGHT);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 1250000);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);

        mEncoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        Surface surface = mEncoder.createInputSurface();
        mEncoder.start();

        DisplayManager dm = (DisplayManager) mContext.getSystemService(Context.DISPLAY_SERVICE);
        VirtualDisplay vd = dm.createVirtualDisplay("PresentationTest", WIDTH, HEIGHT, DENSITY,
                surface, DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION, null, null);

        mPresentation = new DemoPresentation(mContext, vd.getDisplay());
        mPresentation.show();

        mEncodingThread.start();
    }

    void stopPresentation() {
        mRunning = false;
    }
}