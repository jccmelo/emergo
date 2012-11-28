package br.ufpe.cin.emergo.preprocessor;

import java.io.File;

import br.ufpe.cin.emergo.util.Diff;

public class TestTool {

	/**
	 * This method tests if the results returned 
	 * by the tool are correct.
	 * 
	 * @param originalDir - where the correct results are.
	 * @param newDir - where the new results are.
	 */
	public void doTest(String originalDir, String newDir) {
		String dirname = "../Preprocessor4SPL/results/";

		File dir = new File(dirname + originalDir +"/");
		File anotherDir = new File(dirname + newDir +"/");

		if (dir.exists()) {
			// compares the result with original data
			// dir = original data

			String s[] = dir.list();
			String anotherStr[] = anotherDir.list();
			
			for (int i = 0; i < s.length; i++) {
				// gets original result and new result
				File oldFile = new File(dirname + originalDir +"/" + s[i]);
				
				
				for (int j = 0; j < anotherStr.length; j++) {
					File newFile = new File(dirname + newDir +"/" + s[j]);
					
					if (oldFile.getName().equals(newFile.getName())) {
						System.out.println("Testing analysis "+oldFile.getName());
						// tests if there is diff between the files
						String diff = Diff.getDiff(oldFile, newFile);
						if (!diff.isEmpty()){
							System.out.println("Ops! Error occurred in the analysis "+oldFile.getName());
							System.out.println("DIFF:\n\n"+diff);
						}
					}
				}

				
			}
		}
	}
	
	public static void main(String[] args) {
		TestTool tt = new TestTool();
		tt.doTest("Testclass", "Testclass23");
		tt.doTest("Soma", "Soma2");
		tt.doTest("Baseline", "Baseline3");
	}
}
