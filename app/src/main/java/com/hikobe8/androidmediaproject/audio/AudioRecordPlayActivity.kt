package com.hikobe8.androidmediaproject.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import com.hikobe8.androidmediaproject.*
import com.hikobe8.androidmediaproject.audio.AudioRecordCapturer.Companion.DEFAULT_SAMPLE_SIZE
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_audio_record_play.*
import java.io.File
import java.lang.ref.WeakReference

class AudioRecordPlayActivity : BaseActivity(), View.OnClickListener, AudioAdapter.OnItemClickListener,
    AudioRecordCapturer.OnRecordCompleteListener, AudioTrackPlayer.OnPlayCompletelyListener {

    override fun onPlayCompletely() {
        runOnUiThread {
            mRecordAdapter.stop()
        }
    }

    override fun onRecordCompleted(audioRecordBean: AudioRecordBean?) {
        if (audioRecordBean != null) {
            mRecordAdapter.addData(audioRecordBean)
        }
    }

    override fun onPlayClicked(audioRecordBean: AudioRecordBean) {
        mRecordAdapter.update(audioRecordBean)
        mAudioTrackPlayer.play(audioRecordBean.path)
    }

    override fun onStopClicked(audioRecordBean: AudioRecordBean) {
        mAudioTrackPlayer.stop()
    }

    companion object {
        fun launch(context: Context) {
            context.startActivity(Intent(context, AudioRecordPlayActivity::class.java))
        }
    }

    val mCompositeDisposable: CompositeDisposable = CompositeDisposable()

    class TimerHandler(activity: AudioRecordPlayActivity) : Handler() {

        private var mWeakReference: WeakReference<AudioRecordPlayActivity> = WeakReference(activity)

        private var mSeconds = 0

        fun start() {
            sendMsg()
        }

        fun stop() {
            mSeconds = 0
            removeMessages(0)
        }

        private fun sendMsg() {
            mSeconds++
            sendEmptyMessageDelayed(0, 1000L)
        }

        @SuppressLint("StringFormatMatches")
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            val activity = mWeakReference.get()
            if (activity != null) {
                activity.tv_duration.text = activity.resources.getString(R.string.text_recording, mSeconds)
                sendMsg()
            }
        }
    }

    private val mAudioRecordCapturer: AudioRecordCapturer by lazy {
        val audioRecordCapturer = AudioRecordCapturer()
        audioRecordCapturer.mOnRecordCompleteListener = this
        audioRecordCapturer
    }

    private val mTimeHandler: TimerHandler by lazy {
        TimerHandler(this)
    }

    private val mAudioTrackPlayer:AudioTrackPlayer by lazy {
        val audioTrackPlayer = AudioTrackPlayer()
        audioTrackPlayer.mOnPlayCompletelyListener = this
        audioTrackPlayer
    }

    private val mRecordAdapter: AudioAdapter by lazy {
        val audioAdapter = AudioAdapter()
        audioAdapter.mOnItemClickListener = this
        audioAdapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_record_play)
        setSupportActionBar(toolbar)
        setHomeAsUpEnabled()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (PermissionUtils.checkPermissionGranted(mActivity, Manifest.permission.RECORD_AUDIO)
                && PermissionUtils.checkPermissionGranted(mActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            ) {
                initViews()
            } else {
                val permissionArray: ArrayList<String> = ArrayList()
                if (!PermissionUtils.checkPermissionGranted(mActivity, Manifest.permission.RECORD_AUDIO)) {
                    permissionArray.add(Manifest.permission.RECORD_AUDIO)
                }
                if (!PermissionUtils.checkPermissionGranted(mActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    permissionArray.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
                PermissionUtils.requestPermission(mActivity, permissionArray.toArray(arrayOf()), 10)
            }
        } else {
            initViews()
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_start -> {
                mAudioRecordCapturer.startCapture()
                mTimeHandler.start()
                tv_duration.text = resources.getString(R.string.text_recording, 0)
                tv_duration.visibility = View.VISIBLE
            }
            R.id.btn_stop -> {
                mTimeHandler.stop()
                mAudioRecordCapturer.stopCapture()
                tv_duration.visibility = View.INVISIBLE
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 10 && permissions.isNotEmpty()) {
            initViews()
        } else {
            finish()
        }
    }

    private fun initViews() {
        btn_start.setOnClickListener(this)
        btn_stop.setOnClickListener(this)
        rv_audios.layoutManager = LinearLayoutManager(this)
        rv_audios.setHasFixedSize(true)
        rv_audios.adapter = mRecordAdapter
        rv_audios.addItemDecoration(DividerItemDecoration(mActivity, RecyclerView.VERTICAL))
        Observable.create(ObservableOnSubscribe<AudioRecordBean> {
            val rootDir = File(FileUtils.getStorageDir())
            val files = rootDir.listFiles()
            for (file in files) {
                it.onNext(
                    AudioRecordBean(
                        file.absolutePath.substring(file.absolutePath.lastIndexOf(File.separator) + 1),
                        file.absolutePath,
                        file.length() / (DEFAULT_SAMPLE_SIZE * 2 * 2)
                    )
                )
            }
            it.onComplete()
        }).subscribeOn(Schedulers.io())
            .unsubscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<AudioRecordBean> {
                override fun onComplete() {

                }

                override fun onSubscribe(d: Disposable) {
                    mCompositeDisposable.add(d)
                }

                override fun onNext(audioRecordBean: AudioRecordBean) {
                    mRecordAdapter.addData(audioRecordBean)
                }

                override fun onError(e: Throwable) {
                }

            })
    }

    override fun onDestroy() {
        super.onDestroy()
        mAudioRecordCapturer.stopCapture()
        mCompositeDisposable.dispose()
    }


}
