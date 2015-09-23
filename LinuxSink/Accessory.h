/*
 * Copyright (c) 2015 Wind River Systems, Inc.
 *
 * The right to copy, distribute, modify, or otherwise make use
 * of this software may be licensed only pursuant to the terms
 * of an applicable Wind River license agreement.
 */

#ifndef LINUXSINK_ACCESSORY_H
#define LINUXSINK_ACCESSORY_H


#include <usb.h>

class Accessory {
public:
    bool init(int vid, int pid);
    void deinit();
    int readUsb(unsigned char *buf, int buf_size);

private:
    struct libusb_device_handle *mHandle;
    int mVid;
    int mPid;

    void printError(int code);

};


#endif //LINUXSINK_ACCESSORY_H
