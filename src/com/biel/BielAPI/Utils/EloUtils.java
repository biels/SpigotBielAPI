package com.biel.BielAPI.Utils;

import java.util.ArrayList;
import java.util.Collections;

public class EloUtils {
	public static Pair<Double, Double> calculateEloChange(double elo1, double elo2, int winner, double K, boolean absolute){
		//WINNER 1, 2, [0] <- Empat
		double R1 = Math.pow(10, elo1 / 400.0),
			   R2 = Math.pow(10, elo2 / 400.0); 
		double E1 = R1 / (R1 + R2),
			   E2 = R2 / (R1 + R2); 
		double S1 = (winner == 1 ? 1 : (winner == 0 ? 0.5 : 0)),
			   S2 = (winner == 2 ? 1 : (winner == 0 ? 0.5 : 0));
		double r1 = (absolute ? elo1 : 0) + K * (S1 - E1),
			   r2 = (absolute ? elo2 : 0) + K * (S2 - E2);
		return new Pair<Double, Double>(r1, r2);
	}
	public static Pair<Double, Double> calculateExpectedPercentage(double elo1, double elo2){
		//WINNER 1, 2, [0] <- Empat
		double R1 = Math.pow(10, elo1 / 400.0),
			   R2 = Math.pow(10, elo2 / 400.0); 
		double E1 = R1 / (R1 + R2),
			   E2 = R2 / (R1 + R2); 
		return new Pair<Double, Double>(E1, E2);
	}
	/**
	 * Rating changes for a match between two groups, in the order the groups were
	 * given: result 0 pairs with {@code winners}, result 1 with {@code loosers}.
	 *
	 * Players are addressed by position throughout. This used to find each rating's
	 * index by value, so two players with equal ratings shared one accumulator and
	 * the loser list came back shorter than the losers - and equal ratings are the
	 * norm, since every newcomer starts at the same average.
	 */
	public static ArrayList<ArrayList<Double>> calculateEloGroupChange(ArrayList<Double> winners, ArrayList<Double> loosers, double K, boolean absolute){
		ArrayList<Double> winnersUpdated = new ArrayList<Double>(winners.size());
		ArrayList<Double> loosersUpdated = new ArrayList<Double>(Collections.nCopies(loosers.size(), 0D));
		for (int w = 0; w < winners.size(); w++) {
			double winnerChange = 0D;
			for (int l = 0; l < loosers.size(); l++) {
				Pair<Double, Double> link = calculateEloChange(winners.get(w), loosers.get(l), 1, K / loosers.size(), false);
				winnerChange += link.getFirst() * 1.15;
				loosersUpdated.set(l, loosersUpdated.get(l) + link.getSecond());
			}
			winnersUpdated.add(winnerChange);
		}
		ArrayList<ArrayList<Double>> results = new ArrayList<ArrayList<Double>>();
		results.add(winnersUpdated);
		results.add(loosersUpdated);
		return results;
	}
	/**
	 * Rating changes for a ranked finish, one per entry of {@code orderedWinners} in
	 * the same order; earlier entries beat every later one. Positional for the same
	 * reason as above: comparing indices found by value made two equally rated
	 * players skip each other's pairing entirely.
	 */
	public static ArrayList<Double> calculateEloGroupChange(ArrayList<Double> orderedWinners, double K, boolean absolute){
		ArrayList<Double> result = new ArrayList<Double>(orderedWinners.size());
		for (int w = 0; w < orderedWinners.size(); w++) {
			double change = 0D;
			for (int o = 0; o < orderedWinners.size(); o++) {
				if (w == o) continue;
				Pair<Double, Double> link = calculateEloChange(orderedWinners.get(w), orderedWinners.get(o), (w < o ? 1 : 2), K / orderedWinners.size(), false);
				change += link.getFirst();
			}
			result.add(change);
		}
		return result;
	}
}
