import java.io.IOException;
import java.util.List;


public class Main {

	static public double tlim = 10000; // ideally no a-priori known time limit

	public static void main(String[] args) throws NumberFormatException, IOException {
		
		checkCommandLine(args);
		final String instance = args[0];
		
		double start = System.currentTimeMillis();

		int populationSize = 20;
		
		FileManager fm = new FileManager(instance, true); // if you want to print "true" info (numStud, numTS, numExams)
		GGColoring ggc = new GGColoring(fm);		
		
		List<Solution> initialPopulation = ggc.getInitialPopulation(populationSize);
		
		System.out.println("\nSoluzione iniziale (populazion size = " + populationSize + ") in : " + updateTime(start) +"s\n");
		
//		System.exit(0); // check initial pop
		
		
		Population population = new Population(populationSize, ggc.getBestSolution(), initialPopulation);
		
		GeneticAlgorithm ga = new GeneticAlgorithm(fm, population);
		
		double timeElapsed = updateTime(start);
		
		// check time limit 'tlim'
		while( tlim > timeElapsed ) {
			
			double bestPenalty = ga.getPopulation().getBestSolution().getFitness(fm);
			ga.newGeneration();
			
			double currentPenalty = ga.getPopulation().getBestSolution().getFitness(fm);
			if (currentPenalty < bestPenalty)
				System.out.println("Current penalty: " + currentPenalty + " in " + timeElapsed + " seconds");
			
			// print the best solution in the file
			fm.writeSolutionFile(ga.getPopulation().getBestSolution()); // write the solution on file at every iteration! 
			
			// update time
			timeElapsed = updateTime(start);
		}
		
		// Output
		System.out.println("\nBest solution penalty (" + timeElapsed + " sec): " + ga.getPopulation().getBestSolution().getFitness(fm));
		System.out.println("Solution has been written in file " + instance + fm.SOL_SUFFIX + " (in the same folder of the file .jar)\n\n");
					
	}
	
	//check command line and save time limit 'tlim'
	private static void checkCommandLine(String[] args) {
		/* Command line: $ java -jar ETPsolver_DMOgroup12.jar instancename -t tlim
		 * example: instance01 -t 10
		 * 
		 * args[0] -> instanceName 
		 * args[1] -> optionalTime (-t) 
		 * args[2] -> time limit
		 */
		
		if (args.length != 3) {
			if (args.length != 1) {
				System.out.println("Command line usage: instancename -t timelimit");
				System.exit(-1);
			}
		} else { // if time limit is specified
			String optionalTime = args[1];
			if (optionalTime.equals("-t"))
				tlim = Integer.parseInt(args[2]); // save time limit
		}
	}
	
	// update the time
	public static double updateTime(double start){
		return (double) ( (System.currentTimeMillis() - start) / 1000.0 );
	}
}
