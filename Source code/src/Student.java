import java.util.ArrayList;
import java.util.List;

public class Student {
	String id;
	List<Exam> exams;
	
	@Override
	public String toString() {
		return "Student [id=" + id + "]";
	}

	public Student(String id) {
		this.id = id;
		this.exams = new ArrayList<Exam>();
	}

	
}
