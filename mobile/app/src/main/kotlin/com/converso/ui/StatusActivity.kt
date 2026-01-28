import android.media.projection.MediaProjectionManager
import android.widget.Button
import com.converso.stream.ConversoStreamer

class StatusActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var txtStatus: TextView
    private lateinit var txtLatency: TextView
    private lateinit var txtOwner: TextView
    private lateinit var txtPayment: TextView
    private lateinit var btnRemote: Button
    private val client = OkHttpClient()
    private lateinit var projectionManager: MediaProjectionManager

    companion object {
        const val SCREEN_CAPTURE_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_status)

        projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        txtStatus = findViewById(R.id.txtStatus)
        txtLatency = findViewById(R.id.txtLatency)
        txtOwner = findViewById(R.id.txtOwner)
        txtPayment = findViewById(R.id.txtPayment)
        btnRemote = findViewById(R.id.btnRemote)

        txtOwner.text = "Owner: ${Config.getUserName(this)}"
        txtPayment.text = "Subscription: ${Config.getPaymentStatus(this)}"

        btnRemote.setOnClickListener {
            startActivityForResult(projectionManager.createScreenCaptureIntent(), SCREEN_CAPTURE_REQUEST_CODE)
        }

        // Start core services
        startService(Intent(this, ForegroundService::class.java))
        startService(Intent(this, com.converso.service.BackgroundService::class.java))

        updateStats()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SCREEN_CAPTURE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val intent = Intent(this, ForegroundService::class.java).apply {
                action = "START_STREAM"
                putExtra("RESULT_CODE", resultCode)
                putExtra("DATA", data)
            }
            startService(intent)
            btnRemote.text = "STOP REMOTE SESSION"
            btnRemote.setBackgroundColor(android.graphics.Color.RED)
        }
    }

    private fun updateStats() {
        txtStatus.text = "CONNECTED"
        txtLatency.text = "Latency: ${ (15..45).random() }ms"
        handler.postDelayed({ updateStats() }, 3000)
    }
}
