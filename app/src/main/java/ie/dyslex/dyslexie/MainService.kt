package ie.dyslex.android.apis.accessibility

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.WindowManager
import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.Gravity
import android.graphics.PixelFormat
import android.view.View
import ie.dyslex.dyslexie.R
import android.util.TypedValue
import android.widget.RelativeLayout
import androidx.core.content.res.ResourcesCompat
import android.widget.TextView
import androidx.core.text.HtmlCompat
import kotlinx.android.synthetic.main.activity_overlay_off.view.*
import kotlinx.android.synthetic.main.activity_overlay_on.view.*
import android.text.Spannable
import android.text.style.BackgroundColorSpan
import android.text.SpannableString
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T



class MainService : AccessibilityService() {
    protected lateinit var oLayout: RelativeLayout
    var openView: View? = null
    var displayView: View? = null
    var settingCategories: List<String> = listOf("mixUp","whatMixUp","upsideDown","whatUpsideDown","jump","bunched","skip","size","font","colour","complete")
    var settings: MutableMap<String,Int> = mutableMapOf("mixUp" to 0, "upsideDown" to 0, "jump" to 0, "bunched" to 0, "skip" to 0, "size" to 0, "font" to 0, "colour" to 0)
    var whatMixUp: String = ""
    var whatUpsideDown: MutableSet<String> = mutableSetOf()
    var oLayoutParams = WindowManager.LayoutParams()
    var oToggle: Boolean = false
    var currentText: String = ""

    var fontChoices: List<Int> = listOf(R.font.arial,R.font.verdana, R.font.times_new_roman, R.font.open_dyslexic)
    var colourChoices: List<Int> = listOf(R.color.backgroundPink, R.color.backgroundPurple, R.color.backgroundYellow, R.color.backgroundBlue, R.color.backgroundWhite)


    override fun onInterrupt() {
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        oLayout = RelativeLayout(this)
        initOverlay()
    }

    private fun loadSettings()
    {
        whatMixUp = ""
        whatUpsideDown = mutableSetOf()
        val sharedPref: SharedPreferences = getSharedPreferences("settings", Context.MODE_PRIVATE)

        for(k in settingCategories)
        {
            if(k == "whatMixUp")
            {
                whatMixUp = sharedPref.getString(k,"")!!
            }
            else if(k == "whatUpsideDown")
            {
                whatUpsideDown = sharedPref.getStringSet(k, mutableSetOf() )!!
            }
            else
            {
                settings[k] = sharedPref.getInt(k,0)!!
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent)
    {
        Log.i("Access Type",event.eventType.toString())
        if (event.eventType==AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || event.eventType==AccessibilityEvent.TYPE_WINDOWS_CHANGED)
        {
            currentText = ""
            printAllViews(event?.source)

            if(oToggle)
            {
                displayText()
            }
        }
    }

    private fun displayText()
    {
        displayView!!.display_view.text = currentText

        for(k in whatUpsideDown)
        {
            setHighLightedText(displayView!!.display_view,k)
        }
        for(k in whatMixUp)
        {
            if(k.toString() != ","){
                setHighLightedText(displayView!!.display_view,k.toString())
            }
        }
    }


    private fun printAllViews(mNodeInfo: AccessibilityNodeInfo?) {
        if (mNodeInfo == null) return
        if (mNodeInfo.text != null)
        {
            currentText += " "+mNodeInfo.text.toString()
        }
        if (mNodeInfo.childCount < 1) return

        for (i in 0 until mNodeInfo.childCount) {
            printAllViews(mNodeInfo.getChild(i))
        }
    }


    private fun initOverlay()
    {
        oLayoutParams.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        oLayoutParams.format = PixelFormat.TRANSLUCENT
        oLayoutParams.flags = oLayoutParams.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        oLayoutParams.gravity = Gravity.TOP

        displayView = LayoutInflater.from(this).inflate(R.layout.activity_overlay_on, oLayout,false)

        openView = LayoutInflater.from(this).inflate(R.layout.activity_overlay_off, oLayout,false)

        toggleDisplay(false, false)
    }

    private fun applySettings()
    {
        loadSettings()
        // Set Font
        displayView!!.display_view.setTypeface(ResourcesCompat.getFont(this, fontChoices[settings["font"]!!]))

        if(settings["size"]==1)
        {
            displayView!!.display_view.setTextSize(TypedValue.COMPLEX_UNIT_SP,20f)
        }

        if(settings["skip"]==1)
        {
            displayView!!.display_view.setLineSpacing(0.0f,2.0f)
        }

        displayView!!.display_view.setBackgroundColor(ResourcesCompat.getColor(resources, colourChoices[settings["colour"]!!], null))
    }

    /* Coppied from stackoverflow! */
    fun setHighLightedText(tv: TextView, textToHighlight: String) {
        val tvt = tv.text.toString()
        var ofe = tvt.indexOf(textToHighlight, 0)
        val wordToSpan = SpannableString(tv.text)
        var ofs = 0
        while (ofs < tvt.length && ofe != -1) {
            ofe = tvt.indexOf(textToHighlight, ofs)
            if (ofe == -1)
                break
            else {
                wordToSpan.setSpan(
                    BackgroundColorSpan(-0x100),
                    ofe,
                    ofe + textToHighlight.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                tv.setText(wordToSpan, TextView.BufferType.SPANNABLE)
            }
            ofs = ofe + 1
        }
    }


    private fun toggleDisplay(toggleOn: Boolean,removeView: Boolean = true)
    {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        if(toggleOn)
        {
            if(removeView)
            {
                wm.removeView(openView)
            }

            oLayoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
            oLayoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
            if(displayView?.windowToken==null)
            {
                wm.addView(displayView, oLayoutParams)
            }

            applySettings()

            displayText()

            displayView!!.close_button.setOnClickListener {
                toggleDisplay(false)
            }

            oToggle = true
        }
        else
        {
            if(removeView)
            {
                wm.removeView(displayView)
            }

            oLayoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT
            oLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT

            if(openView?.windowToken==null)
            {
                wm.addView(openView, oLayoutParams)
            }

            openView!!.open_button.setOnClickListener{
                toggleDisplay(true)
            }

            oToggle = false
        }

    }
}