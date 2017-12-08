/*
 * Copyright 2017 Maxst, Inc. All Rights Reserved.
 */
package com.maxst.ar.sample.imageTracker;

import android.app.Activity;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;

import com.maxst.ar.CameraDevice;
import com.maxst.ar.MaxstAR;
import com.maxst.ar.MaxstARUtil;
import com.maxst.ar.Trackable;
import com.maxst.ar.TrackerManager;
import com.maxst.ar.TrackingResult;
import com.maxst.ar.TrackingState;
import com.maxst.ar.sample.arobject.ChromaKeyVideoQuad;
import com.maxst.ar.sample.arobject.ColoredCube;
import com.maxst.ar.sample.arobject.TexturedCube;
import com.maxst.ar.sample.arobject.VideoQuad;
import com.maxst.ar.sample.util.BackgroundRenderHelper;
import com.maxst.videoplayer.VideoPlayer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


class ImageTrackerRenderer implements Renderer {

	public static final String TAG = ImageTrackerRenderer.class.getSimpleName();

	private TexturedCube texturedCube;
	private ColoredCube coloredCube;
	private VideoQuad videoQuad;
	private ChromaKeyVideoQuad chromaKeyVideoQuad;

	private int surfaceWidth;
	private int surfaceHeight;
	private BackgroundRenderHelper backgroundRenderHelper;

	private final Activity activity;
	private float [] touchTransform;

	ImageTrackerRenderer(Activity activity) {
		this.activity = activity;
		touchTransform = new float[16];
		Matrix.setIdentityM(touchTransform, 0);
	}

	@Override
	public void onSurfaceCreated(GL10 unused, EGLConfig config) {
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

		backgroundRenderHelper = new BackgroundRenderHelper();
		backgroundRenderHelper.init();

		Bitmap bitmap = MaxstARUtil.getBitmapFromAsset("MaxstAR_Cube.png", activity.getAssets());

		texturedCube = new TexturedCube();
		texturedCube.setTextureBitmap(bitmap);

		coloredCube = new ColoredCube();

		videoQuad = new VideoQuad();
		VideoPlayer player = new VideoPlayer(activity);
		videoQuad.setVideoPlayer(player);
		player.openVideo("VideoSample.mp4");

		chromaKeyVideoQuad = new ChromaKeyVideoQuad();
		player = new VideoPlayer(activity);
		chromaKeyVideoQuad.setVideoPlayer(player);
		player.openVideo("ShutterShock.mp4");

		MaxstAR.onSurfaceCreated();
	}

	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) {
		surfaceWidth = width;
		surfaceHeight = height;

		MaxstAR.onSurfaceChanged(width, height);
	}

	@Override
	public void onDrawFrame(GL10 unused) {
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
		GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight);

		TrackingState state = TrackerManager.getInstance().updateTrackingState();
		TrackingResult trackingResult = TrackerManager.getInstance().getTrackingResult(state);

		backgroundRenderHelper.drawBackground();

		boolean legoDetected = false;
		boolean blocksDetected = false;

		float[] projectionMatrix = CameraDevice.getInstance().getProjectionMatrix();
		float [] transform = new float[16];

		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		for (int i = 0; i < trackingResult.getCount(); i++) {
			Trackable trackable = trackingResult.getTrackable(i);
			if (trackable.getName().equals("Lego")) {
				legoDetected = true;
				if (videoQuad.getVideoPlayer().getState() == VideoPlayer.STATE_READY ||
						videoQuad.getVideoPlayer().getState() == VideoPlayer.STATE_PAUSE) {
					videoQuad.getVideoPlayer().start();
				}

				Matrix.multiplyMM(transform, 0, trackable.getPoseMatrix(), 0, touchTransform, 0);
				videoQuad.setProjectionMatrix(projectionMatrix);
				videoQuad.setTransform(transform);
				videoQuad.setTranslate(0.0f, 0.0f, 0.0f);
				videoQuad.setScale(
						1.0f,
						-0.560f,
						1.0f
				);
				videoQuad.draw();
			} else if (trackable.getName().equals("Blocks")) {
				blocksDetected = true;
				if (chromaKeyVideoQuad.getVideoPlayer().getState() == VideoPlayer.STATE_READY ||
						chromaKeyVideoQuad.getVideoPlayer().getState() == VideoPlayer.STATE_PAUSE) {
					chromaKeyVideoQuad.getVideoPlayer().start();
				}

				Matrix.multiplyMM(transform, 0, trackable.getPoseMatrix(), 0, touchTransform, 0);

				chromaKeyVideoQuad.setProjectionMatrix(projectionMatrix);
				chromaKeyVideoQuad.setTransform(transform);
				chromaKeyVideoQuad.setTranslate(0.0f, 0.0f, 0.0f);
				chromaKeyVideoQuad.setScale(
						1.0f,
						-0.70f,
						1.0f
				);
				chromaKeyVideoQuad.draw();
			} else if (trackable.getName().equals("Glacier")) {
				Matrix.multiplyMM(transform, 0, trackable.getPoseMatrix(), 0, touchTransform, 0);
				texturedCube.setProjectionMatrix(projectionMatrix);
				texturedCube.setTransform(transform);
				texturedCube.setTranslate(0, 0, -0.05f);
				texturedCube.setScale(0.3f, 0.3f, 0.1f);
				texturedCube.draw();
			} else {
				Matrix.multiplyMM(transform, 0, trackable.getPoseMatrix(), 0, touchTransform, 0);
				coloredCube.setProjectionMatrix(projectionMatrix);
				coloredCube.setTransform(transform);
				coloredCube.setScale(0.3f, 0.3f, 0.01f);
				coloredCube.draw();
			}
		}

		if (!legoDetected) {
			if (videoQuad.getVideoPlayer().getState() == VideoPlayer.STATE_PLAYING) {
				videoQuad.getVideoPlayer().pause();
			}
		}

		if (!blocksDetected) {
			if (chromaKeyVideoQuad.getVideoPlayer().getState() == VideoPlayer.STATE_PLAYING) {
				chromaKeyVideoQuad.getVideoPlayer().pause();
			}
		}
	}

	void destroyVideoPlayer() {
		videoQuad.getVideoPlayer().destroy();
		chromaKeyVideoQuad.getVideoPlayer().destroy();
	}

	void setTranslate(float x, float y) {
		Matrix.translateM(touchTransform, 0, x, y, 0);
	}
}