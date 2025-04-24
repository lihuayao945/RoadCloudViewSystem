package com.RoadCloudVisualizationSystem.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class CoordinateUtil {

    @Value("${amap.key}")
    private String amapKey;

    private final RestTemplate restTemplate;

    public CoordinateUtil(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    public double[] convertCoordinate(String longitude, String latitude) {
        String locations = longitude + "," + latitude;
        String url = "https://restapi.amap.com/v3/assistant/coordinate/convert?locations=" +
                locations + "&coordsys=gps&key=" + amapKey;

        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

        if (response.getStatusCode() == HttpStatus.OK && "1".equals(response.getBody().get("status"))) {
            String convertedLocations = (String) response.getBody().get("locations");
            String[] coords = convertedLocations.split(",");
            return new double[]{Double.parseDouble(coords[0]), Double.parseDouble(coords[1])};
        }
        throw new RuntimeException("坐标转换失败: " + response.getBody());
    }


    public List<double[]> convertCoordinates(List<String> longitudes, List<String> latitudes) {
        if (longitudes.size() != latitudes.size()) {
            throw new IllegalArgumentException("经度列表和纬度列表长度不一致");
        }

        // 构造坐标字符串，格式为"经度,纬度|经度,纬度..."
        String locations = IntStream.range(0, longitudes.size())
                .mapToObj(i -> longitudes.get(i) + "," + latitudes.get(i))
                .collect(Collectors.joining("|"));

        String url = "https://restapi.amap.com/v3/assistant/coordinate/convert?locations=" +
                locations + "&coordsys=gps&key=" + amapKey;

        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

        if (response.getStatusCode() == HttpStatus.OK && "1".equals(response.getBody().get("status"))) {
            String convertedLocations = (String) response.getBody().get("locations");
            return Arrays.stream(convertedLocations.split(";"))
                    .map(s -> {
                        String[] coords = s.split(",");
                        return new double[]{Double.parseDouble(coords[0]), Double.parseDouble(coords[1])};
                    })
                    .collect(Collectors.toList());
        }
        throw new RuntimeException("坐标转换失败: " + response.getBody());
    }

    public double[] convertCoordinateLocal(String longitude, String latitude) {
        return wgs84ToGcj02(Double.parseDouble(longitude), Double.parseDouble(latitude));
    }

    public List<double[]> convertCoordinatesLocal(List<String> longitudes, List<String> latitudes) {
        if (longitudes.size() != latitudes.size()) {
            throw new IllegalArgumentException("经度列表和纬度列表长度不一致");
        }

        return IntStream.range(0, longitudes.size())
                .mapToObj(i -> wgs84ToGcj02(
                        Double.parseDouble(longitudes.get(i)),
                        Double.parseDouble(latitudes.get(i))))
                .collect(Collectors.toList());
    }

    // WGS84转GCJ02本地算法实现
    private static final double PI = 3.1415926535897932384626;
    private static final double EE = 0.00669342162296594323;
    private static final double A = 6378245.0;

    private double[] wgs84ToGcj02(double longitude, double latitude) {
        if (outOfChina(longitude, latitude)) {
            return new double[]{longitude, latitude};
        }
        double dLat = transformLat(longitude - 105.0, latitude - 35.0);
        double dLon = transformLon(longitude - 105.0, latitude - 35.0);
        double radLat = latitude / 180.0 * PI;
        double magic = Math.sin(radLat);
        magic = 1 - EE * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * PI);
        dLon = (dLon * 180.0) / (A / sqrtMagic * Math.cos(radLat) * PI);
        return new double[]{longitude + dLon, latitude + dLat};
    }

    private boolean outOfChina(double lon, double lat) {
        return lon < 72.004 || lon > 137.8347 || lat < 0.8293 || lat > 55.8271;
    }

    private double transformLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * PI) + 40.0 * Math.sin(y / 3.0 * PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * PI) + 320 * Math.sin(y * PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    private double transformLon(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * PI) + 40.0 * Math.sin(x / 3.0 * PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * PI) + 300.0 * Math.sin(x / 30.0 * PI)) * 2.0 / 3.0;
        return ret;
    }
}