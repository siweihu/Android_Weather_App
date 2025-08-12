// server.js
const express = require('express');
const https = require('https');
const bodyParser = require('body-parser');
const cors = require('cors');

const app = express();

app.use(bodyParser.json());
app.use(cors());

// 获取天气消息
const tomorrowApiKey = 'kMx68iqzUyzmgBAp6SN8LIF4KXLSHhq6';
const getWeatherUrl = `https://api.tomorrow.io/v4/timelines?apikey=${tomorrowApiKey}`;

const weatherCodeMapping = {
    1000: "Clear", 1100: "Mostly Clear", 1101: "Partly Cloudy", 1102: "Mostly Cloudy", 1001: "Cloudy",
    2000: "Fog", 2100: "Light Fog", 3000: "Light Wind", 3001: "Windy", 3002: "Strong Wind",
    4000: "Drizzle", 4001: "Rain", 4200: "Light Rain", 4201: "Heavy Rain",
    5000: "Snow", 5001: "Flurries", 5100: "Light Snow", 5101: "Heavy Snow",
    6000: "Freezing Drizzle", 6001: "Freezing Rain", 6200: "Light Freezing Rain", 6201: "Heavy Freezing Rain",
    7000: "Ice Pellets", 7101: "Heavy Ice Pellets", 7102: "Light Ice Pellets", 8000: "Thunderstorm"
};

const precipitationTypeMapping = {
    0: "No Precipitation", 1: "Rain", 2: "Snow", 3: "Freezing Rain", 4: "Ice Pellets"
};

app.get('/get_weather', (req, res) => {
    const { latitude, longitude } = req.query;

    if (!latitude || !longitude) {
        return res.status(400).json({ error: '缺少纬度或经度' });
    }

    const location = `${latitude},${longitude}`;
    const currentParams = {
        location,
        fields: ["temperature", "humidity", "pressureSeaLevel", "windSpeed", "visibility", "cloudCover", "uvIndex", "weatherCode"],
        timesteps: "1h",
        startTime: "now",
        endTime: "nowPlus1h",
        timezone: "America/Los_Angeles",
        units: "imperial"
    };

    const futureParams = {
        location,
        fields: ["temperatureMax", "temperatureMin", "windSpeed", "weatherCode", "precipitationType", "precipitationProbability", "humidity", "visibility", "sunriseTime", "sunsetTime", "temperatureApparent", "cloudCover", "temperature"],
        timesteps: "1d",
        startTime: "now",
        endTime: "nowPlus5d",
        timezone: "America/Los_Angeles",
        units: "imperial"
    };

    const hourlyParams = {
        location,
        fields: ["temperature", "humidity", "pressureSeaLevel", "windSpeed", "windDirection"],
        timesteps: "1h",
        startTime: "now",
        endTime: "nowPlus5d",
        timezone: "America/Los_Angeles",
        units: "imperial"
    };

    const fetchWeatherData = (params) => {
        return new Promise((resolve, reject) => {
            const url = `${getWeatherUrl}&location=${params.location}&fields=${params.fields.join(',')}&timesteps=${params.timesteps}&startTime=${params.startTime}&endTime=${params.endTime}&timezone=${params.timezone}&units=${params.units}`;
            https.get(url, (weatherRes) => {
                let data = '';
                weatherRes.on('data', (chunk) => data += chunk);
                weatherRes.on('end', () => {
                    try {
                        resolve(JSON.parse(data));
                    } catch (error) {
                        console.error('JSON 解析失败:', error);
                        reject(error);
                    }
                });
            }).on('error', (error) => {
                console.error('API 请求失败:', error);
                reject(error);
            });
        });
    };

    Promise.all([fetchWeatherData(currentParams), fetchWeatherData(futureParams), fetchWeatherData(hourlyParams)])
        .then(([currentData, futureData, hourlyData]) => {
            const currentInfo = currentData.data.timelines[0].intervals[0].values;
            const futureInfo = futureData.data.timelines[0].intervals.map(interval => ({
                date: interval.startTime,
                dayOfWeek: new Date(interval.startTime).toLocaleDateString('zh-CN', { weekday: 'long' }),
                temperatureMax: interval.values.temperatureMax,
                temperatureMin: interval.values.temperatureMin,
                windSpeed: interval.values.windSpeed,
                precipitationType: precipitationTypeMapping[interval.values.precipitationType] || "Unknown",
                precipitationProbability: interval.values.precipitationProbability,
                humidity: interval.values.humidity,
                visibility: interval.values.visibility,
                sunrise: interval.values.sunriseTime,
                sunset: interval.values.sunsetTime,
                temperatureApparent: interval.values.temperatureApparent,
                cloudCover: interval.values.cloudCover,
                weatherDescription: weatherCodeMapping[interval.values.weatherCode] || "Unknown",
                temperature: interval.values.temperature
            }));
            const hourlyInfo = hourlyData.data.timelines[0].intervals.map(interval => ({
                time: interval.startTime,
                temperature: interval.values.temperature,
                humidity: interval.values.humidity,
                pressureSeaLevel: interval.values.pressureSeaLevel,
                windSpeed: interval.values.windSpeed,
                windDirection: interval.values.windDirection
            }));

            res.json({
                status: 'success',
                current_weather_info: {
                    temperature: currentInfo.temperature,
                    humidity: currentInfo.humidity,
                    pressure: currentInfo.pressureSeaLevel,
                    windSpeed: currentInfo.windSpeed,
                    visibility: currentInfo.visibility,
                    cloudCover: currentInfo.cloudCover,
                    uvIndex: currentInfo.uvIndex,
                    weatherDescription: weatherCodeMapping[currentInfo.weatherCode] || "Unknown"
                },
                future_weather_info: futureInfo,
                hourly_info: hourlyInfo
            });
        })
        .catch(error => {
            console.error('获取天气数据失败:', error);
            res.status(500).json({ error: '获取天气数据失败' });
        });
});


// 获取收藏夹消息
const mongoose = require('mongoose');

mongoose.connect('mongodb+srv://siweihu:ZrTHR5RzhSyQn3_@locationcluster0.7la9l.mongodb.net/?retryWrites=true&w=majority&appName=LocationCluster0', {
    useNewUrlParser: true,
    useUnifiedTopology: true
})
.then(() => console.log('Connected to MongoDB Atlas'))
.catch(error => console.error('Error connecting to MongoDB Atlas:', error));

const favoriteSchema = new mongoose.Schema({
    city: String,
    state: String,
    createdAt: {
        type: Date,
        default: Date.now
    }
});

const Favorite = mongoose.model('Favorite', favoriteSchema);

// 添加收藏
app.get('/favorites/add', async (req, res) => {
    const { city, state } = req.query;

    if (!city || !state) {
        return res.status(400).json({ error: 'City and state are required' });
    }

    try {
        const newFavorite = new Favorite({ city, state });
        await newFavorite.save();
        res.status(200).json({ message: 'Favorite added successfully' });
    } catch (error) {
        console.error('Error adding favorite:', error);
        res.status(500).json({ error: 'Error adding favorite' });
    }
});


// 移除收藏
app.get('/favorites/remove', async (req, res) => {
    const { city, state } = req.query; 

    if (!city || !state) {
        return res.status(400).json({ error: 'City and state are required' });
    }

    try {
        await Favorite.deleteOne({ city, state });
        res.status(200).json({ message: 'Favorite removed successfully' });
    } catch (error) {
        console.error('Error removing favorite:', error);
        res.status(500).json({ error: 'Error removing favorite' });
    }
});

// 列出所有收藏
app.get('/favorites/list', async (req, res) => {
    try {
        const favorites = await Favorite.find();
        res.status(200).json(favorites);
    } catch (error) {
        console.error('Error fetching favorites:', error);
        res.status(500).json({ error: 'Error fetching favorites' });
    }
});


googleApiKey = 'AIzaSyDjORg0iR-NHKOr8S5L9XZhpmy1MtuAP_c';

// 处理获取城市建议请求
app.get('/fetch_city_suggestions', (req, res) => {
    const { input } = req.query; 
    if (!input) {
        return res.status(400).json({ error: 'Input is required' });
    }

    const apiUrl = `https://maps.googleapis.com/maps/api/place/autocomplete/json?input=${encodeURIComponent(input)}&key=${googleApiKey}`;
    https.get(apiUrl, (apiRes) => {
        let data = '';
        apiRes.on('data', (chunk) => data += chunk);
        apiRes.on('end', () => {
            try {
                res.json(JSON.parse(data));
            } catch (error) {
                res.status(500).json({ error: 'Error parsing response' });
            }
        });
    }).on('error', (error) => {
        res.status(500).json({ error: 'API request failed' });
    });
});


// 处理经纬度到地址的请求
app.get('/coords2location', (req, res) => {
    const { latitude, longitude } = req.query; 

    if (!latitude || !longitude) {
        return res.status(400).json({ error: 'Latitude and longitude are required' });
    }

    const apiUrl = `https://maps.googleapis.com/maps/api/geocode/json?latlng=${latitude},${longitude}&key=${googleApiKey}`;
    https.get(apiUrl, (apiRes) => {
        let data = '';
        apiRes.on('data', (chunk) => data += chunk);
        apiRes.on('end', () => {
            try {
                res.json(JSON.parse(data));
            } catch (error) {
                res.status(500).json({ error: 'Error parsing response' });
            }
        });
    }).on('error', (error) => {
        res.status(500).json({ error: 'API request failed' });
    });
});


// 处理地址到经纬度的请求
app.get('/location2coord', (req, res) => {
    const { address } = req.query; 
    if (!address) {
        return res.status(400).json({ error: 'Address is required' });
    }

    const apiUrl = `https://maps.googleapis.com/maps/api/geocode/json?address=${encodeURIComponent(address)}&key=${googleApiKey}`;
    https.get(apiUrl, (apiRes) => {
        let data = '';
        apiRes.on('data', (chunk) => data += chunk);
        apiRes.on('end', () => {
            try {
                res.json(JSON.parse(data));
            } catch (error) {
                res.status(500).json({ error: 'Error parsing response' });
            }
        });
    }).on('error', (error) => {
        res.status(500).json({ error: 'API request failed' });
    });
});




const PORT = process.env.PORT;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
