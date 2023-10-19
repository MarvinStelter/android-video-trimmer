package idv.luchafang.videotrimmerexample

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ClippingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import idv.luchafang.videotrimmer.VideoTrimmerView
import idv.luchafang.videotrimmerexample.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity(), VideoTrimmerView.OnSelectedRangeChangedListener {
    private val REQ_PICK_VIDEO = 100
    private val REQ_PERMISSION = 200

    private val player: SimpleExoPlayer by lazy {
        ExoPlayerFactory.newSimpleInstance(this).also {
            it.repeatMode = SimpleExoPlayer.REPEAT_MODE_ALL
            binding.playerView.player = it
        }
    }

    private val dataSourceFactory: DataSource.Factory by lazy {
        DefaultDataSourceFactory(this, "VideoTrimmer")
    }

    private var videoPath: String = ""
    private lateinit var binding: ActivityMainBinding // ViewBinding

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Request permissions in the onCreate method
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_MEDIA_VIDEO),
            REQ_PERMISSION
        )

        binding.pickVideoBtn.setOnClickListener {
            Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                .apply {
                    type = "video/*"
                }
                .also { startActivityForResult(it, REQ_PICK_VIDEO) }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQ_PICK_VIDEO -> {
                if (resultCode == Activity.RESULT_OK) {
                    videoPath = getRealPathFromMediaData(data?.data)
                    displayTrimmerView(videoPath)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQ_PERMISSION && grantResults.firstOrNull() != PackageManager.PERMISSION_GRANTED) {
            finish()
        }
    }

    override fun onSelectRangeStart() {
        player.playWhenReady = false
    }

    override fun onSelectRange(startMillis: Long, endMillis: Long) {
        showDuration(startMillis, endMillis)
    }

    override fun onSelectRangeEnd(startMillis: Long, endMillis: Long) {
        showDuration(startMillis, endMillis)
        playVideo(videoPath, startMillis, endMillis)
    }

    private fun displayTrimmerView(path: String) {
        binding.videoTrimmerView
            .setVideo(File(path))
            .setMaxDuration(30_000)
            .setMinDuration(3_000)
            .setFrameCountInWindow(8)
            .setExtraDragSpace(dpToPx(2f))
            .setOnSelectedRangeChangedListener(this)
            .show()
    }

    private fun playVideo(path: String, startMillis: Long, endMillis: Long) {
        if (path.isBlank()) return

        val source = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(Uri.parse(path))
            .let {
                ClippingMediaSource(
                    it,
                    startMillis * 1000L,
                    endMillis * 1000L
                )
            }

        player.playWhenReady = true
        player.prepare(source)
    }

    private fun getRealPathFromMediaData(data: Uri?): String {
        data ?: return ""

        val cursor = contentResolver.query(
            data,
            arrayOf(MediaStore.Video.Media.DATA),
            null,
            null,
            null
        )

        return cursor?.use {
            it.moveToFirst()
            val col = it.getColumnIndex(MediaStore.Video.Media.DATA)
            it.getString(col)
        } ?: ""
    }

    private fun showDuration(startMillis: Long, endMillis: Long) {
        val duration = (endMillis - startMillis) / 1000L
        binding.durationView.text = "$duration seconds selected"
    }

    private fun dpToPx(dp: Float): Float {
        val density = resources.displayMetrics.density
        return dp * density
    }
}
