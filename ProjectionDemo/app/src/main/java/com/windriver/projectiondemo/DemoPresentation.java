package com.windriver.projectiondemo;

import android.app.Presentation;
import android.content.Context;
import android.content.res.Resources;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Display;

class DemoPresentation extends Presentation {
    private GLSurfaceView mSurfaceView;

    public DemoPresentation(Context context, Display display) {
        super(context, display);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Be sure to call the super class.
        super.onCreate(savedInstanceState);

        // Get the resources for the context of the presentation.
        // Notice that we are getting the resources from the context of the presentation.
        Resources r = getContext().getResources();

        // Inflate the layout.
        setContentView(R.layout.presentation_with_media_router_content);

        // Set up the surface view for visual interest.
        mSurfaceView = (GLSurfaceView)findViewById(R.id.surface_view);
        mSurfaceView.setRenderer(new CubeRenderer(false));
    }
}


