cmake_minimum_required(VERSION 3.2)
project(LinuxSink)


set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -D_GNU_SOURCE=1 -D_REENTRANT -I/usr/include/SDL")

include_directories(/usr/local/include /usr/include/libusb-1.0)
link_directories(/usr/local/lib)

set(SOURCE_FILES main.cpp)
add_executable(LinuxSink ${SOURCE_FILES} Accessory.cpp Accessory.h)
target_link_libraries(LinuxSink avcodec avutil swscale usb-1.0 SDL)