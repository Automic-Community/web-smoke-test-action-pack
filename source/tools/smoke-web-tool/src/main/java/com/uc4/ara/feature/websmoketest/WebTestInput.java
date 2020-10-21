package com.uc4.ara.feature.websmoketest;

public class WebTestInput {
	private String url;
	private String username;
	private String password;
	private int timeout = 10000;
	private boolean ignoreServerCert = false;
	private String proxyHost;
	private int proxyPort;
	private String proxyUsername;
	private String proxyPassword;

	public WebTestInput(String url) {
		this.url = url;
	}

	public WebTestInput(String url, int timeout) {
		this.url = url;
		this.timeout = timeout;
	}

	public WebTestInput(String url, String username, String password) {
		this.url = url;
		this.username = username;
		this.password = password;
	}

	public WebTestInput(String url, String username, String password, int timeout, boolean ignoreServerCert,
			String proxyHost, int proxyPort, String proxyUsername, String proxyPassword) {
		super();
		this.url = url;
		this.username = username;
		this.password = password;
		this.timeout = timeout;
		this.ignoreServerCert = ignoreServerCert;
		this.proxyHost = proxyHost;
		this.proxyPort = proxyPort;
		this.proxyUsername = proxyUsername;
		this.proxyPassword = proxyPassword;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public boolean isIgnoreServerCert() {
		return ignoreServerCert;
	}

	public void setIgnoreServerCert(boolean ignoreServerCert) {
		this.ignoreServerCert = ignoreServerCert;
	}

	public String getProxyHost() {
		return proxyHost;
	}

	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	public int getProxyPort() {
		return proxyPort;
	}

	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}

	public String getProxyUsername() {
		return proxyUsername;
	}

	public void setProxyUsername(String proxyUsername) {
		this.proxyUsername = proxyUsername;
	}

	public String getProxyPassword() {
		return proxyPassword;
	}

	public void setProxyPassword(String proxyPassword) {
		this.proxyPassword = proxyPassword;
	}

}
