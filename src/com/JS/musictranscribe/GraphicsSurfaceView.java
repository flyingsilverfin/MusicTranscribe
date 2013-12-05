package com.JS.musictranscribe;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.os.Handler;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GraphicsSurfaceView extends SurfaceView implements SurfaceHolder.Callback{

	private SurfaceHolder sh;
	private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private GraphicsThread thread;
	private Context ctx;
	
	public GraphicsSurfaceView(Context context) {
		super(context);
		sh = getHolder();
		sh.addCallback(this);
		paint.setColor(Color.BLUE);
		paint.setStyle(Style.FILL);
		ctx = context;
		setFocusable(true);
	}
	
	public GraphicsThread getThread(){
		return thread;
	}
	
	@Override
	public void surfaceCreated(SurfaceHolder holder){
		thread = new GraphicsThread(sh, ctx, new Handler());
		thread.setRunning(true);
		thread.start();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		thread.setSurfaceSize(width, height);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		killThread();
	}
	
	public void killThread(){
		boolean retry = true;
		thread.setRunning(false);
		while(retry){
			try {
				thread.join();
				retry = false;
			} catch (InterruptedException e) {
				
			}
		}
	}
	
	//Draw Canvas using another thread to get off of main UI thread
	class GraphicsThread extends Thread {
		private int canvasWidth = 200;
		private int canvasHeight = 400;
		private static final int SPEED = 2;
		private boolean run = false;
		
		private float circleX;
		private float circleY;
		private float headingX;
		private float headingY;
		
		public GraphicsThread(SurfaceHolder surfaceHolder, Context context, Handler handler){
			sh = surfaceHolder;
			handler = handler;
			ctx = context;
		}
		
		public void doStart(){
			synchronized(sh){
				circleX = canvasWidth/2;
				circleY = canvasHeight/2;
				headingX = (float) (-1 + (Math.random() * 2));
				headingY = (float) (-1 + (Math.random() * 2));
			}
		}
		
		public void run(){
			while(run){
				Canvas c = null;
				try{
					c = sh.lockCanvas(null);
					synchronized(sh) {
						doDraw(c);
					}
				} finally {
					if (c != null) {
						sh.unlockCanvasAndPost(c);
					}
				}
			}
		}
		
		public void setRunning(boolean b){
			run = b;
		}
		
		public void setSurfaceSize(int width, int height){
			synchronized(sh) {
				canvasWidth = width;
				canvasHeight = height;
				doStart();
			}
		}
		
		private void doDraw(Canvas canvas){
			circleX = circleX + (headingX * SPEED);
			circleY = circleY + (headingY * SPEED);
			canvas.restore();
			canvas.drawColor(Color.BLACK);
			canvas.drawCircle(circleX, circleY, 50, paint);
		}
		
	}
}
