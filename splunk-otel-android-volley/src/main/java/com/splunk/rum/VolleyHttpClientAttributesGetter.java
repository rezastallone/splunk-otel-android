/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.splunk.rum;

import static java.util.Collections.emptyList;

import androidx.annotation.Nullable;
import com.android.volley.AuthFailureError;
import com.android.volley.Header;
import com.android.volley.Request;
import com.android.volley.toolbox.HttpResponse;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesGetter;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

enum VolleyHttpClientAttributesGetter
        implements HttpClientAttributesGetter<RequestWrapper, HttpResponse> {
    INSTANCE;

    @Override
    @Nullable
    public String getUrlFull(RequestWrapper requestWrapper) {
        return requestWrapper.getRequest().getUrl();
    }

    @Nullable
    @Override
    public String getServerAddress(RequestWrapper requestWrapper) {
        return UrlParser.getHost(requestWrapper.getRequest().getUrl());
    }

    @Nullable
    @Override
    public Integer getServerPort(RequestWrapper requestWrapper) {
        return UrlParser.getPort(requestWrapper.getRequest().getUrl());
    }

    @Nullable
    @Override
    public String getHttpRequestMethod(RequestWrapper requestWrapper) {
        Request<?> request = requestWrapper.getRequest();
        switch (request.getMethod()) {
            case Request.Method.GET:
                return "GET";
            case Request.Method.POST:
                return "POST";
            case Request.Method.PUT:
                return "PUT";
            case Request.Method.DELETE:
                return "DELETE";
            case Request.Method.HEAD:
                return "HEAD";
            case Request.Method.OPTIONS:
                return "OPTIONS";
            case Request.Method.TRACE:
                return "TRACE";
            case Request.Method.PATCH:
                return "PATCH";
            default:
                return null;
        }
    }

    @Override
    public List<String> getHttpRequestHeader(RequestWrapper requestWrapper, String name) {
        Request<?> request = requestWrapper.getRequest();
        try {
            Map<String, String> headers = request.getHeaders();
            Map<String, String> additionalHeaders = requestWrapper.getAdditionalHeaders();
            List<String> result = new ArrayList<>();
            result.addAll(findCaseInsensitive(name, headers));
            result.addAll(findCaseInsensitive(name, additionalHeaders));
            return result;
        } catch (AuthFailureError e) {
            return emptyList();
        }
    }

    private List<String> findCaseInsensitive(String name, Map<String, String> headers) {
        List<String> result = new ArrayList<>(headers.size());
        for (Map.Entry<String, String> header : headers.entrySet()) {
            if (header.getKey().equalsIgnoreCase(name)) {
                result.add(header.getValue());
            }
        }
        return result;
    }

    @Override
    public Integer getHttpResponseStatusCode(
            RequestWrapper requestWrapper, HttpResponse response, @Nullable Throwable error) {
        return response.getStatusCode();
    }

    @Override
    public List<String> getHttpResponseHeader(
            RequestWrapper requestWrapper, @Nullable HttpResponse response, String name) {
        if (response == null) {
            return emptyList();
        }
        return headersToList(response.getHeaders(), name);
    }

    static List<String> headersToList(List<Header> headers, String name) {
        if (headers.size() == 0) {
            return emptyList();
        }

        List<String> headersList = new ArrayList<>();
        for (Header header : headers) {
            if (header.getName().equalsIgnoreCase(name)) {
                headersList.add(header.getValue());
            }
        }
        return headersList;
    }

    @Override
    public String getTransport(RequestWrapper requestWrapper, HttpResponse httpResponse) {
        return SemanticAttributes.NetTransportValues.IP_TCP;
    }

    @Override
    public String getNetworkTransport(RequestWrapper requestWrapper, HttpResponse httpResponse) {
        return "tcp";
    }
}
