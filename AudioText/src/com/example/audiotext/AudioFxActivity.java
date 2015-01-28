package com.example.audiotext;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.Equalizer;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.SeekBar;

@SuppressLint("NewApi")
public class AudioFxActivity extends Activity {
	private static final String TAG = "AudioFxActivity";

	private static final float VISUALIZER_HEIGHT_DIP = 360f;
	private SeekBar seekBar;
	private MediaPlayer mMediaPlayer;
	private Visualizer mVisualizer;

	private LinearLayout mLinearLayout;

	private VisualizerView mVisualizerView;

	private Equalizer mEqualizer;

	Handler handler = new Handler();
	Runnable updateThread = new Runnable() {
		public void run() {
			// 获得歌曲现在播放位置并设置成播放进度条的值
			seekBar.setProgress(mMediaPlayer.getCurrentPosition());
			// 每次延迟100毫秒再启动线程
			handler.postDelayed(updateThread, 100);
		}
	};

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.activity_main);
		mLinearLayout = (LinearLayout) findViewById(R.id.ll);

		seekBar = (SeekBar) findViewById(R.id.seekbar);
		// Create the MediaPlayer
		mMediaPlayer = MediaPlayer.create(this, R.raw.a);
		Log.d(TAG,
				"MediaPlayer audio session ID: "
						+ mMediaPlayer.getAudioSessionId());
		seekBar.setMax(mMediaPlayer.getDuration());

		setupVisualizerFxAndUI();

		mVisualizer.setEnabled(true);
		
		// 设置了均衡器就与音量大小无关拉
		mEqualizer = new Equalizer(0, mMediaPlayer.getAudioSessionId());
		mEqualizer.setEnabled(true);

		mMediaPlayer
				.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
					public void onCompletion(MediaPlayer mediaPlayer) {
						mVisualizer.setEnabled(false);
						getWindow().clearFlags(
								WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
						setVolumeControlStream(AudioManager.STREAM_SYSTEM);

					}
				});

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		mMediaPlayer.start();
		handler.post(updateThread);

		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				// fromUser判断是用户改变的滑块的值
				if (fromUser == true) {
					mMediaPlayer.seekTo(progress);
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
			}
		});

	}

	private void setupVisualizerFxAndUI() {
		mVisualizerView = new VisualizerView(this);
		mVisualizerView.setLayoutParams(new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.FILL_PARENT,
				(int) (VISUALIZER_HEIGHT_DIP * getResources()
						.getDisplayMetrics().density)));

		mVisualizerView.setProgress(13);// 设置波形的高度
		mVisualizerView.setmHeight(8);// 让水位处于最高振幅
		mLinearLayout.addView(mVisualizerView);

		final int maxCR = Visualizer.getMaxCaptureRate();
		// 实例化Visualizer，参数SessionId可以通过MediaPlayer的对象获得
		mVisualizer = new Visualizer(mMediaPlayer.getAudioSessionId());
		// 设置需要转换的音乐内容长度，专业的说这就是采样,该采样值一般为2的指数倍
		mVisualizer.setCaptureSize(256);
		// 接下来就好理解了设置一个监听器来监听不断而来的所采集的数据。一共有4个参数，第一个是监听者，第二个单位是毫赫兹，表示的是采集的频率，第三个是是否采集波形，第四个是是否采集频率
		mVisualizer.setDataCaptureListener(
		// 这个回调应该采集的是波形数据
				new Visualizer.OnDataCaptureListener() {
					public void onWaveFormDataCapture(Visualizer visualizer,
							byte[] bytes, int samplingRate) {
						mVisualizerView.updateVisualizer(bytes); // 按照波形来画图
					}

					// 这个回调应该采集的是快速傅里叶变换有关的数据
					public void onFftDataCapture(Visualizer visualizer,
							byte[] fft, int samplingRate) {
						mVisualizerView.updateVisualizer(fft);
					}
				}, maxCR / 2, false, true);
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (isFinishing() && mMediaPlayer != null) {
			handler.removeCallbacks(updateThread);
			mVisualizer.release();
			mMediaPlayer.release();
			mMediaPlayer = null;
		}
	}

}