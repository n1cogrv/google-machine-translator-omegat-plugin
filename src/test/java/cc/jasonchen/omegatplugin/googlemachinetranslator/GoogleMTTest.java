package cc.jasonchen.omegatplugin.googlemachinetranslator;

import org.junit.Assert;
import org.junit.Test;
import org.omegat.core.TestCore;
import org.omegat.util.Language;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * @Time 2022-08-13 9:08 PM
 * @Author MijazzChan
 */

public class GoogleMTTest extends TestCore {

    private static final String CHINESE_CHAR_REG = "\\P{sc=Han}";

    // Test for local development only, this test will fail in CI build process.
    //@Test
    public void translationTest() throws Exception {
        GoogleMT g = new GoogleMT();
        Path path = Paths.get(this.getClass().getResource("/test/data/Augustin-Louis Cauchy - Wikipedia.txt").toURI());
        Stream<String> lines = Files.lines(path);
        String unTransalatedText = lines.findAny().orElse("Hello World!");
        System.out.println("Untranslated: " + unTransalatedText);
        String translatedText = g.translate(new Language("en_US"), new Language("zh_CN"), unTransalatedText);
        System.out.println("Translated: " + translatedText);
        Assert.assertTrue(Pattern.compile(CHINESE_CHAR_REG).matcher(translatedText).find());
    }
}
