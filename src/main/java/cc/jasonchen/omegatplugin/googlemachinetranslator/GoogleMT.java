package cc.jasonchen.omegatplugin.googlemachinetranslator;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.omegat.core.CoreEvents;
import org.omegat.core.events.IApplicationEventListener;
import org.omegat.core.machinetranslators.BaseTranslate;
import org.omegat.core.machinetranslators.MachineTranslators;
import org.omegat.util.Language;
import org.omegat.util.PatternConsts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * @Time 2022-08-13 8:07 PM
 * @updated by MijazzChan
 */
public class GoogleMT extends BaseTranslate {

    protected static final String[] GT_URLS = {
            "https://translate.googleapis.com/translate_a/single?client=gtx",
            "https://translate.google.com/translate_a/t?client=dict-chrome-ex"
    };

    protected static final String MARK_BEG = "{\"trans\":\"";
    protected static final String MARK_END = "\",\"orig\":\"";
    protected static final Pattern RE_UNICODE = Pattern.compile("\\\\u([0-9A-Fa-f]{4})");
    protected static final Pattern RE_HTML = Pattern.compile("&#([0-9]+);");
    protected static final ResourceBundle res = ResourceBundle.getBundle("GoogleMT", Locale.getDefault());
    private static final Logger logger = LoggerFactory.getLogger(GoogleMT.class);
    /**
     * User Agents have updated from
     * [User Agents Database](https://developers.whatismybrowser.com/useragents/explore/)
     */
    private static final String[] USER_AGENTS = {
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.5005.63 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.127 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/101.0.4951.67 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/101.0.4951.54 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.5060.134 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.198 Safari/E7FBAF",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/E7FBAF",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.5060.114 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.51 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.5060.114 Safari/537.36 Edg/103.0.1264.62",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.5060.134 Safari/537.36 Edg/103.0.1264.71",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.5060.134 Safari/537.36 Edg/103.0.1264.77",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/101.0.4951.64 Safari/537.36 Edg/101.0.1210.53",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.127 Safari/537.36 Edg/100.0.1185.44",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:102.0) Gecko/20100101 Firefox/102.0",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:103.0) Gecko/20100101 Firefox/103.0",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:98.0) Gecko/20100101 Firefox/98.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:102.0) Gecko/20100101 Firefox/102.0"
    };
    private final static int USER_AGENTS_LENGTH = USER_AGENTS.length;
    private final Map<String, String> params = new HashMap<String, String>();
    private final Random rnd = new Random();
    private final Map<String, String> headers = new HashMap<>();
    private final Set<String> failures = new HashSet<>();

    {
        params.put("dt", "t");
        params.put("dj", "1");
    }

    {
        headers.put("Accept", "text/html,application/xhtml+xm…ml;q=0.9,image/webp,*/*;q=0.8");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Connection", "keep-alive");
        headers.put("Cookie", "BL_D_PROV=; BL_T_PROV=Google");
        headers.put("Host", "translate.googleapis.com");
        headers.put("Referer", "https://translate.google.com/");
        headers.put("TE", "Trailers");
        headers.put("Upgrade-Insecure-Requests", "1");
    }

    // Plugin setup
    public static void loadPlugins() {
        CoreEvents.registerApplicationEventListener(new IApplicationEventListener() {
            @Override
            public void onApplicationStartup() {
                MachineTranslators.add(new GoogleMT());
            }

            @Override
            public void onApplicationShutdown() {
                /* empty */
            }
        });
    }

    public static void unloadPlugins() {
        /* empty */
    }

    public static byte[] getURLasByteArray(String address, Map<String, String> params,
                                           Map<String, String> additionalHeaders) throws IOException {
        StringBuilder s = new StringBuilder(address);
        boolean next = false;
        if (!address.contains("?")) {
            s.append('?');
        } else {
            next = true;
        }

        for (Map.Entry<String, String> p : params.entrySet()) {
            if (next) {
                s.append('&');
            } else {
                next = true;
            }
            s.append(p.getKey());
            s.append('=');
            s.append(URLEncoder.encode(p.getValue(), StandardCharsets.UTF_8.name()));
        }
        String url = s.toString();

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        try {
            conn.setRequestMethod("GET");
            if (additionalHeaders != null) {
                for (Map.Entry<String, String> en : additionalHeaders.entrySet()) {
                    conn.setRequestProperty(en.getKey(), en.getValue());
                }
            }
            return IOUtils.toByteArray(conn.getInputStream());
        } finally {
            conn.disconnect();
        }
    }

    public String getName() {
        return res.getString("PluginName");
    }

    @Override
    protected String getPreferenceName() {
        return "Google Machine Translator OmegaT Plugin";
    }

    @Override
    protected String translate(Language sLang, Language tLang, String text) throws Exception {
        String trText = text.length() > 5000 ? text.substring(0, 4997) + "..." : text;
        String prev = getFromCache(sLang, tLang, trText);
        if (prev != null) {
            return prev;
        }

        logger.debug("trText={}", trText);
        String targetLang = tLang.getLanguageCode();
        // Differentiate in target between simplified and traditional Chinese
        if ((tLang.getLanguage().compareToIgnoreCase("zh-cn") == 0)
                || (tLang.getLanguage().compareToIgnoreCase("zh-tw") == 0)) {
            targetLang = tLang.getLanguage();
        } else if ((tLang.getLanguage().compareToIgnoreCase("zh-hk") == 0)) {
            targetLang = "ZH-TW"; // Google doesn't recognize ZH-HK
        }

        params.put("sl", sLang.getLanguageCode());
        params.put("tl", targetLang);
        params.put("q", trText);

        headers.put("User-Agent", USER_AGENTS[rnd.nextInt(USER_AGENTS_LENGTH)]);

        byte[] answer = null;
        List<String> urls = new ArrayList<>(Arrays.asList(GT_URLS));
        while (failures.size() != GT_URLS.length && answer == null) {
            String url = urls.get(rnd.nextInt(urls.size()));
            try {
                logger.debug("url={}", url);
                answer = getURLasByteArray(url, params, headers);
            } catch (IOException e) {
                logger.info("Exception {} with url={}", e.getMessage(), url);
                failures.add(url);
                urls.remove(url);
            }
        }
        if (failures.size() == GT_URLS.length) {
            return null;
        }
        GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(answer));
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(gis, StandardCharsets.UTF_8));
        StringBuilder outStr = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            outStr.append(line);
        }
        String v = outStr.toString();
        logger.debug("outStr={}", v);
        while (true) {
            Matcher m = RE_UNICODE.matcher(v);
            if (!m.find()) {
                break;
            }
            String g = m.group();
            char c = (char) Integer.parseInt(m.group(1), 16);
            v = v.replace(g, Character.toString(c));
        }
        v = v.replace("\\n", "\n");
        v = v.replace("\\\"", "\"");
        while (true) {
            Matcher m = RE_HTML.matcher(v);
            if (!m.find()) {
                break;
            }
            String g = m.group();
            char c = (char) Integer.parseInt(m.group(1));
            v = v.replace(g, Character.toString(c));
        }

        List<String> items = new ArrayList<>();
        int beg = -1, end = -1;
        do {
            beg = StringUtils.indexOf(v, MARK_BEG, end) + MARK_BEG.length();
            end = StringUtils.indexOf(v, MARK_END, beg);
            logger.debug("beg={}, end={}", beg, end);
            String tr = v.substring(beg, end);
            items.add(tr);
        } while (beg != StringUtils.lastIndexOf(v, MARK_BEG) + MARK_BEG.length());

        List<String> resultItems = new ArrayList<>();
        for (String tr : items) {
            String newTr = tr;
            Matcher tag = PatternConsts.OMEGAT_TAG_SPACE.matcher(newTr);
            while (tag.find()) {
                String searchTag = tag.group();
                if (text.indexOf(searchTag) == -1) {
                    String replacement = searchTag.substring(0, searchTag.length() - 1);
                    newTr = newTr.replace(searchTag, replacement);
                }
            }

            tag = PatternConsts.SPACE_OMEGAT_TAG.matcher(newTr);
            while (tag.find()) {
                String searchTag = tag.group();
                if (text.indexOf(searchTag) == -1) {
                    String replacement = searchTag.substring(1);
                    newTr = newTr.replace(searchTag, replacement);
                }
            }
            resultItems.add(newTr);
        }
        String result = StringUtils.join(resultItems, "");
        putToCache(sLang, tLang, trText, result);
        return result;
    }
}
