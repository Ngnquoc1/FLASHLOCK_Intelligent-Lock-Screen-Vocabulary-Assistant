package com.nhom18.flashlock.data.remote.dictionary;

import java.util.List;

public class DictionaryResponse {
    public String word;
    public String phonetic;
    public List<Meaning> meanings;

    public static class Meaning {
        public String partOfSpeech;
        public List<Definition> definitions;
    }

    public static class Definition {
        public String definition;
        public String example;
    }
}
