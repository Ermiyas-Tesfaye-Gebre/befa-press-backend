package com.befapress.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Telebirr payment integration.
 * All sensitive values are loaded from environment variables.
 */
@Configuration
public class TelebirrConfig {

    @Value("${telebirr.fabric-app-id:1572189077811204}")
    private String fabricAppId;

    @Value("${telebirr.app-secret:914978}")
    private String appSecret;

    @Value("${telebirr.merchant-app-id:fad0f06383c6297f545876694b974599}")
    private String merchantAppId;

    @Value("${telebirr.merchant-code:c4182ef8-9249-458a-985e-06d191f4d505}")
    private String merchantCode;

    @Value("${telebirr.private-key:}")
    private String privateKey;

    @Value("${telebirr.base-url:https://developerportal.ethiotelebirr.et:38443/apiaccess/payment/gateway}")
    private String baseUrl;

    @Value("${telebirr.web-checkout-url:https://developerportal.ethiotelebirr.et:38443/payment/web/paygate?}")
    private String webCheckoutUrl;

    @Value("${telebirr.notify-url:}")
    private String notifyUrl;

    @Value("${telebirr.redirect-url:}")
    private String redirectUrl;

    public String getFabricAppId() {
        return fabricAppId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public String getMerchantAppId() {
        return merchantAppId;
    }

    public String getMerchantCode() {
        return merchantCode;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getWebCheckoutUrl() {
        return webCheckoutUrl;
    }

    public String getNotifyUrl() {
        return notifyUrl;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }
}
