package com.by.cloud.api.imagesearch.sub;

import com.by.cloud.enums.ErrorCode;
import com.by.cloud.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 获取图片列表接口（API）
 *
 * @author lzh
 */
@Slf4j
public class GetImageFirstUrlApi {

    /**
     * 获取图片列表接口
     *
     * @param searchResultUrl 搜索结果列表
     * @return 图片列表URL
     */
    public static String getImageFirstUrl(String searchResultUrl) {
        try {
            // 使用 Jsoup 获取 HTML 内容
            Document document = Jsoup.connect(searchResultUrl)
                    .timeout(5000)
                    .get();

            // 获取所有 script 标签
            Elements elements = document.getElementsByTag("script");

            // 遍历所有标签找到 firstUrl 脚本内容
            for (Element element : elements) {
                String scriptContent = element.html();
                if (scriptContent.contains("\"firstUrl\"")) {
                    // 正则表达式提取 firstUrl 的值
                    Pattern pattern = Pattern.compile("\"firstUrl\"\\s*:\\s*\"(.*?)\"");
                    Matcher matcher = pattern.matcher(scriptContent);
                    if (matcher.find()) {
                        String firstUrl = matcher.group(1);
                        // 处理转义字符
                        firstUrl = firstUrl.replace("\\/", "/");
                        return firstUrl;
                    }
                }
            }
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未找到 firstUrl");
        } catch (Exception e) {
            log.error("调用获取图片列表接口失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "调用失败");
        }
    }

    public static void main(String[] args) {
        // 请求目标 URL
        String url = "https://graph.baidu.com/s?card_key=&entrance=GENERAL&extUiData[isLogoShow]=1&f=all&isLogoShow=1&session_id=11556632806729372880&sign=126afe97cd54acd88139901737251403&tpl_from=pc";
        String imageFirstUrl = getImageFirstUrl(url);
        System.out.println("调用成功，结果 URL：" + imageFirstUrl);
    }
}
