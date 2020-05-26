
public class Spostamento {

	private int esame;
	private int timeSlotDisponibile;
	private int timeSlotDellaSoluzione;
	public Spostamento(int esame, int timeSlotDisponibile, int timeSlotDellaSoluzione) {
	
		this.esame = esame;
		this.timeSlotDisponibile = timeSlotDisponibile;
		this.timeSlotDellaSoluzione = timeSlotDellaSoluzione;
	}
	public int getEsame() {
		return esame;
	}
	public void setEsame(int esame) {
		this.esame = esame;
	}
	public int getTimeSlotDisponibile() {
		return timeSlotDisponibile;
	}
	public void setTimeSlotDisponibile(int timeSlotDisponibile) {
		this.timeSlotDisponibile = timeSlotDisponibile;
	}
	public int getTimeSlotDellaSoluzione() {
		return timeSlotDellaSoluzione;
	}
	public void setTimeSlotDellaSoluzione(int timeSlotDellaSoluzione) {
		this.timeSlotDellaSoluzione = timeSlotDellaSoluzione;
	}

	
	
}
