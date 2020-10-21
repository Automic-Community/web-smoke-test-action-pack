package com.uc4.ara.feature.websmoketest;

import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

import com.uc4.ara.feature.globalcodes.ErrorCodes;
import com.uc4.ara.util.Logger;

public class GetSite extends AbstractWebSmokeTest {
    
  @Override
  public void initialize() {
    super.initialize();
    parser.setDescription("Checks if a page ('URL') can be opened and downloaded within a certain period of time ('Open timeout').\n"
        + "3xx Redirect return codes should be resolved and the redirect destination called.");
    parser.setExamples("java -jar ARATools websmoketest GetSite -url \"http://www.yoursite.com\" -u \"yourusername\" -p \"yourpassword\" -t 10000");
  }
  
  @Override
  public int run(String[] args) throws Exception {
    super.run(args);
        
    // parse the common input parameters
    WebTestInput input = parseInput();
    
    if (input.isIgnoreServerCert()) {
      Logger.logInfo("SSL Certificate validation is ignored", this.loglevelValue);
    }
    
    HttpClient httpclient = null;
    int statusCode = ErrorCodes.EXCEPTION;
    try {
      httpclient = WebTestUtils.getHttpClient(input);
    } catch (MalformedURLException e) {
      Logger.log("Error-Message: Invalid URL.", this.loglevelValue);
      return statusCode;
    }
    
    Logger.log("Making the GET request to " + input.getUrl() + " ...", this.loglevelValue);
    
    if (!StringUtils.isEmpty(input.getUsername())) {
      Logger.log("Authentication username: " + input.getUsername(), this.loglevelValue);
    }
    
    if (!StringUtils.isEmpty(input.getProxyHost())) {
      Logger.log("Use proxy server: " + input.getProxyHost() + ":" + input.getProxyPort(), this.loglevelValue);
    }
    
    // make GET request
    HttpUriRequest httpGet = new HttpGet(input.getUrl());
    
    try {
      HttpResponse response = httpclient.execute(httpGet);
      statusCode = response.getStatusLine().getStatusCode();
      Logger.log("Received status code " + statusCode, this.loglevelValue);
      
      if (statusCode >= 200 && statusCode < 300) {
        Logger.log("Successfully get site " + input.getUrl(), this.loglevelValue);
        return OK;
      }
      
      if (statusCode == 401) {
        Logger.log("Server authentication failed!", this.loglevelValue);
        return WRONG_CREDENTIALS;    
      }
      
      if (statusCode == 407) {
        Logger.log("Proxy server authentication failed!", this.loglevelValue);
        return WRONG_PROXY_CREDENTIALS;
      }
      
      Logger.log("Get site finished unsuccessfully. The status code is " + statusCode, this.loglevelValue);
      return statusCode;
      
    } catch (SocketTimeoutException e) {
      Logger.log("Socket read timeout! Runbook failed.", this.loglevelValue);
      return TIMEOUT;
    } catch (UnknownHostException e) {
      Logger.log("Error: Unknown host.", this.loglevelValue);
      return SERVER_NOT_AVAILABLE;
    } catch (NoRouteToHostException e) {
      if (!StringUtils.isEmpty(input.getProxyHost())) {
        Logger.log("Error: Not able to connect to the proxy server.", this.loglevelValue);
        return PROXY_SERVER_NOT_AVAILABLE;
      } else {
        Logger.log("Error-Message: " + e.getMessage(), this.loglevelValue);
        return statusCode;
      }
    } catch(SSLException e) {
      Logger.log("SSL error: Not trusted server certificate", this.loglevelValue);
      return SERVER_CERTIFICATE_FAILED;
    } catch (ConnectException e) {
      Logger.log("Error: Failed to connect to the server.", this.loglevelValue);
      if (StringUtils.isEmpty(input.getProxyHost())) return SERVER_NOT_AVAILABLE;
      else return PROXY_SERVER_NOT_AVAILABLE;
    } catch (Exception e) {
      Logger.logDebug(ExceptionUtils.getStackTrace(e), this.loglevelValue);
      Logger.log("Error-Message: " + e.getMessage(), this.loglevelValue);
      return statusCode;
    } finally {
      httpclient.getConnectionManager().shutdown();
    }    
  }

}
