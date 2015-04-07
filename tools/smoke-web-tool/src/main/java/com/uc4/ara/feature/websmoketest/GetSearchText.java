package com.uc4.ara.feature.websmoketest;

import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.net.ssl.SSLException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import com.uc4.ara.feature.globalcodes.ErrorCodes;
import com.uc4.ara.feature.utils.CmdLineParser;
import com.uc4.ara.util.Logger;

public class GetSearchText extends AbstractWebSmokeTest {

  private CmdLineParser.Option<String> expectedText;
  private CmdLineParser.Option<String> isRegex;
  
  @Override
  public void initialize() {
    super.initialize();
    parser.setDescription("Opens a page ('URL') and checks if the 'Expected text' appears somewhere on the side. HTML tags will be stripped before evaluation.");
    parser.setExamples("java -jar ARATools websmoketest GetSearchText -url \"http://www.yoursite.com\" -et \"My expected text\" -t 10000");
    
    expectedText = parser.addHelp(parser.addStringOption("et", "expectedText", true),
        "Expected text on the web page. Either the text given as parameter has to appear somewhere on the page or a regular expression is expected."); 
    
    /*isRegex = parser.addHelp(parser.addBooleanOption("re", "regex", false), 
        "If set to \"yes\", than the text is treated as regular expression. Otherwise an exact matching is expected.");*/
    
    isRegex = parser.addHelp(parser.addStringOption("re", "regex", false), 
        "If set to \"yes\", than the text is treated as regular expression. Otherwise an exact matching is expected.");
  }
  
  @Override
  public int run(String[] args) throws Exception {
    super.run(args);
    
    WebTestInput input = parseInput();

    String expectedTextValue = parser.getOptionValue(expectedText);
    
    boolean isRegexValue = true;
    try {
      String isRegexStr = parser.getOptionValue(isRegex);
      isRegexValue = "YES".equalsIgnoreCase(isRegexStr) || "TRUE".equalsIgnoreCase(isRegexStr);
    } catch (Exception e) {}
    
    if (input.isIgnoreServerCert()) {
      Logger.logInfo("SSL Certificate validation is ignored.", this.loglevelValue);
    }
    
    Logger.log("Regular expression is " + (isRegexValue?"enabled":"disabled"), this.loglevelValue);
    
    // check the regex
    if (isRegexValue) {
      try {
        Pattern.compile(expectedTextValue);
      } catch (PatternSyntaxException e) {
        Logger.log("The pattern" + expectedTextValue + " is not valid", this.loglevelValue);
        //return TEXT_NOT_FOUND;
        return ErrorCodes.EXCEPTION;
      }
    }
    
    HttpClient httpclient = null;
    int statusCode = ErrorCodes.EXCEPTION;
    try {
      httpclient = WebTestUtils.getHttpClient(input);
    } catch (MalformedURLException e) {
      Logger.log("Error-Message: Invalid URL.", this.loglevelValue);
      return statusCode;
    }
    
    Logger.log("Making the request to " + input.getUrl() + " ...", this.loglevelValue);   
    
    if (!StringUtils.isEmpty(input.getUsername())) {
      Logger.log("Authentication username: " + input.getUsername(), this.loglevelValue);
    }
    
    if (!StringUtils.isEmpty(input.getProxyHost())) {
      Logger.log("Use proxy server: " + input.getProxyHost() + ":" + input.getProxyPort(), this.loglevelValue);
    }
    
    HttpGet httpGet = new HttpGet(input.getUrl());
    
    if (!StringUtils.isEmpty(input.getProxyHost())) {
      Logger.log("Use proxy server: " + input.getProxyHost() + ":" + input.getProxyPort(), this.loglevelValue);
    }
    
    try {           
      HttpResponse response = httpclient.execute(httpGet);
      statusCode = response.getStatusLine().getStatusCode();
      Logger.log("Received status code " + statusCode, this.loglevelValue);
      
      if (statusCode == 401) {
        Logger.log("Server authentication failed!", this.loglevelValue);
        return WRONG_CREDENTIALS;
      }
      
      if (statusCode == 407) {
        Logger.log("Proxy server authentication failed!", this.loglevelValue);
        return WRONG_PROXY_CREDENTIALS;
      }
           
      if (statusCode >= 200 && statusCode < 300) {
        Logger.log("Successfully get site " + input.getUrl(), this.loglevelValue);
        HttpEntity entity = response.getEntity();
        String content = EntityUtils.toString(entity, "UTF-8");
        String cleanedContent = WebTestUtils.html2text(content);
        if (StringUtils.isEmpty(cleanedContent)) {
          Logger.log("Received empty content page. Return now..", this.loglevelValue);
          return TEXT_NOT_FOUND;
        }
        
        //Logger.logDebug("Page content: " + cleanedContent, this.loglevelValue);        
        Logger.log("Check if " + (isRegexValue ? "part of page content matches the regular expression: "
                : "page content contains expected text: ") + expectedTextValue, this.loglevelValue);
        
        if (isRegexValue) {
          Pattern p = Pattern.compile(expectedTextValue, Pattern.CASE_INSENSITIVE | Pattern.DOTALL); 
          Matcher m = p.matcher(cleanedContent);
          statusCode = m.find() ? OK : TEXT_NOT_FOUND;
          Logger.log(statusCode == OK ? "The given regular expression matches.": "The given regular expression does not match.", this.loglevelValue);
          return statusCode;
        } else {
          statusCode = cleanedContent.toLowerCase().contains(expectedTextValue.toLowerCase()) ? OK : TEXT_NOT_FOUND;
          Logger.log(statusCode == OK ? "Found the given text in page content.":"Couldn't find the given text in page content.",  this.loglevelValue);
          return statusCode; 
        }
      } else {
        Logger.log("Get site finished unsuccessfully. The status code is " + statusCode, this.loglevelValue);
        return statusCode;
      }
      
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
