package com.dingxianginc.ctu.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dingxianginc.ctu.client.model.CaptchaResponse;
import com.dingxianginc.ctu.client.model.CaptchaStatus;
import com.dingxianginc.ctu.client.util.HttpClientPool;
import com.dingxianginc.ctu.client.util.InputStreamUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

import java.util.concurrent.ExecutionException;

/** Created by dingxiang-inc on 2017/7/31. */
public class CaptchaClient {

  private String captchaUrl = "https://cap.dingxiang-inc.com/api/tokenVerify";
  private String appId;
  private String appSecret;
  RequestConfig requestConfig = null;
  CloseableHttpClient httpClient = null;

  public CaptchaClient(String appId, String appSecret) {
    this.appId = appId;
    this.appSecret = appSecret;
    this.httpClient = HttpClientPool.getInstance().getHttpClient();
    this.requestConfig = HttpClientPool.getInstance().getRequestConfig();
  }

  public CaptchaClient(
      String appId,
      String appSecret,
      int connectTimeout,
      int connectionRequestTimeout,
      int socketTimeout) {
    this.appId = appId;
    this.appSecret = appSecret;
    this.httpClient = HttpClientPool.getInstance().getHttpClient();
    this.requestConfig =
        RequestConfig.custom()
            .setConnectTimeout(connectTimeout)
            .setConnectionRequestTimeout(connectionRequestTimeout)
            .setSocketTimeout(socketTimeout)
            .build();
  }

  public CaptchaResponse verifyToken(String token) throws Exception {

    if (StringUtils.isBlank(token)
        || StringUtils.isBlank(appId)
        || StringUtils.isBlank(appSecret) || (token.length() > 1024)) {
      return new CaptchaResponse(false, CaptchaStatus.WRONG_PARAMETER);
    }
    String[] args = token.split(":");

    String sign = getVerifySign(appSecret, args[0]);
    String key = null;
    if (args.length == 2) {
      key = args[1];
    }else{
      key = "";
    }
    String reqUrl =
        String.format(
            "%s?token=%s&constId=%s&appKey=%s&sign=%s", captchaUrl, args[0], key, appId, sign);
    HttpGet httpGet = null;
    boolean flag = false;
    try {
      httpGet = new HttpGet(reqUrl);
      httpGet.setConfig(requestConfig);
    } catch (Exception e) {
      flag = true;
      e.printStackTrace();
    }
    if (flag || (httpGet == null)) {
      return new CaptchaResponse(false, CaptchaStatus.WRONG_PARAMETER);
    }
    CloseableHttpResponse response = null;
    try {
      response = httpClient.execute(httpGet);
      if (response.getStatusLine().getStatusCode() == 200) {
        String responseData = InputStreamUtils.readToString(response.getEntity().getContent());
        JSONObject resObject = JSON.parseObject(responseData);
        Boolean result = Boolean.parseBoolean(resObject.getString("success"));
        return new CaptchaResponse(result, CaptchaStatus.SERVER_SUCCESS);
      } else {
        return new CaptchaResponse(true, CaptchaStatus.SERVER_FAILED);
      }
    } catch (Exception e) {
      return new CaptchaResponse(true, "server connect error:" + e.getMessage());
    } finally {
      if (response != null) {
        response.close();
      }
      httpGet.releaseConnection();
    }
  }

  public void setCaptchaUrl(String captchaUrl) {
    this.captchaUrl = captchaUrl;
  }

  private String getVerifySign(String appSecret, String token) {
    StringBuilder sb = new StringBuilder();
    sb.append(appSecret).append(token).append(appSecret);
    return DigestUtils.md5Hex(sb.toString());
  }

}