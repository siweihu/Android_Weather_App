package com.example.weatherapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.highsoft.highcharts.common.hichartsclasses.*
import com.highsoft.highcharts.core.HIChartView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.highsoft.highcharts.common.HIColor
import com.highsoft.highcharts.common.HIGradient
import com.highsoft.highcharts.common.HIStop
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.LinkedList
import java.util.Locale

class WeeklyFragment : Fragment() {

    private lateinit var futureWeather: List<Map<String, Any>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            val futureWeatherJson = it.getString("futureWeather")
            val gson = Gson()
            futureWeather = gson.fromJson(futureWeatherJson, object : TypeToken<List<Map<String, Any>>>() {}.type)
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_weekly, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 找到 Highcharts View
        val chartView = view.findViewById<HIChartView>(R.id.chartcontainer)
        drawChart(chartView)
    }

    private fun drawChart(chartView: HIChartView) {

        println("接收到的消息$futureWeather")

        val options = HIOptions()

        val chart = HIChart()
        chart.type = "arearange"
        options.chart = chart


        // 设置标题
        val title = HITitle()
        title.text = "Temperature variation by day"
        val titleStyle = HICSSObject()
        titleStyle.color = "#808080"
        titleStyle.fontSize = "16px"

        title.style = titleStyle
        options.title = title

        // 设置 X 轴
        val xaxis = HIXAxis()
        xaxis.type = "datetime" // 设置为日期时间类型
        xaxis.tickInterval = 24 * 3600 * 1000 // 设置刻度间隔为一天
        xaxis.lineColor = HIColor.initWithRGB(195, 195, 195)
        xaxis.tickColor = HIColor.initWithRGB(195, 195, 195)

        val labels = HILabels()
        val cssObject = HICSSObject()
        cssObject.color = "rgb(128, 128, 128)"
        labels.style = cssObject
        xaxis.labels = labels

        options.xAxis = arrayListOf(xaxis)


        // 设置y轴
        val yaxis = HIYAxis()
        yaxis.title = HITitle().apply {
            val titleStyle = HICSSObject()
            titleStyle.color = "#808080"
            style = titleStyle
        }
        val yLabels = HILabels()
        val yLabelStyle = HICSSObject()
        yLabelStyle.color = "rgb(128, 128, 128)" // 标签文字颜色为灰色
        yLabels.style = yLabelStyle
        yaxis.labels = yLabels

        yaxis.title = HITitle()
        options.yAxis = arrayListOf(yaxis)

        val tooltip = HITooltip()
        tooltip.valueSuffix = "°F"
        options.tooltip = tooltip

        val legend = HILegend()
        legend.enabled = false
        options.legend = legend

        val series = HIArearange()
        series.name = "Temperatures"

        // 设置线条颜色和填充颜色
        series.color = HIColor.initWithLinearGradient(
            HIGradient(),
            LinkedList<HIStop>().apply {
                add(HIStop(0.0F, HIColor.initWithRGBA(255, 131, 10, 0.8)))
                add(HIStop(1.0F, HIColor.initWithRGBA(124, 181, 236, 0.8)))
            }
        )

        series.lineWidth = 1


        series.fillColor = HIColor.initWithLinearGradient(
            HIGradient(),
            LinkedList<HIStop>().apply {
                add(HIStop(0.0F, HIColor.initWithRGBA(255, 131, 10, 0.8))) // 起点：橙色，上方，50% 透明度
                add(HIStop(1.0F, HIColor.initWithRGBA(124, 181, 236, 0.8))) // 终点：蓝色，50% 透明度
            }
        )

        series.marker = HIMarker().apply {
            fillColor = HIColor.initWithLinearGradient(
                HIGradient(),
                LinkedList<HIStop>().apply {
                    add(HIStop(0.0F, HIColor.initWithRGBA(255, 131, 10, 0.8))) // 橙色（顶部）
                    add(HIStop(1.0F, HIColor.initWithRGBA(124, 181, 236, 0.8))) // 蓝色（底部）
                }
            )
        }


        val seriesData = futureWeather.map { weather ->
            val dateStr = weather["date"] as String

            // 尝试将温度字符串转换为整数，如果转换失败则默认值为0
            val maxTemp = (weather["temperatureMax"] as? String)
                ?.toIntOrNull()
                ?: run {
                    println("Invalid maxTemp: ${weather["temperatureMax"]}, using default value 0")
                    0
                }

            val minTemp = (weather["temperatureMin"] as? String)
                ?.toIntOrNull()
                ?: run {
                    println("Invalid minTemp: ${weather["temperatureMin"]}, using default value 0")
                    0
                }

            // 将日期字符串转换为时间戳
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
            val date = format.parse(dateStr)
            val timestamp = date?.time ?: 0L

            arrayOf<Any?>(timestamp, minTemp, maxTemp)
        }.toTypedArray()


//        println("数据格式：$seriesData")

        series.data = ArrayList(Arrays.asList(*seriesData))

        options.series = arrayListOf(series)

        chartView.options = options
    }

    private fun extractTemperature(tempString: String): Double {
        return tempString.replace("°F", "").trim().toDoubleOrNull() ?: 0.0
    }

    private fun formatDateToShort(dateString: String): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
        val date = formatter.parse(dateString) ?: return "Unknown"
        return SimpleDateFormat("dd.MMM", Locale.US).format(date)
    }
}
