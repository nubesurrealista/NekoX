package tw.nekomimi.nekogram.transtale.deepl;

import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.SharedConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DeepLTranslatorRaw {

    public String translate(String query, String source_lang, String target_lang) throws IOException, JSONException {
        JSONObject jsonPayload = new JSONObject();
        jsonPayload.put("text", query);
        jsonPayload.put("source_lang", source_lang);
        jsonPayload.put("target_lang", target_lang);

        String response = request("https://dplx.xi-xu.me/deepl", jsonPayload.toString());
        JSONObject jsonResponse = new JSONObject(response);

        if (jsonResponse.getInt("code") == 200) {
            return jsonResponse.getString("data");
        } else {
            throw new IOException("Translation error: " + jsonResponse.optString("message", "Unknown error"));
        }
    }

    private String request(String url, String body) throws IOException {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        try {
            URL targetUrl = new URL(url);
            Proxy proxy = SharedConfig.proxyInfo != null && SharedConfig.proxyInfo.isEnabled()
                    ? SharedConfig.proxyInfo.getProxy()
                    : null;
            connection = proxy != null ? (HttpURLConnection) targetUrl.openConnection(proxy)
                    : (HttpURLConnection) targetUrl.openConnection();

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            connection.addRequestProperty("Content-Type", "application/json");
            connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4147.105 Safari/537.36");
            connection.addRequestProperty("Accept", "application/json");
            connection.addRequestProperty("Connection", "keep-alive");

            try (OutputStream output = connection.getOutputStream()) {
                output.write(body.getBytes(StandardCharsets.UTF_8));
                output.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP error code: " + responseCode);
            }

            ByteArrayOutputStream result = new ByteArrayOutputStream();
            inputStream = connection.getInputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, bytesRead);
            }

            return new String(result.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
