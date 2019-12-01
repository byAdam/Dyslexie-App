package ie.dyslex.dyslexie

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.res.ResourcesCompat
import kotlinx.android.synthetic.main.activity_survey.*
import kotlinx.android.synthetic.main.layout_survey_bool.view.*
import kotlinx.android.synthetic.main.layout_survey_choice.view.*
import kotlinx.android.synthetic.main.layout_survey_complete.view.*
import kotlinx.android.synthetic.main.layout_survey_enter.view.*
import kotlinx.android.synthetic.main.layout_survey_enter_mixup.view.*
import kotlinx.android.synthetic.main.layout_survey_enter_upsidedown.view.*
import kotlin.math.max
import android.provider.Settings


class SurveyActivity : AppCompatActivity() {
    private val questions: List<String> = listOf("mixUp","whatMixUp","upsideDown","whatUpsideDown","jump","bunched","skip","size","font","colour","complete")
    private val questionStrings: Map<String,Int> = mapOf("mixUp" to R.string.survey_mixUp,"whatMixUp" to R.string.survey_whatMixUp, "upsideDown" to R.string.survey_upsideDown, "whatUpsideDown" to R.string.survey_whatUpsideDown, "jump" to R.string.survey_jump, "bunched" to R.string.survey_bunched, "skip" to R.string.survey_skip, "size" to R.string.survey_size, "font" to R.string.survey_font, "colour" to R.string.survey_colour)
    private var questionNo: Int = 0

    private var settings: MutableMap<String,Int> = mutableMapOf("mixUp" to 0, "upsideDown" to 0, "jump" to 0, "bunched" to 0, "skip" to 0, "size" to 0, "font" to 0, "colour" to 0)
    private var whatMixUp: String = ""
    private var whatUpsideDown: String = ""

    private val fontChoices: List<Int> = listOf(R.font.arial,R.font.verdana, R.font.times_new_roman, R.font.open_dyslexic)
    private val colourChoices: List<Int> = listOf(R.color.backgroundPink, R.color.backgroundPurple, R.color.backgroundYellow, R.color.backgroundBlue, R.color.backgroundWhite)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_survey)
    }

    override fun onStart() {
        super.onStart()
        displayQuestion(questions[questionNo])
    }

    private fun onAnswer(questionId: String,answer: Int) {
        settings[questionId] = answer


        if(questionId == "mixUp" || questionId == "upsideDown")
        {
            if(answer == 1)
            {
                questionNo++
            }
            else
            {
                questionNo+=2
            }
        }
        else
        {
            questionNo++
        }

        displayQuestion(questions[questionNo])
    }

    private fun onSurveyComplete()
    {
        val completeView = LayoutInflater.from(this).inflate(R.layout.layout_survey_complete, question_container, false)
        question_container.addView(completeView)

        val sharedPref: SharedPreferences = getSharedPreferences("SettingsActivity", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPref.edit()
        for((k,v) in settings)
        {
            when(k) {
                "mixUp" -> editor.putString(k, whatMixUp)
                "upsideDown" -> editor.putString(k, whatUpsideDown)
                else -> editor.putInt(k, v)
            }
        }
        editor.putBoolean("update",true)
        editor.apply()


        completeView.enable_button.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }
    }

    private fun displayQuestion(questionId:String)
    {
        // If not first question
        if(questionId != "mixUp")
        {
            question_container.removeViewAt(0)
        }

        when(questionId)
        {
            "mixUp" -> displayBoolQuestion("mixUp")
            "whatMixUp" -> displayEnterQuestion("whatMixUp")
            "upsideDown" -> displayBoolQuestion("upsideDown")
            "whatUpsideDown" -> displayEnterQuestion("whatUpsideDown")
            "jump" -> displayBoolQuestion("jump")
            "bunched" -> displayBoolQuestion("bunched")
            "skip" -> displayBoolQuestion("skip")
            "size" -> displayBoolQuestion("size")
            "font" -> displayChoiceQuestion("font")
            "colour" -> displayChoiceQuestion("colour")
            "complete" -> onSurveyComplete()
        }

    }

    private fun addEnterBox(questionId: String,oldView:View? = null)
    {
        lateinit var inputView: View

        if (questionId == "whatMixUp")
        {
            inputView = LayoutInflater.from(this).inflate(R.layout.layout_survey_enter_mixup, question_container.input_container, false)
            question_container.input_container.addView(inputView)

            if(oldView!=null)
            {
                oldView.input_button_mixup.setOnClickListener {
                    question_container.input_container.removeView(oldView)
                }
                oldView.input_button_mixup.text = resources.getString(R.string.remove_button)
            }

            inputView.input_button_mixup.setOnClickListener {
                addEnterBox(questionId,inputView)
            }

        }
        else
        {
            inputView = LayoutInflater.from(this).inflate(R.layout.layout_survey_enter_upsidedown, question_container.input_container, false)
            question_container.input_container.addView(inputView)

            if(oldView!=null)
            {
                oldView.input_button_upsidedown.setOnClickListener {
                    question_container.input_container.removeView(oldView)
                }
                oldView.input_button_upsidedown.text = resources.getString(R.string.remove_button)
            }

            inputView.input_button_upsidedown.setOnClickListener {
                addEnterBox(questionId,inputView)
            }
        }

    }

    private fun displayEnterQuestion(questionId: String)
    {

        val enterView = LayoutInflater.from(this).inflate(R.layout.layout_survey_enter, question_container, false)
        question_container.addView(enterView)

        enterView.enter_question_view.text = resources.getString(questionStrings.getValue(questionId))

        addEnterBox(questionId)

        enterView.submit_button.setOnClickListener{
            for(i in 0 until question_container.input_container.childCount)
            {
                val tempView = question_container.input_container.getChildAt(i)
                if(questionId == "whatMixUp")
                {
                    whatMixUp += tempView.letter_a.text.toString() + tempView.letter_b.text.toString()
                }
                else
                {
                    whatUpsideDown += tempView.letter.text.toString()
                }
            }
            onAnswer(questionId,0)
        }
    }

    private fun onChoiceAnswer(questionId: String,answer: Int,other:Int,choiceView: View) {
        val newOption = max(other,answer) + 1
        if(questionId == "font" && newOption == 4){
            onAnswer(questionId,answer)
            return
        }
        if(questionId == "colour" && newOption == 5){
            onAnswer(questionId,answer)
            return
        }

        displayChoiceOptions(questionId, answer, newOption, choiceView)
    }

    private fun displayChoiceQuestion(questionId: String) {

        val choiceView = LayoutInflater.from(this).inflate(R.layout.layout_survey_choice, question_container, false)
        question_container.addView(choiceView)

        choiceView.choice_question_view.text = resources.getString(questionStrings.getValue(questionId))

        displayChoiceOptions(questionId, 0, 1, choiceView)
    }

    private fun displayChoiceOptions(questionId: String,optionA:Int,optionB:Int,choiceView: View)
    {
        if (questionId == "font") {
            choiceView.choice_view_1.typeface = ResourcesCompat.getFont(this, fontChoices[optionA])
            choiceView.choice_view_2.typeface = ResourcesCompat.getFont(this, fontChoices[optionB])
        }
        else
        {
            choiceView.choice_view_1.setBackgroundColor(ResourcesCompat.getColor(resources, colourChoices[optionA], null))
            choiceView.choice_view_2.setBackgroundColor(ResourcesCompat.getColor(resources, colourChoices[optionB], null))
        }

        choiceView.choice_view_1.setOnClickListener {
            onChoiceAnswer(questionId,optionA, optionB, choiceView)
        }

        choiceView.choice_view_2.setOnClickListener {
            onChoiceAnswer(questionId, optionB, optionA, choiceView)
        }

    }


    private fun displayBoolQuestion(questionId: String)
    {
        val boolLayout = LayoutInflater.from(this).inflate(R.layout.layout_survey_bool, question_container, false)
        question_container.addView(boolLayout)
        boolLayout.bool_question_view.text = resources.getString(questionStrings.getValue(questionId))

        boolLayout.no_button.setOnClickListener {
            onAnswer(questionId,0)
        }

        boolLayout.yes_button.setOnClickListener {
            if(questionId == "size" || questionId == "skip")
            {
                onAnswer(questionId,3)
            }
            else
            {
                onAnswer(questionId,1)
            }
        }
    }
}
