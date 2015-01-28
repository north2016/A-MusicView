package com.example.audiotext;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.view.View;

/**
 * 自定义声音波浪
 * 
 * @author baixiaokang
 * @category https://github.com/north2014
 */
public class VisualizerView extends View {

	private byte[] mBytes;// 波形数组
	private Paint mPaint = new Paint();// 主画笔
	private int mSpectrumNum = 7;// 取样数
	private int Padding = 100;// 左右边距

	// 顶部大圆系数
	RectF cricleR;
	private int R = 120;
	private int RcenterX;
	private int RcenterY;

	// 顶部小圆系数
	RectF cricleM;
	private int Rm = 5;// 顶部小圆半径

	// 波浪顶部动态圆系数
	RectF cricle2;
	private int r;// 动态圆半径

	// 波浪底部固定圆系数
	RectF cricle1;
	private int r1;// 底部圆半径

	Path path1;// 底部梯形
	Path path2;// 顶部梯形

	private int mOnBallHeight = 80;// 触发弹射小球的高度

	List<Statue> mStatues;// 所有波浪的弹射状态
	List<Boll> mBolls;// 所有的小球
	List<Boll> mDrageBolls;// 所有处于拉伸状态的的小球
	/**
	 * 所有 波浪的参数
	 */

	private int mViewWidth;// 宽
	private int mViewHeight;// 高

	private float mLevelLine = 0;// 水位线
	private float mWaveHeight = 80;// 波浪起伏幅度
	private float mWaveWidth = 200;// 波长
	private float mLeftSide;// 被隐藏的最左边的波形
	private float mMoveLen;// 水波平移的距离
	public static final float SPEED = 1.7f;// 水波平移速度
	private List<Point> mPointsList;// 波浪顶部的点阵
	private Path mWavePath;// 波浪的路径
	private boolean isMeasured = false;

	private Timer timer;
	private MyTimerTask mTask;

	private float progress = 100;// 进度
	private double mHeight;// 波浪幅度

	public void setProgress(float i) {
		this.progress = (100 - i) / 100f;
	}

	protected float getProgress() {
		return this.progress;
	}

	public double getmHeight() {
		return this.mHeight;
	}

	public void setmHeight(double i) {
		this.mHeight = i;
	}

	@SuppressLint("HandlerLeak")
	Handler updateHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			// 记录平移总位移
			mMoveLen += SPEED;
			// 根据View高度和传过来的进度计算水位线高度
			mLevelLine = getProgress() * mViewHeight;
			// 根据View宽度和传过来的振幅计算波形峰值
			mWaveHeight = (float) (mViewWidth / (getmHeight() * 10f));

			mLeftSide += SPEED;
			// 波形平移
			for (int i = 0; i < mPointsList.size(); i++) {
				mPointsList.get(i).setX(mPointsList.get(i).getX() + SPEED);
				switch (i % 4) {
				case 0:
				case 2:
					mPointsList.get(i).setY(mLevelLine);
					break;
				case 1:
					mPointsList.get(i).setY(mLevelLine + mWaveHeight);
					break;
				case 3:
					mPointsList.get(i).setY(mLevelLine - mWaveHeight);
					break;
				}
			}
			if (mMoveLen >= mWaveWidth) {
				// 波形平移超过一个完整波形后复位
				mMoveLen = 0;
				resetPoints();
			}

		}

	};

	/**
	 * 所有点的x坐标都还原到初始状态，也就是一个周期前的状态
	 */
	private void resetPoints() {
		mLeftSide = -mWaveWidth;
		for (int i = 0; i < mPointsList.size(); i++) {
			mPointsList.get(i).setX(i * mWaveWidth / 4 - mWaveWidth);
		}
	}

	public VisualizerView(Context context) {
		super(context);
		init();
	}

	private void init() {
		mBytes = null;

		mStatues = new ArrayList<Statue>();// 所有小球处于的状态
		mBolls = new ArrayList<Boll>();// 所有的小球
		mDrageBolls = new ArrayList<Boll>();// 所有处于拉伸状态的的小球

		for (int i = 0; i < mSpectrumNum; i++) {
			mStatues.add(new Statue(0, i));
		}

		mPaint.setAntiAlias(true);
		mPaint.setColor(Color.parseColor("#E54B4C"));

		mPointsList = new ArrayList<Point>();
		timer = new Timer();
		mWavePath = new Path();
	}

	/**
	 * 根据声波数组更新波浪的数据
	 * 
	 * @param fft
	 */
	public void updateVisualizer(byte[] fft) {

		byte[] model = new byte[fft.length / 2 + 1];

		model[0] = (byte) Math.abs(fft[0]);
		for (int i = 2, j = 1; j < mSpectrumNum;) {
			model[j] = (byte) Math.hypot(fft[i], fft[i + 1]);
			i += 2;
			j++;
		}
		mBytes = model;
		invalidate();// 重绘

	}

	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		super.onWindowFocusChanged(hasWindowFocus);
		// 开始波动
		start();
	}

	private void start() {
		if (mTask != null) {
			mTask.cancel();
			mTask = null;
		}
		mTask = new MyTimerTask(updateHandler);
		timer.schedule(mTask, 0, 10);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		if (!isMeasured) {
			isMeasured = true;

			mViewWidth = getMeasuredWidth();
			mViewHeight = getMeasuredWidth();

			// 根据View宽度计算波形峰值
			mWaveHeight = mViewWidth / 10f;

			// 水位线从最底下开始上升
			mLevelLine = mViewHeight;

			// 波长等于四倍View宽度也就是View中只能看到四分之一个波形，这样可以使起伏更明显
			mWaveWidth = mViewWidth / 4;
			// 左边隐藏的距离预留一个波形
			mLeftSide = -mWaveWidth;
			// 这里计算在可见的View宽度中能容纳几个波形，注意n上取整
			int n = (int) Math.round(mViewWidth / mWaveWidth + 0.5);
			// n个波形需要4n+1个点，但是我们要预留一个波形在左边隐藏区域，所以需要4n+5个点
			for (int i = 0; i < (4 * n + 5); i++) {
				// 从P0开始初始化到P4n+4，总共4n+5个点
				float x = i * mWaveWidth / 4 - mWaveWidth;
				float y = 0;
				switch (i % 4) {
				case 0:
				case 2:
					// 零点位于水位线上
					y = mLevelLine + mWaveHeight;
					break;
				case 1:
					// 往下波动的控制点
					y = mLevelLine + 2 * mWaveHeight;
					break;
				case 3:
					// 往上波动的控制点
					y = mLevelLine;
					break;
				}
				mPointsList.add(new Point(x, y));
			}
		}
	}

	@SuppressLint("DrawAllocation")
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.drawColor(Color.WHITE);
		if (mBytes == null) {
			return;
		}

		final int baseX = (getWidth() - 2 * Padding) / mSpectrumNum;
		final int height = getHeight();
		r = baseX / 2;

		// 绘制频谱
		for (int i = 0; i < mSpectrumNum; i++) {
			if (mBytes[i] < 0) {
				mBytes[i] = 127;
			}

			int centerx = baseX * i + baseX / 2 + Padding;

			int left = baseX * i + Padding;
			int right = left + 2 * r;
			int top = height - mBytes[i] - 2 * r;
			int bottom = height;

			cricle1 = new RectF(left, height - 2 * r, right, bottom);// 底部圆

			r1 = (int) (r - (mBytes[i] / 8) * 1.2);
			cricle2 = new RectF(centerx - r1, top, centerx + r1, top + 2// 顶部圆
					* r1);

			canvas.drawOval(cricle1, mPaint);// 画圆
			canvas.drawOval(cricle2, mPaint);// 画圆

			path1 = new Path(); // 梯
			path1.moveTo(left, height - r);
			path1.lineTo(centerx - r1, top + r1);
			path1.lineTo(centerx + r1, top + r1);
			path1.lineTo(right, height - r);
			path1.lineTo(left, height - r);

			canvas.drawPath(path1, mPaint);// 画梯形

			if (mBytes[i] >= mOnBallHeight
					&& mStatues.get(i).getBollsNum() == 0) {// 本来没有而且现在到了触发位置，则添加小球
				int mid = (mSpectrumNum % 2 == 0) ? mSpectrumNum / 2
						: mSpectrumNum / 2 + 1;
				if (i == mid && mDrageBolls.size() > 0) {// 中间有，就不加了吧，否则太混乱了
					continue;
				} else {// 否则添加小球，根据mBytes[i]，设置初始位置和初始速度
					int v0 = (mBytes[i] - mOnBallHeight) / 8 + 40;
					int centery = height - mBytes[i] - r1;
					Boll mBoll = new Boll(centerx, centery, i, v0);
					mBolls.add(mBoll);
					mStatues.get(i).addNum();
				}

			}
		}

		int num = 0;
		mDrageBolls = new ArrayList<Boll>();// 所有处于拉伸状态的的小球
		for (int j = 0; j < mBolls.size(); j++) {// 保证最多只有1个小球处于拉伸下滴状态，后来的小球会和之前的小球凝聚起来变大
			if (mBolls.get(j).isDrage && mBolls.get(j).centerX == RcenterX) {
				num++;
				mDrageBolls.add(mBolls.get(j));
				if (mDrageBolls.size() > 1) {
					mStatues.get(mBolls.get(j).getId()).removeNum();
					mBolls.remove(j);
					int r = 8 + 2 * num;// 凝聚在一起的小球半径会线性变大
					mDrageBolls.get(0).setR(r);
				}
			}
		}

		for (int j = 0; j < mBolls.size(); j++) {// 更新所有小球的数据并画图
			mBolls.get(j).update();
			mBolls.get(j).draw(canvas);

			if (mBolls.get(j).newy > height) {// 清除移动到画面以外的小球
				mStatues.get(mBolls.get(j).getId()).removeNum();
				mBolls.remove(j);
			}
		}

		RcenterX = getWidth() / 2;
		RcenterY = R;

		// 这两个圆只是用来参考坐标的，没有必要画出来
		// 绘制顶部大圆
		// cricleR = new RectF(getWidth() / 2 - R, 0, getWidth() / 2 + R, R *
		// 2);
		// canvas.drawOval(cricleR, mPaint);

		// 绘制顶部小圆
		// cricleM = new RectF(getWidth() / 2 - Rm, 2 * R - 2 * Rm, getWidth() /
		// 2
		// + Rm, 2 * R);
		// canvas.drawOval(cricleM, mPaint);

		// 画底部的波浪
		mWavePath.reset();
		int i = 0;
		mWavePath.moveTo(mPointsList.get(0).getX(), mPointsList.get(0).getY());
		for (; i < mPointsList.size() - 2; i = i + 2) {
			mWavePath.quadTo(mPointsList.get(i + 1).getX(),
					mPointsList.get(i + 1).getY(), mPointsList.get(i + 2)
							.getX(), mPointsList.get(i + 2).getY());
		}
		mWavePath.lineTo(mPointsList.get(i).getX(), height);
		mWavePath.lineTo(mLeftSide, height);
		mWavePath.close();
		// mPaint的Style是FILL，会填充整个Path区域
		canvas.drawPath(mWavePath, mPaint);
	}

	/**
	 * 波浪的状态类
	 * 
	 * @author Administrator
	 * 
	 */
	public class Statue {
		private int bollsNum;// 抛出的小球数
		private int index;// 波浪的id

		public Statue(int i, int index) {
			this.setBollsNum(i);
			this.index = index;
		}

		public void addNum() {
			this.setBollsNum(this.getBollsNum() + 1);
		}

		public void removeNum() {
			this.setBollsNum(this.getBollsNum() - 1);
		}

		public int getIndex() {
			return index;
		}

		public void setIndex(int index) {
			this.index = index;
		}

		public int getBollsNum() {
			return bollsNum;
		}

		public void setBollsNum(int bollsNum) {
			this.bollsNum = bollsNum;
		}

	}

	/**
	 * 小球实体类
	 */
	public class Boll {

		private int index;// id
		private int r;// 半径
		private int centerX;// 圆心x坐标
		private int oldy;// 之前的圆心y坐标
		private int newy;// 当前圆心y坐标
		private int v0;// 初始速度
		private int vt;// t时刻速度

		private int a;// 加速度
		private int t;// 小球的生命时长时间

		private int vR;// 被吸附时的速度
		private int aR;// 被吸附时的加速度
		private double tR;// 被吸附的时间长度

		private boolean isGingtoTap = false;// 是否将要被吸附
		boolean isTap = false;// 是否被吸附

		boolean isDrage = false;// 是否在拖拽状态
		boolean isGingtoDrag = false;// 是否将要被拖拽

		public Boll(int x, int y, int index, int v0) {
			this.index = index;
			this.r = 10;
			this.centerX = x;
			this.oldy = y;
			this.newy = y;
			this.v0 = v0;
			this.vt = v0;
			this.vR = 0;
			this.a = 4;
			this.t = 0;
			this.tR = 0;
			this.aR = 1;
		}

		public void setR(int r) {
			this.r = r;

		}

		public int getId() {
			return this.index;
		}

		/**
		 * 更新小球的坐标
		 */
		public void update() {
			t++;
			if (vt > 0) {// 减速上升阶段
				vt -= a;
				newy = oldy - vt;
			} else {// 加速向下掉阶段
				vt += a;
				newy = oldy + vt * (t - v0 / a);
			}

			// 判断是否吸附到顶部大圆上
			if (r + R > Math.sqrt((centerX - RcenterX) * (centerX - RcenterX)
					+ (newy - RcenterY) * (newy - RcenterY))) {
				newy = (int) Math.sqrt(R * R - (centerX - RcenterX)
						* (centerX - RcenterX))
						+ R - r;
				isGingtoTap = true;
			}

			if (isGingtoTap) {
				if (newy <= 2 * R) {// 在吸附状态
					isTap = true;
					tR = tR + 1;
				} else { // 逃离吸附状态
					isTap = false;
					isDrage = true;// 进入拉伸状态
				}
			}

			if (isTap) {// 在吸附状态时，计算x坐标，让小球贴着大球滑行
				vR += aR;
				newy = (int) (oldy + vR * tR);
				if (centerX > RcenterX) {// 在中心右边
					centerX = (int) Math.sqrt(R * R - (newy - RcenterY)
							* (newy - RcenterY))
							+ RcenterX;
				} else {// 在中心左边
					centerX = RcenterX
							- (int) Math.sqrt(R * R - (newy - RcenterY)
									* (newy - RcenterY));
				}

			}

			if (isDrage) {// 进入拉伸状态
				if (newy < 2 * R + 50) {// 在拉升距离以内，拉伸
					centerX = RcenterX;
					isDrage = true;
				} else {// 在拉升距离以外，不拉伸
					isDrage = false;
				}
			}

			oldy = newy;// 更新oldy
		}

		/**
		 * 画小球
		 */
		private void draw(Canvas canvas) {
			if (isDrage) {// 在拉伸状态，画出拉伸的梯形

				path2 = new Path(); // 梯形
				path2.moveTo(getWidth() / 2 - Rm, 2 * R - Rm);
				path2.lineTo(getWidth() / 2 + Rm, 2 * R - Rm);
				path2.lineTo(centerX + r, newy);
				path2.lineTo(centerX - r, newy);
				path2.lineTo(getWidth() / 2 - Rm, 2 * R - Rm);
				canvas.drawPath(path2, mPaint);
			}
			// 画小球
			RectF cricle = new RectF(centerX - r, newy + r, centerX + r, newy
					- r);
			canvas.drawOval(cricle, mPaint);
		}
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

	/**
	 * 坐标点的实体类
	 * 
	 * @author Administrator
	 * 
	 */

	class Point {
		private float x;
		private float y;

		public float getX() {
			return x;
		}

		public void setX(float x) {
			this.x = x;
		}

		public float getY() {
			return y;
		}

		public void setY(float y) {
			this.y = y;
		}

		public Point(float x, float y) {
			this.x = x;
			this.y = y;
		}

	}

}
