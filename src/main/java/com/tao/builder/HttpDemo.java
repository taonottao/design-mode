package com.tao.builder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * @version 1.0
 * @Author T-WANG
 * @Date 2025/8/23 17:00
 */
public class HttpDemo {
    public static void main(String[] args) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.baidu.com"))
                .POST(HttpRequest.BodyPublishers.ofString("hello"))
                .build();
        client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
