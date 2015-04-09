package com.uc4.ara.feature.websmoketest;

import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.uc4.ara.feature.globalcodes.ErrorCodes;
import com.uc4.ara.feature.utils.CmdLineParser;
import com.uc4.ara.util.Logger;

public class GetSearchElement extends AbstractWebSmokeTest {
  
  private CmdLineParser.Option<String> expectedElement;
  
  @Override
  public void initialize() {
    super.initialize();
    parser.setDescription("Opens a page ('URL') and checks if a certain HTML-element appears on the page. The search pattern is defined as an XPath expression. " +
    		"For example you can check the existence of links, paragraphs, divisions <div>, etc..");
    parser.setExamples("java -jar ARATools websmoketest GetSearchElement -url \"http://www.yoursite.com\" -el \"//div[@id='main-content']\" -t 10000");
    
    expectedElement = parser.addHelp(parser.addStringOption("el", "expectedElement", true), "Expected element identified by XPath expression."); 
    
  }
  
  @Override
  public int run(String[] args) throws Exception {
    super.run(args);
    
    WebTestInput input = parseInput();

    if (input.isIgnoreServerCert()) {
      Logger.logInfo("SSL Certificate validation is ignored", this.loglevelValue);
    }
    
    String expectedElementValue = parser.getOptionValue(expectedElement);
    
    // validate the xpath 
    XPath xpath = XPathFactory.newInstance().newXPath();
    XPathExpression xpathExpr = null;
    try {
      xpathExpr = xpath.compile(expectedElementValue);
    } catch (XPathExpressionException e) {
      Logger.log("Error: Invalid XPath expression.", this.loglevelValue);
      //return ELEMENT_NOT_FOUND;
      return ErrorCodes.EXCEPTION;
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
        HtmlCleaner htmlCleaner = new HtmlCleaner();
        String content = EntityUtils.toString(entity, "UTF-8");
        //Logger.logDebug("Page content: " + content, this.loglevelValue);
        TagNode tagnode = htmlCleaner.clean(content);
        EntityUtils.consume(entity);
     
        CleanerProperties prop = new CleanerProperties();
        prop.setNamespacesAware(false);
        Document doc = new DomSerializer(prop).createDOM(tagnode);
        Logger.log("Evaluating XPath expression: " + expectedElementValue, this.loglevelValue);
        Node node = (Node) xpathExpr.evaluate(doc, XPathConstants.NODE);
        if (node != null) {
          Logger.log("Element given by XPath exists.", this.loglevelValue);
          return OK;
        } else {
          Logger.log("Cannot find element given by XPath.", this.loglevelValue);
          return ELEMENT_NOT_FOUND;
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
       if (httpclient != null) httpclient.getConnectionManager().shutdown();
    }
  }
}
