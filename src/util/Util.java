package util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Util {
	public static List<String> listFilesForFolder(final String folderName)
			throws IOException {
		List<String> filePaths = new ArrayList<>();

		Files.walk(Paths.get(folderName)).forEach(filePath -> {
			if (Files.isRegularFile(filePath)) {
				filePaths.add(filePath.toString());
			}
		});
		return filePaths;
	}

	public static int[] toIntArray(List<Integer> list) {
		int[] ret = new int[list.size()];
		for (int i = 0; i < ret.length; i++)
			ret[i] = list.get(i);
		return ret;
	}

	public static boolean isNumeric(String str) {
		try {
			double d = Double.parseDouble(str);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}

	public static String removeNonChars(final CharSequence input) {
		final StringBuilder sb = new StringBuilder(input.length());
		for (int i = 0; i < input.length(); i++) {
			final char c = input.charAt(i);
			if ((c > 64 && c < 91) || (c > 96 && c < 123) || (c == ' ')) {
				sb.append(c);
			} else {
				if (c == '.' || c == ',')
					sb.append(' ');
				else
					sb.append(" ");
			}

		}
		return sb.toString();
	}
}
