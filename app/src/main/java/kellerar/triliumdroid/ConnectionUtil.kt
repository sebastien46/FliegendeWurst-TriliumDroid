package kellerar.triliumdroid

import android.content.SharedPreferences
import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.lang.Exception


object ConnectionUtil {
	private const val TAG: String = "ConnectionUtil"
	private var client: OkHttpClient? = null
	var server: String = "http://0.0.0.0"
	private var prefs: SharedPreferences? = null
	private var csrf: String = ""
	private var loginFails = 0

	fun setup(prefs: SharedPreferences, callback: () -> Unit, callbackError: (Exception) -> Unit) {
		this.prefs = prefs
		server = prefs.getString("hostname", null)!!
		val password = prefs.getString("password", null)!!
		connect(server, password, callback, callbackError)
	}

	fun connect(
		server: String,
		password: String,
		callback: () -> Unit,
		callbackError: (Exception) -> Unit
	) {
		client = OkHttpClient.Builder()
			.cookieJar(object : CookieJar {
				// TODO: this is a terrible cookie jar
				private var cookieStore: MutableList<Cookie> = ArrayList()
				override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
					for (cookie in cookies) {
						Log.i(TAG, "cookie ${cookie.path} ${cookie.name} = ${cookie.value}")
						if (cookie.name == "_csrf") {
							csrf = cookie.value
						}
						var added = false
						for (i in cookieStore.indices) {
							if (cookieStore[i].name == cookie.name) {
								cookieStore[i] = cookie
								added = true
								break
							}
						}
						if (!added) {
							cookieStore.add(cookie)
						}
					}
				}

				override fun loadForRequest(url: HttpUrl): List<Cookie> {
					return cookieStore
				}
			})
			.build()

		this.server = server
		val obj = JSONObject()
		obj.put("password", password)
		Log.i(TAG, "$server/login")
		val form = FormBody.Builder()
			.add("password", password)
			.add("remember_me", "1")
			.build()
		val req = Request.Builder()
			.url("$server/login")
			.post(form)
			.build()
		Log.i(TAG, "/login")
		client!!.newCall(req).enqueue(object : Callback {
			override fun onResponse(call: Call, response: Response) {
				response.use {
					loginFails = 0
					callback()
				}
			}

			override fun onFailure(call: Call, e: IOException) {
				Log.e(TAG, "fail", e)

				loginFails++
				callbackError(e)
			}
		})
	}

	fun doSyncRequest(uri: String, callback: (JSONObject) -> Unit) {
		if (csrf == "") {
			Log.e(TAG, "tried to sync without token")
			return
		}
		val req = Request.Builder()
			.get()
			.url("$server$uri")
			.build()
		Log.i(TAG, uri)
		client!!.newCall(req).enqueue(object : Callback {
			override fun onResponse(call: Call, response: Response) {
				response.use {
					callback(JSONObject(response.body!!.string()))
				}
			}

			override fun onFailure(call: Call, e: IOException) {
				Log.e(TAG, "failed to fetch sync data", e)
			}
		})
	}
}