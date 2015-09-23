# ProjectionDemo

This is a sample application that illustrates projection flow between Android application and Linux sink briefly.

ProjectionDemo android application has three major steps. First, it creates virtual screen which is projected into sink. Second, it encode
the virtual screen into H.264 stream, And finally, it transmit the stream through AOAP.

LinuxSink application initialize AOAP, decode H.264 stream, and display in SDL window.
