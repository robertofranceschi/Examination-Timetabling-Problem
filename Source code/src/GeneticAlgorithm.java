import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Random;

public class GeneticAlgorithm {
	private FileManager fm;
	private Population population;
	private static final Random rand = new Random();
	private static PriorityQueue<SolutionByFitness> solutionsByFitness = new PriorityQueue<SolutionByFitness>();
	private static int nonImprovingIterCounter = 0;

	// CONSTRUCTOR
	public GeneticAlgorithm(FileManager fm, Population population) {
		this.fm = fm;
		this.population = population;
	}

	// METHODS
	
	/*
		Get a random solution from the current population WITH REPLACEMENT
	*/
	private Solution getRandomSolution() {
		return this.population.getSolution(rand.nextInt(this.population.getSize()));
	}
	
	public Population getPopulation() {
		return population;
	}

	/*
		Generate a new generation of solutions starting from the current one.
		Uses GA with 3 operatos:
			- Rescheduling (reschedule a set of exams randomly)
			- Swapping (swaps a couple of timeslots)
			- Multiple mutation (tries moving all exams in "better" timeslots)
	*/
	public void newGeneration() {

		int maxNonImprovingIter = 100;
		double topSolutionPercentage = 0.2;
		
		// Sort solution based on their fitness
		for(int s = 0; s < this.population.getSize(); s++)
			solutionsByFitness.add( new SolutionByFitness(population.getSolution(s).getFitness(fm), population.getSolution(s)) );
		
		// Temp population -> at the end of the method it will be used to update the current one
		Population tempPopulation = new Population( this.population.getSize(), null, new LinkedList<Solution>() );
		
		// The idea is to maintain the top (=lower fitness) 20% elements of the population, and preserve them in the new generation 
		int topSolutionSize = (int)Math.ceil(this.population.getSize()*topSolutionPercentage);
		// => best solutions are untouchable
		
		// Store top solutions
		for(int i=0; i< topSolutionSize; i++)
			tempPopulation.saveSolution(fm, i, solutionsByFitness.poll().getSolution());
		
		// Actually compute the new generation
		for(int i=topSolutionSize; i<this.population.getSize(); i++){
			
			Solution sol = getRandomSolution();
			Solution newSol;
			
			/*
				Rescheduling probability increases over time.
				it helps us escape from local minima when we fall into one
			*/
			double reschedulingProbability = 0.10 + (0.30*Math.min(maxNonImprovingIter, nonImprovingIterCounter)/maxNonImprovingIter);
			
			/*
				OPERATOR SELECTION
				Randomly select one operation between the three proposed
			*/
			if(rand.nextDouble() < reschedulingProbability){
				// System.out.println("rescheduling Operator");
				newSol = reschedulingOperator(sol, rand.nextDouble() );
			}else{
				if(rand.nextDouble() > 0.5) {
					// System.out.println("swapping Operator");
					newSol = swappingOperator(sol);
				}
				else {
					// System.out.println("multipleMutation Operator");
					newSol = multipleMutationOperator(sol);
				}
			}
			
			//System.out.println("op " + newSol.getSolArray().toString());
			tempPopulation.saveSolution(fm, i, newSol);
		}
		
		
		// Update the nonImprovingIterCounter in order to escape from local minima, if needed
		if( population.getBestSolution().getFitness(fm) == tempPopulation.getBestSolution().getFitness(fm) ) 
			nonImprovingIterCounter++;
		else
			nonImprovingIterCounter = 0;
		
		// The new generation has been computed
		this.population = tempPopulation;		
	}

	/*
		1) RESCHEDULING OPERATOR

		Used to unscheduling a random set of exams and reschedule them
		based on the initial Greedy Graph Coloring approach.

		This way we can generate a new feasible solution starting from the parent
		one, that is generally different and possibly worse and potentially
		allows us to escape	from local minima.
	*/
	private Solution reschedulingOperator(Solution sol, double deschedulationRate) {

		/*
			Solution representations used in the Greedy Graph Coloring approach
			are different in structure and so we have to convert them before
			calling this operator (Due to different people working on different algorithms)
		*/
		sol.computeS1andS2(fm);
		GGColoring ggc = new GGColoring(fm);
		
		Solution newSol = ggc.escapeFromLocalMinimum(sol,deschedulationRate);
		
		/*
			Convert the new solution found back to the solution representations
			used by the other two operators.
			The solution might also be the same as before, in case the graph Coloring
			approach took too long.
		*/
		if(newSol == null) {
			sol.convertSolutionToArray(fm.getNUM_EXAMS());
			//System.out.println(sol.getSolArray().toString());
			return sol;
		}
		else {
			newSol.convertSolutionToArray(fm.getNUM_EXAMS());
			//System.out.println("new " + newSol.getSolArray().toString());
			return newSol;
		}
	}
	
	/*
		2) SWAPPING OPERATOR

		Operator that swaps 2 timeslots with all their respective exams.
		This way we mantain feasibility	and assure a neighbour better solution
		by examining all combinations of two timeslots in our solution and
		choosing the one that most decrease fitness.
		ONLY A BETTER CHILD CAN BE PRODUCED, OTHERWISE KEEP THE SAME PARENT
	*/
	private Solution swappingOperator(Solution old){
		Solution sol = new Solution(old);
		sol.swapTimeslots(this.fm);
		return sol;
	}
	
	/*
		2) MULTIPLE MUTATION

		Try mutating exams, by moving them in their other available timeslots.
		In this operator, we try moving all the exams to the timeslot that most
		decreases fitness, one by one in a greedy fashion.
		ONLY A BETTER CHILD CAN BE PRODUCED, OTHERWISE KEEP THE SAME PARENT
	*/
	private Solution multipleMutationOperator(Solution old){
		Solution sol = new Solution(old);
		sol.computeMultipleMutation(this.fm);
		return sol;
	}
	
}

/*
	Custom structure to sort Solution by fitness and
	save the top 20% of them in the next generation to preserve them
*/
class SolutionByFitness implements Comparable<SolutionByFitness>{
	private double fitness;
	private Solution solution;

	public SolutionByFitness(double fitness, Solution solution){
		this.fitness = fitness;
		this.solution = solution;
	}
	
	public double getFitness(){
		return this.fitness;
	}
	public Solution getSolution(){
		return this.solution;
	}
	
	// Compares solutions based on their fitness
	@Override
	public int compareTo(SolutionByFitness otherSol){
		if (this.getFitness() < otherSol.getFitness()) return -1;
		else if (this.getFitness() > otherSol.getFitness()) return 1;
		else return 0;
	}
}
