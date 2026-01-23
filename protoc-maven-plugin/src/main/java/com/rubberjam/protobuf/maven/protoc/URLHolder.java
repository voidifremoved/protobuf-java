
package com.rubberjam.protobuf.maven.protoc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

public class URLHolder
{
	private URL url;
	
	private Proxy proxy;
	
	public URLHolder(String url) throws MalformedURLException
	{
		this.url = URI.create(url).toURL();
	}

	public URLHolder(URL url)
	{
		this.url = url;
	}

	public URLHolder(String url, String host, int port) throws MalformedURLException
	{
		this.url = URI.create(url).toURL();
		if (host != null) proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
	}

	public URLHolder(String url, Proxy proxy) throws MalformedURLException
	{
		this.url = URI.create(url).toURL();
		this.proxy = proxy;
	}

	public URLHolder(URL url, Proxy proxy)
	{
		this.url = url;
		this.proxy = proxy;
	}

	public URLConnection openConnection() throws IOException
	{
		if (proxy == null) return url.openConnection();
		return url.openConnection(proxy);
	}

	@Override
	public String toString()
	{
		if (proxy == null) return url.toString();
		return url.toString() + " [" + proxy + "]";
	}


}

