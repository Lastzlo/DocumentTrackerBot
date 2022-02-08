package lastzlo.doctrackerbot.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Documents {

	public static boolean isCaseNumber(String caseNumber) {
		// Compile regular expression
		final Pattern pattern = Pattern.compile("([0-9]+(/)+[0-9]+(/)+[0-9]+)", Pattern.CASE_INSENSITIVE);
		// Match regex against input
		final Matcher matcher = pattern.matcher(caseNumber);
		// Use results...
		return matcher.matches();
	}
}
