package com.douyinlive.http;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 签名服务的「接入方式」。
 *
 * <p>同一套 sign / status 接口契约，不同接入方式只是 baseUrl、路径前缀与鉴权头不同：
 * <ul>
 *   <li>{@link #rapidApi(String)} —— 通过 RapidAPI 网关订阅（当前推荐的付费入口）；</li>
 *   <li>{@link #apiKey(String, String)} —— 独立站直连 + apiKey（后续作者自建收款后启用）；</li>
 *   <li>{@link #selfHosted(String)} —— 自建 / 本地无鉴权。</li>
 * </ul>
 *
 * <p>RapidAPI 只是挡在签名服务器前面的反向代理 + 计费网关，响应体与直连一致，
 * 仅 baseUrl、路径与鉴权头不同（注意 RapidAPI 的 status 路径是 {@code /sign/status}）。
 * 因此从 RapidAPI 迁移到独立站只需更换本对象的构造方式，业务调用代码无需改动。
 *
 * <p>{@link #fromConfig(Function)} 按配置 key 自动推断接入方式，用户改 {@code .env} 即可切换。
 */
public final class SignProvider {

    // ===== RapidAPI 网关（douyin-sign-api, tikhub-team）真实参数 =====
    /** RapidAPI 网关地址。 */
    public static final String RAPIDAPI_BASE_URL = "https://douyin-sign-api.p.rapidapi.com";
    /** RapidAPI Host 头的值（与 baseUrl 域名一致）。 */
    public static final String RAPIDAPI_HOST = "douyin-sign-api.p.rapidapi.com";
    /** RapidAPI 订阅页（鉴权失败时引导用户去订阅）。 */
    public static final String RAPIDAPI_LISTING_URL =
            "https://rapidapi.com/tikhub-team-tikhub-team-default/api/douyin-sign-api/playground/apiendpoint_4a3adc62-7f1a-4bed-9df2-cf14b34e2837";
    /** RapidAPI 签名端点路径。 */
    public static final String RAPIDAPI_SIGN_PATH = "/api/v1/douyin/sign";
    /** RapidAPI 状态查询端点路径（注意是 /sign/status）。 */
    public static final String RAPIDAPI_STATUS_PATH = "/api/v1/douyin/sign/status";

    /** 自建 / 独立站直连的默认端点路径。 */
    public static final String DEFAULT_SIGN_PATH = "/sign";
    public static final String DEFAULT_STATUS_PATH = "/status";

    /** 自建模式默认地址。 */
    public static final String DEFAULT_BASE_URL = "http://localhost:18080";

    private final String baseUrl;
    private final String signPath;
    private final String statusPath;
    private final Map<String, String> authHeaders;
    /** 鉴权失败（401/403）时附加的引导文案，可为 null。 */
    private final String authErrorHint;

    private SignProvider(String baseUrl, String signPath, String statusPath,
                         Map<String, String> authHeaders, String authErrorHint) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl 不能为空");
        }
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.signPath = signPath;
        this.statusPath = statusPath;
        this.authHeaders = Collections.unmodifiableMap(new LinkedHashMap<>(authHeaders));
        this.authErrorHint = authErrorHint;
    }

    /**
     * 通过 RapidAPI 网关接入（当前推荐的付费入口）。
     *
     * @param rapidApiKey RapidAPI 账户的 X-RapidAPI-Key，在 {@link #RAPIDAPI_LISTING_URL} 订阅后获取
     */
    public static SignProvider rapidApi(String rapidApiKey) {
        if (rapidApiKey == null || rapidApiKey.isBlank()) {
            throw new IllegalArgumentException(
                    "缺少 RapidAPI Key。请先到 " + RAPIDAPI_LISTING_URL + " 订阅并复制 X-RapidAPI-Key。");
        }
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("x-rapidapi-key", rapidApiKey);
        headers.put("x-rapidapi-host", RAPIDAPI_HOST);
        return new SignProvider(RAPIDAPI_BASE_URL, RAPIDAPI_SIGN_PATH, RAPIDAPI_STATUS_PATH, headers,
                "RapidAPI 鉴权失败：请确认已在 " + RAPIDAPI_LISTING_URL + " 订阅且套餐额度未用尽。");
    }

    /**
     * 独立站直连 + apiKey（后续作者开通收款后启用）。
     *
     * @param baseUrl 独立签名服务地址，例如 https://api.yourdomain.com
     * @param apiKey  在独立站后台创建的 API Key
     */
    public static SignProvider apiKey(String baseUrl, String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("缺少 apiKey，请在独立站后台创建后传入。");
        }
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + apiKey);
        return new SignProvider(baseUrl, DEFAULT_SIGN_PATH, DEFAULT_STATUS_PATH, headers,
                "apiKey 鉴权失败：请确认 Key 有效且订阅未过期。");
    }

    /** 自建 / 本地无鉴权（如本地 http://localhost:18080）。 */
    public static SignProvider selfHosted(String baseUrl) {
        return new SignProvider(baseUrl, DEFAULT_SIGN_PATH, DEFAULT_STATUS_PATH,
                Collections.emptyMap(), null);
    }

    /**
     * 按配置 key 自动推断接入方式（不依赖任何配置框架）。优先级：
     * <ol>
     *   <li>{@code RAPIDAPI_KEY} 非空 → {@link #rapidApi(String)}</li>
     *   <li>否则 {@code SIGN_API_KEY} 非空 → {@link #apiKey(String, String)}（baseUrl 取 {@code SIGN_URL}）</li>
     *   <li>否则 → {@link #selfHosted(String)}（baseUrl 取 {@code SIGN_URL}，缺省 {@link #DEFAULT_BASE_URL}）</li>
     * </ol>
     *
     * @param get 配置查找函数，如 dotenv 的 {@code env::get}、{@code System::getenv} 或 {@code map::get}
     */
    public static SignProvider fromConfig(Function<String, String> get) {
        String rapidKey = get.apply("RAPIDAPI_KEY");
        if (rapidKey != null && !rapidKey.isBlank()) {
            return rapidApi(rapidKey);
        }
        String baseUrl = get.apply("SIGN_URL");
        String siteApiKey = get.apply("SIGN_API_KEY");
        if (siteApiKey != null && !siteApiKey.isBlank()) {
            return apiKey((baseUrl == null || baseUrl.isBlank()) ? DEFAULT_BASE_URL : baseUrl, siteApiKey);
        }
        return selfHosted((baseUrl == null || baseUrl.isBlank()) ? DEFAULT_BASE_URL : baseUrl);
    }

    String baseUrl() {
        return baseUrl;
    }

    String signPath() {
        return signPath;
    }

    String statusPath() {
        return statusPath;
    }

    Map<String, String> authHeaders() {
        return authHeaders;
    }

    String authErrorHint() {
        return authErrorHint;
    }
}
