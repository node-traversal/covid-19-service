package io.nodetraversal.poi;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import javax.inject.Named;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

@Named
public class HttpClientRemoteWorkbookProvider implements RemoteWorkbookProvider {

    public <R> R get(String url, Function<Workbook, R> convert) {
        HttpEntity responseEntity = null;

        try (CloseableHttpClient httpclient =
                     HttpClients.custom()
                             .setSSLSocketFactory(ignoreCerts())
                             .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                             .build()) {
            try (CloseableHttpResponse response = httpclient.execute(new HttpGet(url))) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new ConnectionFailedException(url, response.getStatusLine().getStatusCode());
                }
                responseEntity = response.getEntity();
                if (responseEntity == null) {
                    throw new IllegalStateException(url + " did not return an entity");
                }
                Workbook workbook = readWorkbook(responseEntity.getContent());

                return convert.apply(workbook);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            EntityUtils.consumeQuietly(responseEntity);
        }
    }

    private Workbook readWorkbook(InputStream inputStream) {
        try {
            return WorkbookFactory.create(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static SSLConnectionSocketFactory ignoreCerts() {
        try {
            SSLContextBuilder sslContextBuilder = SSLContextBuilder.create();
            sslContextBuilder.loadTrustMaterial(new org.apache.http.conn.ssl.TrustSelfSignedStrategy());
            SSLContext sslContext = sslContextBuilder.build();
            return new SSLConnectionSocketFactory(sslContext, new org.apache.http.conn.ssl.DefaultHostnameVerifier());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
