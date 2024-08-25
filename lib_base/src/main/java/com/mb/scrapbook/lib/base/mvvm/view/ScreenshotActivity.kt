package com.mb.scrapbook.lib.base.mvvm.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.hardware.display.DisplayManager
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.BarUtils
import com.mb.scrapbook.lib.base.R
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 屏幕截屏
 *
 * @author donglin6
 * @date 2024/06/21
 */
class ScreenshotActivity : AppCompatActivity() {

    /** 伴生对象 */
    companion object {
        /** 日志 */
        private const val TAG = "MB-Screenshot"
        /** 虚拟屏名字 */
        private const val VIRTUAL_SCREEN_NAME = "mb-virtual-screen"

        /** 截屏延迟500毫秒 */
        private const val START_CAPTURE_SCREEN_DELAY = 500L
        /** 截屏图片地址 */
        const val EXTRA_PATH_SCREENSHOT = "extraPathScreenshot"
        /** 截屏图片尺寸 */
        const val EXTRA_RECT_SCREENSHOT = "extraRectScreenshot"
        /** 截图申请 */
        const val REQUEST_MEDIA_PROJECTION = 0xDD00D1
    }

    /** 最后一次图片保存地址(from intent) */
    private var mScreenshotPath: String? = null
    /** 最后一次图片保存尺寸(from intent) */
    private var mScreenshotRect: Rect? = null
    /** 最后一次截屏对象 */
    private var mCaptureScreen: MediaProjection? = null
    /** 本次已截取屏幕 */
    private val alreadyCaptureScreen = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.screenshot_activity)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        BarUtils.setStatusBarLightMode(this, true)
        sendScreenshot() // 开始截屏
    }

    /** 截图 */
    private fun sendScreenshot() {
        intent?.let { intent ->
            intent.extras?.let { extras ->
                // 图片路径
                mScreenshotPath = extras.getString(EXTRA_PATH_SCREENSHOT)
                // 图片尺寸
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    mScreenshotRect = extras.getParcelable(EXTRA_RECT_SCREENSHOT, Rect::class.java)
                } else {
                    mScreenshotRect = extras.getParcelable(EXTRA_RECT_SCREENSHOT)
                }
                // 申请截屏
                val mgrMediaProjection = (getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager)
                startActivityForResult(mgrMediaProjection.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION)
            }
        }
    }

    override fun onActivityResult(reqCode: Int, resCode: Int, data: Intent?) {
        super.onActivityResult(reqCode, resCode, data)
        if (Activity.RESULT_OK == resCode) {
            if (REQUEST_MEDIA_PROJECTION == reqCode) {
                data?.let { startScreenshot(resCode, it) } // 通过Virtual截屏
            }
        }
    }

    /** 截屏操作 */
    private fun startScreenshot(resultCode: Int, data: Intent) {
        // 1、创建MediaProjection令牌
        val mgrMediaProjection = (getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager)
        mCaptureScreen = mgrMediaProjection.getMediaProjection(resultCode, data)
        // 2、发送延迟截屏事件
        val mH = Handler(Looper.getMainLooper())
        mH.postDelayed({ setupImageReader() // 初始化截屏
        }, START_CAPTURE_SCREEN_DELAY) // 500/ms 必须延迟发送，否则截屏为黑色
    }

    /** 初始化截屏 */
    private fun setupImageReader() {
        val dm = resources.displayMetrics
        ImageReader.newInstance(
                dm.widthPixels, dm.heightPixels, PixelFormat.RGBA_8888, 1
        ).apply {
            setOnImageAvailableListener({
                if (alreadyCaptureScreen.compareAndSet(false, true)) {
                    saveImageTask(it)
                }
            }, null)
            // 使用virtual映射截屏
            mCaptureScreen?.createVirtualDisplay(
                VIRTUAL_SCREEN_NAME, dm.widthPixels, dm.heightPixels, dm.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, surface, null, null)
        }
    }

    /** 保存图片 */
    private fun saveImageTask(reader: ImageReader) {
        var image: Image? = null
        try {
            // 获取捕获的照片数据
            image = reader.acquireLatestImage()
            val width = image.width
            val height = image.height
            val plane = image.planes[0]

            // 因为内存对齐问题，每个 buffer 宽度不同所以通过pixelStride * width 得到大概的宽度，
            // 然后通过 rowStride 去减，得到大概的内存偏移量，不过一般都是对齐的。
            val buffer: ByteBuffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * width
            val bitmap = Bitmap.createBitmap(
                width + rowPadding / pixelStride,
                height, Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            // 裁剪bitmap至屏幕尺寸后保存至本地
            mScreenshotRect?.let { rect -> saveBitmapToPath(
                Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height())
            ) }
            mCaptureScreen?.stop()
            // stopForeground(true)
        } catch (e: Exception) {
            Log.e(TAG, ":-> Save image failed. $e")
        } finally {
            try {
                image?.close()
                // 关闭截图Activity
                finish()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    /** 保存图片至 mScreenshotPath 目录 */
    private fun saveBitmapToPath(bitmap: Bitmap) {
        mScreenshotPath?.let { path ->
            val original = File(path)
            if (original.isFile) {
                original.parentFile?.mkdirs()
            }
            if (!original.exists()) {
                original.createNewFile()
            }
            try {
                Log.d(TAG, "Image Path: $path")
                FileOutputStream(original).use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            } catch (exception: Exception) {
                Log.e(TAG, ":-> Save bitmap failed. $exception")
            }
        }
    }

}