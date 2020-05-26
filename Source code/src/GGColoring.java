import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

public class GGColoring {
	
	// VARIABLES
	private int maxBacktracks = 2000;
	private int currentBacktracks = 0;
	private int firstNExamsPerLargestDegree = 0;
	private List<Integer> saturationDegreePerExam;
	private int[] vettoreEsamiOrdinati;
	private List<Integer> orderedTimeslots;
	private List<Solution> population;
	private List<Integer> esamiAncoraDaMettere;
	private Solution bestSolution = null; // best initial sol
	private double bestFitness = 1000000000.0; // ideally + INF
	
	private FileManager fm;
	private int tMax = 0;
	private int nExam = 0;
	private int nStudents = 0;
	private int[][] M = null;
	private Map<Integer, List<Integer>> listAdj = new TreeMap<Integer, List<Integer>>();

	// CONSTRUCTOR
	public GGColoring(FileManager filem) {
		this.fm = filem;
		this.tMax = filem.getTMAX();
		this.nExam = filem.getNUM_EXAMS();
		this.nStudents = filem.getNUM_STUDENTS();
		M = filem.getConflictMatrix();
		this.listAdj = filem.getListAdj();
	}

	public List<Solution> getInitialPopulation(int populationSize) {
		
		// come prima cosa creo un vettore che abbia ordinati gli indici degli esami
		this.vettoreEsamiOrdinati = sortExamsPerLargestWeightedDegree(nExam, M);
		
		/**
		 *  ora creo un vettore che invece ordini i timeslot in maniera 
		 *  intelligente per aumentare la fitness della prima soluzione feasible 
		 */
		this.orderedTimeslots = getOrderedTimeslots(tMax);
		
		this.population = new LinkedList<Solution>();
		
		int maxTentativi = populationSize*3;
		int tentativi = 0;
		
		int populationCounter = 0;
		
		while(populationCounter < populationSize) {
			
			/**
			* 
			* Creo una popolazione di soluzioni iniziali
			*
			*
			*/

			Solution s = new Solution(nExam,tMax);
			
			esamiAncoraDaMettere = new LinkedList<Integer>();
			for(int i=0; i<this.nExam; i++) {
				esamiAncoraDaMettere.add(i);
			}
			
			this.currentBacktracks = 0;
			
			/**
			 * SATURATION DEGREE PER EXAM:
			 * 
			 * Struttura che tiene salvati il numero 
			 * di timeslot disponibili per gli esami ancora da 
			 * mettere, viene aggiornato ad ogni iterazione
			 *
			 */
			this.saturationDegreePerExam = new LinkedList<Integer>();
			for (int i= 0; i<this.nExam; i++)
				this.saturationDegreePerExam.add(tMax);
			
			
			// Inizia l'algoritmo ricorsivo per colorare il 
			// grafo con approccio greedy 
			
			if(ricorsione(0,this.M,tMax,s) == true) {
				
				if(this.currentBacktracks>this.maxBacktracks) {
					/*
						
						Limite nel numero di backtrack superato
						Riprova con un ordinamento di timeslot differente

					*/
					System.out.println("ATTENZIONE - Troppi backtrack effettuati. "
							+ "Ci si mette troppo con questo ordinamento oppure non è "
							+ "proprio possibile trovare una soluzione feasible.\" ");
				} else {
					
					/*
						Soluzione feasible trovata
						Controllo feasibility per sicurezza e calcolo fitness
					*/
					
					if(checkFeasibility(M, s)==true) {
						double fitness = computeFitness(s, this.nExam, this.M, this.tMax, this.nStudents);
						
						if(bestFitness > fitness) {
							bestFitness = fitness;
							bestSolution = new Solution(s.getS1(),s.getS2(),fitness);
						}

						Solution nuova = new Solution(s.getS1(),s.getS2(),fitness);
						population.add(nuova);
						populationCounter++;
						// System.out.println(nuova.getS1().toString() + " ----- " + nuova.getFitness());
						
					} else {
						/*
							ERRORE FATALE
						*/

						System.out.println(s.getS1().toString());	
						System.out.println("nBack = " + this.currentBacktracks);
						System.out.println("ATTENZIONE - La soluzione trovata in realtà NON E' FEASIBLE anche se lo pensavo");

						return null;						
					}
					
				}
				
			} else {
				// Non ho trovato soluzione feasible
				
				System.out.println("ATTENZIONE - Non sono riuscito a trovare la soluzione feasible a partire da questo ordinamento di esami");

			}

			tentativi++;
			if(tentativi >= maxTentativi) {
				/*
					Non sono riuscito a costruire una popolazione della cardinalità richiesta
				*/
				
				System.out.println("Non sono risucito a costruire una popolazione della cardinalità richiesta");
				break;
			}
			
			// Randomizza le soluzioni successive, ordinando casualmente i timeslot in cui mettere gli esami 
			Collections.shuffle(orderedTimeslots, new Random());

		}
			
		System.out.println("\n\nBEST FITNESS ----------"+bestFitness);
		return population;
	}
	
	private boolean ricorsione(int k, int[][] M, int tMax, Solution s) {
		
		if(k==this.nExam)
			return true;
		if(this.currentBacktracks>this.maxBacktracks)
			return true;
		
		
		// Tabu list con timeslots già provati:
		List<Integer> tabuList_timeslot = new LinkedList<Integer>();
		
		// Salva la satDegree attuale per ripristinarla in un eventuale backtrack
		List<Integer> oldSaturationDegreePerExam;
		
		/**Seleziono l'esameDaMettere per minima saturationDegree, 
		oppure se sono all'inizio li prendo in 
		ordine di largestWeightedDegree*/
		int esameDaMettere = -1;
		
		if (k>this.firstNExamsPerLargestDegree) { // siamo nel caso in cui tutti i t.slot hanno 1 esame
			//l'esame da mettere è il più piccolo indice presente in saturationDegreePerExam
			
			int ottimo = 90000;
		
			for(int i=0;i<this.saturationDegreePerExam.size(); i++) {
				if(this.saturationDegreePerExam.get(i)<ottimo) {
					ottimo = this.saturationDegreePerExam.get(i);
					esameDaMettere = i;
				}
			}
			
//			System.out.println("SAT.DEGREE = " + this.saturationDegreePerExam.toString());
//			if (test.contains((Integer) esameDaMettere)) {
//				System.out.println("test =  " + this.saturationDegreePerExam.get(esameDaMettere) + " exam = " + esameDaMettere);
//			} else {
//				test.add(esameDaMettere);
//			}
	
		}
		else {
			esameDaMettere = this.vettoreEsamiOrdinati[k]; // non ho problemi, ci sono t.slot liberi
		
		}
		
		// provo a mettere l'esameDaMettere in ognuno dei suoi timeSlot Disponibili:		
		for(int i=0;i<orderedTimeslots.size(); i++) {
			int timeslot = orderedTimeslots.get(i);
			if(isTimeSlotAvailableForExam(timeslot,esameDaMettere,s.getS2(),this.M)) {
				
				/**
				# Sto provando a metterlo nel timeslot t che è disponibile
				# Se alla fine NON riesco a trovare una soluzione feasible così, aggiungi questo timeslot
				# alla tabulist e non provare a liberare questo timeslot nella funzione tryMakingRoomForThisExam
				#
				 */
				
				tabuList_timeslot.add(timeslot);
				
				// salva la saturationDegree Attuale per un eventuale backtrack
				oldSaturationDegreePerExam = new LinkedList<Integer>(this.saturationDegreePerExam);
				
				// aggiorno la saturationDegree:
				
				for(int j : this.listAdj.get(esameDaMettere)) {
					if(s.getS1().get(j)==-1) {
						if(isTimeSlotAvailableForExam(timeslot, j, s.getS2(), M)) {
							int temp = this.saturationDegreePerExam.get(j);
							
							this.saturationDegreePerExam.set(j, temp-1);
						}
					}
				}
				
				
				this.saturationDegreePerExam.set(esameDaMettere, 10000);
//				if ( s.getS2().get(timeslot).contains((Integer)esameDaMettere)) {
//					System.out.println("lo conteneva già TS=" + timeslot + " esame = " + esameDaMettere);
//					System.out.println(s.getS2().toString());
//				}
				
				s.getS2().get(timeslot).add(esameDaMettere);
				s.getS1().replace(esameDaMettere, s.getS1().get(esameDaMettere), timeslot);
				this.esamiAncoraDaMettere.remove((Integer)esameDaMettere);
			
			
				if(ricorsione(k+1, M, tMax,s)==true) {
					return true;
				}
				else {
					
					/**
					 * Mettendo l'esame in questo timeslot sono arrivato in un punto di stallo e non son più potuto andare avanti.
					   Faccio backtrack e provo con il prossimo timeslot
					 */
					
					s.getS2().get(timeslot).remove((Integer)esameDaMettere);
					s.getS1().replace(esameDaMettere, s.getS1().get(esameDaMettere), -1);
					esamiAncoraDaMettere.add(esameDaMettere);
					
					this.saturationDegreePerExam = oldSaturationDegreePerExam;
					
				}
			}
		}
		
		/**
		 *  Se arrivo qui vuol dire che non esiste un timeslot disponibile in cui mettere "esameDaMettere"
		# o quelli disponibili li ho già provati e la ricorsione è tornata in backtrack.
		# Provo ad agire spostando degli esami già messi e provando a far spazio in un timeslot qualsiasi per questo esameDaMettere
		 */
		
		
		oldSaturationDegreePerExam = new LinkedList<>(this.saturationDegreePerExam);
		
		Map<Integer,Integer> olds1 = new HashMap<Integer,Integer>(s.getS1());
		Map<Integer, List<Integer>> olds2 = new HashMap<Integer,List<Integer>>();
		
		for(Map.Entry<Integer, List<Integer>> entry : s.getS2().entrySet()) {
			olds2.put(entry.getKey(), new LinkedList<Integer>(entry.getValue()));
		}
		
		if(tryMakingRoomForThisExam(M,tMax,nExam,esameDaMettere,tabuList_timeslot, s, k)==true) {
			
			// si è trovato un modo per spostare degli esami e far spazio all'esameDaMettere
			
			if(ricorsione(k+1, M, tMax,s)==true) {
				return true;
			}
			else {
				
				//backtrack:
				s.setS1(olds1);
				s.setS2(olds2);
				this.esamiAncoraDaMettere.add(esameDaMettere);
				this.saturationDegreePerExam = oldSaturationDegreePerExam;
				
			}
			
		}
		
		currentBacktracks++;		
		return false;
	}
	
	public Solution unscheduleSomeRandomExams(Solution startingSolution, int numberOfExamsToUnschedule) {
		
		Map<Integer,Integer> cloneS1 = new HashMap<Integer,Integer>(startingSolution.getS1());
		Map<Integer, List<Integer>> cloneS2 = new HashMap<Integer,List<Integer>>();
		
		for(Map.Entry<Integer, List<Integer>> entry : startingSolution.getS2().entrySet()) {
			cloneS2.put(entry.getKey(), new LinkedList<Integer>(entry.getValue()));
		}
		
		Solution halfSolution = new Solution(cloneS1, cloneS2, startingSolution.getFitness(fm));
		this.esamiAncoraDaMettere = new LinkedList<Integer>();


		List<Integer> esamiDaTogliere = new LinkedList<Integer>();
		for (int i=0; i<this.nExam; i++)
			esamiDaTogliere.add(i);
		Collections.shuffle(esamiDaTogliere, new Random());

		int currentTimeslot = -1;

		for (int i=0; i < numberOfExamsToUnschedule; i++) {
			//Unschedule exam i

			currentTimeslot = halfSolution.getS1().get(esamiDaTogliere.get(i));

			halfSolution.getS2().get(currentTimeslot).remove((Integer) esamiDaTogliere.get(i));
			halfSolution.getS1().replace(esamiDaTogliere.get(i), -1);

			this.esamiAncoraDaMettere.add(esamiDaTogliere.get(i));
		}

		//Calcolo la saturationDegreePerExam per questa nuova situazione
		this.saturationDegreePerExam = new LinkedList<Integer>();
		for (int i=0; i<this.nExam; i++)
			this.saturationDegreePerExam.add(10000);

		List<Integer> initialAvailableTimeSlot = new LinkedList<Integer>();
		for(int i=0; i<this.tMax; i++)
			initialAvailableTimeSlot.add(i);

		List<Integer> availableTimeSlot;
		int saturationDegreeNumber;

		for (Integer esameAncoraDaMettere : this.esamiAncoraDaMettere) {

			availableTimeSlot = new LinkedList<Integer>(initialAvailableTimeSlot);
			saturationDegreeNumber = this.tMax;

			for(Integer e2 : this.listAdj.get(esameAncoraDaMettere)) {
				if(halfSolution.getS1().get(e2) != -1) {
					if( availableTimeSlot.contains((Integer)halfSolution.getS1().get(e2)) ) {
						availableTimeSlot.remove((Integer)halfSolution.getS1().get(e2));
						saturationDegreeNumber--;
					}

				}
			}

			this.saturationDegreePerExam.set(esameAncoraDaMettere, saturationDegreeNumber);
		}

		return halfSolution;
	}

	public Solution escapeFromLocalMinimum(Solution startingSolution, double percentage) {

		// come prima cosa creo un vettore che abbia ordinati gli indici degli esami
		// this.vettoreEsamiOrdinati = sortExamsPerLargestWeightedDegree(nExam, M);
		
		/**
		 *  ora creo un vettore che invece ordini i timeslot in maniera 
		 *  intelligente per aumentare la fitness della prima soluzione feasible 
		 */
		this.orderedTimeslots = new LinkedList<Integer>();
		for (int i=0; i<this.tMax; i++)
			this.orderedTimeslots.add(i);
		Collections.shuffle(this.orderedTimeslots, new Random());

		int numberOfExamsToUnschedule = (int) Math.ceil(this.nExam*percentage);
		int numberOfScheduledExams = this.nExam-numberOfExamsToUnschedule;
		
//		System.out.println(startingSolution.getSolArray().toString());
//		System.out.println(startingSolution.getS1().toString());
//		System.out.println("S2pre " + startingSolution.getS2().toString());
//		int sum=0;
//		for(Map.Entry<Integer, List<Integer>> entry : startingSolution.getS2().entrySet() ) {
//			sum += entry.getValue().size();
//		}
//		System.out.println("S2pre sum = "+sum); 
		
		Solution halfSolution = unscheduleSomeRandomExams(startingSolution, numberOfExamsToUnschedule);
		
//		System.out.println("numberOfExamsToUnschedule = " + numberOfExamsToUnschedule + "\t numberOfScheduledExams = " + numberOfScheduledExams);
//		System.out.println(this.saturationDegreePerExam.toString());
//		System.out.println(halfSolution.getSolArray().toString());
//		System.out.println(halfSolution.getS1().toString());
		
//		System.out.println("S2post" + halfSolution.getS2().toString());
	
//		sum=0;
//		for(Map.Entry<Integer, List<Integer>> entry : halfSolution.getS2().entrySet() ) {
//			sum += entry.getValue().size();
//		}
//		System.out.println("S2post sum = "+sum); 
		
		
//		System.out.println("DaMettere: " + this.esamiAncoraDaMettere.toString());
		
//		System.out.println("--------------");
		
		this.currentBacktracks = 0;
		this.firstNExamsPerLargestDegree = -1;
		this.maxBacktracks = 20; // statisticamente con 50/100 si ottengono risultati migliori
		
		/*
			Trova una soluzione con approccio Greedy del Graph Coloring, reinserendo gli esami deschedulati
		*/
		if(ricorsione(numberOfScheduledExams, this.M, this.tMax, halfSolution) == true) {
			
			if(this.currentBacktracks > this.maxBacktracks) {
				/*
					
					Limite nel numero di backtrack superato
					Riprova con un ordinamento di timeslot differente

				*/
//				System.out.println("numero di backtrack superato");
				return null;
			} else {
				
				/*
					Soluzione feasible trovata
					Controllo feasibility per sicurezza e calcolo fitness
				*/
				
				if(checkFeasibility(this.M, halfSolution)==true) {
					double fitness = computeFitness(halfSolution, this.nExam, this.M, this.tMax, this.nStudents);

					//System.out.println("nBack = " + this.currentBacktracks);
					return new Solution(halfSolution.getS1(), halfSolution.getS2(), fitness);
					
				} else {
					/*
						ERRORE FATALE
					*/

					System.out.println(halfSolution.getS1().toString());						
					System.out.println("ATTENZIONE - La soluzione trovata in realtà NON E' FEASIBLE anche se lo pensavo");

					return null;
				}
				
			}
			
		} else {
			// Non ho trovato soluzione feasible
			System.out.println("ATTENZIONE - Non sono riuscito a trovare la soluzione feasible a partire da questo ordinamento di esami");
			return null;
		}
	}
	
	private boolean checkFeasibility(int[][] M, Solution s) {
		
		int nExamPresenti = 0;
		for(int i=0; i<s.getS2().size(); i++) {
			List<Integer> temp = s.getS2().get(i);
			for(int j=0; j<temp.size();j++) {
				int esame = temp.get(j);
				nExamPresenti++;
				int somma = 0;
				int sum = 0;
				for(int l=0; l<temp.size();l++) {
					somma += M[esame][temp.get(l)];
					if(M[esame][temp.get(l)] > 0)
						sum++;
				}
				if(somma!=0) {
					System.out.println("[1-] " + somma + " sum = " + sum);
					return false;
				}
				
			}
		}
		if(nExamPresenti!=this.nExam) {
			System.out.println("[2-] " + nExamPresenti + " nEXAM = " + this.nExam);
			return false;
		}
		for(int i= 0; i<s.getS1().size();i++) {
			int t = s.getS1().get(i);
			if(t==-1) {
				System.out.println("[3-]");
				return false;
			}
		}
		
		return true;
	}
	
	private double computeFitness(Solution s, int nExam, int[][]M, int tMax, int nStudents) {
		
		double fitness = 0.0;
		for(int i=1; i<nExam; i++) {
			for(int j=0; j<i; j++) {
				if(M[i][j]!=0) {
					double d = Math.abs(s.getS1().get(i)-s.getS1().get(j));
					if(d<=5) {
						fitness += Math.pow(2, (5-d))*M[i][j];
					}
				}
			}
		}
		
		return fitness/this.nStudents;
	}
	
	
	private boolean tryMakingRoomForThisExam(int[][] M, int tMax, int nExam, int esameDaMettere,
		List<Integer> tabuList_timeslot, Solution s, int k) {
		
		//System.out.println("E capitato ora il tryMakingRoom");

		/*

			Trovo i timeslot disponibili per ogni esame ancora non messo
		
		*/
		Map<Integer, List<Integer>> availableTimeSlotPerExam = new HashMap<Integer, List<Integer>>();
		List<Integer> listaIniziale = new LinkedList<Integer>();
		for(int i= 0; i<nExam; i++)
			availableTimeSlotPerExam.put(i, listaIniziale);

		for(int esDaMett:this.esamiAncoraDaMettere) {
			
			for(int j=0; j<tMax; j++)
				availableTimeSlotPerExam.get(esDaMett).add(1);

			for(int e=0; e<listAdj.get(esDaMett).size();e++) {
				int inConflitto = listAdj.get(esDaMett).get(e);
				if(s.getS1().get(inConflitto)!= -1) {
					int slotDaCambiare = s.getS1().get(inConflitto);

					List<Integer> temporaneamente = new LinkedList<Integer>(availableTimeSlotPerExam.get(esDaMett));				
					temporaneamente.set(slotDaCambiare, 0);
					
					availableTimeSlotPerExam.put(esDaMett, temporaneamente);
				}
			}
		}


		/*
			
			Trovo il numero e la lista di esami in conflitto
			che l'esame da mettere ha in ogni timeslot

		*/
		List<Integer> nConflittiPerTimeslot = new LinkedList<Integer>();
		for(int i=0; i<tMax; i++)
			nConflittiPerTimeslot.add(0);

		Map<Integer, List<Integer>> conflictsPerTimeslot = new HashMap<Integer, List<Integer>>();
		List<Integer> listaIniziale2 = new LinkedList<Integer>();
		for(int i= 0; i<tMax; i++)
			conflictsPerTimeslot.put(i, listaIniziale2);		
		
		for(int esameInConflitto : listAdj.get(esameDaMettere)) {
			
			if(s.getS1().get(esameInConflitto)!=-1) {
				int temp = s.getS1().get(esameInConflitto);
				int precedente = nConflittiPerTimeslot.get(temp);
				nConflittiPerTimeslot.set(temp, precedente+1);
			
				List<Integer> temporanea = new LinkedList<Integer>(conflictsPerTimeslot.get(temp));
				temporanea.add(esameInConflitto);
				conflictsPerTimeslot.replace(temp, temporanea);
		
			}
		}



		/*

			Ordina i timeslot per numero crescente di conflitti con l'esame da mettere

		*/
		Integer[] vettoreVecchio = new Integer [nConflittiPerTimeslot.size()];
		for (int i=0; i<vettoreVecchio.length; i++)
			vettoreVecchio[i] = nConflittiPerTimeslot.get(i);

		Integer[] vettoreNuovo = new Integer [nConflittiPerTimeslot.size()];
		vettoreNuovo = myArgsort(vettoreVecchio);		
		List<Integer> timeSlotOrdered = new LinkedList<Integer>();
		
		for (int i=vettoreNuovo.length-1; i>-1; i--)
			timeSlotOrdered.add(vettoreNuovo[i]);
				


		/*

			Non considero tutti i timeslot per mettere questo esame
			(alcuni magari li ho già provati e sono tornati in backtrack)
		
		*/
		for(int i=0; i<tabuList_timeslot.size();i++) {
			Integer t = tabuList_timeslot.get(i);
			timeSlotOrdered.remove(t);
		}



		/*
			
			Provo a liberare un timeslot per far spazio all'esame da mettere,
			partendo dal timeslot con meno conflitti presenti.

		*/
		List<Integer> availableTimeSlot;
		
		for(Integer t : timeSlotOrdered) {	
			boolean flagSpostatiTutti = true;
			List<Spostamento> spostamenti = new LinkedList<Spostamento>();
			Set<Integer> timeSlotInteressati = new HashSet<Integer>();

			for(Integer e : conflictsPerTimeslot.get(t)) {
				
				// Trova i suoi timeSlot disponibili oltre a quello in cui è già
				availableTimeSlot = new LinkedList<Integer>();
				for(int j=0; j<tMax; j++)
					if(j!=t)
						availableTimeSlot.add(j);

				for(Integer e2 : listAdj.get(e)) {
					if(s.getS1().get(e2)!=-1) {
						try {
							availableTimeSlot.remove(s.getS1().get(e2));
						}
						catch (Exception ValueError) {
							// Ignore
						}						
					}
				}

				if(availableTimeSlot.size() > 0) {

					/**
					 * Sposta l'esame "e" in uno qualsiasi dei suoi timeslot disponibili
					 * così da iniziare a liberare il timeslot t dai conflitti esistenti
					 * con l'esameDaMettere
					 * 
					 * Mi salvo gli spostamenti fatti per tornare indietro qualora non 
					 * riuscissi a togliere tutti i conflitti.
					 */
					Spostamento spostamento = new Spostamento(e, availableTimeSlot.get(0), s.getS1().get(e));
					spostamenti.add(spostamento);
					timeSlotInteressati.add(availableTimeSlot.get(0));
					timeSlotInteressati.add(s.getS1().get(e));

					s.getS2().get(s.getS1().get(e)).remove(e);
					s.getS2().get(availableTimeSlot.get(0)).add(e);
					
					s.getS1().replace(e, availableTimeSlot.get(0));
				
				}
				else {
					/**
					 * Questo esame non posso spostarlo da nessuna altra parte,
					 * riporto tutti gli esami precedentemente spostati
					 * di nuovo qui.
					 */
					if(spostamenti.size()>0) {
						for(Spostamento sp : spostamenti) {
							s.getS2().get(sp.getTimeSlotDisponibile()).remove((Integer)sp.getEsame());
							s.getS2().get(sp.getTimeSlotDellaSoluzione()).add(sp.getEsame());
							s.getS1().replace(sp.getEsame(), sp.getTimeSlotDellaSoluzione());
						}
					}
					flagSpostatiTutti = false;
					break;
				}
			}
			
			if(flagSpostatiTutti==true) {
				/**
				 * Ho liberato con successo questo timeslot:
				 * Ci metto dentro l'esameDaMettere
				 */
				s.getS2().get(t).add(esameDaMettere);
				s.getS1().replace(esameDaMettere, t);
				this.esamiAncoraDaMettere.remove((Integer)esameDaMettere);
				
				int tempora = this.saturationDegreePerExam.get(esameDaMettere);
			
				this.saturationDegreePerExam.set(esameDaMettere, tempora+10000);

				/**
				 * Devo aggiornare i saturationDegree degli esami 
				 * non ancora messi in base agli spostamenti fatti
				 * per liberare spazio
				 */

				for(int eIdex : this.esamiAncoraDaMettere) {
					
					for(int timeslot : timeSlotInteressati) {
						boolean oraPosso = isTimeSlotAvailableForExam(timeslot, eIdex, s.getS2(), M);
						int change = 0;
						if(oraPosso == true) {

							if(availableTimeSlotPerExam.get(eIdex).contains(timeslot))
								change = 0; // Ora posso e potevo anche prima
							else
								change = 1; // Ora posso e prima non potevo
						}
						else {

							if(availableTimeSlotPerExam.get(eIdex).contains(timeslot))
								change = -1; // prima potevo e ora non posso
							else
								change = 0; // prima non potevo e ora non posso
						}
						int tea = this.saturationDegreePerExam.get(eIdex);						
						this.saturationDegreePerExam.set(eIdex, tea+change);
					}
				}

				return true;
			}


		}

		return false;
	}

	// ritorna vero se è disponibile un timeslot
	private boolean isTimeSlotAvailableForExam(int timeslot, int esameDaMettere, Map<Integer, List<Integer>> s2 , int[][] M) {
		
//		System.out.println(timeslot + "\n" + s2.toString());

		for(int esameCorrente : s2.get(timeslot)) {
			
			if(M[esameDaMettere][esameCorrente]!=0) {
				return false;
			}
		}

		return true;
	}

	/**
	# Ordina i timeslots in maniera "intelligente" per posizionare un esame a distanza di 6 timeslots
	# quando viene trovato un timeslot in conflitto
	# Es. tMax = 10 [0,1,2,3,4,5,6,7,8,9] ---> [0,6,3,9, etc.]
	# NB: questo vale solo per la prima soluzione generata, le altre vengono piazzate in timeslots ordinati casualmente
	 */

	private List<Integer> getOrderedTimeslots(int tMax) {
	
		int N = getNumeroN(tMax);
		
		int currPos = 0; 
		
		List<Integer> orderedTimeslots = new LinkedList<Integer>();
		
		this.firstNExamsPerLargestDegree = 0;
		
		
		for(int i=0; i<(N/3); i++) {
			int currActualTimeslot = (currPos+(6*i))%N;
			if(currActualTimeslot >= tMax) {
				continue;
			}
			orderedTimeslots.add(currActualTimeslot);
			this.firstNExamsPerLargestDegree += 1;
			
		}
		for(int i=0; i<N+1; i++) {
			int currActualTimeslot = (currPos+(8*i))%N;
			if((currActualTimeslot%3 == 0)||(currActualTimeslot>=tMax)) {
				continue;
			}
			else
				orderedTimeslots.add(currActualTimeslot);			
		}		
		return orderedTimeslots;
	}

	/**
	 * Trova un numero N che permetta di ordinare i timeslots di 6 
	 * in 6 con l'approccio del resto delle divisioni.
	 * N è intero t.c. N = i*6 + 3, con i più piccolo possibile e N >= tMax
	 */

	private int getNumeroN(int tMax) {
		
		if(tMax<7) {
			return 9;
		}
		else {
			int resto = tMax % 6;
			if (resto == 0) {
				return tMax+3;
			}
			else {
				if(resto<3)
					return (tMax/6*6 +3);
				else if(resto == 3)
					return tMax;
				else
					return (tMax/6*6 +9);
			}
		}
	}

	private int []  sortExamsPerLargestWeightedDegree(int nExam, int[][] M) {

		// Creo un vettore della dimensione di nExam e lo inizializzo a zero:
		
		int [] weightedDegrees = new int[nExam];
		for(int i=0; i<weightedDegrees.length; i++) {
			weightedDegrees[i]=0;
		}
		
		for (int k=1; k<nExam; k++) {
			for (int j=1; j<k; j++) {
				if (M[k][j] != 0) {
					weightedDegrees[k] += M[k][j];
					weightedDegrees[j] += M[k][j];
				}
			}
		}
		
		int [] indexes = new int[nExam];
		for(int i=0; i<weightedDegrees.length; i++) {
			indexes[i]=i;
		}
		
		for(int i=0; i<nExam-1; i++) {
			int max = i;
			for(int j=i; j<nExam; j++) {
				if(weightedDegrees[indexes[j]]>weightedDegrees[indexes[max]]) {
					max=j;
				}
			}
			if (max!=i) {
				int temp = indexes[max];
				indexes[max] = indexes[i];
				indexes[i] = temp;
			}
		}

		return indexes;
	}
	
	private static Integer[] myArgsort(Integer[] contributi) {
		Integer[] contributiIndexes = new Integer[contributi.length];
		
		for(int i=0; i<contributi.length; i++)
			contributiIndexes[i]=i;
		
		for(int i=0; i<contributi.length-1; i++) {
			int curr_max = i;
			for(int j=i+1; j<contributi.length; j++) {
				if(contributi[contributiIndexes[j]] > contributi[contributiIndexes[curr_max]]) {
					curr_max = j;
				}
			}

			int temp = contributiIndexes[curr_max];
			contributiIndexes[curr_max] = contributiIndexes[i];
			contributiIndexes[i] = temp;
		}
		
		return contributiIndexes;
	}
	
	public Solution getBestSolution() {
		return this.bestSolution;
	}

}
