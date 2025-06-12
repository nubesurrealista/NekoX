package moe.hx030.momogram.util.res;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public interface Resource {

    String getName();

    URL getUrl();

    InputStream getStream();

    default boolean isModified(){
        return false;
    }

    default void writeTo(OutputStream out) throws RuntimeException {
        try (InputStream in = getStream()) {
            int len = 0;
            byte[] buf = new byte[1024];
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    default BufferedReader getReader(Charset charset) {
        return new BufferedReader(new InputStreamReader(getStream(), charset));
    }

    default String readStr(Charset charset) throws RuntimeException {
        BufferedReader reader = getReader(charset);
        return reader.lines().collect(Collectors.joining("\n"));
    }

    default String readUtf8Str() throws RuntimeException {
        return readStr(StandardCharsets.UTF_8);
    }

    default byte[] readBytes() throws RuntimeException, IOException {
        var out = new ByteArrayOutputStream();
        var buf = new byte[1024];
        var stream = getStream();
        int read = 0;
        while ((read = stream.read(buf)) > 0) {
            out.write(buf, 0, read);
        }
        return out.toByteArray();
    }
}
