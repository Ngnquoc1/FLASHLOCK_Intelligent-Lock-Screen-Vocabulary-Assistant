package com.nhom18.flashlock.ui.vocabulary;

import com.nhom18.flashlock.data.model.Word;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class WordFilterTest {

    @Test
    public void apply_filtersByStatusAndQuery() {
        Word word1 = new Word();
        word1.setTerm("analyze");
        word1.setDefinition("to examine");
        word1.setStatus(Word.STATUS_NEW);

        Word word2 = new Word();
        word2.setTerm("deploy");
        word2.setDefinition("release to production");
        word2.setStatus(Word.STATUS_MASTERED);

        List<Word> result = WordFilter.apply(Arrays.asList(word1, word2), Word.STATUS_NEW, "analy");

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("analyze", result.get(0).getTerm());
    }

    @Test
    public void apply_handlesEmptyQuery() {
        Word word1 = new Word();
        word1.setTerm("coherent");
        word1.setStatus(Word.STATUS_LEARNING);

        List<Word> result = WordFilter.apply(Arrays.asList(word1), Word.STATUS_LEARNING, "");

        Assert.assertEquals(1, result.size());
    }
}

