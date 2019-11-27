package ie.dyslex.android.apis.accessibility

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
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
import android.text.style.ClickableSpan
import android.view.*
import androidx.core.text.isDigitsOnly
import org.w3c.dom.Text
import java.util.regex.Matcher
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.text.method.LinkMovementMethod
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.opengl.Visibility
import android.os.Handler
import android.os.Looper
import android.provider.Contacts
import android.text.TextPaint
import android.util.DisplayMetrics
import android.widget.ImageView
import android.widget.ScrollView
import androidx.core.graphics.component1
import androidx.core.graphics.component2
import androidx.core.graphics.component3
import org.json.JSONObject
import java.net.URL
import kotlin.concurrent.thread


class MainService : AccessibilityService() {
    protected lateinit var oLayout: RelativeLayout
    var openView: View? = null
    var displayView: View? = null
    var settingCategories: List<String> = listOf("mixUp","upsideDown","jump","bunched","skip","size","font","colour","complete")
    var settings: MutableMap<String,Int> = mutableMapOf("mixUp" to 0, "upsideDown" to 0, "jump" to 0, "bunched" to 0, "skip" to 0, "size" to 0, "font" to 0, "colour" to 0)
    var whatHighlight = ""
    var oLayoutParams = WindowManager.LayoutParams()
    var oToggle: Boolean = false
    var currentText: String = ""
    var currentEventSource: AccessibilityNodeInfo? = null
    var screenWidth: Int = 0
    var fontChoices: List<Int> = listOf(R.font.arial,R.font.verdana, R.font.times_new_roman, R.font.open_dyslexic)
    var colourChoices: List<Int> = listOf(R.color.backgroundPink, R.color.backgroundPurple, R.color.backgroundYellow, R.color.backgroundBlue, R.color.backgroundWhite)


    override fun onInterrupt() {
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        oLayout = RelativeLayout(this)
        loadSettings()
        initOverlay()

        var displayMetrics = DisplayMetrics()
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm.getDefaultDisplay().getMetrics(displayMetrics)
        screenWidth = displayMetrics.widthPixels
    }

    private fun loadSettings()
    {
        whatHighlight = ""
        val sharedPref: SharedPreferences = getSharedPreferences("settings", Context.MODE_PRIVATE)

        for(k in settingCategories)
        {
            if(k == "mixUp" || k == "upsideDown")
            {
                whatHighlight += sharedPref.getString(k,"")!!
            }
            else
            {
                settings[k] = sharedPref.getInt(k,0)!!
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent)
    {
        if (event.eventType==AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || event.eventType==AccessibilityEvent.TYPE_WINDOWS_CHANGED)
        {
            if(event?.source!=null)
            {
                currentEventSource = event?.source
            }
        }
    }

    private fun displayText()
    {
        currentText = ""
        printAllViews(highestParent(currentEventSource))

        displayView!!.display_view.text = currentText
        displayView!!.scroll_view.scrollTo(0,0)
        setClickSpannables(displayView!!.display_view)


        for(k in whatHighlight)
        {
            setHighLightedText(displayView!!.display_view,k.toString())
        }
    }

    private fun highestParent(node: AccessibilityNodeInfo?): AccessibilityNodeInfo?

    {
        if(node?.parent != null)
        {
            return highestParent(node!!.parent)
        }
        else
        {
            return node
        }
    }

    private fun printAllViews(mNodeInfo: AccessibilityNodeInfo?) {

        val syllableRegex = "[^aeiouy]*[aeiouy]+(?:[^aeiouy]*$|[^aeiouy](?=[^aeiouy]))?".toRegex()

        if (mNodeInfo == null) return
        var nodeRect = Rect()
        mNodeInfo.getBoundsInScreen(nodeRect)
        if(nodeRect.left >= 0 && nodeRect.right <= screenWidth && nodeRect.right >= 0 && nodeRect.left <= screenWidth)
        {
            if (mNodeInfo.text != null && mNodeInfo.text.isNotBlank() && !mNodeInfo.text.isDigitsOnly())
            {
                var newString = mNodeInfo.text.toString()
                if(newString.split(" ").size > 2) {
                    if (settings["bunched"] == 1) {
                        var syllableSequence = syllableRegex.findAll(newString)
                        var syllableSequenceConcat: Sequence<String> = syllableSequence.map { it.groupValues.joinToString("•") }
                        newString = syllableSequenceConcat.joinToString("•")
                        newString = newString.replace("• ", " ")
                        newString = newString.replace(" •", " ")
                    }
                    currentText += newString + "\n\n"

                    var output = Rect()
                    mNodeInfo.getBoundsInScreen(output)
                }
            }
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
        // Set Font
        displayView!!.display_view.setTypeface(ResourcesCompat.getFont(this, fontChoices[settings["font"]!!]))

        if(settings["size"]==1)
        {
            displayView!!.display_view.setTextSize(TypedValue.COMPLEX_UNIT_SP,20f)
        }

        if(settings["skip"]==1)
        {
            displayView!!.display_view.setLineSpacing(0.0f,1.5f)
        }

        displayView!!.display_view.setBackgroundColor(ResourcesCompat.getColor(resources, colourChoices[settings["colour"]!!], null))
        displayView!!.image_view.setBackgroundColor(ResourcesCompat.getColor(resources, colourChoices[settings["colour"]!!], null))
    }

    /* Coppied from stackoverflow! */
    private fun setHighLightedText(tv: TextView, textToHighlight: String) {
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
                    BackgroundColorSpan(resources.getColor(R.color.highlight)),
                    ofe,
                    ofe + textToHighlight.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            ofs = ofe + 1
        }
        tv.setText(wordToSpan, TextView.BufferType.SPANNABLE)
    }

    private fun onWordClick(word: String)
    {
        var url: URL? = null
        var bmp: Bitmap? = null
        var cleanedWord = word.filter { it.isLetterOrDigit() }
        var t = thread(start = true) {
            val apiResponse = URL("https://www.googleapis.com/customsearch/v1?key=AIzaSyD78nsjpP4DeXhDjVclClIeo7vGVplzef0&cx=000786388690677003860:ic6wap8cf4w&q="+cleanedWord+"+clipart&searchType=image&alt=json&num=1&start=1").readText()
            var json: JSONObject = JSONObject(apiResponse)

            url = URL(json.getJSONArray("items").getJSONObject(0).getString("link"))
            bmp = BitmapFactory.decodeStream(url?.openConnection()?.getInputStream())

            Handler(Looper.getMainLooper()).post(Runnable {

                displayView!!.image_view.setImageBitmap(bmp)
                displayView!!.image_view.visibility = View.VISIBLE
            })


        }

    }

    private fun setClickSpannables(tv: TextView)
    {
        var textToSpan = SpannableString(tv.text)

        var regex = "[^\\s]*".toRegex()


        val results = regex.findAll(textToSpan);
        results.forEach {
            var clickableSpan = object: ClickableSpan() {
                override fun onClick(widget: View) { onWordClick(tv.text.substring(it.range))}
                override fun updateDrawState(ds: TextPaint) {
                    ds.setUnderlineText(false);    // this remove the underline
                }
            }
            textToSpan.setSpan(clickableSpan, it.range.first, it.range.last+1, 0);

        }
        tv.setText(textToSpan)
        tv.setMovementMethod(LinkMovementMethod.getInstance())

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

            displayView!!.image_view.visibility = View.GONE
            displayView!!.image_view.setOnClickListener {
                displayView!!.image_view.visibility = View.GONE
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