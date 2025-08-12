package com.example.weatherapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.highsoft.highcharts.common.HIColor
import com.highsoft.highcharts.common.hichartsclasses.HIBackground
import com.highsoft.highcharts.common.hichartsclasses.HICSSObject
import com.highsoft.highcharts.common.hichartsclasses.HIChart
import com.highsoft.highcharts.common.hichartsclasses.HIData
import com.highsoft.highcharts.common.hichartsclasses.HIDataLabels
import com.highsoft.highcharts.common.hichartsclasses.HIEvents
import com.highsoft.highcharts.common.hichartsclasses.HIOptions
import com.highsoft.highcharts.common.hichartsclasses.HIPane
import com.highsoft.highcharts.common.hichartsclasses.HIPlotOptions
import com.highsoft.highcharts.common.hichartsclasses.HIShadowOptionsObject
import com.highsoft.highcharts.common.hichartsclasses.HISolidgauge
import com.highsoft.highcharts.common.hichartsclasses.HITitle
import com.highsoft.highcharts.common.hichartsclasses.HITooltip
import com.highsoft.highcharts.common.hichartsclasses.HIYAxis
import com.highsoft.highcharts.core.HIChartView
import com.highsoft.highcharts.core.HIFunction
import java.util.Arrays


class DataFragment : Fragment(R.layout.fragment_data) {

    private lateinit var currentWeather: Map<String, Any>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 获取传递的 currentWeather 数据
        arguments?.let {
            val currentWeatherJson = it.getString("currentWeather")
            val gson = Gson()
            currentWeather = gson.fromJson(currentWeatherJson, object : TypeToken<Map<String, Any>>() {}.type)

            println("tab3收到的消息$currentWeather")

        }

    }




    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val humidity = (currentWeather["Humidity"] as? String)
            ?.replace("%", "")
            ?.toIntOrNull()
            ?: 0

        val cloudCover = (currentWeather["Cloud Cover"] as? String)
            ?.replace("%", "")
            ?.toIntOrNull()
            ?: 0

        val precipitation = (currentWeather["Precipitation"] as? String)
            ?.replace("%", "")
            ?.toIntOrNull()
            ?: 0


        val view = inflater.inflate(R.layout.fragment_data, container, false)
        val chartView: HIChartView = view.findViewById(R.id.hc_sample)

        val options = HIOptions()

        val chart = HIChart()
        chart.type = "solidgauge"
        chart.events = HIEvents()
        chart.events.render = HIFunction(renderIconsString)
        options.chart = chart

        val title = HITitle()
        title.text = "Stat Summary"
        title.style = HICSSObject()
        title.style.fontSize = "18px"
        title.style.color= "gray"
        options.title = title

        val tooltip = HITooltip()
        tooltip.borderWidth = 0
        tooltip.backgroundColor = HIColor.initWithName("none")

        val shadowOptions = HIShadowOptionsObject()
        shadowOptions.opacity = 0.0
        shadowOptions.offsetX = 0
        shadowOptions.offsetY = 0
        shadowOptions.color = "transparent"
        tooltip.shadow = shadowOptions

        tooltip.style = HICSSObject()
        tooltip.style.fontSize = "16px"
        tooltip.pointFormat =
            "<div style=\"display: inline-block; text-align: center;\">" +
                    "{series.name}<br>" +
                    "<span style=\"font-size: 2em; color: {point.color}; font-weight: bold;\">{point.y}%</span>" +
                    "</div>"



        tooltip.positioner = HIFunction(
            "function (labelWidth) {" +
                    "   return {" +
                    "       x: (this.chart.chartWidth - labelWidth) /2," +
                    "       y: (this.chart.plotHeight / 2) + 15" +
                    "   };" +
                    "}"
        )
        options.tooltip = tooltip

        val pane = HIPane()
        pane.startAngle = 0
        pane.endAngle = 360

        val paneBackground1 = HIBackground()
        paneBackground1.outerRadius = "112%"
        paneBackground1.innerRadius = "88%"
        paneBackground1.backgroundColor = HIColor.initWithRGBA(66, 255, 121, 0.35)
        paneBackground1.borderWidth = 0

        val paneBackground2 = HIBackground()
        paneBackground2.outerRadius = "87%"
        paneBackground2.innerRadius = "63%"
        paneBackground2.backgroundColor = HIColor.initWithRGBA(66, 187, 255, 0.35)
        paneBackground2.borderWidth = 0

        val paneBackground3 = HIBackground()
        paneBackground3.outerRadius = "62%"
        paneBackground3.innerRadius = "38%"
        paneBackground3.backgroundColor = HIColor.initWithRGBA(255, 99, 66, 0.35)//浅红
        paneBackground3.borderWidth = 0

        pane.background =
            ArrayList(listOf(paneBackground1, paneBackground2, paneBackground3))
        options.pane = arrayListOf(pane)

        val yaxis = HIYAxis()
        yaxis.min = 0
        yaxis.max = 100
        yaxis.lineWidth = 0
        yaxis.tickPositions = ArrayList() // to remove ticks from the chart
        options.yAxis = ArrayList(listOf(yaxis))

        val plotOptions = HIPlotOptions()
        val solidgaugeOptions = HISolidgauge()

        val dataLabels = HIDataLabels()
        dataLabels.enabled = false

        solidgaugeOptions.dataLabels = arrayListOf(dataLabels)
        solidgaugeOptions.linecap = "round"
        solidgaugeOptions.stickyTracking = false
        solidgaugeOptions.rounded = true

        plotOptions.solidgauge = solidgaugeOptions
        options.plotOptions = plotOptions


        val solidgauge1 = HISolidgauge()
        solidgauge1.name = "Humidity"
        val data1 = HIData()
        data1.color = HIColor.initWithRGB(66, 255, 121)// 绿色,湿度
        data1.radius = "112%"
        data1.innerRadius = "88%"
        data1.y = humidity
        solidgauge1.data = ArrayList(listOf(data1))

        val solidgauge2 = HISolidgauge()
        solidgauge2.name = "Precipitation"
        val data2 = HIData()
        data2.color = HIColor.initWithRGB(66, 187, 255)//浅蓝,降水
        data2.radius = "87%"
        data2.innerRadius = "63%"
        data2.y = precipitation
        solidgauge2.data = ArrayList(listOf(data2))

        val solidgauge3 = HISolidgauge()
        solidgauge3.name = "Cloud Cover"
        val data3 = HIData()
        data3.color = HIColor.initWithRGB(255, 99, 66)//浅红,cloud cover
        data3.radius = "62%"
        data3.innerRadius = "38%"
        data3.y = cloudCover
        solidgauge3.data = ArrayList(listOf(data3))

        options.series = ArrayList(Arrays.asList(solidgauge1, solidgauge2, solidgauge3))

        chartView.options = options
        return view
    }

    private val renderIconsString = "function renderIcons() {" +
            "                            if(!this.series[0].icon) {" +
            "                               this.series[0].icon = this.renderer.path(['M', -8, 0, 'L', 8, 0, 'M', 0, -8, 'L', 8, 0, 0, 8]).attr({'stroke': '#303030','stroke-linecap': 'round','stroke-linejoin': 'round','stroke-width': 2,'zIndex': 10}).add(this.series[2].group);}this.series[0].icon.translate(this.chartWidth / 2 - 10,this.plotHeight / 2 - this.series[0].points[0].shapeArgs.innerR -(this.series[0].points[0].shapeArgs.r - this.series[0].points[0].shapeArgs.innerR) / 2); if(!this.series[1].icon) {this.series[1].icon = this.renderer.path(['M', -8, 0, 'L', 8, 0, 'M', 0, -8, 'L', 8, 0, 0, 8,'M', 8, -8, 'L', 16, 0, 8, 8]).attr({'stroke': '#ffffff','stroke-linecap': 'round','stroke-linejoin': 'round','stroke-width': 2,'zIndex': 10}).add(this.series[2].group);}this.series[1].icon.translate(this.chartWidth / 2 - 10,this.plotHeight / 2 - this.series[1].points[0].shapeArgs.innerR -(this.series[1].points[0].shapeArgs.r - this.series[1].points[0].shapeArgs.innerR) / 2); if(!this.series[2].icon) {this.series[2].icon = this.renderer.path(['M', 0, 8, 'L', 0, -8, 'M', -8, 0, 'L', 0, -8, 8, 0]).attr({'stroke': '#303030','stroke-linecap': 'round','stroke-linejoin': 'round','stroke-width': 2,'zIndex': 10}).add(this.series[2].group);}this.series[2].icon.translate(this.chartWidth / 2 - 10,this.plotHeight / 2 - this.series[2].points[0].shapeArgs.innerR -(this.series[2].points[0].shapeArgs.r - this.series[2].points[0].shapeArgs.innerR) / 2);}"
}
