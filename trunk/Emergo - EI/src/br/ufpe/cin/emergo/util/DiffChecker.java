package br.ufpe.cin.emergo.util;

import java.io.File;

import br.ufpe.cin.emergo.util.Diff;

public class DiffChecker {

	/**
	 * This method tests if the results returned 
	 * by the tool are correct.
	 * 
	 * @param originalDir - where the correct results are.
	 * @param newDir - where the new results are.
	 */
	public boolean doTest(String originalDir, String newDir) {
//		String dirname = "/Users/paolaaccioly/Documents/Working Copies/emergo/trunk/Emergo - EI/results/";
		String dirname = ".\\results\\";

		File dir = new File(dirname + originalDir +"/");
		File anotherDir = new File(dirname + newDir +"/");

		if (dir.exists() && anotherDir.exists()) {
			// compares the result with original data
			// dir = original data

			String s[] = dir.list();
			String anotherStr[] = anotherDir.list();
			
			for (int i = 0; i < s.length; i++) {
				// gets original result and new result
				File oldFile = new File(dirname + originalDir +"/" + s[i]);
				
				for (int j = 0; j < anotherStr.length; j++) {
					File newFile = new File(dirname + newDir +"/" + anotherStr[j]);
					
					if (oldFile.getName().equals(newFile.getName())) {
						// tests if there is diff between the files
						String diff = Diff.getDiff(oldFile, newFile);
						if (!diff.isEmpty()){
							System.out.println("Ops! Error occurred in the file "+oldFile.getName());
							System.out.println("DIFF:\n\n"+diff);
							return false;
						} 
					}
				}

				
			}
			return true;
		} else {
			System.out.println("Directory "+dir+" or "+anotherDir+" not found!");
			return false;
		}
	}
}
