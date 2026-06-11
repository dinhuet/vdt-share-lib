package com.pm.sharedlib.runtime;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        this(request, -1);
    }

    public CachedBodyHttpServletRequest(HttpServletRequest request, long maxBodyBytes) throws IOException {
        super(request);
        this.cachedBody = readBody(request, maxBodyBytes);
    }

    @Override
    public ServletInputStream getInputStream() {
        return new CachedBodyServletInputStream(cachedBody);
    }

    @Override
    public BufferedReader getReader() {
        var encoding = getCharacterEncoding() != null ? getCharacterEncoding() : StandardCharsets.UTF_8.name();
        return new BufferedReader(new InputStreamReader(getInputStream(), java.nio.charset.Charset.forName(encoding)));
    }

    public byte[] getCachedBody() {
        return cachedBody.clone();
    }

    private byte[] readBody(HttpServletRequest request, long maxBodyBytes) throws IOException {
        if (maxBodyBytes < 0) {
            return request.getInputStream().readAllBytes();
        }

        var output = new ByteArrayOutputStream();
        var buffer = new byte[8192];
        long total = 0;
        int read;
        var inputStream = request.getInputStream();
        while ((read = inputStream.read(buffer)) != -1) {
            total += read;
            if (total > maxBodyBytes) {
                throw new RuntimeSecurityException(413, "REQUEST_TOO_LARGE", "Request body is too large");
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static class CachedBodyServletInputStream extends ServletInputStream {

        private final ByteArrayInputStream inputStream;

        CachedBodyServletInputStream(byte[] body) {
            this.inputStream = new ByteArrayInputStream(body);
        }

        @Override
        public int read() {
            return inputStream.read();
        }

        @Override
        public boolean isFinished() {
            return inputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            if (readListener == null) {
                return;
            }
            try {
                readListener.onDataAvailable();
                if (isFinished()) {
                    readListener.onAllDataRead();
                }
            } catch (IOException e) {
                readListener.onError(e);
            }
        }
    }
}
