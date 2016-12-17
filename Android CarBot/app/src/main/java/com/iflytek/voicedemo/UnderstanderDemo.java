package com.iflytek.voicedemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;


import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUnderstander;
import com.iflytek.cloud.SpeechUnderstanderListener;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.TextUnderstander;
import com.iflytek.cloud.TextUnderstanderListener;
import com.iflytek.cloud.UnderstanderResult;
import com.iflytek.speech.setting.UnderstanderSettings;
import com.iflytek.speech.util.JsonParser;
import com.iflytek.sunflower.FlowerCollector;

import java.util.Iterator;
import java.util.Set;

public class UnderstanderDemo extends Activity implements OnClickListener {
	private static String TAG = UnderstanderDemo.class.getSimpleName();
	// 语义理解对象（语音到语义）。
	private SpeechUnderstander mSpeechUnderstander;
	// 语义理解对象（文本到语义）。
	// 语音合成对象
	private SpeechSynthesizer mTts;
	// 默认发音人
	private String voicer = "xiaoyan";

	// 缓冲进度
	private int mPercentForBuffering = 0;
	// 播放进度
	private int mPercentForPlaying = 0;

	// 云端/本地单选按钮
	private RadioGroup mRadioGroup;
	// 引擎类型
	private String mEngineType = SpeechConstant.TYPE_CLOUD;


	private TextUnderstander   mTextUnderstander;
	private Toast mToast;	
	private EditText mUnderstanderText;
	
	private SharedPreferences mSharedPreferences;


	@SuppressLint("ShowToast")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.understander);

		//Create Bluretooth
		BluetoothAdapter Adapter = BluetoothAdapter.getDefaultAdapter();
		if(!Adapter.isEnabled()) {
			Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

			//request to turn on Bluetooth device
			startActivity(intent);
		}
		Set<BluetoothDevice> devices = Adapter.getBondedDevices();
		if (devices.size() > 0)
		{
			for (Iterator<BluetoothDevice> it = devices.iterator(); it.hasNext();)
			{
				BluetoothDevice device = (BluetoothDevice)it.next();
				// print the physical address of the Bluetooth devices
				System.out.println(device.getAddress());
			}
		}
		else
		{
			System.out.println("Hasnt connected to a BT device");
		}

		initLayout();
		/**
		 * 申请的appid时，我们为开发者开通了开放语义（语义理解）
		 * 由于语义理解的场景繁多，需开发自己去开放语义平台：http://www.xfyun.cn/services/osp
		 * 配置相应的语音场景，才能使用语义理解，否则文本理解将不能使用，语义理解将返回听写结果。
		 */
		// 初始化对象
		mSpeechUnderstander = SpeechUnderstander.createUnderstander(UnderstanderDemo.this, mSpeechUdrInitListener);
		mTextUnderstander = TextUnderstander.createTextUnderstander(UnderstanderDemo.this, mTextUdrInitListener);

		// 初始化合成对象
		mTts = SpeechSynthesizer.createSynthesizer(UnderstanderDemo.this, mTtsInitListener);
		mToast = Toast.makeText(UnderstanderDemo.this, "", Toast.LENGTH_SHORT);
	}

	/**
	 * 初始化Layout。
	 */
	private void initLayout(){
		findViewById(R.id.text_understander).setOnClickListener(UnderstanderDemo.this);
		findViewById(R.id.start_understander).setOnClickListener(UnderstanderDemo.this);
		
		mUnderstanderText = (EditText)findViewById(R.id.understander_text);
		
		findViewById(R.id.understander_stop).setOnClickListener(UnderstanderDemo.this);
		findViewById(R.id.understander_cancel).setOnClickListener(UnderstanderDemo.this);
		findViewById(R.id.image_understander_set).setOnClickListener(UnderstanderDemo.this);
		
		mSharedPreferences = getSharedPreferences(UnderstanderSettings.PREFER_NAME, Activity.MODE_PRIVATE);
	}

    /**
     * 初始化监听器（语音到语义）。
     */
    private InitListener mSpeechUdrInitListener = new InitListener() {
    	
		@Override
		public void onInit(int code) {
			Log.d(TAG, "speechUnderstanderListener init() code = " + code);
			if (code != ErrorCode.SUCCESS) {
        		showTip("初始化失败,错误码："+code);
        	}			
		}
    };
    
    /**
     * 初始化监听器（文本到语义）。
     */
    private InitListener mTextUdrInitListener = new InitListener() {

		@Override
		public void onInit(int code) {
			Log.d(TAG, "textUnderstanderListener init() code = " + code);
			if (code != ErrorCode.SUCCESS) {
        		showTip("初始化失败,错误码："+code);
        	}
		}
    };
	private InitListener mTtsInitListener = new InitListener() {
		@Override
		public void onInit(int code) {
			Log.d(TAG, "InitListener init() code = " + code);
			if (code != ErrorCode.SUCCESS) {
				showTip("初始化失败,错误码："+code);
			} else {
				// 初始化成功，之后可以调用startSpeaking方法
				// 注：有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成，
				// 正确的做法是将onCreate中的startSpeaking调用移至这里
			}
		}
	};

	
    
	int ret = 0;// 函数调用返回值
	@Override
	public void onClick(View view) {				
		switch (view.getId()) {
		// 进入参数设置页面
		case R.id.image_understander_set:
			Intent intent = new Intent(UnderstanderDemo.this, UnderstanderSettings.class);
			startActivity(intent);
			break;
		// 开始文本理解
		case R.id.text_understander:
			mUnderstanderText.setText("");
			String text = "合肥明天的天气怎么样？";	
			showTip(text);
			
			if(mTextUnderstander.isUnderstanding()){
				mTextUnderstander.cancel();
				showTip("取消");
			}else {
				ret = mTextUnderstander.understandText(text, mTextUnderstanderListener);
				if(ret != 0)
				{
					showTip("语义理解失败,错误码:"+ ret);
				}
			}
			break;
		// 开始语音理解
		case R.id.start_understander:
			mUnderstanderText.setText("");
			// 设置参数
			setParam_SpeechUnderstander();
	
			if(mSpeechUnderstander.isUnderstanding()){// 开始前检查状态
				mSpeechUnderstander.stopUnderstanding();
				showTip("停止录音");
			}else {
				ret = mSpeechUnderstander.startUnderstanding(mSpeechUnderstanderListener);
				if(ret != 0){
					showTip("语义理解失败,错误码:"	+ ret);
				}else {
					showTip(getString(R.string.text_begin));
				}
			}
			break;
		// 停止语音理解
		case R.id.understander_stop:
			mSpeechUnderstander.stopUnderstanding();
			showTip("停止语义理解");
			break;
		// 取消语音理解
		case R.id.understander_cancel:
			mSpeechUnderstander.cancel();
			showTip("取消语义理解");
			break;
		default:
			break;
		}
	}
	
	private TextUnderstanderListener mTextUnderstanderListener = new TextUnderstanderListener() {
		
		@Override
		public void onResult(final UnderstanderResult result) {
			if (null != result) {
				// 显示
				String text = result.getResultString();
				if (!TextUtils.isEmpty(text)) {
					mUnderstanderText.setText(text);
				}
			} else {
				Log.d(TAG, "understander result:null");
				showTip("识别结果不正确。");
			}
		}
		
		@Override
		public void onError(SpeechError error) {
			// 文本语义不能使用回调错误码14002，请确认您下载sdk时是否勾选语义场景和私有语义的发布
			showTip("onError Code："	+ error.getErrorCode());
			
		}
	};

    /**
     * 语义理解回调。
     */
    private SpeechUnderstanderListener mSpeechUnderstanderListener = new SpeechUnderstanderListener() {

		@Override
		public void onResult(final UnderstanderResult result) {
			if (null != result) {
				Log.d(TAG, result.getResultString());

				// 显示
				String text = result.getResultString();
				if (!TextUtils.isEmpty(text)) {


					//Set the JSON string to be the context of EditText
					mUnderstanderText.setText(text);


					//SAY SOMETHING
					// 设置参数
					setParam_Speaking();
					int code = mTts.startSpeaking("嗨我是車車", mTtsListener);


					/**
					 //			 * 只保存音频不进行播放接口,调用此接口请注释startSpeaking接口
					 //			 * text:要合成的文本，uri:需要保存的音频全路径，listener:回调接口
					 //			*/
					//			String path = Environment.getExternalStorageDirectory()+"/tts.pcm";
					//			int code = mTts.synthesizeToUri(text, path, mTtsListener);

					if (code != ErrorCode.SUCCESS) {
						if(code == ErrorCode.ERROR_COMPONENT_NOT_INSTALLED){
							//未安装则跳转到提示安装页面
							//mInstaller.install();
						}else {
							showTip("语音合成失败,错误码: " + code);
						}
					}
				}
			} else {
				showTip("识别结果不正确。");
			}
		}
    	
        @Override
        public void onVolumeChanged(int volume, byte[] data) {
        	showTip("当前正在说话，音量大小：" + volume);
        	Log.d(TAG, data.length+"");
        }
        
        @Override
        public void onEndOfSpeech() {
        	// 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
        	showTip("结束说话");
        }
        
        @Override
        public void onBeginOfSpeech() {
        	// 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
        	showTip("开始说话");
        }

		@Override
		public void onError(SpeechError error) {
			showTip(error.getPlainDescription(true));
		}

		@Override
		public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
			// 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
			//	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
			//		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
			//		Log.d(TAG, "session id =" + sid);
			//	}
		}
    };

	/**
	 * 合成回调监听。
	 */
	private SynthesizerListener mTtsListener = new SynthesizerListener() {

		@Override
		public void onSpeakBegin() {
			showTip("开始播放");
		}

		@Override
		public void onSpeakPaused() {
			showTip("暂停播放");
		}

		@Override
		public void onSpeakResumed() {
			showTip("继续播放");
		}

		@Override
		public void onBufferProgress(int percent, int beginPos, int endPos,
									 String info) {
			// 合成进度
			mPercentForBuffering = percent;
			showTip(String.format(getString(R.string.tts_toast_format),
					mPercentForBuffering, mPercentForPlaying));
		}

		@Override
		public void onSpeakProgress(int percent, int beginPos, int endPos) {
			// 播放进度
			mPercentForPlaying = percent;
			showTip(String.format(getString(R.string.tts_toast_format),
					mPercentForBuffering, mPercentForPlaying));
		}

		@Override
		public void onCompleted(SpeechError error) {
			if (error == null) {
				showTip("播放完成");
			} else if (error != null) {
				showTip(error.getPlainDescription(true));
			}
		}

		@Override
		public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
			// 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
			// 若使用本地能力，会话id为null
			//	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
			//		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
			//		Log.d(TAG, "session id =" + sid);
			//	}
		}
	};

    @Override
    protected void onDestroy() {
    	super.onDestroy();
        // 退出时释放连接
    	mSpeechUnderstander.cancel();
    	mSpeechUnderstander.destroy();
    	if(mTextUnderstander.isUnderstanding())
    		mTextUnderstander.cancel();
    	mTextUnderstander.destroy();    
    }

	private void showTip(final String str) {
		mToast.setText(str);
		mToast.show();
	}
	
	/**
	 * 参数设置
	 * @param param
	 * @return 
	 */
	public void setParam_SpeechUnderstander(){
		String lang = mSharedPreferences.getString("understander_language_preference", "mandarin");
		if (lang.equals("en_us")) {
			// 设置语言
			mSpeechUnderstander.setParameter(SpeechConstant.LANGUAGE, "en_us");
		}else {
			// 设置语言
			mSpeechUnderstander.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
			// 设置语言区域
			mSpeechUnderstander.setParameter(SpeechConstant.ACCENT, lang);
		}
		// 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
		mSpeechUnderstander.setParameter(SpeechConstant.VAD_BOS, mSharedPreferences.getString("understander_vadbos_preference", "4000"));
		
		// 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
		mSpeechUnderstander.setParameter(SpeechConstant.VAD_EOS, mSharedPreferences.getString("understander_vadeos_preference", "1000"));
		
		// 设置标点符号，默认：1（有标点）
		mSpeechUnderstander.setParameter(SpeechConstant.ASR_PTT, mSharedPreferences.getString("understander_punc_preference", "1"));
		
		// 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
		// 注：AUDIO_FORMAT参数语记需要更新版本才能生效
		mSpeechUnderstander.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
		mSpeechUnderstander.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/sud.wav");
	}

	private void setParam_Speaking(){
		// 清空参数
		mTts.setParameter(SpeechConstant.PARAMS, null);
		// 根据合成引擎设置相应参数
		if(mEngineType.equals(SpeechConstant.TYPE_CLOUD)) {
			mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
			// 设置在线合成发音人
			mTts.setParameter(SpeechConstant.VOICE_NAME, voicer);
			//设置合成语速
			mTts.setParameter(SpeechConstant.SPEED, mSharedPreferences.getString("speed_preference", "50"));
			//设置合成音调
			mTts.setParameter(SpeechConstant.PITCH, mSharedPreferences.getString("pitch_preference", "50"));
			//设置合成音量
			mTts.setParameter(SpeechConstant.VOLUME, mSharedPreferences.getString("volume_preference", "50"));
		}else {
			mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
			// 设置本地合成发音人 voicer为空，默认通过语记界面指定发音人。
			mTts.setParameter(SpeechConstant.VOICE_NAME, "");
			/**
			 * TODO 本地合成不设置语速、音调、音量，默认使用语记设置
			 * 开发者如需自定义参数，请参考在线合成参数设置
			 */
		}
		//设置播放器音频流类型
		mTts.setParameter(SpeechConstant.STREAM_TYPE, mSharedPreferences.getString("stream_preference", "3"));
		// 设置播放合成音频打断音乐播放，默认为true
		mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");

		// 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
		// 注：AUDIO_FORMAT参数语记需要更新版本才能生效
		mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
		mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/tts.wav");
	}
	@Override
	protected void onResume() {
		//移动数据统计分析
		FlowerCollector.onResume(UnderstanderDemo.this);
		FlowerCollector.onPageStart(TAG);
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		//移动数据统计分析
		FlowerCollector.onPageEnd(TAG);
		FlowerCollector.onPause(UnderstanderDemo.this);
		super.onPause();
	}

	// procces the str we get from text
	void str_proccess(String text)
	{

	}
}
