package kellerar.triliumdroid

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.widget.addTextChangedListener
import androidx.navigation.fragment.NavHostFragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kellerar.triliumdroid.databinding.ActivityMainBinding
import java.util.TreeMap


class MainActivity : AppCompatActivity() {
	private lateinit var binding: ActivityMainBinding
	private var jumpRequestId = 0

	companion object {
		private const val TAG = "MainActivity"
		public const val JUMP_TO_NOTE_ENTRY = "JUMP_TO_NOTE_ENTRY"
		var tree: TreeItemAdapter? = null
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		binding = ActivityMainBinding.inflate(layoutInflater)
		setContentView(binding.root)
		val handler = Handler(applicationContext.mainLooper)

		Cache.initializeDatabase(applicationContext)

		val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
		val adapter = TreeItemAdapter {
			navigateTo(it.note)
		}
		binding.treeList.adapter = adapter
		tree = adapter

		binding.fab.setOnClickListener {
			val dialog = AlertDialog.Builder(this)
				.setTitle(R.string.jump_to_dialog)
				.setView(R.layout.dialog_jump)
				.create()
			dialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
			dialog.show()
			val input = dialog.findViewById<EditText>(R.id.jump_input)!!
			val list = dialog.findViewById<RecyclerView>(R.id.jump_to_list)!!
			val adapter2 = TreeItemAdapter {
				dialog.dismiss()
				scrollTreeTo(it.note)
				navigateTo(it.note)
			}
			list.adapter = adapter2
			input.requestFocus()
			input.addTextChangedListener {
				val searchString = input.text.toString()
				if (searchString.length < 3) {
					adapter2.submitList(emptyList())
					return@addTextChangedListener
				}
				val results = Cache.getJumpToResults(searchString)
				val m = TreeMap<Int, Branch>()
				val stuff = results.map {
					Pair(
						Branch(JUMP_TO_NOTE_ENTRY, it.id, null, 0, null, false, m),
						0
					)
				}.toList()
				if (adapter2.currentList != stuff) {
					adapter2.submitList(stuff)
				}
				/* TODO: dispatch sql query on I/O thread
				lifecycleScope.launch(Dispatchers.IO) {

				}
				 */
			}
		}

		if (prefs.getString("hostname", null) == null) {
			Log.i(TAG, "starting setup!")
			val intent = Intent(this, SetupActivity::class.java)
			startActivity(intent)
		} else {
			ConnectionUtil.setup(this, prefs) {
				handler.post {
					Cache.sync(this) {
						Cache.getTreeData()
						handler.post {
							val items = Cache.getTreeList("root", 0)
							Log.i(TAG, "about to show ${items.size} tree items")
							tree!!.submitList(items)
							getNoteFragment().load("root", true)
							scrollTreeTo("root")
						}
					}
				}
			}

			try {
				binding.drawerLayout.openDrawer(GravityCompat.START)
			} catch (t: Throwable) {
				Log.e("Main", "fatality!", t)
			}
		}
	}

	public fun scrollTreeTo(noteId: String) {
		tree!!.select(noteId)
		val pos = Cache.branchPosition[noteId] ?: return
		(binding.treeList.layoutManager!! as LinearLayoutManager).scrollToPositionWithOffset(pos, 5)
		//val treeItem = binding.treeList.getChildAt(pos)
	}

	public fun navigateTo(noteId: String) {
		tree!!.select(noteId)
		supportFragmentManager.beginTransaction()
			.replace(R.id.fragment_container, NoteFragment(noteId, true))
			.addToBackStack(null)
			.commit()
		binding.drawerLayout.closeDrawers()
	}

	override fun onDestroy() {
		super.onDestroy()
		Cache.closeDatabase()
	}

	private fun getNoteFragment(): NoteFragment {
		val hostFragment =
			supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
		val frags = hostFragment.childFragmentManager.fragments
		return frags[0] as NoteFragment
	}
}