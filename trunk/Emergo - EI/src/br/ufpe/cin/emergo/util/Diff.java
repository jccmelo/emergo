package br.ufpe.cin.emergo.util;

import java.io.File;

public class Diff {

	public static String getDiff(File file0, File file1) {
		// read in lines of each file
		Input in0 = new Input(file0);
		Input in1 = new Input(file1);

		String str0 = in0.readAll();

//		if (str0 == null) {
//			System.out.println("str0 is NULL");
//		}

		String str1 = in1.readAll();

		String[] originalFile = new String[0];
		String[] changedFile = new String[0];

		if (str0 != null)
			originalFile = str0.split("\\n");
		if (str1 != null)
			changedFile = str1.split("\\n");

		String result = "";

		// number of lines of each file
		int M = originalFile.length;
		int N = changedFile.length;

		// opt[i][j] = length of LCS of x[i..M] and y[j..N]
		int[][] opt = new int[M + 1][N + 1];

		// compute length of LCS and all subproblems via dynamic programming
		for (int i = M - 1; i >= 0; i--) {
			for (int j = N - 1; j >= 0; j--) {
				if (originalFile[i].equals(changedFile[j]))
					opt[i][j] = opt[i + 1][j + 1] + 1;
				else
					opt[i][j] = Math.max(opt[i + 1][j], opt[i][j + 1]);
			}
		}

		int ok = 0;

		// compute some rule
		for (int i = 0; i < originalFile.length; i++) {
			String[] s = originalFile[i].split(" ");

			for (int j = 0; j < s.length; j++) {

				if (s[j].trim().matches("\\wType|int|String|char|float|Float")) {

					String[] str = originalFile[i].split(" ");
					for (int k = 0; k < str.length; k++) {
						if (ok == 1) {
							System.out.print(s[k]+ " ");
						}
						if (str[k].equals(s[j]))
							ok = 1;

					}
				}
			}
		}

		// recover LCS itself and print out non-matching lines to standard
		// output
		int i = 0, j = 0;
		while (i < M && j < N) {
			if (originalFile[i].equals(changedFile[j])) {
//				result += "= " + originalFile[i] + "\n";// add

				i++;
				j++;
			} else if (opt[i + 1][j] >= opt[i][j + 1])
				result += "-- " + originalFile[i++] + "\n";
			else
				result += "++ " + changedFile[j++] + "\n";
		}

		// dump out one remainder of one string if the other is exhausted
		while (i < M || j < N) {
			if (i == M)
				result += "++ " + changedFile[j++] + "\n";
			else if (j == N)
				result += "-- " + originalFile[i++] + "\n";
		}

		return result;
	}
}
