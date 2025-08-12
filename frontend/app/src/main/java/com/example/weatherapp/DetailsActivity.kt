package com.example.weatherapp

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


class DetailsActivity : AppCompatActivity() {

    private var showAddress: String = "No address provided"
    private var weatherDescription: String = "Unknown weather"
    private var currentWeather: Map<String, String>? = null
    private var futureWeather: List<Map<String, String>>? = null
    private lateinit var currentWeatherJson: String
    private lateinit var futureWeatherJson: String

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var tabAdapter: TabAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gson=Gson()

        // 获取传递的信息
        showAddress = intent.getStringExtra("showAddress") ?: "No address provided"
        weatherDescription = intent.getStringExtra("weatherDescription") ?: "Unknown weather"
        currentWeatherJson = intent.getStringExtra("currentWeatherJson")?: ""
        futureWeatherJson = intent.getStringExtra("futureWeatherJson")?: ""

        val weatherDetailsType = object : TypeToken<Map<String, String>>() {}.type
        val futureWeatherType = object : TypeToken<List<Map<String, String>>>() {}.type

        currentWeather = gson.fromJson(currentWeatherJson, weatherDetailsType)
        futureWeather = gson.fromJson(futureWeatherJson, futureWeatherType)

        println("接收到的消息$currentWeather")

        // 显示actionbar
        switchToDetailsBar()


        window.decorView.setBackgroundColor(Color.parseColor("#FF1E1E1E"))

        setContentView(R.layout.activity_details)

        // 展示tabs并且控制tab切换
        setUpTabs()

    }


    private fun switchToDetailsBar() {

        val addressTextView = TextView(this).apply {
            text = showAddress
            textSize = 18f
            setTextColor(ContextCompat.getColor(this@DetailsActivity, R.color.light_white)) // 修改为白色字体
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
                weight = 1f
            }
        }

        // 左侧返回图标
        val leftIcon = ImageView(this).apply {
            setImageResource(R.drawable.arrow_left)

            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
                setMargins(16, 0, 16, 0)
            }
            this.layoutParams = layoutParams

            setPadding(16, 0, 16, 0)
            setOnClickListener {
                finish() // 返回到上一个 Activity
            }
        }

        // 右侧 "X" 图标
        val xIcon = ImageView(this).apply {
            setImageResource(R.drawable.logo_white)

            val layoutParams = LinearLayout.LayoutParams(96, 96).apply {
                gravity = Gravity.END
            }
            this.layoutParams = layoutParams

            setPadding(16, 0, 16, 0)
            setOnClickListener {
                shareToX()
            }
        }

        // 包裹的container
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 0) }
            addView(leftIcon)
            addView(addressTextView)
            addView(xIcon)
        }

        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayShowCustomEnabled(true)
            customView = container.apply {
                setPadding(0, 0, 0, 0)
                (layoutParams as? LinearLayout.LayoutParams)?.setMargins(0, 0, 0, 0)
            }
        }
    }



    // 分享到X
    private fun shareToX() {


        // 获取 temperature 和 address
        val temperature = (currentWeather?.get("Temperature") as? String) ?: "N/A"
        val address = showAddress ?: "Unknown Place"

        // 构建分享文本
        val shareText = "Check out $address's weather! It is $temperature! #CSCI571WeatherSearch"

        // 构建分享 URL
        val shareUrl = "https://twitter.com/intent/tweet?text=${Uri.encode(shareText)}"

        // 启动浏览器打开分享链接
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(shareUrl))
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No browser available to open the link", Toast.LENGTH_SHORT).show()
        }
    }



    private fun setUpTabs() {
        // TabLayout 和 ViewPager2
        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)

        tabAdapter = TabAdapter(this)
        viewPager.adapter = tabAdapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            val customTabView = layoutInflater.inflate(R.layout.custom_tab_layout, null)
            val tabIcon = customTabView.findViewById<ImageView>(R.id.tabIcon)
            val tabText = customTabView.findViewById<TextView>(R.id.tabText)

            when (position) {
                0 -> {
                    tabIcon.setImageResource(R.drawable.calendar_today_selector) // TODAY 的选择器
                    tabText.text = "TODAY"
                }
                1 -> {
                    tabIcon.setImageResource(R.drawable.trending_up_selector) // WEEKLY 的选择器
                    tabText.text = "WEEKLY"
                }
                2 -> {
                    tabIcon.setImageResource(R.drawable.thermometer_selector) // WEATHER DATA 的选择器
                    tabText.text = "WEATHER DATA"
                }
            }
            tab.customView = customTabView
            tab.customView = customTabView
        }.attach()

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                tab.customView?.findViewById<TextView>(R.id.tabText)?.isSelected = true
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                tab.customView?.findViewById<TextView>(R.id.tabText)?.isSelected = false
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
            }
        })

    }

    // Adapter for ViewPager2
    inner class TabAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        // 返回 Tab 的数量
        override fun getItemCount(): Int = 3

        // 根据 Tab 的位置返回不同的 Fragment
        override fun createFragment(position: Int): Fragment {


            return when (position) {
                // tabs之间的切换
                0 -> TodayFragment().apply {
                    arguments = Bundle().apply {
                        putString("currentWeather", currentWeatherJson)
                        putString("weatherDescription", weatherDescription)
                    }
                }
                1 -> WeeklyFragment().apply {
                    arguments = Bundle().apply {
                        putString("futureWeather", futureWeatherJson)
                    }
                }
                2 -> DataFragment().apply {
                    arguments = Bundle().apply {
                        putString("currentWeather", currentWeatherJson)
                    }
                }
                else -> TodayFragment().apply {
                    arguments = Bundle().apply {
                        putString("currentWeather", currentWeatherJson)
                        putString("weatherDescription", weatherDescription)
                    }
                }
            }
        }
    }
}





