import java.util.List;

public class Population {
	private int size;
	private Solution bestSol;
	private List<Solution> solutions = null;
	
	
	// CONSTRUCTOR
	public Population(int size, Solution bestSol, List<Solution> population) {
		this.size = size;
		this.bestSol = bestSol;
		this.solutions = population;
	}

	// GETTER
	// return the size of the population
	public int getSize() {
		return size;
	}
	
	// return the solution corresponding to the index 'id'
	public Solution getSolution(int i) {
		return solutions.get(i);
	}
		
	// return the solution with the lowest penalty
	public Solution getBestSolution() {
		return bestSol;
	}
	
	// save the solution at index 'pos'
	public void saveSolution(FileManager fm, int pos, Solution currentSol) {
		// update the best solution
		if( bestSol == null || currentSol.getFitness(fm) < bestSol.getFitness(fm) ){
			bestSol = currentSol;
		}
		// save the solution
		solutions.add(pos,currentSol);
	}
	
}
