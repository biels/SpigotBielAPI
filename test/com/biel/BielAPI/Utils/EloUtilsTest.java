package com.biel.BielAPI.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

/**
 * Equal ratings are the common case, not the corner case: every newcomer starts
 * at the same seed. Both group calculations once looked players up by rating
 * value, so equally rated players collapsed into one slot or skipped each other.
 */
class EloUtilsTest {
	private static final double K = 12;

	private static ArrayList<Double> ratings(double... values) {
		ArrayList<Double> list = new ArrayList<Double>();
		for (double value : values) list.add(value);
		return list;
	}

	@Test
	void equallyRatedWinnersAndLosersEachGetTheirOwnChange() {
		ArrayList<ArrayList<Double>> result = EloUtils.calculateEloGroupChange(ratings(1200, 1200), ratings(1200, 1200, 1200), K, false);
		ArrayList<Double> winners = result.get(0);
		ArrayList<Double> loosers = result.get(1);

		assertEquals(2, winners.size(), "one change per winner");
		assertEquals(3, loosers.size(), "one change per loser, even when their ratings are equal");
		assertEquals(winners.get(0), winners.get(1), 1e-9, "equal winners earn the same");
		assertEquals(loosers.get(0), loosers.get(1), 1e-9);
		assertEquals(loosers.get(1), loosers.get(2), 1e-9);
		assertTrue(winners.get(0) > 0);
		assertTrue(loosers.get(0) < 0);

		double gainedBeforeBonus = (winners.get(0) + winners.get(1)) / 1.15;
		double lost = -(loosers.get(0) + loosers.get(1) + loosers.get(2));
		assertEquals(gainedBeforeBonus, lost, 1e-9, "apart from the winners' bonus, rating is conserved");
	}

	@Test
	void orderedFinishStillPairsEquallyRatedPlayers() {
		ArrayList<Double> result = EloUtils.calculateEloGroupChange(ratings(1200, 1200, 1200), K, false);

		assertEquals(3, result.size());
		assertTrue(result.get(0) > 0, "first place gains against both others");
		assertEquals(0, result.get(1), 1e-9, "second place wins one pairing and loses one");
		assertTrue(result.get(2) < 0, "last place loses to both others");
		assertEquals(-result.get(0), result.get(2), 1e-9, "the podium is symmetric between equals");
	}
}
