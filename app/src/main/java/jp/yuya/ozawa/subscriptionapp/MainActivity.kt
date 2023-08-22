package jp.yuya.ozawa.subscriptionapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import jp.yuya.ozawa.subscriptionapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        PreferenceHelper.setUp(this)

        binding.btn1.setOnClickListener{
            val intent = Intent(this, Subs::class.java)
            startActivity(intent)
        }

        if (!PreferenceHelper.getBoolean("key_purchased_standard", false) ||!PreferenceHelper.getBoolean("key_purchased_pro", false)) {
            AlertDialog.Builder(this).apply {
                setMessage(R.string.subs_message_subs)
                setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    val intent = Intent(this@MainActivity, Subs::class.java)
                    startActivity(intent)
                }
                setCancelable(false)
            }.show()
        }

    }
}