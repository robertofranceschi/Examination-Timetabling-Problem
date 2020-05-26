import java.util.Comparator;
import java.util.Vector;

public class Exam {
	
	private int originalId; // the original id from file is of the type "0001"
	private int id; // ordinal id 
	// maintain both ids
 	private int studEnrolled; // not useful
 	
	private double impactOnObjFunction; // how much this exam impacts on the cost of the obj. funct.
	public Vector<Integer> conflictingExams; // list of ids of conflicting exams
	public int numberOfConflict = 0; // size list above

	// CONSTRUCTOR
	public Exam(int originalId, int studEnrolled, int ordinalId) {
		this.originalId = originalId;
		this.studEnrolled = studEnrolled;
		this.id = ordinalId;
		this.conflictingExams = new Vector<Integer>();
	}
	
	// GETTER & SETTER	
	public int getOriginalId() {
		return originalId;
	}
	
	public void setImpactOnObjFunction(double i) {
		this.impactOnObjFunction = i;
	}
	
	public double getImpactOnObjFunction() {
		return impactOnObjFunction;
	}

	public void setOriginalId(int originalId) {
		this.originalId = originalId;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getStudEnrolled() {
		return studEnrolled;
	}

	public void setStudEnrolled(int studEnrolled) {
		this.studEnrolled = studEnrolled;
	}
	
	@Override
	public boolean equals(Object o){
		Exam other = (Exam) o;
		return (other.id == this.id);
	}
	
}


//required to compare exams on their impact on the objective function
class ImpactComparator implements Comparator<Exam> {
	
	public int compare(Exam e1, Exam e2){
		if(e1.getImpactOnObjFunction() < e2.getImpactOnObjFunction())
			return 1;
		else if(e1.getImpactOnObjFunction() == e2.getImpactOnObjFunction())
			return 0;
		else
			return -1;
	}
	
}
