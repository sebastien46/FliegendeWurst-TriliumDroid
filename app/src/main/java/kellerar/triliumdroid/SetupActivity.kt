package kellerar.triliumdroid

import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import kellerar.triliumdroid.databinding.ActivitySetupBinding

class SetupActivity : AppCompatActivity() {
	private lateinit var binding: ActivitySetupBinding

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivitySetupBinding.inflate(layoutInflater)
		setContentView(binding.root)
		binding.setupButton.setOnClickListener { setup() }
	}

	fun setup() {
		val server = binding.server.text.toString().trimEnd('/')
		val username = binding.username.text.toString()
		val password = binding.password.text.toString()
		if (server == "" || username == "" || password == "") {
			return
		}
		val prefs = PreferenceManager.getDefaultSharedPreferences(this)
		prefs.edit()
				.putString("hostname", server)
				.putString("username", username)
				.putString("password", password)
				.apply()
		val looper = applicationContext.mainLooper
		val handler = Handler(looper)
		ConnectionUtil.connect(this, server, password) {
			handler.post {
				Cache.sync(this) {
					finish() // TODO: check success status
				}
			}
		}
	}
}