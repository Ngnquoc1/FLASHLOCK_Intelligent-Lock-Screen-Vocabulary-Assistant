package com.nhom18.flashlock.domain.word;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import com.nhom18.flashlock.data.model.Word;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class WordOfTheDayPickerTest {

    private final WordOfTheDayPicker picker = new WordOfTheDayPicker();

    private Word word(String id) {
        Word w = new Word();
        w.setWordId(id);
        w.setTerm(id);
        return w;
    }

    private Date date(int y, int m, int d) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(y, m - 1, d);
        return c.getTime();
    }

    @Test
    public void emptyPool_returnsNull() {
        assertNull(picker.pick("u1", new Date(), new ArrayList<>()));
        assertNull(picker.pick("u1", new Date(), null));
    }

    @Test
    public void nullDate_returnsNull() {
        assertNull(picker.pick("u1", null, Arrays.asList(word("a"))));
    }

    @Test
    public void singleWordPool_alwaysReturnsThatWord() {
        Word only = word("solo");
        assertSame(only, picker.pick("u1", date(2026, 1, 1), Arrays.asList(only)));
    }

    @Test
    public void sameUidAndDate_returnsSameWord() {
        List<Word> pool = Arrays.asList(word("a"), word("b"), word("c"), word("d"));
        Word w1 = picker.pick("u1", date(2026, 5, 10), pool);
        Word w2 = picker.pick("u1", date(2026, 5, 10), pool);
        assertEquals(w1.getWordId(), w2.getWordId());
    }

    @Test
    public void inputOrderDoesNotAffectResult() {
        List<Word> a = Arrays.asList(word("a"), word("b"), word("c"), word("d"));
        List<Word> b = new ArrayList<>(a);
        Collections.reverse(b);
        Word p1 = picker.pick("u1", date(2026, 5, 10), a);
        Word p2 = picker.pick("u1", date(2026, 5, 10), b);
        assertEquals(p1.getWordId(), p2.getWordId());
    }

    @Test
    public void differentDays_usuallyDifferentWords() {
        List<Word> pool = new ArrayList<>();
        for (int i = 0; i < 30; i++) pool.add(word("w" + i));
        Word d1 = picker.pick("u1", date(2026, 5, 10), pool);
        Word d2 = picker.pick("u1", date(2026, 5, 11), pool);
        // Có thể trùng do hash collision, nhưng với pool 30 phần tử rất hiếm.
        assertNotEquals(d1.getWordId(), d2.getWordId());
    }
}
