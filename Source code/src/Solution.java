import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Vector;

public class Solution {
	/*
		3 different solution representation are used for different purposes:

		The first two are the ones needed by the Greedy Graph Coloring algorithm,
		while a more simple Vector<Integer> solution representation is used in
		the Genetic Algorithm
	*/
	private Map<Integer, Integer> s1; // Solution representation for GGC
	private Map<Integer, List<Integer>> s2; // Solution representation for GGC
	private Vector<Integer> solArray; // Solution representation for GA
	private double fitness = 0;
	private boolean feasibilityAlreadyCalculated = false;


	// CONSTRUCTORS
	public Solution(int nExams, int tMax){
		s1 = new HashMap<Integer, Integer>();
		s2 = new HashMap<Integer, List<Integer>>();

		// Initialization of the two solution representations
		for(int i=0; i<nExams;i++)
			s1.put(i, -1);
		for(int i=0; i<tMax; i++)
			s2.put(i, new LinkedList<Integer>());
		
		// Initialization of the third solution representation solArray
		convertSolutionToArray(nExams);
	}

	public Solution(Solution sol){
		// Create a new solution from the current one 
		solArray = new Vector<Integer>(sol.solArray); // clone only the solArray -> then (if needed) it will be converted to the other solution representations s1 ans s2
	}
	
	public Solution(Map<Integer, Integer> s1, Map<Integer, List<Integer>> s2, double fitness) {
		super();
		this.s1 = s1;
		this.s2 = s2;
		this.fitness = fitness;
		this.solArray = new Vector<>( (int) s1.size() );
		convertSolutionToArray(s1.size());
	}
	
	
	// GETTER & SETTER 
	public Map<Integer, Integer> getS1() {
		return s1;
	}

	public void setS1(Map<Integer, Integer> s1) {
		this.s1 = s1;
	}

	public Map<Integer, List<Integer>> getS2() {
		return s2;
	}

	public void setS2(Map<Integer, List<Integer>> s2) {
		this.s2 = s2;
	}

	public double getFitness(FileManager fm) {
		if(!this.feasibilityAlreadyCalculated){
			this.fitness = computeObjectiveFunction(fm);
			this.feasibilityAlreadyCalculated = true;
		}
		return fitness;
	}

	public void setFitness(double fitness) {
		this.fitness = fitness;
	}	
	
	protected double getImpact(FileManager fm, int idExam) {
		return getImpactOnObjFunction(fm,idExam);
	}
	
	public int getExamTimeSlot(int e) {
		return solArray.get(e);
	}
	
	public void setTimeslot(int e, int t) {
		solArray.set(e, t);
	}	
	
	public Vector<Integer> getSolArray() {
		return solArray;
	}
	
	// METHODS
	
	/*
		Use of operator MULTIPLE MUTATION
		(see more in GeneticAlgorithm.java)
	*/
	public void computeMultipleMutation(FileManager fm){
		
		// Ordina per contributo/impatto nell'obj.func
		PriorityQueue<Exam> examByImpact = getOrderedExams(fm);
		
		while( ! examByImpact.isEmpty() ){
			/*
				For each exam of this instance,
				try moving it to another available timeslot
				in order to reduce parent fitness
			*/

			Exam e = examByImpact.poll();
			HashSet<Integer> eFeasibleTimeslots = getAvailableTimeslots(fm, e);
			
			if( ! eFeasibleTimeslots.isEmpty() ){
				/*
					Find the best timeslot to move this exam to,
					based on decreasing fitness
				*/

				int bestTS = -1;
				double bestDelta = 0;
				
				for(Integer ts : eFeasibleTimeslots){
					double delta = this.deltaMoving(fm, e.getId(), ts);

					if(delta < bestDelta){
						bestTS = ts;
						bestDelta = delta;						
					}
				}
				
				// If a better timeslot has been found
				if( bestTS != -1)
					setTimeslot(e.getId(), bestTS);				
			}
		}
	}
	
	private HashSet<Integer> getAvailableTimeslots(FileManager fm, Exam e){

		HashSet<Integer> tsAvailable = new HashSet<Integer>(fm.getTimeslotsSet());

		for (int i = 0; i < e.conflictingExams.size(); i++){
			
			int e2 = e.conflictingExams.get(i);
			
			boolean alreadyScheduled = (getExamTimeSlot(e2) != -1);
			
			if( alreadyScheduled )
				tsAvailable.remove(getExamTimeSlot(e2));
		}

		return tsAvailable;
	}
	
	public void swapTimeslots(FileManager fm){
		int finalT1 = -1;
		int finalT2 = -1;
		
		/*
			Find best couple of timeslots to swap:
			For all possible combinations of two timeslots,
			compute the change in fitness if that swap was performed.
		*/
		double bestDelta = 0;
		
		for (int ts1=0; ts1< fm.getTMAX() - 1; ts1++) {
			for (int ts2=ts1+1 ; ts2<fm.getTMAX(); ts2++) {
				double delta = deltaSwap(fm, ts1, ts2);
				if(delta < bestDelta){
					finalT1=ts1;
					finalT2=ts2;
					bestDelta = delta;
				}
			}
		}

		// If a convenient swap has been found, swap all respective exams
		if (bestDelta < 0){
			
			for (int e= 0; e< fm.getNUM_EXAMS(); e++){
				if (getExamTimeSlot(e) == finalT1)
					setTimeslot(e, finalT2);
				else if (getExamTimeSlot(e) == finalT2)
					setTimeslot(e, finalT1);
			}
			
		}
		
		// THE PARENT REMAINS UNCHANGED OTHERWISE
	}
	
	private PriorityQueue<Exam> getOrderedExams(FileManager fm){
		/*
			Order exams based on how much they affect the objective function
			(their mathematical contribute)
		*/
		PriorityQueue<Exam> orderedExams = new PriorityQueue<Exam>(10, new ImpactComparator());
		
		for (int e = 0; e < fm.getNUM_EXAMS(); e++){
			fm.getExamByIndex(e).setImpactOnObjFunction( getImpact(fm,e) );
			orderedExams.add(fm.getExamByIndex(e));
		}
		
		return orderedExams;
	}

	/*
		Compute solution representation s1 and s2,
		based on the solution representation solArray
	*/
	public void computeS1andS2(FileManager fm) {
		this.s1 = new HashMap<Integer, Integer>();
		this.s2 = new HashMap<Integer, List<Integer>>();
		
		// Initialize the two solution representation
		for(int i=0; i<fm.getNUM_EXAMS();i++)
			s1.put(i, -1);
		for(int i=0; i<fm.getTMAX(); i++)
			s2.put(i, new LinkedList<Integer>());
		
		for(int i=0; i<fm.getNUM_EXAMS();i++) {
			s1.replace(i, solArray.get(i));
			// System.out.println(s2.toString());
			// System.out.print(solArray.get(i) + " ");
			// System.out.println(s2.get(solArray.get(i)));
			s2.get(solArray.get(i)).add(i);
		}
	}
	
	private double deltaMoving(FileManager fm, int exam, int newTimeslot) {
		
		/*
			We use a temp solution to compute the difference (in terms of impact on the obj func)
			if we move examToMove in the 'newTimeSlot' w.r.t. the original solution
		*/
		
		Solution tempSol = new Solution(this);
		tempSol.setTimeslot(exam, newTimeslot); // move the exam in the temporary solution

		return (tempSol.getImpact(fm,exam) -  this.getImpact(fm, exam));
	}
	

	/*
		Compute the difference on the obj function 
		if all the exams in t1 were swapped with all the exams in t2
	*/
	private double deltaSwap(FileManager fm, int t1, int t2){
		
		double delta= 0;
		HashSet<Integer> currentExams = new HashSet<Integer>();
		
		for (int e=0; e<fm.getNUM_EXAMS(); e++){
			if(getExamTimeSlot(e) == t1 || getExamTimeSlot(e) == t2)
				currentExams.add(e);
		}
		
		/*
			We scan all the exams belonging to either t1 or t2,
			and for each we scan all its conflicting exams and check
			how the fitness would be affected with such a swap
		*/
		
		for(Integer e1 : currentExams ) {
	
			for(int j=0; j<fm.getExamByIndex(e1).conflictingExams.size(); j++){
				
				int e2= fm.getExamByIndex(e1).conflictingExams.get(j);

				int otherTimeslot;
				int currentTimeslot;
				if( !currentExams.contains(e2)){

					currentTimeslot = getExamTimeSlot(e1);
					if (currentTimeslot == t1) otherTimeslot = t2;
					else otherTimeslot = t1;

					delta += ( getPenalty(fm, e1, otherTimeslot, e2, getExamTimeSlot(e2)) - getPenalty(fm, e1, currentTimeslot, e2, getExamTimeSlot(e2)) );
				}
			}
			
		}
		return delta;
	}
	
	public void convertSolutionToArray(int nExams) {
		/*
			Used to update the solution representation solArray from s1
		*/
		
		this.solArray = new Vector<>(nExams);
		for(Map.Entry<Integer,Integer> entry : this.s1.entrySet() ) {
			this.solArray.add(entry.getKey(), entry.getValue());
		}
	}
	
	// Computes fitness of this solution, if not already calculated
	public double computeObjectiveFunction(FileManager fm) {
		double fitness = 0.0;
		
		for(int e1=0; e1<fm.getNUM_EXAMS(); e1++) {
			int ts1 = this.getExamTimeSlot(e1);
			for(int e2=e1+1; e2<fm.getNUM_EXAMS(); e2++) {
				int ts2 = this.getExamTimeSlot(e2);
				int d = Math.abs(ts1 - ts2);
				if(d <= 5)
					fitness += Math.pow(2,5-d)*fm.getNumOfStudentsInConflict(e1, e2);
			}
		}
		return fitness/fm.getNUM_STUDENTS();
	}
	
	/*
		Returns the pentalty of exams e1 and e2 being in timeslots ts1 and ts2
	*/
	public double getPenalty(FileManager fm, int e1, int ts1, int e2, int ts2){
		double penalty = 0.0;
		int d = Math.abs(ts1 - ts2);
		
		if(d<=5)
			penalty = Math.pow(2,5-d)*fm.getNumOfStudentsInConflict(e1, e2);
		return penalty;
	}
	
	public double getImpactOnObjFunction(FileManager fm, int e1) {
		/*
			Compute the impact of having e1 in its timeslot
			on the total objective function
		*/	
		
		double impact = 0.0;
		
		int ts1 = this.getExamTimeSlot(e1);
		
		for (int e2=0; e2<fm.getNUM_EXAMS() && fm.getNumOfStudentsInConflict(e1, e2) != 0; e2++) {
			
			if(e1 != e2) {
				int ts2 = this.getExamTimeSlot(e2);
				int d = Math.abs(ts1 - ts2);
				
				if(d <= 5)
					impact += Math.pow(2,5-d)*fm.getNumOfStudentsInConflict(e1, e2);
			}
		}
		return impact/fm.getNUM_STUDENTS()*0.5;
	}

	/*
	public String toString(FileManager fm) {
		String sol = "";
		for(int i=0; i<fm.getNUM_EXAMS(); i++) {
			sol += (i + 1) + " " + (getExamTimeSlot(i) + 1) + "\n";
		}
		sol += "Penalty: " + getFitness(fm);
		return sol;
	}
	*/
}
