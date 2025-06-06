package com.awesomecopilot.oauth2;

import com.awesomecopilot.json.jsonpath.JsonPathUtils;
import com.awesomecopilot.networking.utils.HttpUtils;
import org.junit.Test;

public class OAuth2Test {

	@Test
	public void testGetAccessTokenByCode() {
		String json = HttpUtils.form("http://localhost:9001/oauth2/token")
				.formData("grant_type", "authorization_code")
				.formData("code", "weL60eZKKoWs42Z4DcTdq6Dc7yMS-K6U88ZSb0cAritTuzZvybR2raSNVBO7DOeEcfwedUc3EOm8MotUjQCuGAL04d2KVJ1kJLK9Ro7TGDTwYFgrj-Da2WvmYfUCBc6g")
				.formData("redirect_uri", "http://www.baidu.com")
				.basicAuth("oidc-client", "123456")
				.request();

		String accessToken = JsonPathUtils.readNode(json, "$.access_token");
		System.out.println(accessToken);
	}
}
