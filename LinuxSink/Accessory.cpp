/*
 * Copyright (c) 2015 Wind River Systems, Inc.
 *
 * The right to copy, distribute, modify, or otherwise make use
 * of this software may be licensed only pursuant to the terms
 * of an applicable Wind River license agreement.
 */

#include "Accessory.h"

#include <stdio.h>
#include <usb.h>
#include <libusb.h>
#include <string.h>
#include <unistd.h>

#define IN 0x81
#define OUT 0x02

#define ACCESSORY_PID 0x2D01
#define ACCESSORY_PID_ALT 0x2D00

const char *MANUFACTURER = "WindRiver";
const char *MODEL = "ProjectionDemo";
const char *DESCRIPTION = "Description";
const char *VERSION = "1.0";
const char *URI = "";
const char *SERIAL = "12345";

void Accessory::deinit(){
    if(mHandle != NULL) {
        libusb_release_interface(mHandle, 0);
        mHandle = NULL;
    }
    libusb_exit(NULL);
}

bool Accessory::init(int vid, int pid) {
    mVid = vid;
    mPid = pid;

    libusb_init(NULL);
    if((mHandle = libusb_open_device_with_vid_pid(NULL, mVid, mPid)) == NULL){
        fprintf(stdout, "Problem acquireing mHandle\n");
        return false;
    }
    libusb_claim_interface(mHandle, 0);

    unsigned char aoaVer[2];
    int r;

#define CHECK(x) r = x; if (r < 0) { printError(r); return false; }

    CHECK(libusb_control_transfer(mHandle, 0xC0, 51, 0, 0, aoaVer, 2, 0));
    printf("AOAP version = %d.%d\n", (int)aoaVer[0], (int)aoaVer[1]);

    usleep(1000);

    // set info
    CHECK(libusb_control_transfer(mHandle, 0x40, 52, 0, 0, (unsigned char*)MANUFACTURER,strlen(MANUFACTURER)+1,0));
    CHECK(libusb_control_transfer(mHandle, 0x40, 52, 0, 1, (unsigned char*)MODEL,strlen(MODEL)+1,0));
    CHECK(libusb_control_transfer(mHandle, 0x40, 52, 0, 2, (unsigned char*)DESCRIPTION,strlen(DESCRIPTION)+1,0));
    CHECK(libusb_control_transfer(mHandle, 0x40, 52, 0, 3, (unsigned char*)VERSION,strlen(VERSION)+1,0));
    CHECK(libusb_control_transfer(mHandle, 0x40, 52, 0, 4, (unsigned char*)URI,strlen(URI)+1,0));
    CHECK(libusb_control_transfer(mHandle, 0x40, 52, 0, 5, (unsigned char*)SERIAL,strlen(SERIAL)+1,0));

    // put device into accessory mode
    CHECK(libusb_control_transfer(mHandle,0x40,53,0,0,NULL,0,0));

    libusb_release_interface (mHandle, 0);

    mHandle = NULL;
    const int ntries = 5;
    for (int i = 0; i < ntries; i++) {
        if((mHandle = libusb_open_device_with_vid_pid(NULL, mVid, ACCESSORY_PID)) != NULL){
            break;
        }
        sleep(1);
    }
    if (!mHandle) {
        fprintf(stderr, "Can't connect device in accessory mode\n");
        return false;
    }

    libusb_claim_interface(mHandle, 0);
    return true;
}

void Accessory::printError(int code){
    fprintf(stderr, "libusb error: code = %d\n", code);
}

const int BUF_SZ = 4096*10;
int Accessory::readUsb(unsigned char *buf, int buf_size) {
    static unsigned char b[BUF_SZ];
    static int offset = 0;
    static int avail = 0;

    int left = buf_size;
    unsigned char *p = buf;

    while (left > 0) {
        if (avail <= 0) {
            offset = 0;
            int r = libusb_bulk_transfer(mHandle,IN,b,BUF_SZ, &avail,0);
            if(r < 0){
                printError(r);
                return -1;
            }

//            for (int i = 0; i < avail; i++)
//                printf("%02x ", b[i]);
//            printf("\n");
            continue;
        }

        int n = buf_size < avail ? buf_size : avail;

        memcpy(buf, b+offset, n);

        offset += n;
        avail -= n;

        left -= n;
        p += n;
    }

    return buf_size;
}