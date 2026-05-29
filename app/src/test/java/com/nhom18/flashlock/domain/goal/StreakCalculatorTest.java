package com.nhom18.flashlock.domain.goal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

public class StreakCalculatorTest {

    private final StreakCalculator calculator = new StreakCalculator();

    private Date daysAgo(int days) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, -days);
        return c.getTime();
    }

    @Test
    public void firstCompletion_startsStreakAtOne() {
        StreakCalculator.Result r = calculator.evaluate(0, null, true, new Date());
        assertTrue(r.needsUpdate);
        assertEquals(1, r.streak);
    }

    @Test
    public void notMetAndNeverCompleted_doesNothing() {
        StreakCalculator.Result r = calculator.evaluate(0, null, false, new Date());
        assertFalse(r.needsUpdate);
        assertEquals(0, r.streak);
    }

    @Test
    public void metAfterYesterday_incrementsStreak() {
        StreakCalculator.Result r = calculator.evaluate(3, daysAgo(1), true, new Date());
        assertTrue(r.needsUpdate);
        assertEquals(4, r.streak);
    }

    @Test
    public void notMetButCompletedYesterday_keepsStreak() {
        StreakCalculator.Result r = calculator.evaluate(3, daysAgo(1), false, new Date());
        assertFalse(r.needsUpdate);
        assertEquals(3, r.streak);
    }

    @Test
    public void alreadyCompletedToday_isIdempotent() {
        StreakCalculator.Result r = calculator.evaluate(5, new Date(), true, new Date());
        assertFalse(r.needsUpdate);
        assertEquals(5, r.streak);
    }

    @Test
    public void missedAFullDayAndNotMet_resetsStreak() {
        StreakCalculator.Result r = calculator.evaluate(7, daysAgo(3), false, new Date());
        assertTrue(r.needsUpdate);
        assertEquals(0, r.streak);
    }

    @Test
    public void missedAFullDayButMetToday_restartsAtOne() {
        StreakCalculator.Result r = calculator.evaluate(7, daysAgo(3), true, new Date());
        assertTrue(r.needsUpdate);
        assertEquals(1, r.streak);
    }
}
