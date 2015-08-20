package com.example.audiorecod;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class AudioRecorder {
	private static final String TAG = "AudioRecorder";
	private static final int SAMPLE_RATE = 44100; // ������(CD����)
	private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO; // ��Ƶͨ��(������)
	private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT; // ��Ƶ��ʽ
	private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC; // ��ƵԴ����˷磩
	private boolean is_recording = false;
	private RecorderTask recorderTask = new RecorderTask();

	private int mAudioSource = -1;
	private int mSampleRateInHz = -1;
	private int mChannelConfig = -1;
	private int mAudioFormat = -1;

	private OnCollectingAudioDataCallback mCallback = null;
	private AudioRecord mAudioRecord = null;
	private int bufferSizeInBytes;
	public short[] buffer;

	/**
	 * ���캯��������ͬ android.media.AudioRecord
	 * 
	 * @param audioSource
	 * @param sampleRateInHz
	 * @param channelConfig
	 * @param audioFormat
	 */
	public AudioRecorder(int audioSource, int sampleRateInHz,
			int channelConfig, int audioFormat) {
		// ������Ĭ�ϵĲ���
		mAudioSource = AUDIO_SOURCE;
		mSampleRateInHz = SAMPLE_RATE;
		mChannelConfig = CHANNEL_CONFIG;
		mAudioFormat = AUDIO_FORMAT;
		// ��ȡ��С��������С
		bufferSizeInBytes = AudioRecord.getMinBufferSize(mSampleRateInHz,
				mChannelConfig, mAudioFormat);
		Log.d(TAG, "getMinBufferSize = " + bufferSizeInBytes);
		mAudioRecord = new AudioRecord(mAudioSource, // ��ƵԴ
				mSampleRateInHz, // ������
				mChannelConfig, // ��Ƶͨ��
				mAudioFormat, // ��Ƶ��ʽ
				bufferSizeInBytes // ������
		);
	}

	/*
	 * ��ʼ¼��
	 */
	public void startAudioRecording() {
		if (!is_recording)
			new Thread(recorderTask).start();
	}

	/*
	 * ֹͣ¼��
	 */
	public void stopAudioRecording() {
		if (is_recording){
			is_recording = false;
		}else
			return;
		if (mAudioRecord != null) {
			mAudioRecord.setRecordPositionUpdateListener(null);
			mAudioRecord.stop();
		}
	}

	/**
	 * ����recorder������ռ��Ӳ����Դ
	 * 
	 * @author wangtonggui
	 */
	public void releaseAudioRecorder() {
		is_recording = false;
		if (mAudioRecord.getState() == AudioRecord.RECORDSTATE_RECORDING)
			mAudioRecord.stop();
		mAudioRecord.release();
	}

	class RecorderTask implements Runnable {
		int bufferReadResult = 0;
		public int samples_per_frame = 4096;

		@Override
		public void run() {
			if (mAudioRecord.getState() == AudioRecord.STATE_UNINITIALIZED)
				mAudioRecord = new AudioRecord(mAudioSource, // ��ƵԴ
						mSampleRateInHz, // ������
						mChannelConfig, // ��Ƶͨ��
						mAudioFormat, // ��Ƶ��ʽ
						bufferSizeInBytes // ������
				);
			mAudioRecord.startRecording();
			is_recording = true;

			while (is_recording) {
				buffer = new short[samples_per_frame];
				// �ӻ������ж�ȡ���ݣ����뵽buffer�ֽ�����������
				bufferReadResult = mAudioRecord.read(buffer, 0,
						samples_per_frame);
				Log.d(TAG, "" + bufferReadResult + "/" + samples_per_frame);
				// �ж��Ƿ��ȡ�ɹ�
				if (bufferReadResult == AudioRecord.ERROR_BAD_VALUE
						|| bufferReadResult == AudioRecord.ERROR_INVALID_OPERATION)
					Log.e(TAG, "Read error");
				else{
					FileOperator1.append(buffer, bufferReadResult);
					if (mAudioRecord != null) {
						if (mCallback != null) {
//							mCallback.onData(buffer, System.nanoTime());
						}
					}
				}
			}
		}
	}

	public void setOnCollectingAudioDataCallback(
			OnCollectingAudioDataCallback cb) {
		mCallback = cb;
	}

	/**
	 * ʹ������ӿ�����ȡ��Ƶ����
	 * 
	 * @author wangtonggui
	 */
	public static interface OnCollectingAudioDataCallback {
		void onData(byte[] data, long timeStamp);
	}
}
