package me.chenjun.similarity.plugin;

import org.apache.lucene.index.LeafReaderContext;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.ScriptPlugin;
import org.elasticsearch.script.ScoreScript;
import org.elasticsearch.script.ScriptContext;
import org.elasticsearch.script.ScriptEngine;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author chenjun
 * @date 2019/10/30 13:42
 */
public class SimilarityNativeScriptPlugin extends Plugin implements ScriptPlugin {
    private static final String NAME = "hl_similarity";

    @Override
    public ScriptEngine getScriptEngine(Settings settings, Collection<ScriptContext<?>> contexts) {
        return new SimilarityScriptEngine();
    }

    private class SimilarityScriptEngine implements ScriptEngine {

        @Override
        public String getType() {
            return NAME;
        }

        @Override
        public <T> T compile(String scriptName, String scriptSource, ScriptContext<T> context, Map<String, String> params) {
            ScoreScript.Factory factory = (p, lookup) -> new ScoreScript.LeafFactory() {
                final String field = p.containsKey("field") ? (String) p.get("field") : null;
                final String text = p.containsKey("term") ? p.get("term").toString() : null;

                @Override
                public ScoreScript newInstance(LeafReaderContext context) {
                    return new ScoreScript(p, lookup, context) {
                        @Override
                        public double execute() {
                            Object v = lookup.source().get(field);
                            if ("hamming".equals(scriptSource)) {
                                final String test = v.toString();
                                return 100 - hammingDistance(new BigInteger(test), new BigInteger(text));
                            } else if ("levenshtein".equals(scriptSource)) {
                                if (v instanceof ArrayList) {
                                    List<String> list = (ArrayList<String>) v;
                                    if (list.size() == 0) {
                                        return 0D;
                                    }
                                    double total = 0D;
                                    for (String s : list) {
                                        total = Math.max(total, getSimilarity(text, s));
                                    }
                                    return total;
                                } else if (v instanceof String) {
                                    return getSimilarity(text, v.toString());
                                }
                                return 0D;
                            } else {
                                return 0D;
                            }
                        }
                    };
                }

                @Override
                public boolean needs_score() {
                    return false;
                }
            };
            return context.factoryClazz.cast(factory);
        }
    }

    private static int hammingDistance(BigInteger intSimHash1, BigInteger intSimHash2) {
        BigInteger x = intSimHash1.xor(intSimHash2);
        int tot = 0;
        while (x.signum() != 0) {
            tot += 1;
            x = x.and(x.subtract(new BigInteger("1")));
        }
        return tot;
    }

    private static int getDistance(String s1, String s2) {

        int m = (s1 == null) ? 0 : s1.length();
        int n = (s2 == null) ? 0 : s2.length();
        if (m == 0) {
            return n;
        }
        if (n == 0) {
            return m;
        }
        //左上方
        int tl;
        //上方
        int[] t = new int[n];
        //左方
        int[] l = new int[m];
        //初始化
        for (int i = 0; i < t.length; i++) {
            t[i] = i + 1;
        }
        for (int i = 0; i < l.length; i++) {
            l[i] = i + 1;
        }
        int d = 0;
        for (int i = 0; i < l.length; i++) {
            tl = i;
            char s1C = s1.charAt(i);
            for (int j = 0; j < t.length; j++) {
                d = Math.min(Math.min(l[i], t[j]) + 1, tl + ((s1C == s2.charAt(j)) ? 0 : 1));
                tl = t[j];
                l[i] = d;
                t[j] = d;
            }
        }
        return d;
    }

    private static double getSimilarity(String s1, String s2) {
        double d = getDistance(s1, s2);
        return (1d - (d / Math.max(s1.length(), s2.length()))) * 100;
    }
}
