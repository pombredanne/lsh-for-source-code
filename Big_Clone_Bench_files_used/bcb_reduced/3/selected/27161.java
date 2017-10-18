package t2haiku.page;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import org.t2framework.commons.util.StringUtil;
import org.t2framework.t2.annotation.core.Amf;
import org.t2framework.t2.annotation.core.Page;
import org.t2framework.t2.format.amf.navigation.AmfResponse;
import org.t2framework.t2.spi.Navigation;
import t2haiku.dto.HaikuComment;
import t2haiku.dto.HaikuData;
import t2haiku.util.NGWordCipher;

@Page("/haiku")
public class HaikuPage {

    private static final Map<Integer, String> commentMap = new HashMap<Integer, String>() {

        private static final long serialVersionUID = 1L;

        {
            put(0, "「{0}」　がけっこういいね おしゃれだね");
            put(1, "「{0}」を 使うあなたは 自由人");
            put(2, "ぶっちゃけて 「{0}」は ありえな");
            put(3, "わりとあり わりとありかと やっぱなし");
            put(4, "この俳句 庶民の君には 似合わない");
            put(5, "センスある！ そんな台詞を いうとでも？");
            put(6, "私なら 「ガッツ{0}」 に しますけど");
            put(7, "そうきたか まさかの「{0}」に おどろいた");
            put(8, "俳句より C++の方が むいてるよ");
            put(9, "この俳句 レベルが米村さん並だ");
            put(10, "もしかして 小林一茶の お孫さん？");
            put(11, "最近は 流行ってないよ 季語なしは");
            put(12, "すばらしい これをパクって 出世しよ");
            put(13, "よねむらの　本名　実は 「{0}」ですよ");
            put(14, "俳句とか　してる暇ない　コード書け！");
            put(15, "プログラム jadってからが 本物だ！");
            put(16, "俳句はね　世界を平和に　してくれる");
            put(17, "いいねそれ　今度ぱくって　いいですか？");
            put(18, "いまいちね 「{0}」には季語が　ありませぬ");
            put(19, "片山の　俳句はいつも 「{0}」三昧");
        }
    };

    private static final String[] NGWORD = { "BGX4kqDzVGxO7VQSxBWhxQ==", "BGX4kqDzVGw1mTZeIKcpKQ==", "BGX4kqDzVGw/JPWFhm1KZA==", "3K6RQV/iuZNO7VQSxBWhxQ==", "rfqI80TZMUmNbvYRQMMxLg==", "oW+E8dZ7Rj2oK9owEngBFA==", "3a24Qeypf5E=" };

    static {
        for (int i = 0; i < NGWORD.length; i++) {
            NGWORD[i] = NGWordCipher.decrypt(NGWORD[i]);
        }
    }

    @Amf
    public Navigation checkHaiku(HaikuData haikuData) throws NoSuchAlgorithmException {
        HaikuComment comment = new HaikuComment();
        if (StringUtil.isEmpty(haikuData.getGo1()) || StringUtil.isEmpty(haikuData.getShichi()) || StringUtil.isEmpty(haikuData.getGo2())) {
            comment.setPoint(0);
            comment.setComment("とりあえず　五七五には　しとこうか");
            return AmfResponse.to(comment);
        }
        String haikuText = haikuData.getGo1() + haikuData.getShichi() + haikuData.getGo2();
        if (this.isNGWord(haikuText)) {
            comment.setPoint(0);
            comment.setComment("下ネタを　言ったあなたは　負け組よ！");
            return AmfResponse.to(comment);
        }
        MessageDigest md5 = MessageDigest.getInstance("SHA-1");
        byte[] digestByte = md5.digest(haikuText.getBytes());
        int point = 0;
        for (int i = 0; i < digestByte.length; i++) {
            point += digestByte[i];
        }
        point = Math.abs(point) % 100;
        comment.setComment(commentMap.get(point % commentMap.size()).replace("{0}", haikuData.getGo2()));
        comment.setPoint(point);
        return AmfResponse.to(comment);
    }

    private boolean isNGWord(final String str) {
        for (int i = 0, len = NGWORD.length; i < len; i++) {
            if (0 <= str.toLowerCase().indexOf(NGWORD[i].toLowerCase())) return true;
        }
        return false;
    }
}
