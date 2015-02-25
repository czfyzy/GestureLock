package com.zengye.gesturelock;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

public class GestureLockView extends View {

	private List<Point> choosePointList;
	private Point[] points;
	private int width;
	private int height;
	private boolean isInitPointList;
	private float pointRadius = 25;
	private static final int ROW_NUMBER = 3;
	private float offsetX;
	private float offsetY;
	private float pointDistance;
	private Paint pointPaint;
	private Path fingerPath;
	private Paint pathPaint;
	private StringBuilder result;
	private CompletedListener completedListener;
	
	private Paint centerPaint;

	private String right = "012";

	public GestureLockView(Context context, AttributeSet attrs) {
		super(context, attrs);
		pointRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				pointRadius, context.getResources().getDisplayMetrics());

		points = new Point[ROW_NUMBER * ROW_NUMBER];

		fingerPath = new Path();

		pointPaint = new Paint();

		pointPaint.setAntiAlias(true);
		pointPaint.setDither(true);
		pointPaint.setColor(Color.WHITE);
		pointPaint.setStyle(Paint.Style.STROKE);
		pointPaint.setStrokeWidth(3);
		
		centerPaint = new Paint();
		centerPaint.setDither(true);
		centerPaint.setColor(Color.WHITE);
		centerPaint.setAntiAlias(true);

		
		pathPaint = new Paint();
		pathPaint.setAntiAlias(true);
		pathPaint.setColor(Color.YELLOW);
		pathPaint.setStyle(Paint.Style.STROKE);
		pathPaint.setStrokeCap(Paint.Cap.ROUND);
		pathPaint.setStrokeJoin(Paint.Join.ROUND);
		pathPaint.setStrokeWidth(20);
		pathPaint.setAlpha(80);

		choosePointList = new ArrayList<Point>();

	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// TODO Auto-generated method stub
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		width = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
		height = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();

		if (!isInitPointList && width != 0 && height != 0) {
			if (width >= height) {
				offsetX = (width - height) / 2 + getPaddingLeft();
				offsetY = getPaddingTop();
				pointDistance = (height - ROW_NUMBER * pointRadius * 2)
						/ (ROW_NUMBER - 1);
			} else {
				offsetY = (height - width) / 2 + getPaddingTop();
				offsetX = getPaddingLeft();
				pointDistance = (width - ROW_NUMBER * pointRadius * 2)
						/ (ROW_NUMBER - 1);

			}

			float firstPointX = offsetX + pointRadius;
			float firstPointY = offsetY + pointRadius;
			int index = 0;
			for (int i = 0; i < ROW_NUMBER; i++) {
				for (int j = 0; j < ROW_NUMBER; j++) {
					Point point = new Point(index, firstPointX + j
							* (pointRadius * 2 + pointDistance), firstPointY
							+ i * (pointRadius * 2 + pointDistance),
							pointRadius);
					points[index] = point;
					index++;
				}
			}

			isInitPointList = true;
		}

	}

	class Point {
		public float x;
		public float y;
		public float radius;
		public static final int STATUS_NORMAL = 0, STATUS_FINGER_ON = 1,
				STATUS_ERROR = 2;
		public int status;

		public int id;

		public Point(int id, float x, float y, float radius) {
			this.id = id;
			this.x = x;
			this.y = y;
			this.radius = radius;
			status = STATUS_NORMAL;
		}

		public boolean isInPoint(float tx, float ty) {

			return Math.sqrt((x - tx) * (x - tx) + (y - ty) * (y - ty)) <= radius;

		}

	}

	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		super.onDraw(canvas);

		canvas.drawPath(fingerPath, pathPaint);

		if (points[points.length - 1] != null) {
			for (int i = 0; i < points.length; i++) {

				switch (points[i].status) {
				case Point.STATUS_FINGER_ON:
					pointPaint.setColor(Color.GREEN);
					centerPaint.setColor(Color.GREEN);
					canvas.drawCircle(points[i].x, points[i].y, pointRadius - 30,
							centerPaint);
					break;
				case Point.STATUS_NORMAL:
					pointPaint.setColor(Color.WHITE);
					break;
				case Point.STATUS_ERROR:
					pointPaint.setColor(Color.RED);
					centerPaint.setColor(Color.RED);
					canvas.drawCircle(points[i].x, points[i].y, pointRadius - 30,
							centerPaint);
					break;

				}
				canvas.drawCircle(points[i].x, points[i].y, pointRadius,
						pointPaint);
			}
		}

	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		float x = event.getX();
		float y = event.getY();

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			fingerPath.reset();
			setPointStatus(Point.STATUS_NORMAL);
			choosePointList.clear();
			result = new StringBuilder();
			addPoint(x, y);
			break;
		case MotionEvent.ACTION_MOVE:

			addPoint(x, y);
			drawFingerPath();

			if (!choosePointList.isEmpty()) {

				fingerPath.lineTo(x, y);
			}
			break;
		case MotionEvent.ACTION_UP:
			drawFingerPath();
			String str = result.toString();
			if (!right.equals(str)) {
				setPointStatus(Point.STATUS_ERROR);
				if (completedListener != null) {
					completedListener.fail();
				}
			} else {
				if (completedListener != null) {

					completedListener.success();
				}

			}
			break;
		}
		postInvalidate();
		return true;
	}

	private void addPoint(float x, float y) {
		for (int i = 0; i < points.length; i++) {
			if (points[i].isInPoint(x, y)) {
				Point lastPoint = null;
				if (!choosePointList.isEmpty()) {
					lastPoint = choosePointList.get(choosePointList.size() - 1);
				}
				if (!points[i].equals(lastPoint)) {
					choosePointList.add(points[i]);
					points[i].status = Point.STATUS_FINGER_ON;
					result.append(points[i].id);
				}
			}
		}
	}

	private void drawFingerPath() {
		fingerPath.reset();
		for (int i = 0; i < choosePointList.size(); i++) {
			Point point = choosePointList.get(i);
			if (i == 0) {
				fingerPath.moveTo(point.x, point.y);
			} else {
				fingerPath.lineTo(point.x, point.y);
			}
		}
	}

	private void setPointStatus(int pointStatus) {
		for (Point point : choosePointList) {
			point.status = pointStatus;
		}
	}

	public void setCompletedListener(CompletedListener completedListener) {
		this.completedListener = completedListener;
	}

	public interface CompletedListener {
		public void success();

		public void fail();
	}
}
