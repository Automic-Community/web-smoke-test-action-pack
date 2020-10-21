package com.uc4.ara.feature.websmoketest;

import org.apache.commons.lang3.StringUtils;

import com.uc4.ara.feature.AbstractPublicFeature;
import com.uc4.ara.feature.utils.CmdLineParser;
import com.uc4.ara.util.Logger;

/**
 *
 */
public abstract class AbstractWebSmokeTest extends AbstractPublicFeature {
  
  /**
   * Return codes
   */
  public static final int OK = 0;
  public static final int TIMEOUT = 1;
  public static final int SERVER_NOT_AVAILABLE = 2;
  public static final int WRONG_CREDENTIALS = 3;
  public static final int PROXY_SERVER_NOT_AVAILABLE = 4;
  public static final int WRONG_PROXY_CREDENTIALS = 5;
  public static final int SERVER_CERTIFICATE_FAILED = 6;
  
  public static final int TITLE_NOT_MATCH = 7;
  public static final int TEXT_NOT_FOUND = 7;
  public static final int ELEMENT_NOT_FOUND = 7;
  public static final int TEXT_NOT_FOUND_AFTER_SUBMIT = 10;
  
  /**
   * Common input parameters
   */
  protected CmdLineParser.Option<String> url;
  protected CmdLineParser.Option<String> username;
  protected CmdLineParser.Option<String> password;
  protected CmdLineParser.Option<String> timeout;
  protected CmdLineParser.Option<String> ignoreServerCert;
  protected CmdLineParser.Option<String> proxyHost;
  protected CmdLineParser.Option<String> proxyPort;
  protected CmdLineParser.Option<String> proxyUsername;
  protected CmdLineParser.Option<String> proxyPassword;
  
  @Override
  public void initialize() {
    super.initialize();
    
    url = parser.addHelp(parser.addStringOption("url", "url", true),
        "The url of the page to test (i.e. http://www.google.com). Both http and https can be used. Default port: 80."); 
    
    username = parser.addHelp(parser.addStringOption("u", "username", false),
        "Optional username if the web server requires basic authentication.");    
    
    password = parser.addHelp(parser.addPasswordOption("p", "password", false), 
        "Optional password if the web server requires basic authentication.");
        
    timeout = parser.addHelp(parser.addStringOption("t", "timeout", false),
        "Optional password if the web server requires basic authentication.");
    
    ignoreServerCert = parser.addHelp(parser.addStringOption("ic", "ignoreServerCert", false), 
        "If \"yes\" the client ignores the server's certificate if otherwise the server certificate wouldn't be accepted.");
            
    proxyHost = parser.addHelp(parser.addStringOption("ph", "proxyHost", false), "Host name or IP of the proxy host.");  
    proxyPort = parser.addHelp(parser.addStringOption("pp", "proxyPort", false), "Proxy port to use.");  
    proxyUsername = parser.addHelp(parser.addStringOption("pu", "proxyUsername", false), "Username used to authenticate with proxy server.");  
    proxyPassword = parser.addHelp(parser.addPasswordOption("ppwd", "proxyPassword", false), "Password used to authenticate with proxy server.");  
  }
    
  /**
   * Utility method parses the common input parameters from command line
   * @return <code>WebTestInput</code> object
   * @throws Exception
   */
  public WebTestInput parseInput() throws Exception {
    String urlValue = parser.getOptionValue(url);
    String usernameValue = parser.getOptionValue(username);
    String passwordValue = parser.getOptionValue(password);
    
    int timeoutValue = 10000;
    boolean ignoreServerCertValue = false;
    int proxyPortValue = 80;
    try {
      timeoutValue = Integer.parseInt(parser.getOptionValue(timeout));
    } catch (NumberFormatException e) {
      Logger.log("Cannot parse the timeout value, use the default value 10000", this.loglevelValue);
    } // swallow so those values remain as default value
    
    String ignoreServerCertStr = parser.getOptionValue(ignoreServerCert);
    if ("YES".equalsIgnoreCase(ignoreServerCertStr)) {
      ignoreServerCertValue = true;
    }
    
    String proxyHostValue = parser.getOptionValue(proxyHost);
    
    try { 
      proxyPortValue = Integer.parseInt(parser.getOptionValue(proxyPort)); 
    } catch (NumberFormatException e) {
      if (!StringUtils.isEmpty(proxyHostValue)) {
        Logger.log("Cannot parse the proxy port value, use the default value 80", this.loglevelValue);
      }
    } 

    String proxyUsernameValue = parser.getOptionValue(proxyUsername);
    String proxyPasswordValue = parser.getOptionValue(proxyPassword);
    
    // input params
    Logger.logDebug("Input parameters\nURL: " + urlValue, this.loglevelValue);
    Logger.logDebug("Username: " + usernameValue, this.loglevelValue);
    Logger.logDebug("Timeout: " + timeoutValue, this.loglevelValue);
    Logger.logDebug("Ignore server certificate: " + ignoreServerCertValue, this.loglevelValue);
    Logger.logDebug("Proxy host: " + proxyHostValue + ", proxy port: " + proxyPortValue, this.loglevelValue);
    Logger.logDebug("Proxy username: " + proxyUsernameValue, this.loglevelValue);
        
    return new WebTestInput(urlValue, usernameValue, passwordValue, timeoutValue, ignoreServerCertValue, proxyHostValue, proxyPortValue, proxyUsernameValue, proxyPasswordValue);
  }
 
}
