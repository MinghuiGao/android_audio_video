package bf.cloud.datasource;

import java.io.IOException;
import java.util.List;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.Log;

/**
 * ͨ������ͷ¼����Ƶ
 * 
 * @author wangtonggui
 * 
 */
public class CameraRecorder {
	private String TAG = CameraRecorder.class.getSimpleName();
	private Camera mCamera = null;
	private SurfaceTexture mSurfaceTexture = null;
	private Parameters parameters;
	private int bufferSize;
	private byte[] gBuffer;
	private OnCollectingVideoDataCallback mCallback = null;
	private boolean mIsCollecting = false;
	private int mCameraIndex = -1;
	private long mTimeStamp = 0;
	private long mBaseTimeStamp = -1;

	/**
	 * 
	 * @param cameraIndex
	 *            �������
	 */
	public CameraRecorder(int cameraIndex) {
		mSurfaceTexture = new SurfaceTexture(10);
		if (cameraIndex >= 0 && cameraIndex <= 1)
			mCameraIndex = cameraIndex;
		else
			Log.d(TAG, "cameraIndex is error");
	}

	/**
	 * ������ҪԤ���ı���
	 * 
	 * @param st
	 * ��Ҫ��Ҫ�ı���
	 */
	public void setSurfaceTexture(SurfaceTexture st) {
		mSurfaceTexture = st;
	}

	/**
	 * ��������
	 * 
	 * @param bitrate
	 */
	public void setBitRate(int bitrate) {

	}

	/**
	 * ����֡��
	 * 
	 * @param framerate
	 */
	public void setFrameRate(int framerate) {

	}

	/**
	 * ���ùؼ�֡���
	 * 
	 * @param interval ��λΪ��
	 */
	public void setIFrameInterval(int interval) {

	}

	/**
	 * ������ͷ�������ص���������
	 */
	private void init() {
		mCamera.setDisplayOrientation(90);
		try {
			mCamera.setPreviewTexture(mSurfaceTexture);
		} catch (IOException e) {
			Log.d(TAG, e.getMessage());
		}

		parameters = mCamera.getParameters();
		List<Size> preSize = parameters.getSupportedPreviewSizes();
		int previewWidth = 640;// preSize.get(0).width;
		int previewHeight = 480;// preSize.get(0).height;

		Log.d(TAG, "preSize = " + preSize.size() + "/preview = " + previewWidth
				+ "..." + previewHeight);
		parameters.setPreviewSize(previewWidth, previewHeight);
		parameters.setPreviewFormat(ImageFormat.NV21);
		mCamera.setParameters(parameters);
		bufferSize = previewWidth * previewHeight;
		bufferSize = bufferSize
				* ImageFormat.getBitsPerPixel(parameters.getPreviewFormat())
				/ 8;
		gBuffer = new byte[bufferSize];
		mCamera.setPreviewCallbackWithBuffer(new PreviewCallback() {

			@Override
			public void onPreviewFrame(byte[] data, Camera camera) {
				camera.addCallbackBuffer(gBuffer);
				try {
					mSurfaceTexture.updateTexImage();
				} catch (Exception e) {
					Log.d(TAG, e.getMessage());
				}
				if (mBaseTimeStamp < 0) {
					mBaseTimeStamp = mSurfaceTexture.getTimestamp() / 1000;
				}
				mTimeStamp = mSurfaceTexture.getTimestamp() / 1000
						- mBaseTimeStamp;
				// �ص���ԭʼ��Ƶ����
				if (mCallback != null) {
					mCallback.onData(data, mTimeStamp / 1000);
				}
			}
		});
		mCamera.setErrorCallback(new ErrorCallback() {

			@Override
			public void onError(int error, Camera camera) {
				Log.d(TAG, "onerror");
			}
		});

		int[] frameRate = new int[2];
		parameters.getPreviewFpsRange(frameRate);
		int averageFrameRate = (frameRate[0] + frameRate[1]) / 2;
		Log.d(TAG, "Camera getPreviewFpsRange = " + averageFrameRate);
	}

	/**
	 * ͨ�����ԣ���stop��ʱ�������������stopPreview����������startPreview���¿�����
	 * �������ﲻ���Ѷ�ѡ����release��������
	 */
	public void open() {
		if (mIsCollecting)
			return;
		if (mCamera == null)
			mCamera = Camera.open(mCameraIndex);
		init();
		Log.d(TAG, "startCollecting");
		mCamera.stopPreview();
		Log.d(TAG, gBuffer.length + "");
		mCamera.addCallbackBuffer(gBuffer);
		mCamera.startPreview();
		mIsCollecting = true;
	}

	/**
	 * ֹͣԤ�������ͷ�����ͷ
	 */
	public void close() {
		if (!mIsCollecting)
			return;
		try {
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
			mIsCollecting = false;
		} catch (Exception e) {
			Log.d(TAG, e.getMessage());
		}
	}

	/**
	 * �ͷ�����ͷ��ֻ�е���release����������ſ���ʹ��
	 */
	public void release() {
		if (mCamera != null)
			mCamera.release();
	}

	/**
	 * ���û�ȡ����ͷԴ���ݻص�����ʱ�Ȳ��������������ó�Ϊprivate
	 * 
	 * @param cb �ص�
	 */
	public void setDataCallback(OnCollectingVideoDataCallback cb) {
		mCallback = cb;
	}

	public static interface OnCollectingVideoDataCallback {
		void onData(byte[] data, long timeStamp);
	}
}
