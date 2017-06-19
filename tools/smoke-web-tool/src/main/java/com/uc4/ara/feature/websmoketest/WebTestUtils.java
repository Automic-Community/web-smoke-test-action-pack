package com.uc4.ara.feature.websmoketest;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.jsoup.Jsoup;

public final class WebTestUtils {

	private static final String KERBEROS_SCHEMA = "Kerberos";
	private static final String DIGEST_SCHEMA = "digest";
	private static final String NTLM_SCHEMA = "ntlm";
	private static final String BASIC_SCHEMA = "basic";

	private WebTestUtils() {
	}

	/**
	 * Extracts title from html content
	 * 
	 * @param html
	 * @return
	 */
	public static String extractTitle(String html) {
		Pattern p = Pattern.compile("<head.*?>.*?<title.*?>(.*?)</title>.*?</head>",
				Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		Matcher m = p.matcher(html);
		while (m.find()) {
			return m.group(1).trim();
		}
		return null;
	}

	/**
	 * Strips html tags from html content
	 * 
	 * @param html
	 * @return
	 */
	public static String html2text(String html) {
		return Jsoup.parse(html).getElementsByTag("body").get(0).text();
	}

	/**
	 * Builds a new <code>HttpClient</code> object from input information
	 * 
	 * @param
	 * @return
	 * @throws MalformedURLException
	 */
	public static HttpClient getHttpClient(WebTestInput input) throws MalformedURLException {
		URL url = new URL(input.getUrl());
		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpParams params = httpclient.getParams();
		HttpConnectionParams.setSoTimeout(params, input.getTimeout());
		HttpClientParams.setRedirecting(params, true);

		if (input.isIgnoreServerCert())
			httpclient = wrapClient(httpclient);

		CredentialsProvider credentialsProvider = httpclient.getCredentialsProvider();
		HttpHost host = new HttpHost(url.getHost(), url.getPort(), url.getProtocol());
		if (!StringUtils.isEmpty(input.getUsername())) {
			if (input.getUsername().matches("[^\\\\]+\\\\[^\\\\]+")) {
				Credentials ntlm = new NTCredentials(
						input.getUsername().replaceAll("\\\\", "/") + ":" + input.getPassword());
				credentialsProvider
						.setCredentials(new AuthScope(host.getHostName(), host.getPort(), AuthScope.ANY_REALM), ntlm);
			} else {
				Credentials credentials = new UsernamePasswordCredentials(input.getUsername(), input.getPassword());
				credentialsProvider.setCredentials(
						new AuthScope(host.getHostName(), host.getPort(), AuthScope.ANY_REALM, BASIC_SCHEMA),
						credentials);
				credentialsProvider.setCredentials(
						new AuthScope(host.getHostName(), host.getPort(), AuthScope.ANY_REALM, NTLM_SCHEMA),
						credentials);
				credentialsProvider.setCredentials(
						new AuthScope(host.getHostName(), host.getPort(), AuthScope.ANY_REALM, DIGEST_SCHEMA),
						credentials);
				credentialsProvider.setCredentials(
						new AuthScope(host.getHostName(), host.getPort(), AuthScope.ANY_REALM, KERBEROS_SCHEMA),
						credentials);
			}
		}

		if (!StringUtils.isEmpty(input.getProxyHost())) {
			HttpHost proxy = new HttpHost(input.getProxyHost(), input.getProxyPort());
			httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
		}

		if (!StringUtils.isEmpty(input.getProxyUsername())) {
			credentialsProvider.setCredentials(
					new AuthScope(input.getProxyHost(), input.getProxyPort(), AuthScope.ANY_REALM),
					new UsernamePasswordCredentials(input.getProxyUsername(), input.getProxyPassword()));
		}

		return httpclient;
	}

	/**
	 * Gets a new HtmlUnit <code>WebClient</code> instance.
	 * 
	 * @param input
	 * @return
	 * @throws MalformedURLException
	 */
	/*
	 * public static WebClient getWebClient(WebTestInput input) throws
	 * MalformedURLException { URL uRL = new URL(input.getUrl()); WebClient
	 * webClient = new WebClient(BrowserVersion.getDefault());
	 * webClient.getOptions().setTimeout(input.getTimeout());
	 * webClient.getOptions().setRedirectEnabled(true); if
	 * (input.isIgnoreServerCert())
	 * webClient.getOptions().setUseInsecureSSL(true);
	 * 
	 * webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
	 * webClient.getOptions().setThrowExceptionOnScriptError(false);
	 * webClient.getOptions().setCssEnabled(false);
	 * webClient.getOptions().setAppletEnabled(false);
	 * //webClient.getOptions().setJavaScriptEnabled(false);
	 * 
	 * // disable cookie webClient.getCookieManager().setCookiesEnabled(false);
	 * java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level
	 * .OFF);
	 * 
	 * java.util.logging.Logger.getLogger(ResponseProcessCookies.class.
	 * getCanonicalName()).setLevel(Level.OFF);
	 * 
	 * // is it insecure? webClient.setRefreshHandler(new
	 * ThreadedRefreshHandler());
	 * 
	 * if (!StringUtils.isEmpty(input.getUsername())) {
	 * DefaultCredentialsProvider creds = new DefaultCredentialsProvider();
	 * creds.addCredentials(input.getUsername(), input.getPassword(),
	 * uRL.getHost(), uRL.getPort(), AuthScope.ANY_REALM);
	 * //creds.addNTLMCredentials(username, password, "portal.uc4.com", 80,
	 * "PC0609", "sbb01.spoc.global"); webClient.setCredentialsProvider(creds);
	 * }
	 * 
	 * if (!StringUtils.isEmpty(input.getProxyHost())) {
	 * webClient.getOptions().setProxyConfig(new
	 * ProxyConfig(input.getProxyHost(), input.getProxyPort())); }
	 * 
	 * if (!StringUtils.isEmpty(input.getProxyUsername())) {
	 * DefaultCredentialsProvider creds = new DefaultCredentialsProvider();
	 * creds.addCredentials(input.getProxyUsername(), input.getProxyPassword());
	 * webClient.setCredentialsProvider(creds); }
	 * 
	 * return webClient; }
	 */

	/**
	 * Wraps an <code>HttpClient</code> instance to allow it ignores the server
	 * certificate
	 * 
	 * @param base
	 *            the original <code>HttpClient</code> object
	 * @return <code>HttpClient</code> object
	 */
	public static DefaultHttpClient wrapClient(HttpClient base) {
		try {
			SSLContext ctx = SSLContext.getInstance("TLS");
			X509TrustManager tm = new X509TrustManager() {
				public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
				}

				public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
				}

				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
			};
			ctx.init(null, new TrustManager[] { tm }, null);
			SSLSocketFactory ssf = new SSLSocketFactory(ctx, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			ClientConnectionManager ccm = base.getConnectionManager();
			SchemeRegistry sr = ccm.getSchemeRegistry();
			sr.register(new Scheme("https", 443, ssf));
			return new DefaultHttpClient(ccm, base.getParams());
		} catch (Exception ex) {
			return null;
		}
	}
}
