package com.maeagle.htmlparser.bdgj;

import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class BookOrderThread implements Runnable {

	private static String loginUrl = "http://59.108.39.19:81/api/wechatGh/login.do";

	// "http://59.108.39.19:81/api/wechatGh/outDoctorList.do?noType=2&office_id=0000395&officeName=妇科门诊"
	private static String listDocUrl = "http://59.108.39.19:81/api/wechatGh/outDoctorList.do?noType=2&office_id=0000418&officeName=产科门诊";

	private static String ghPageUrl = "http://59.108.39.19:81/api/wechatGh/actualGhPage.do?id=0";

	private static String confirmGhPage = "http://59.108.39.19:81/api/wechatGh/actualGh.do?id=4";

	private BasicCookieStore cookieStore = new BasicCookieStore();

	private AtomicBoolean successFlag;

	public BookOrderThread(AtomicBoolean successFlag) {
		this.successFlag = successFlag;
	}

	@Override
	public void run() {

		if (successFlag.get())
			return;

		CloseableHttpClient httpclient = HttpClients.custom().setDefaultCookieStore(cookieStore).build();
		CloseableHttpResponse response = null;

		// 登陆
		try {
			HttpUriRequest loginRequest = RequestBuilder.post().setUri(loginUrl).addParameter("mobile", "15652993327")
					.addParameter("password", "15901041041").build();
			response = httpclient.execute(loginRequest);
			HttpEntity entity = response.getEntity();
			EntityUtils.consume(entity);
			List<Cookie> cookies = cookieStore.getCookies();
			if (cookies.isEmpty()) {
				throw new NullPointerException();
			} else {
				// System.out.println("Login Cookies : ");
				// for (int i = 0; i < cookies.size(); i++)
				// System.out.println("- " + cookies.get(i).toString());
				// System.out.println("＝＝＝＝＝＝＝＝＝登陆成功！");
			}
		} catch (Exception e) {
			System.out.println("＝＝＝＝＝＝＝＝＝登陆失败！");
			e.printStackTrace();
		} finally {
			try {
				response.close();
			} catch (Exception e) {
			}
			response = null;
		}

		// 读取出诊医生列表
		try {
			HttpUriRequest listPage = RequestBuilder.get().setUri(listDocUrl).build();
			response = httpclient.execute(listPage);
			// HttpEntity entity = response.getEntity();
			// System.out.println(EntityUtils.toString(entity));
			// System.out.println("＝＝＝＝＝＝＝＝＝读取出诊医生列表成功！");
		} catch (Exception e) {
			System.out.println("＝＝＝＝＝＝＝＝＝读取出诊医生列表失败！");
			e.printStackTrace();
		} finally {
			try {
				response.close();
			} catch (Exception e) {
			}
			response = null;
		}

		// 进入预约挂号页面
		try {
			HttpUriRequest page2 = RequestBuilder.get().setUri(new URI(ghPageUrl)).build();
			response = httpclient.execute(page2);
			// HttpEntity entity = response.getEntity();
			// System.out.println(EntityUtils.toString(entity));
			// System.out.println("＝＝＝＝＝＝＝＝＝进入挂号页面成功！");
		} catch (Exception e) {
			System.out.println("＝＝＝＝＝＝＝＝＝进入挂号页面失败！");
			e.printStackTrace();
		} finally {
			try {
				response.close();
			} catch (Exception e) {
			}
			response = null;
		}

		// 进行实际预约挂号
		try {
			HttpUriRequest page3 = RequestBuilder.get().setUri(new URI(confirmGhPage)).build();
			response = httpclient.execute(page3);
			HttpEntity entity = response.getEntity();
			String result = EntityUtils.toString(entity);
			if (result.indexOf("预约成功") > -1) {
				successFlag.set(true);
				System.out.println(confirmGhPage + " : \n" + result);
				System.out.println("＝＝＝＝＝＝＝＝＝实际预约挂号成功！");
			} else {
				// System.out.println("＝＝＝＝＝＝＝＝＝实际预约挂号失败！");
			}
		} catch (Exception e) {
			System.out.println("＝＝＝＝＝＝＝＝＝实际预约挂号失败！");
			e.printStackTrace();
		} finally {
			try {
				response.close();
			} catch (Exception e) {
			}
			response = null;
		}

		// 关闭httpclient
		try {
			httpclient.close();
		} catch (

		Exception e)

		{
			e.printStackTrace();
		}

	}

}
