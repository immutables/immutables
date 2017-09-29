package org.immutables.mongo.repository.internal;

import com.google.gson.JsonElement;
import com.google.gson.internal.bind.TypeAdapters;
import org.bson.json.JsonReader;
import org.junit.Test;

import java.io.IOException;

import static org.immutables.check.Checkers.check;

public class BsonReaderTest {

    @Test
    public void array() throws Exception {
        compare("[]");
        compare("[[]]");
        compare("[[[]]]");
        compare("[[], []]");
        compare("[[], [[]]]");
        compare("[[], [[]], []]");
        compare("[1]");
        compare("[1, 2]");
        compare("[1, 2, 3]");
        compare("[true]");
        compare("[true, true]");
        compare("[true, true, false]");
        compare("[0.11, 11.22, 3]");
        compare("[\"foo\"]");
        compare("[\"\"]");
        compare("[\"\", \"\"]");
        compare("[\"\", \"foo\"]");
        compare("[\"foo\", \"bar\"]");
        compare("[1, true, 0, 1.111]");
        compare("[null]");
        compare("[null, 1, false]");
        compare("[0.0, -1.2, 3]");
        compare("[[0], [1]]");
        compare("[[0], [], 1]");
        compare("[true, [], []]");
        compare("[{}]");
        compare("[{}, {}]");
        compare("[{}, {}, {}]");
        compare("[{\"a\": 1}, {\"b\": null}, {\"c\": false}]");
        compare("[{\"0\": 1}, [], {\"1\": null}, {}]");
    }

    @Test
    public void scalar() throws Exception {
        compare("0");
        compare("0.0");
        compare("-1");
        compare("-200");
        compare(Long.toString(Long.MIN_VALUE));
        compare(Long.toString(Long.MAX_VALUE));
        compare(Integer.toString(Integer.MIN_VALUE));
        compare(Integer.toString(Integer.MAX_VALUE));
        compare("0.1");
        compare("-0.1111");
        compare("-2.222");
        compare("0.11111111111");
        compare("true");
        compare("false");
        compare("null");
        compare("\"foo\"");
        compare("\"\"");
        compare("\"null\"");
    }

    @Test
    public void object() throws Exception {
        compare("{}");
        compare("{\"foo\": \"bar\"}");
        compare("{\"foo\": 1}");
        compare("{\"foo\": true}");
        compare("{\"foo\": 0.1}");
        compare("{\"foo\": null}");
        compare("{\"foo\": {}}");
        compare("{\"foo\": []}");
        compare("{\"foo\": [{}]}");
        compare("{\"foo\": [{}, {}]}");
        compare("{\"foo\": [1, 2, 3]}");
        compare("{\"foo\": [null]}");
        compare("{\"foo\": \"\"}");
        compare("{\"foo\": \"2017-09-09\"}");
        compare("{\"foo\": {\"bar\": \"qux\"}}");
        compare("{\"foo\": 1, \"bar\": 2}");
        compare("{\"foo\": [], \"bar\": {}}");
        compare("{\"foo\": {\"bar\": {\"baz\": true}}}");
    }

    private static void compare(String string) throws IOException {
        JsonElement bson = TypeAdapters.JSON_ELEMENT.read(new BsonReader(new JsonReader(string))); // compare as BSON
        JsonElement gson = TypeAdapters.JSON_ELEMENT.fromJson(string); // compare as JSON
        check(bson).is(gson); // compare the two
    }

}