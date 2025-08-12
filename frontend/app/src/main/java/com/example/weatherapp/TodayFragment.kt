package com.example.weatherapp

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// 参数定义，用于接收传递的值
private const val ARG_CURRENT_WEATHER = "currentWeather"
private const val ARG_PARAM2 = "weatherDescription"

class TodayFragment : Fragment() {

    private lateinit var currentWeather: Map<String, Any>
    private lateinit var weatherDescription: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 获取传递的 currentWeather 数据
        arguments?.let {
            weatherDescription = it.getString(ARG_PARAM2).toString()
            val currentWeatherJson = it.getString(ARG_CURRENT_WEATHER)
            val gson = Gson()
            currentWeather = gson.fromJson(currentWeatherJson, object : TypeToken<Map<String, Any>>() {}.type)
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_today, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 绘制 3x3 网格
        drawWeatherGrid(view)
    }

    private fun drawWeatherGrid(view: View) {
        val gridLayout = view.findViewById<GridLayout>(R.id.gridLayout)

        // 配置 GridLayout 数量
        gridLayout.apply {
            rowCount = 3
            columnCount = 3
        }

        // 数据项
        val items = listOf(
            "Wind Speed" to currentWeather["Wind Speed"].toString(),
            "Pressure" to currentWeather["Pressure"].toString(),
            "Precipitation" to currentWeather.getOrDefault("Precipitation", "N/A").toString(),
            "Temperature" to currentWeather["Temperature"].toString(),
            "Weather Description" to weatherDescription,
            "Humidity" to currentWeather["Humidity"].toString(),
            "Visibility" to currentWeather["Visibility"].toString(),
            "Cloud Cover" to currentWeather["Cloud Cover"].toString(),
            "Ozone" to currentWeather["Ozone"].toString()
        )


        // 动态生成子项
        items.forEach { (title, value) ->
            val itemView = createWeatherBlock(title, value)
            gridLayout.addView(itemView)
        }
    }

    private fun createWeatherBlock(title: String, value: String): View {
        val context = requireContext()

        val block = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 8, 8, 8)
            setBackgroundResource(R.drawable.today_block_card)
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = 0
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(16, 16, 16, 16)
            }
        }



        if (title == "Weather Description") {
            // 中间那个block
            // 第一行：图片
            val imageName = value.lowercase().replace(" ", "_")
            val descriptionImageView = ImageView(context).apply {
                val imageResource = context.resources.getIdentifier(imageName, "drawable", context.packageName)
                setImageResource(imageResource)
                layoutParams = LinearLayout.LayoutParams(250, 250).apply {
                    gravity = Gravity.CENTER
                }
            }

            descriptionImageView.setPadding(0, 100, 0, 48)

            // 第二行：文本
            val descriptionTextView = TextView(context).apply {
                text = value
                textSize = 16f
                setTextColor(Color.GRAY)
                gravity = Gravity.CENTER
            }

            descriptionTextView.setPadding(0,50,0,0)

            block.addView(descriptionImageView)
            block.addView(descriptionTextView)
        } else {
            // 其他block
            val imageName = title.lowercase().replace(" ", "_")
            val iconImageView = ImageView(context).apply {
                val imageResource = context.resources.getIdentifier(imageName, "drawable", context.packageName)
                setImageResource(imageResource)
                layoutParams = LinearLayout.LayoutParams(300,300).apply {
                    gravity = Gravity.CENTER
                }
            }

            iconImageView.setPadding(0,64,0,48)

            val valueTextView = TextView(context).apply {
                text = value
                textSize = 16f
                setTextColor(Color.GRAY)
                gravity = Gravity.CENTER
            }

            val titleTextView = TextView(context).apply {
                text = title
                textSize = 16f
                setTextColor(Color.GRAY)
                gravity = Gravity.CENTER
            }

            block.addView(iconImageView)
            block.addView(valueTextView)
            block.addView(titleTextView)
        }

        return block
    }
}

