package com.example.weather.services;

import com.example.weather.models.Location;
import com.example.weather.models.openweather.SearchResult;
import com.example.weather.models.openweather.WeatherData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.List;
@Service
public class WeatherApiClient {
    private final String apiUrl = "https://api.openweathermap.org/";
    private final String apiKey = "655628e636e79e03375daf04372ebe26";

    private final RestTemplate restTemplate;


    @Autowired
    public WeatherApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public WeatherData getWeatherData(Location location) {
        String url = UriComponentsBuilder.fromHttpUrl(apiUrl + "data/2.5/weather")
                .queryParam("lat", location.getLatitude())
                .queryParam("lon", location.getLongitude())
                .queryParam("appid", apiKey)
                .queryParam("units", "metric")
                .toUriString();

        System.out.println("WeatherData Request URL: " + url);

        return restTemplate.getForObject(url, WeatherData.class);
    }

    public ResponseEntity<List<SearchResult>> getLocationByName(String locationName) {
        String url = UriComponentsBuilder.fromHttpUrl(apiUrl + "geo/1.0/direct")
                .queryParam("q", locationName)
                .queryParam("limit", 5)
                .queryParam("appid", apiKey)
                .toUriString();

        System.out.println("getLocationByName Request URL: " + url);

        ResponseEntity<SearchResult[]> responseEntity;
        try {
            responseEntity = restTemplate.exchange(url, HttpMethod.GET, null, SearchResult[].class);
        } catch (Exception e) {
            System.err.println("Error occurred: " + e.getMessage());
            throw e;
        }

        List<SearchResult> searchResults = Arrays.asList(responseEntity.getBody());
        return new ResponseEntity<>(searchResults, responseEntity.getStatusCode());
    }
}
