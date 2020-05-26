import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.*;

public class FileManager {
	
	// String useful for I/O operation
	final String SOL_SUFFIX = "_DMOgroup12.sol";
	private final static String EXAM_SUFFIX = ".exm";
    private final static String STUDENT_SUFFIX = ".stu";
    private final static String TIMESLOT_SUFFIX = ".slo";
    
    // VARIABLES
    String instanceName;
    private int TMAX;
	private int NUM_EXAMS;
	private int NUM_STUDENTS;
	private int[][] M; // conflict matrix (the element M[e1][e2] contains the number of student that are enrolled both in e1 and e2)
	private HashSet<Integer> timeslotSet;
	
	private Map<String, Student> students = new TreeMap<String, Student>();
	private Map<Integer, Exam> exams = new TreeMap<Integer, Exam>(); 
	private Map<Integer, List<Integer>> listAdj = new TreeMap<Integer, List<Integer>>();
	
	
	
	// CONSTRUCTOR
	public FileManager(String instanceName, boolean printFlag) throws IOException {
		this.instanceName = instanceName;
		NUM_EXAMS = readExamsFile();
		TMAX = readTimeslotsFile();
		NUM_STUDENTS = readStudentsFile();
		
		if (printFlag == true) {
			// for(int i=0; i<M.length; i++) {
			// 	for(int j=0; j<M.length; j++)
			// 		System.out.print("\nMatrice di adiacenza: "+M[i][j]);
			// 
			// }
			System.out.println("\nInstance:\t"+ instanceName + "\nTimeslots:\t"+ TMAX + "\nStudents:\t" + NUM_STUDENTS+ "\nExams:\t\t" + NUM_EXAMS);
		}
	}
	
	// GETTER & SETTER
	public int getTMAX() {
		return TMAX;
	}

	public int getNUM_EXAMS() {
		return NUM_EXAMS;
	}

	public int getNUM_STUDENTS() {
		return NUM_STUDENTS;
	}

	public int[][] getConflictMatrix() {
		return M;
	}

	public Map<Integer, List<Integer>> getListAdj() {
		return listAdj;
	}
	
	public int getNumOfStudentsInConflict(int e1, int e2) {
		return M[e1][e2];
	}
	
	public HashSet<Integer> getTimeslotsSet() {
		return timeslotSet;
	}
	
	public Exam getExamByIndex(int i) {
		return exams.get(i+1);
	}
	
	// METHODS
	
	private int readStudentsFile() throws IOException {
		// leggo file students
		File f = new File(this.instanceName + STUDENT_SUFFIX); 
		BufferedReader br = new BufferedReader(new FileReader(f));   
		String line;
		
		while ( (line = br.readLine()) != null && !line.equals("") ) {
			String[] node = line.split(" ", 2);
			
			String matStud = node[0];
			int originalId = Integer.parseInt(node[1]);
			
			int indexCurrentExam = exams.get(originalId).getId();
			
			// popolo matrice e lista di adiacenza
			if(students.containsKey(matStud)) { // se lo studente è già presente
				for (Exam e : students.get(matStud).exams) {
					
					M[indexCurrentExam][e.getId()]++;
					M[e.getId()][indexCurrentExam]++;
					
					// CREO MAPPA DEI CONFLITTI:
					if(listAdj.containsKey(e.getId())) {
						if(!listAdj.get(e.getId()).contains(indexCurrentExam)) {	
							listAdj.get(e.getId()).add(indexCurrentExam);
							exams.get(e.getOriginalId()).numberOfConflict++;
							exams.get(e.getOriginalId()).conflictingExams.add(indexCurrentExam);
						}
					}
					
					// viceversa:
					if(listAdj.containsKey(indexCurrentExam)) {
						if(!listAdj.get(indexCurrentExam).contains(e.getId())) {
							listAdj.get(indexCurrentExam).add(e.getId());
							exams.get(originalId).numberOfConflict++;
							exams.get(originalId).conflictingExams.add(e.getId());
						}
					}
				}
				
				students.get(matStud).exams.add(exams.get(originalId));
			} else { // se non è presente creo lo studente e aggiungo alla lista students
				Student s = new Student(matStud);
		        s.exams.add(exams.get(originalId));
		        students.put(matStud, s);
			}
		}
		int nStudents = students.size();
		br.close();
		return nStudents;
	}

	private int readTimeslotsFile() throws FileNotFoundException {
		// leggo timeslot
		File file2 = new File(this.instanceName + TIMESLOT_SUFFIX); 
		Scanner sc = new Scanner(file2); 
		int tMax = 0;
	    while (sc.hasNextLine()) {
	    	tMax = Integer.parseInt(sc.nextLine()); 
	    }
	    sc.close();
	    
	    timeslotSet = new HashSet<Integer>(tMax);
		for(int i=0; i<tMax; i++)
			timeslotSet.add(i);
		
		return tMax;
	}

	private int readExamsFile() throws IOException {
		// leggo file exams
		File file1 = new File(this.instanceName + EXAM_SUFFIX); 
		BufferedReader br = new BufferedReader(new FileReader(file1));   
		String line; 
		int currentExamIndex = 0; 
		
		while ( (line = br.readLine()) != null && !line.equals("") ) {
			
			String[] node = line.split(" ", 2);
			
			int originalId = Integer.parseInt(node[0]);
			int numStudEx = Integer.parseInt(node[1]);
			
			if ( !exams.containsKey(originalId) ) {
				Exam e = new Exam(originalId, numStudEx, currentExamIndex++);
				exams.put(e.getOriginalId(), e);
				//exams.put(e.id, e);
			}			
		} 
		
		// matrice di adiacenza 
		int nExam = exams.values().size(); // numero di esami
		M = new int[nExam][nExam];
		
		// inizializzo la matrice e lista di adiacenza
		for(int i=0; i<nExam; i++) {
			for(int j=0; j<nExam; j++) {
				M[i][j] = 0;
			}
			
			listAdj.put(i, new LinkedList<Integer>());
		}
		br.close();
		
		return nExam;
	}

	// Solution Writer -> write solution in the file .sol
	public void writeSolutionFile(Solution best) throws IOException {

		FileWriter fw = new FileWriter(this.instanceName + SOL_SUFFIX);
		BufferedWriter bw = new BufferedWriter(fw);
		
		for(int e=0; e<NUM_EXAMS; e++) {
			bw.write((e+1) + " " + (best.getExamTimeSlot(e)+1));
			if(e < NUM_EXAMS-1)
				bw.write("\r\n");
		}
		
		bw.flush();
		bw.close();

	}
	
}
