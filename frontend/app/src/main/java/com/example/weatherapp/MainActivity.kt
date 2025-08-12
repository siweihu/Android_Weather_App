package com.example.weatherapp

import android.Manifest
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.CursorAdapter
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SearchView
import android.widget.SimpleCursorAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.weatherapp.ui.theme.WeatherAppTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.math.roundToInt

import com.google.gson.Gson
import org.json.JSONArray

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.lifecycle.MutableLiveData


class MainActivity : AppCompatActivity() {

    val apiKey = "AIzaSyDjORg0iR-NHKOr8S5L9XZhpmy1MtuAP_c"

    // 展示的信息
    private var showAddress: String? = null
    private var currentWeather by mutableStateOf<Map<String, Any>?>(null)
    private var futureWeather: List<Map<String, Any>>? = null

    var isSearchMode = mutableStateOf(false)
    var isShowFavorite = mutableStateOf(false)
    var currentPageName = mutableStateOf("")


    // 当前用户的地理位置
    private var userAddress: String? = null
    private var userCurrentWeather: Map<String, Any>? = null
    private var userFutureWeather: List<Map<String, Any>>? = null

    // 关于收藏功能
    private var allFavorites: MutableList<Pair<String, String>> = mutableListOf()

    data class FavoriteWeatherInfo(
        val showAddress: String,
        val currentWeather: Map<String, Any>?,
        val futureWeather: List<Map<String, Any>>?
    )

    private var allFavorites_weatherInfo: MutableList<FavoriteWeatherInfo> = mutableListOf()


    private var hasLocationPermission by mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
    }

    private lateinit var rootLayout: LinearLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    // 关于loading状态
    private var isLoading = MutableLiveData(true)

    //---------------------------------------------------------
    // --------------------默认展示的主页面-----------------------
    // --------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_WeatherApp)
        super.onCreate(savedInstanceState)

        // 初始化 Toolbar
        initializeToolbar()

        // 请求地址权限
        checkAndRequestLocationPermission()

        isLoading.observe(this) { loading ->
            if (loading) {
                // 显示 LoadingSpinner
                setContent {
                    LoadingSpinner()
                }
            } else {
                // 显示主界面
                trueMainScreen()
            }
        }

        // 更新用户的地址和天气信息
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                loadFavorites()
                updateFavoritesWeatherInfo()
                updateLocationAndWeather()
            }
            withContext(Dispatchers.Main) {
                isLoading.postValue(false)
            }
        }
    }

    private fun trueMainScreen() {
        if (!isSearchMode.value) {
            // 正常渲染主界面
            createMainLayout()
            setContentView(rootLayout)
            setupViewPagerWithTabs(viewPager, tabLayout)
        } else {
            isShowFavorite.value = false
            setContent {
                WeatherAppTheme {
                    WeatherScreen()
                }
            }
        }
    }

    private fun createMainLayout() {
        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        viewPager = ViewPager2(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        val tabContainer = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.BLACK)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        tabLayout = TabLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                200
            )
            setBackgroundColor(android.graphics.Color.BLACK)
        }

        tabContainer.addView(tabLayout)
        rootLayout.addView(tabContainer, 0)
        rootLayout.addView(viewPager, 1)
    }



    // 关于多视角展示
    private fun setupViewPagerWithTabs(viewPager: ViewPager2, tabLayout: TabLayout) {
        val pages = mutableListOf<String>()

        // 初始化第一页
        showAddress?.let {
            pages.add(it)
            tabLayout.addTab(tabLayout.newTab().apply { text = it })
        }

        println("当前收藏的城市$allFavorites")
        println("当前天气信息$allFavorites_weatherInfo")

        // 添加收藏地址
        allFavorites_weatherInfo.forEach {
            pages.add(it.showAddress)
            tabLayout.addTab(tabLayout.newTab().apply { text = it.showAddress })
//            println("收藏地址: ${it.showAddress}")
        }

        // 设置适配器
        viewPager.adapter = object : RecyclerView.Adapter<PagerViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagerViewHolder {
                val composeView = ComposeView(parent.context)
                composeView.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                return PagerViewHolder(composeView)
            }

            override fun onBindViewHolder(holder: PagerViewHolder, position: Int) {
                val composeView = holder.itemView as ComposeView
                composeView.setContent {
                    if (position == 0) {
                        isShowFavorite.value = false
                        WeatherAppTheme {
                            WeatherScreen()
                        }
                    } else {
                        isShowFavorite.value = true
                        WeatherAppTheme {
                            val favoriteInfo = allFavorites_weatherInfo[position - 1]
                            WeatherScreen(
                                localShowAddress = favoriteInfo.showAddress,
                                localCurrentWeather = favoriteInfo.currentWeather,
                                localFutureWeather = favoriteInfo.futureWeather,
                                isShowFavorite = true
                            )
                        }
                    }
                }
            }

            override fun getItemCount(): Int {
                return pages.size
            }
        }

        var defaultPosition = 0

        // 设定展示哪一页
        if (currentPageName.value != "") {
            defaultPosition =
                (0 until tabLayout.tabCount).firstOrNull { tabLayout.getTabAt(it)?.text == currentPageName.value }
                    ?: 0

            if (defaultPosition in pages.indices) {
                viewPager.setCurrentItem(defaultPosition, false)
            }
        }

        // 绑定 TabLayout 和 ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = pages[position]



            // 很重要！！！！初始化选择，等会儿返回要用到
            tab.customView = createTabView(tabLayout.context, position == defaultPosition)
        }.attach()


        currentPageName.value = ""

        // Tab 样式设置
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.customView = createTabView(tabLayout.context, true)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                tab?.customView = createTabView(tabLayout.context, false)
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }


    class PagerViewHolder(view: View) : RecyclerView.ViewHolder(view)

    private fun createTabView(context: Context, isSelected: Boolean): View {
        val textView = TextView(context).apply {
            text = "●"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(
                if (isSelected) android.graphics.Color.WHITE
                else android.graphics.Color.GRAY
            )
        }
        return textView
    }




    private fun initializeToolbar() {
        val toolbar = layoutInflater.inflate(R.layout.custom_action_bar, null)
        supportActionBar?.apply {
            setDisplayShowCustomEnabled(true)
            customView = toolbar
        }

        val actionBarImage = toolbar.findViewById<ImageView>(R.id.action_bar_image)
        actionBarImage.setOnClickListener {
            switchToSearchView()
        }
    }

    private fun checkAndRequestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            hasLocationPermission = true
        }
    }





    private suspend fun updateLocationAndWeather() {
        var (address, coordinates) = fetchLocation(this)
        showAddress = address ?: "Unable to fetch address"

        // To TA: Pre-Set address to Los Angeles. You CAN remove following 2 lines for auto-detect.
        showAddress = "Los Angeles, California"
        coordinates = Pair(34.0522, -118.2437)


        userAddress = showAddress

        if (coordinates != null) {
            val (latitude, longitude) = coordinates
            val (currentWeatherData, futureWeatherData) = fetchWeatherData(latitude, longitude)


            if (currentWeatherData != null && futureWeatherData != null) {
                currentWeather = currentWeatherData
                userCurrentWeather = currentWeatherData

                futureWeather = futureWeatherData
                userFutureWeather = futureWeatherData

                //            // 能有什么不一样？
//            println("Local Weather: $futureWeather")

            } else {
                println("Failed to fetch weather data.")
            }
        }
    }


    // 关于收藏
    private suspend fun loadFavorites() {
        withContext(Dispatchers.IO) {
            try {
                val url = "https://csci571-a3-backend-swhu-23.wn.r.appspot.com/favorites/list"
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val favorites = JSONArray(response)
                    val favoriteList = mutableListOf<Pair<String, String>>()

                    for (i in 0 until favorites.length()) {
                        val favorite = favorites.getJSONObject(i)
                        val city = favorite.getString("city")
                        val state = favorite.getString("state")
                        favoriteList.add(Pair(city, state))
                    }

                    withContext(Dispatchers.Main) {
                        allFavorites = favoriteList
                        println("Favorites loaded: $allFavorites")
                    }
                } else {
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    fun addFavorite(city: String, state: String) {
        val url = "https://csci571-a3-backend-swhu-23.wn.r.appspot.com/favorites/add?city=${URLEncoder.encode(city, "UTF-8")}&state=${URLEncoder.encode(state, "UTF-8")}"
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    withContext(Dispatchers.Main) {
                        // 更新 allFavorites
                        allFavorites.add(Pair(city, state))

                        // 简单赋值 currentWeather 和 futureWeather 到 allFavorites_weatherInfo
                        val showAddress = "$city, $state"
                        currentPageName.value = showAddress
                        allFavorites_weatherInfo.add(
                            FavoriteWeatherInfo(
                                showAddress = showAddress,
                                currentWeather = currentWeather,
                                futureWeather = futureWeather
                            )
                        )
                        println("$city, $state added to favorites with current weather info.")
                    }
                } else {
                    println("Error adding favorite: HTTP ${connection.responseCode}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                println("Network error occurred while adding favorite.")
            }
        }
    }




    fun removeFavorite(city: String, state: String) {
        val url = "https://csci571-a3-backend-swhu-23.wn.r.appspot.com/favorites/remove?city=${URLEncoder.encode(city, "UTF-8")}&state=${URLEncoder.encode(state, "UTF-8")}"
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    withContext(Dispatchers.Main) {
                        // 更新 allFavorites
                        allFavorites.remove(Pair(city, state))
                        println("$city, $state removed from favorites.")

                        // 更新 allFavorites_weatherInfo
                        val showAddress = "$city, $state"
                        val indexToRemove = allFavorites_weatherInfo.indexOfFirst { it.showAddress == showAddress }
                        if (indexToRemove != -1) {
                            allFavorites_weatherInfo.removeAt(indexToRemove)
                            println("Removed weather info for $showAddress from allFavorites_weatherInfo.")
                        } else {
                            println("Weather info for $showAddress not found in allFavorites_weatherInfo.")
                        }

                        // 更新 TabLayout
//                        println("找到的tab数${tabLayout.tabCount}")
                        for (i in 0 until tabLayout.tabCount) {
                            val tab = tabLayout.getTabAt(i)
//                            println("Tab $i Text: ${tab?.text}")
                            if (tab?.text == showAddress) {
                                tabLayout.removeTabAt(i) // 移除匹配的 Tab
                                break
                            }
                        }
                    }
                } else {
                    println("Error removing favorite: HTTP ${connection.responseCode}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                println("Network error occurred while removing favorite.")
            }
        }
    }







    fun checkIfFavorite(city: String, state: String): Boolean {
        if (city.isBlank() || state.isBlank()) {
            return false
        }

        var isFavorite = false

        // 从后端获取收藏列表并检查是否匹配
        val url = "https://csci571-a3-backend-swhu-23.wn.r.appspot.com/favorites/list"
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val favorites = JSONArray(response)

                    // 检查收藏列表中是否存在匹配项
                    for (i in 0 until favorites.length()) {
                        val favorite = favorites.getJSONObject(i)
                        val favoriteCity = favorite.getString("city")
                        val favoriteState = favorite.getString("state")

                        if (favoriteCity == city && favoriteState == state) {
                            isFavorite = true
                            break
                        }
                    }

                    withContext(Dispatchers.Main) {
                        println("Is Favorite: $isFavorite")
                    }
                } else {
                    println("Error retrieving favorites list: HTTP ${connection.responseCode}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                println("Network error occurred while retrieving favorites list.")
            }
        }

        return isFavorite
    }

    fun simpleCheckIfFavorite(city: String, state: String): Boolean {

//        println("当前检查的city是：$city")
        return allFavorites.contains(Pair(city, state))
    }

    private suspend fun updateFavoritesWeatherInfo() {
        // 清空已有数据
        allFavorites_weatherInfo.clear()

        // 遍历所有收藏地址并更新天气信息
        for ((city, state) in allFavorites) {
            val showAddress = "$city, $state"

            // 立即插入默认值，currentWeather 和 futureWeather 为 null
            val defaultInfo = FavoriteWeatherInfo(
                showAddress = showAddress,
                currentWeather = null,
                futureWeather = null
            )
            allFavorites_weatherInfo.add(defaultInfo)

            val coordinates = getCoordinatesFromAddress(showAddress)

            if (coordinates != null) {
                val (latitude, longitude) = coordinates
                val (currentWeather, futureWeather) = fetchWeatherData(latitude, longitude)

//                // 能有什么不一样
//                println("favoriteWeather$futureWeather")

                // 在获取天气数据后更新对应的 FavoriteWeatherInfo
                val index = allFavorites_weatherInfo.indexOfFirst { it.showAddress == showAddress }
                if (index != -1) {
                    allFavorites_weatherInfo[index] = FavoriteWeatherInfo(
                        showAddress = showAddress,
                        currentWeather = currentWeather,
                        futureWeather = futureWeather
                    )
                }
            } else {
                println("Failed to get coordinates for $showAddress")
            }
        }
    }










    // loading 页面
    @Composable
    fun LoadingSpinner() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF2E2E2E)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    modifier = Modifier.size(50.dp),
                    color = Color.White,
                    strokeWidth = 4.dp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Fetching weather",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
            }
        }
    }




    @Composable
    fun WeatherScreen(
        localShowAddress: String? = showAddress,
        localCurrentWeather: Map<String, Any>? = currentWeather,
        localFutureWeather: List<Map<String, Any>>? = futureWeather,
        isShowFavorite: Boolean = false
    ) {
        // 调用搜索界面
        if (localCurrentWeather == null && isSearchMode.value) {
//        if (false){
            // 你是对的
            LoadingSpinner()
        } else {
            // 如果天气数据存在，渲染主界面
            MainScreen(
                cityName = localShowAddress ?: "Unknown Address",
                currentWeather = localCurrentWeather?.get("weatherDescription") as? String ?: "N/A",
                currentTemp = formatField(localCurrentWeather?.get("temperature"), "", true),
                weatherDetailsForTrans = mapOf(
                    "Humidity" to formatField(localCurrentWeather?.get("humidity"), "%", true),
                    "Wind Speed" to formatField(localCurrentWeather?.get("windSpeed"), "mph"),
                    "Visibility" to formatField(localCurrentWeather?.get("visibility"), "mi"),
                    "Pressure" to formatField(localCurrentWeather?.get("pressure"), "inHg"),
                    "Precipitation" to formatField(localFutureWeather?.getOrNull(0)?.get("precipitation"), "%", true),
                    "Temperature" to formatField(localCurrentWeather?.get("temperature"), "°F", true),
                    "Cloud Cover" to formatField(localCurrentWeather?.get("cloudCover"), "%", true),
                    "Ozone" to formatField(localCurrentWeather?.get("uvIndex"), "", true)
                ),
                weatherDetails = mapOf(
                    "Humidity" to formatField(localCurrentWeather?.get("humidity"), "%", true),
                    "Wind Speed" to formatField(localCurrentWeather?.get("windSpeed"), "mph"),
                    "Visibility" to formatField(localCurrentWeather?.get("visibility"), "mi"),
                    "Pressure" to formatField(localCurrentWeather?.get("pressure"), "inHg")
                ),
                futureWeather = localFutureWeather?.map { day ->
                    mapOf(
                        "date" to (day["date"] as? String ?: "N/A"),
                        "weatherDescription" to (day["weatherDescription"] as? String ?: "N/A"),
                        "temperatureMax" to formatField(day["temperatureMax"], "", true),
                        "temperatureMin" to formatField(day["temperatureMin"], "", true)
                    )
                } ?: emptyList(),
                isSearchMode = isSearchMode.value,
                isShowFavorite = isShowFavorite
            )
        }
    }




    // 关于搜索功能
    private fun switchToSearchView() {

        val searchView = SearchView(this).apply {
            queryHint = "Search..."
            isIconified = false
            background = null
        }


        // 添加下拉菜单
        enhanceSearchViewWithDropdown(searchView)

        val searchPlate = searchView.findViewById<View>(
            resources.getIdentifier("android:id/search_plate", null, null)
        )
        searchPlate?.setBackgroundResource(0)

        // 重写清除按钮的逻辑
        try {
            val closeButton = searchView.findViewById<ImageView>(
                resources.getIdentifier("android:id/search_close_btn", null, null)
            )
            closeButton?.setOnClickListener {
                searchView.setQuery("", false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 配置 SearchView 的回调，处理搜索结果
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    handleSearchQuery(it)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        // 添加左侧返回图标
        val leftIcon = ImageView(this).apply {
            setImageResource(R.drawable.arrow_left)

            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.gravity = Gravity.CENTER_VERTICAL
            this.layoutParams = layoutParams

            setPadding(16, 0, 16, 0)
            setOnClickListener {
                // 点击左侧图标返回
                restoreActionBar(true)
            }
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(leftIcon)
            addView(searchView, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }

        supportActionBar?.apply {
            setDisplayShowCustomEnabled(false)
            customView = container
            setDisplayShowCustomEnabled(true)
        }
    }

    // 处理搜索查询
    private fun handleSearchQuery(query: String) {
        isSearchMode.value = true

        currentWeather = null
        futureWeather = null

        showAddress = query
        setContent {
            WeatherAppTheme {
                WeatherScreen()
            }
        }

        switchToAddressBar()


        // 根据搜索address更新坐标，再根据坐标更新地址
        var coordinates by mutableStateOf<Pair<Double, Double>?>(null)

        lifecycleScope.launch {
            coordinates = getCoordinatesFromAddress(showAddress!!)

            if (coordinates != null) {
                val (latitude, longitude) = coordinates!!
                lifecycleScope.launch {
                    try {
                        val (currentWeatherData, futureWeatherData) = fetchWeatherData(latitude, longitude)
                        if (currentWeatherData != null && futureWeatherData != null) {
                            currentWeather = currentWeatherData
                            futureWeather = futureWeatherData

                            println("Search Weather: $currentWeather")
                        } else {
                            println("Failed to fetch weather data.")
                        }
                    } catch (e: Exception) {
                        println("Error fetching weather data: ${e.message}")
                    }
                }

            } else {
                println("Coordinates are null, cannot fetch weather data.")
            }

        }
    }

    // 修改下拉菜单
    private fun enhanceSearchViewWithDropdown(searchView: SearchView) {
        val searchAutoComplete = searchView.findViewById<AutoCompleteTextView>(
            resources.getIdentifier("android:id/search_src_text", null, null)
        )

        searchAutoComplete.threshold = 1


        if (searchAutoComplete != null) {
            val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                mutableListOf<String>()
            )
            searchAutoComplete.setAdapter(adapter)

            searchAutoComplete.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {}
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val input = s.toString()
                    if (input.isNotEmpty()) {
                        // 异步获取城市建议
                        lifecycleScope.launch {
                            val suggestions = getCitySuggestions(input)


                            withContext(Dispatchers.Main) {
                                // 通过反射直接修改 mObjects
                                val objectsField = ArrayAdapter::class.java.getDeclaredField("mObjects")
                                objectsField.isAccessible = true
                                val currentObjects = objectsField.get(adapter) as MutableList<Any>

                                currentObjects.clear()
                                currentObjects.addAll(suggestions as Collection<Any>)

                                adapter.notifyDataSetChanged()
                            }
                        }


                    }
                }
            })

            searchAutoComplete.setOnItemClickListener { parent, view, position, id ->
                val selectedCity = parent.getItemAtPosition(position).toString()
                searchView.setQuery(selectedCity, false)
            }
        }
    }

    // 获取城市建议
    suspend fun getCitySuggestions(input: String): List<String> {
        if (input.isEmpty()) return emptyList()

        val url = "https://maps.googleapis.com/maps/api/place/autocomplete/json?input=${URLEncoder.encode(input, "UTF-8")}&types=(cities)&key=$apiKey"

        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val jsonResponse = JSONObject(response)

                    // Parse city and state
                    val predictions = jsonResponse.getJSONArray("predictions")
                    println("predictions是：$predictions")
                    val cityStateSuggestions = mutableListOf<String>()
                    for (i in 0 until predictions.length()) {
                        val prediction = predictions.getJSONObject(i)
                        val terms = prediction.getJSONArray("terms")

                        val city = terms.getJSONObject(0).getString("value")
                        var state = if (terms.length() > 1) terms.getJSONObject(1).getString("value") else ""
                        state = stateFullNameMap[state] ?: state
                        if (state.isNotEmpty()) {
                            cityStateSuggestions.add("$city, $state")
                        }
                    }
                    cityStateSuggestions
                } else {
                    println("Error fetching suggestions: HTTP $responseCode")
                    emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    // 展示搜索结果时的actionbar，即顶端地址
    private fun switchToAddressBar() {
        val currentAddress = showAddress ?: "Unknown Address"

        val addressTextView = TextView(this).apply {
            text = currentAddress
            textSize = 18f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.light_white))
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
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
                restoreActionBar(false)
            }
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(leftIcon)
            addView(addressTextView)
        }

        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayShowCustomEnabled(true)
            customView = container
        }
    }

    // 恢复到home page
    private fun restoreActionBar(isSearchTable: Boolean) {
        isSearchMode.value = false
        setTheme(R.style.Theme_WeatherApp)

        val toolbar = layoutInflater.inflate(R.layout.comeback_action_bar, null)
        supportActionBar?.apply {
            setDisplayShowCustomEnabled(true)
            customView = toolbar
        }

        val actionBarImage = toolbar.findViewById<ImageView>(R.id.action_bar_image)
        actionBarImage.setOnClickListener {
            switchToSearchView()
        }

        if(!isSearchTable){
            showLoadingSpinner {
                // 在 LoadingSpinner 之后执行以下代码
                currentWeather = userCurrentWeather
                futureWeather = userFutureWeather
                showAddress = userAddress

                // 调用 trueMainScreen() 完成界面切换
                trueMainScreen()
            }
        }
        else{
            currentWeather = userCurrentWeather
            futureWeather = userFutureWeather
            showAddress = userAddress

            // 调用 trueMainScreen() 完成界面切换
            trueMainScreen()
        }
    }

    private fun showLoadingSpinner(onComplete: () -> Unit) {
        setContent {
            LoadingSpinner()
        }

        lifecycleScope.launch {
            delay(500)
            onComplete()
        }
    }


    // 降地址转化为经纬度
    private suspend fun getCoordinatesFromAddress(address: String): Pair<Double, Double>? {
        val url = "https://maps.googleapis.com/maps/api/geocode/json?address=${URLEncoder.encode(address, "UTF-8")}&key=$apiKey"
//        println("GEO URL: $url")

        // 在后台线程执行网络请求
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.instanceFollowRedirects = true

                val responseCode = connection.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()

                    val jsonObject = JSONObject(response)
                    val status = jsonObject.getString("status")
                    if (status == "OK") {
                        val results = jsonObject.getJSONArray("results")
                        if (results.length() > 0) {
                            val location = results.getJSONObject(0)
                                .getJSONObject("geometry")
                                .getJSONObject("location")
                            val latitude = location.getDouble("lat")
                            val longitude = location.getDouble("lng")
                            return@withContext Pair(latitude, longitude)
                        }
                    } else {
                        println("Geocoding API returned status: $status")
                    }
                } else {
                    val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error message"
                    println("HTTP request failed with response code: $responseCode, Error: $errorResponse")
                }
            } catch (e: Exception) {
                println("Exception occurred: ${e::class.java.name} - ${e.message}")
                e.printStackTrace()
            }
            return@withContext null
        }
    }


}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WeatherAppTheme {
        Greeting("Android")
    }
}



// 主界面展示
@Composable
fun MainScreen(
    cityName: String,
    currentWeather: String,
    currentTemp: String,
    weatherDetailsForTrans: Map<String, String>,
    weatherDetails: Map<String, String>,
    futureWeather: List<Map<String, String>>,
    isSearchMode: Boolean,
    isShowFavorite: Boolean
) {

    val gson = Gson()
    val currentWeatherJson = gson.toJson(weatherDetailsForTrans)
    val futureWeatherJson = gson.toJson(futureWeather)

    // 此处已经多一天
    //    println("${cityName}处的天气是$futureWeather")



    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2E2E2E)), // 深灰色背景
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card0: 区分是正常展示还是搜索页面
        if (isSearchMode)
            InformationCard(isSearchMode = isSearchMode)

        // Card1：本地地址和天气
        val context = LocalContext.current

        LocationCard(
            title = cityName,
            subtitle = "$currentWeather, $currentTemp°F",
            description = "Current Weather Summary",
            onClick = {
                val intent = Intent(context, DetailsActivity::class.java)
                intent.putExtra("showAddress", cityName)
                intent.putExtra("weatherDescription", currentWeather)
                intent.putExtra("currentWeatherJson", currentWeatherJson)
                intent.putExtra("futureWeatherJson", futureWeatherJson)
                context.startActivity(intent)
            }
        )


        // Card2：天气具体数值
        LocalWeatherCard(details = weatherDetails)

        // Card3：未来一周的天气
        LocalFutureWeatherCard(futureWeather = futureWeather)
    }

    // 是否已经被收藏？
    val city = cityName.substringBefore(",").trim()
    val state = cityName.substringAfter(",").trim()
    val context = LocalContext.current
    val isFavoriteState = remember { mutableStateOf(false) }


    // 关于收藏功能
    // 当搜索出来某个东西时，展示FAB
    if (isSearchMode || isShowFavorite) {
        LaunchedEffect(city, state, context) {
            isFavoriteState.value = (context as? MainActivity)?.simpleCheckIfFavorite(city, state) ?: false
            if (isSearchMode)
                (context as? MainActivity)?.currentPageName?.value = cityName
        }


        CustomFloatingActionButton(
            isMarkerAdded = isFavoriteState.value,
            onFabClick = {
                // 点击，触发添加或删除收藏后的逻辑
                isFavoriteState.value = !isFavoriteState.value
                val mainActivity = context as? MainActivity // 获取 MainActivity 上下文
                if (isFavoriteState.value) {
                    mainActivity?.addFavorite(city, state)
                    Toast.makeText(context, "$city was added to favorites", Toast.LENGTH_SHORT).show()
                } else {
                    mainActivity?.removeFavorite(city, state)
                    Toast.makeText(context, "$city was removed from favorites", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}


// Floating Action Button
@Composable
fun CustomFloatingActionButton(
    isMarkerAdded: Boolean,
    onFabClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        FloatingActionButton(
            onClick = { onFabClick() },
            modifier = Modifier.padding(16.dp),
            containerColor = Color.White,
            shape = CircleShape
        ) {
            Icon(
                painter = painterResource(
                    if (isMarkerAdded) R.drawable.map_marker_minus else R.drawable.map_marker_plus
                ),
                contentDescription = null,
                tint = Color.Black
            )
        }
    }
}


// Card0
@Composable
fun InformationCard(isSearchMode: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        shape = RectangleShape
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ){
            if (isSearchMode) {
                Text(
                    text = "Search Result",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                )
            }
        }
    }
}


// Card1
@Composable
fun LocationCard(title: String, subtitle: String, description: String, onClick: () -> Unit) {
    val parts = subtitle.split(",")
    val weatherDescription = parts.getOrNull(0)?.trim() ?: "Unknown"
    val temperature = parts.getOrNull(1)?.trim() ?: "N/A"


    Card(
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable { onClick() },
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 上半部分：图标 + 天气信息
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // 左侧：天气图标
                WeatherIcon(
                    weatherDescription = weatherDescription,
                    iconSize = 64.dp
                )

                // 右侧：温度 + 天气描述
                Spacer(modifier = Modifier.width(16.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = temperature,
                        color = Color.Gray,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = weatherDescription,
                        color = Color.Gray,
                        fontSize = 24.sp
                    )
                }
            }

            // 下半部分：地址
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = Color.Gray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Image(
                    painter = painterResource(id = R.drawable.information),
                    contentDescription = "Information",
                    modifier = Modifier
                        .size(36.dp)
                        .padding(end = 16.dp)
                )
            }


        }
    }
}


// 获取对应weather图标
@Composable
fun WeatherIcon(
    weatherDescription: String,
    iconSize: Dp = 48.dp
) {
    val iconName = weatherDescription.lowercase().replace(" ", "_")
    val context = LocalContext.current

    val iconResId = context.resources.getIdentifier(iconName, "drawable", context.packageName)

    if (iconResId != 0) {
        Image(
            painter = painterResource(id = iconResId),
            contentDescription = weatherDescription,
            modifier = Modifier.size(iconSize)
        )
    } else {
        Text(
            text = "N/A",
            color = Color.Gray,
            modifier = Modifier.size(iconSize)
        )
    }
}


// Card2
@Composable
fun LocalWeatherCard(details: Map<String, String>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            details.forEach { (label, value) ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 第一行：图标
                    val iconName = label.lowercase().replace(" ", "_")
                    WeatherIcon(
                        weatherDescription = iconName,
                        iconSize = 48.dp
                    )

                    // 第二行：数据值
                    Text(
                        text = value,
                        color = Color.Gray,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // 第三行：标签名
                    Text(
                        text = label,
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

// Card3
@Composable
fun LocalFutureWeatherCard(futureWeather: List<Map<String, String>>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(top = 12.dp),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E2E2E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            futureWeather.forEach { day ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1E1E))
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 左侧：日期
                    Text(
                        text = day["date"]?.substring(0, 10) ?: "N/A",
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(start = 16.dp)
                    )

                    // 中间：图标
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val iconName = day["weatherDescription"]?.lowercase()?.replace(" ", "_") ?: "unknown"
                        WeatherIcon(
                            weatherDescription = iconName,
                            iconSize = 24.dp
                        )
                    }

                    // 右侧：温度
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text(
                            text = day["temperatureMin"] ?: "N/A",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Text(
                            text = day["temperatureMax"] ?: "N/A",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}


suspend fun fetchLocation(context: Context): Pair<String?, Pair<Double, Double>?> {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    return suspendCancellableCoroutine { continuation ->
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            continuation.resume(Pair("Permission denied", null))
            return@suspendCancellableCoroutine
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val formattedAddress = "${address.locality}, ${address.adminArea}"
                    continuation.resume(Pair(formattedAddress, Pair(location.latitude, location.longitude)))
                } else {
                    continuation.resume(Pair("Location not available", null))
                }
            } else {
                continuation.resume(Pair("Location not available", null))
            }
        }.addOnFailureListener {
            continuation.resume(Pair("Failed to get location: ${it.message}", null))
        }
    }
}



// 获取某经纬度下的天气
suspend fun fetchWeatherData(
    latitude: Double,
    longitude: Double
): Pair<Map<String, Any>?, List<Map<String, Any>>?> {
    val url =
        "https://csci571-a3-backend-swhu-23.wn.r.appspot.com/get_weather?latitude=$latitude&longitude=$longitude"
    println(url)
    return withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            delay(500)

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val jsonResponse = JSONObject(response)

                if (jsonResponse.getString("status") == "success") {
                    val currentWeatherInfo = jsonResponse.getJSONObject("current_weather_info")
                    val currentWeather = mapOf(
                        "temperature" to currentWeatherInfo.getDouble("temperature"),
                        "humidity" to currentWeatherInfo.getDouble("humidity"),
                        "pressure" to currentWeatherInfo.getDouble("pressure"),
                        "windSpeed" to currentWeatherInfo.getDouble("windSpeed"),
                        "visibility" to currentWeatherInfo.getDouble("visibility"),
                        "cloudCover" to currentWeatherInfo.getDouble("cloudCover"),
                        "uvIndex" to currentWeatherInfo.getInt("uvIndex"),
                        "weatherDescription" to currentWeatherInfo.getString("weatherDescription")
                    )

                    val futureWeatherInfo = jsonResponse.getJSONArray("future_weather_info")
                    val futureWeather = mutableListOf<Map<String, Any>>()
                    for (i in 0 until futureWeatherInfo.length()) {
                        val dayInfo = futureWeatherInfo.getJSONObject(i)
                        val dayWeather = mapOf(
                            "date" to dayInfo.getString("date"),
                            "dayOfWeek" to dayInfo.getString("dayOfWeek"),
                            "temperatureMax" to dayInfo.getDouble("temperatureMax"),
                            "temperatureMin" to dayInfo.getDouble("temperatureMin"),
                            "weatherDescription" to dayInfo.getString("weatherDescription"),
                            "precipitation" to dayInfo.getDouble("precipitationProbability")
                        )
                        futureWeather.add(dayWeather)
                    }
                    Pair(currentWeather, futureWeather)
                } else {
                    Pair(null, null)
                }
            } else {
                Pair(null, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(null, null)
        }
    }
}


fun formatField(value: Any?, unit: String = "", isTemperature: Boolean = false): String {
    return when (value) {
        is Double -> {
            if (isTemperature) {
                String.format("%d%s", value.roundToInt(), unit)
            } else {
                String.format("%.2f%s", value, unit)
            }
        }
        is Int -> "$value$unit"
        else -> value?.toString() ?: "N/A"
    }
}

val stateFullNameMap = mapOf(
    "AL" to "Alabama",
    "AK" to "Alaska",
    "AZ" to "Arizona",
    "AR" to "Arkansas",
    "CA" to "California",
    "CO" to "Colorado",
    "CT" to "Connecticut",
    "DE" to "Delaware",
    "FL" to "Florida",
    "GA" to "Georgia",
    "HI" to "Hawaii",
    "ID" to "Idaho",
    "IL" to "Illinois",
    "IN" to "Indiana",
    "IA" to "Iowa",
    "KS" to "Kansas",
    "KY" to "Kentucky",
    "LA" to "Louisiana",
    "ME" to "Maine",
    "MD" to "Maryland",
    "MA" to "Massachusetts",
    "MI" to "Michigan",
    "MN" to "Minnesota",
    "MS" to "Mississippi",
    "MO" to "Missouri",
    "MT" to "Montana",
    "NE" to "Nebraska",
    "NV" to "Nevada",
    "NH" to "New Hampshire",
    "NJ" to "New Jersey",
    "NM" to "New Mexico",
    "NY" to "New York",
    "NC" to "North Carolina",
    "ND" to "North Dakota",
    "OH" to "Ohio",
    "OK" to "Oklahoma",
    "OR" to "Oregon",
    "PA" to "Pennsylvania",
    "RI" to "Rhode Island",
    "SC" to "South Carolina",
    "SD" to "South Dakota",
    "TN" to "Tennessee",
    "TX" to "Texas",
    "UT" to "Utah",
    "VT" to "Vermont",
    "VA" to "Virginia",
    "WA" to "Washington",
    "WV" to "West Virginia",
    "WI" to "Wisconsin",
    "WY" to "Wyoming"
)

