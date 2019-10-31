没写测试，以es6.8.3为基础实现的 支持 hamming levenshtein ，有小问题请见谅

安装 elasticsearch-plugin install file://${以/开头的绝对路径},注：file:后是三个/
查询：
````
POST http://localhost:9200/{index}/_search
{
  "query": {
    "function_score": {
      "query": {
        "match_all": {}
      },
      "functions": [
        {
          "script_score": {
            "script": {
                "source": "hamming",
                "lang" : "hl_similarity",
                "params": {
                    "field": "simhash",
                    "term": "14983211072432861123"
                }
            }
          }
        }
      ]
    }
  }
}
````
````
    Map<String, Object> params = new HashMap<>();
    params.put("field", "simhash");
    params.put("term", "14983211072432861123");
    Script sc = new Script(ScriptType.INLINE, "hl_similarity", "hamming", params);
    QueryBuilder queryCondition = QueryBuilders.functionScoreQuery(ScoreFunctionBuilders.scriptFunction(sc));
    SearchQuery query = new NativeSearchQuery(queryCondition);
    return Mono.from(elasticsearchTemplate.execute(client -> {
                SearchRequest request = new SearchRequest("article");
                request.types("article");
                SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
                searchSourceBuilder.query(queryCondition);
                searchSourceBuilder.version(false);
                searchSourceBuilder.trackScores(false);
                searchSourceBuilder.minScore(95f);
                request.source(searchSourceBuilder);
                return client.search(request);
            }));
````
