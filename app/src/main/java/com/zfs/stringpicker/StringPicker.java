package com.zfs.stringpicker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by zeng on 2016/6/19.
 * 滚动选择器
 */
public class StringPicker extends View {
	/** 自动回滚到中间的速度 */
	private static final float SPEED = 2;
	private List<String> dataList = new ArrayList<>();
	/** 选中的位置，这个位置是dataList的中心位置，一直不变 */
	private int currentSelected;
	private Paint paint;
	private float selectedTextSize = -1;
	private float normalTextSize = -1;
    private float textSpace = -1;
	private int selectedTextColor = 0xFF00839A;
	private int normalTextColor = 0x66666666;
	private int height;
	private int width;
	private float lastDownY;
	/** 滑动的距离 */
	private float moveLen = 0;
	private boolean isInit = false;
	private onSelectListener selectListener;
	private Timer timer;
	private MyTimerTask task;
	private GestureDetector gestureDetector;
	private Scroller scroller;
	private boolean isFling;
	
	public StringPicker(Context context) {
		this(context, null);
	}

	public StringPicker(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		timer = new Timer();
		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setStyle(Paint.Style.FILL);
		paint.setTextAlign(Paint.Align.CENTER);        
		gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
				isFling = true;
				scroller.fling((int) e2.getX(), (int) e2.getY(), 0, (int)velocityY, 0, 0, -2000, 2000);
				return super.onFling(e1, e2, velocityX, velocityY);
			}
		});
		scroller = new Scroller(getContext());
	}

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        height = getHeight();
        width = getWidth();
        if (selectedTextSize == -1 || normalTextSize == -1) {
            selectedTextSize = height / 3f;
            normalTextSize = selectedTextSize / 2f;
        }
        if (textSpace == -1) textSpace = normalTextSize * 1.7f;
        isInit = true;
    }
    
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		// 根据index绘制view
		if (isInit)
			drawData(canvas);
	}

	@Override
	public void computeScroll() {
		if (scroller.computeScrollOffset()) {
			if (isFling) doMove(null);
		} else {
			if (isFling) {
				doUp(null);
				isFling = false;
			}
		}
	}
	
	public void setOnSelectListener(onSelectListener listener) {
		selectListener = listener;
	}

	public void setTextColor(int selectedTextColor, int normalTextColor) {
		this.selectedTextColor = selectedTextColor;
		this.normalTextColor = normalTextColor;
        postInvalidate();
	}
	
	public void setTextSize(float selectedTextSize, float normalTextSize) {
		this.selectedTextSize = selectedTextSize;
		this.normalTextSize = normalTextSize;
        postInvalidate();
	}
	
    /**字体间距，指未选中字体间距*/
    public void setTextSpace(float space) {
        textSpace = space;
    }
    
    public void setTypeface(Typeface typeface) {
        paint.setTypeface(typeface);
        postInvalidate();
    }
    
	private void performSelect() {
		if (selectListener != null)
			selectListener.onSelect(dataList.get(currentSelected));
	}

	public void setData(List<String> dataList) {
		if (dataList == null || dataList.size() == 0) return;
		this.dataList = dataList;
		currentSelected = dataList.size() / 2;
		postInvalidate();
	}

	/**
	 * 选择选中的item的index
	 */
	public void setSelected(String item) {
        for (int i = 0; i < dataList.size(); i++) {
            if (dataList.get(i).equals(item)) {
                currentSelected = i;
            }
        }
		int distance = dataList.size() / 2 - currentSelected;
		if (distance < 0) {
			for (int i = 0; i < -distance; i++) {
				moveHeadToTail();
				currentSelected--;
			}
		} else if (distance > 0) {
			for (int i = 0; i < distance; i++) {
				moveTailToHead();
				currentSelected++;
			}
		}
		performSelect();
        postInvalidate();
	}

	private void drawData(Canvas canvas) {
		// 先绘制选中的text再往上往下绘制其余的text
		float scale = parabola(height / 4.0f, moveLen);
		float size = (selectedTextSize - normalTextSize) * scale + normalTextSize;
		paint.setTextSize(size);
		paint.setColor(getColor(scale));
		// text居中绘制，注意baseline的计算才能达到居中，y值是text中心坐标
		float x = width / 2f;
		float y = height / 2f + moveLen;
		Paint.FontMetricsInt fmi = paint.getFontMetricsInt();
		float baseline = y - (fmi.bottom / 2f + fmi.top / 2f);

		int indexs = currentSelected;
		String textData = dataList.get(indexs);
		canvas.drawText(textData, x, baseline, paint);

		// 绘制上方data
		for (int i = 1; (currentSelected - i) >= 0; i++) {
			drawOtherText(canvas, i, -1);
		}
		// 绘制下方data
		for (int i = 1; (currentSelected + i) < dataList.size(); i++) {
			drawOtherText(canvas, i, 1);
		}
	}

	/**
	 * @param position 距离mCurrentSelected的差值
	 * @param type 1表示向下绘制，-1表示向上绘制
	 */
	private void drawOtherText(Canvas canvas, int position, int type) {
		float d = textSpace * position + type * moveLen;
		float scale = parabola(height / 4.0f, d);
		float size = (selectedTextSize - normalTextSize) * scale + normalTextSize;
		paint.setTextSize(size);
		paint.setColor(getColor(scale));        
		float y = (float) (height / 2.0 + type * d);
		Paint.FontMetricsInt fmi = paint.getFontMetricsInt();
		float baseline = (float) (y - (fmi.bottom / 2.0 + fmi.top / 2.0));

		int indexs = currentSelected + type * position;
		String textData = dataList.get(indexs);
		canvas.drawText(textData, (float) (width / 2.0), baseline, paint);
	}

	private int getColor(float scale) {
		int Aa = normalTextColor >> 24 & 0xff;
		int Ra = normalTextColor >> 16 & 0xff;
		int Ga = normalTextColor >> 8 & 0xff;
		int Ba = normalTextColor & 0xff;
		int Ab = selectedTextColor >> 24 & 0xff;
		int Rb = selectedTextColor >> 16 & 0xff;
		int Gb = selectedTextColor >> 8 & 0xff;
		int Bb = selectedTextColor & 0xff;
		int a = (int) (Aa + (Ab - Aa) * scale);
		int r = (int) (Ra + (Rb - Ra) * scale);
		int g = (int) (Ga + (Gb - Ga) * scale);
		int b = (int) (Ba + (Bb - Ba) * scale);
		return Color.argb(a, r, g, b);
	}

	/**
	 * 抛物线 
	 * @param zero 零点坐标
	 * @param x 偏移量
	 */
	private float parabola(float zero, float x) {
		float f = (float) (1 - Math.pow(x / zero, 2));
		return f < 0 ? 0 : f;
	}
	
	private void moveHeadToTail() {
		dataList.add(dataList.remove(0));
	}

	private void moveTailToHead() {
		dataList.add(0, dataList.remove(dataList.size() - 1));
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		gestureDetector.onTouchEvent(event);

		switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				doDown(event);
				break;
			case MotionEvent.ACTION_MOVE:
				doMove(event);
				break;
			case MotionEvent.ACTION_UP:
				doUp(event);
				break;
		}
		return true;
	}

	private void doDown(MotionEvent event) {
		if(!scroller.isFinished()) {
			scroller.abortAnimation();
		}
		if (task != null) {
			task.cancel();
			task = null;
		}
		lastDownY = event.getY();
	}

	private void doMove(MotionEvent event) {
		float currY;
		if (event == null) {
			currY = scroller.getCurrY();
		} else {
			currY = event.getY();
		}
		moveLen += currY - lastDownY;

		if (moveLen > textSpace / 2) {
			// 往下滑超过离开距离
			moveTailToHead();
			moveLen = moveLen - textSpace;
		} else if (moveLen < -textSpace / 2) {
			// 往上滑超过离开距离
			moveHeadToTail();
			moveLen = moveLen + textSpace;
		}
		lastDownY = currY;
		invalidate();
	}

	private void doUp(MotionEvent event) {
		// 抬起手后mCurrentSelected的位置由当前位置move到中间选中位置
		if (Math.abs(moveLen) < 0.0001) {
			moveLen = 0;
			return;
		}
		if (task != null) {
			task.cancel();
			task = null;
		}
		task = new MyTimerTask(updateHandler);
		timer.schedule(task, 0, 10);
	}

	class MyTimerTask extends TimerTask {
		Handler handler;

		public MyTimerTask(Handler handler) {
			this.handler = handler;
		}

		@Override
		public void run() {
			handler.sendMessage(handler.obtainMessage());
		}

	}

	public interface onSelectListener {
		void onSelect(String text);
	}
	
	final Handler updateHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (Math.abs(moveLen) < SPEED) {
				moveLen = 0;
				if (task != null) {
					task.cancel();
					task = null;
					performSelect();
				}
			} else
				// 这里mMoveLen / Math.abs(mMoveLen)是为了保有mMoveLen的正负号，以实现上滚或下滚
				moveLen = moveLen - moveLen / Math.abs(moveLen) * SPEED;
			invalidate();
		}
	};
}
