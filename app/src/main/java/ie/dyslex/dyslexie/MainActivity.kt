package ie.dyslex.dyslexie

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        survey_button.setOnClickListener{
            onStartSurvey()
        }
    }

    private fun onStartSurvey()
    {
        val intent = Intent(this, SurveyActivity::class.java)
        startActivity(intent)
    }
}
