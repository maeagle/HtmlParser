package com.maeagle.htmlparser.sunshine;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpVersion;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;

public class ProductParser {

	private static final String PRODUCT_LIST_URL = "http://product.sinosig.com/product_prdtList2014.action?queryProps=&categoryId=105&currentPageNum=#";

	private static final String PRODUCT_JS_URL = "http://product.sinosig.com/js/quote/#.js";

	private static final int pageSize = 4;

	private static final Pattern patProductInfo = Pattern
			.compile("http://product\\.sinosig\\.com/product/(\\d+)\\.html\\?spnid=(\\d+)");

	private static final Pattern patRateInfo = Pattern.compile("/xml/(.+)\\.xml");

	public Map<String, String> parseProductInfo() {

		Map<String, String> productInfo = new HashMap<String, String>();
		try {
			for (int i = 1; i <= pageSize; i++) {
				String contents = Request.Get(PRODUCT_LIST_URL.replace("#", Integer.toString(i))).execute()
						.returnContent().asString();
				Matcher productCodeMatcher = patProductInfo.matcher(contents);
				while (productCodeMatcher.find()) {
					String productCode = productCodeMatcher.group(1);
					if (productInfo.containsKey(productCode))
						continue;
					String productPlanId = productCodeMatcher.group(2);
					productInfo.put(productCode, productPlanId);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return productInfo;
	}

	public static void main(String[] args) {
		ProductParser parser = new ProductParser();
		Map<String, String> productInfo = parser.parseProductInfo();

		Iterator<Entry<String, String>> iter = productInfo.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<String, String> entry = iter.next();
			System.out.println(entry.getKey() + " : " + entry.getValue());
			parser.parseQuotePage(entry.getKey(), entry.getValue());
		}
	}

	public void parseQuotePage(String productCode, String planId) {

		try {

			String contents = new String(
					Request.Get(PRODUCT_JS_URL.replace("#", productCode)).execute().returnContent().asBytes(), "UTF-8");
			// String contents = new String(
			// Request.Post("http://product.sinosig.com/toulianxianInsurance_quote.action")
			// .version(
			// HttpVersion.HTTP_1_1)
			// .bodyForm(Form.form().add("productId", productCode).add("planId",
			// planId)
			// .add("processId",
			// "buy-process-286d6e6f-d49f-4875-be70-bc3cf453274c")
			// .add("orderSource", "product-center").add("platform", "pc")
			// .add("statisticProductType", "02").add("statisticProductId",
			// productCode).build())
			// .execute().returnContent().asBytes(), "UTF-8");
			Matcher productRateInfoMatcher = patRateInfo.matcher(contents);
			while (productRateInfoMatcher.find()) {
				String matchUrl = productRateInfoMatcher.group(1);
				System.out.println(matchUrl);
			}
			System.out.println("=====================================================");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
