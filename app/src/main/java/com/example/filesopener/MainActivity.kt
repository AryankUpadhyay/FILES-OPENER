package com.example.filesopener

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.SurfaceHolder
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import coil.load
import com.example.filesopener.databinding.ActivityMainBinding
import com.filestack.Config
import com.filestack.FileLink
import com.filestack.Sources
import com.filestack.android.FilestackPicker
import com.filestack.android.FsConstants
import com.filestack.android.Selection
import com.filestack.android.Theme
import java.util.*

class MainActivity : AppCompatActivity(),MediaPlayer.OnPreparedListener {

    private val TAG = MainActivity::class.java.simpleName

    private val displayUrl =  "https://cdn.filestackcontent.com/"
    private var videoUrl:String?= null
    private val mediaPlayer = MediaPlayer()

    private  val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private  val API_KEY = "AKQon9JvSeObrRqEB2i4Az"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)


        val intentFilter = IntentFilter(FsConstants.BROADCAST_UPLOAD)
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(uploadReceiver, intentFilter)

        binding.selectfab.setOnClickListener{
            launchpicker()
        }

        mediaPlayer.setOnPreparedListener(this)
        binding.playButton.isEnabled = true
        binding.playButton.setOnClickListener{
            if(mediaPlayer.isPlaying){
                mediaPlayer.pause()
                binding.playButton.setImageResource(android.R.drawable.ic_media_play)
            }else{
                mediaPlayer.start()
                binding.playButton.setImageResource(android.R.drawable.ic_media_pause)
            }
        }
        mediaPlayer.setOnCompletionListener {
            binding.playButton.setImageResource(android.R.drawable.ic_media_play)
        }

    }

    private fun launchpicker(){
        val mimeTypes = mutableListOf("application/pdf", "image/*", "video/*")
        val config =  Config(API_KEY)
        val sources = mutableListOf(
            Sources.CAMERA,
            Sources.DEVICE,
            Sources.GOOGLE_DRIVE,
            Sources.FACEBOOK,
            Sources.INSTAGRAM,
            Sources.AMAZON_DRIVE,
        )

        val theme = Theme.Builder()
            .accentColor(ContextCompat.getColor(this, R.color.white))
            .backgroundColor(
                ContextCompat.getColor(this, R.color.design_default_color_primary_variant)
            )
            .textColor(ContextCompat.getColor(this, R.color.white))
            .build()

        val picker = FilestackPicker.Builder()
            .config(config)
            .sources(sources)
            .autoUploadEnabled(true)
            .mimeTypes(mimeTypes)
            .multipleFilesSelectionEnabled(false)
            .theme(theme)
            .build()
        picker.launch(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(FilestackPicker.canReadResult(requestCode, resultCode)){
            val selections = FilestackPicker.getSelectedFiles(data)
            val selection = selections[0]
            val name = String.format(Locale.ROOT, selection.name)
            Log.i(TAG, "$name has been selected")
        }
    }

    private val uploadReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val status = intent?.getStringExtra(FsConstants.EXTRA_STATUS)
            val selection = intent?.getParcelableExtra<Selection>(FsConstants.EXTRA_SELECTION)
            val fileLink = intent?.getSerializableExtra(FsConstants.EXTRA_FILE_LINK)
            as FileLink
            val name = selection?.name
            val handle = if(!fileLink.handle.isNullOrBlank()) fileLink.handle else "NO LINK FOUND"

            Log.d(TAG, "$handle and $name and ${selection?.mimeType} has been received")
            when{
                selection?.mimeType!!.contains("image")->{
                    binding.imageview.isVisible = true
                    binding.videoview.isVisible=false
                    binding.playButton.isVisible=false
                    binding.imageview.load("$displayUrl$handle"){
                        size(500, 500)
                    }
                }
                selection.mimeType!!.contains("pdf")->{
                    val browserIntent = Intent(Intent.ACTION_VIEW,
                        Uri.parse("$displayUrl$handle"))
                    startActivity(browserIntent)
                }
                selection?.mimeType!!.contains("video")->{
                    binding.imageview.isVisible = false
                    binding.videoview.isVisible=true
                    binding.playButton.isVisible=true
                    videoUrl = "$displayUrl$handle"
                    binding.videoview.holder.addCallback(object: SurfaceHolder.Callback{
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            mediaPlayer.apply{
                                if(videoUrl!=null){
                                    reset()
                                    setDataSource(videoUrl)
                                    setDisplay(holder)
                                    prepareAsync()
                                }
                            }
                        }

                        override fun surfaceChanged(
                            holder: SurfaceHolder,
                            format: Int,
                            width: Int,
                            height: Int
                        ) {
                            TODO("Not yet implemented")
                        }

                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            TODO("Not yet implemented")
                        }

                    })
                }

            }
        }

    }


    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(uploadReceiver)
        mediaPlayer.release()
    }

    override fun onPrepared(mp: MediaPlayer?) {
        binding.playButton.isEnabled = true
    }

}