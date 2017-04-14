import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class PlayerSkeleton {
    
    //weights
    double a = -9.665744834427922; //landingHeight
    double b = 19.817566268489543; //completeLines
    double c = -5.992424473345767; //rowTransitions
    double d = -21.322795310814993; //colTransitions
    double e = -28.252131468783272; //holes
    double g = -10.117947734955948; //wellSum
    
    
    /* Variables required for GA
    static long seed = 0;
    static BufferedWriter bw = null;
    
    static AI ai;
    */

	//implement this function to have a working system
	public int[] pickMove(State s, int[][] legalMoves) {
	    double bestEvaluation = f(s, legalMoves[0]);
	    int[] bestMove = legalMoves[0];	    
	    
	    for (int i = 1; i < legalMoves.length; i++) {
	        double evaluation = f(s, legalMoves[i]);
	        if (bestEvaluation < evaluation) {
	            bestEvaluation = evaluation;
	            bestMove = legalMoves[i];
	        }
	    }
	    //System.out.println("=== Best Move: " + bestMove[0] + "," + bestMove[1] + " ===");
	    return bestMove;
	}
	
	public double f(State s, int[] move) {
	    int[][] field = copy(s.getField());
	    int[] top = simulateField(s, field, move[0], move[1]);
	    
	    //heuristics
	    double landingHeight = getLandingHeight(top, s.getNextPiece(), move[0], move[1]);
	    double completeLines = getCompleteLines(field); //number of lines completed
	    double rowTransitions = getRowTransitions(field);
	    double colTransitions = getColTransitions(field);
	    double holes = getHoles(field, top); //number of holes present
	    double wellSum = getWellSum(field);
	    
	    double f = a * landingHeight + b * completeLines + c * rowTransitions + d * colTransitions + e * holes + g * wellSum;
	    return f;
	}
	
	public int[] simulateField(State s, int[][] field, int orient, int slot) {
	    int[] top = copy(s.getTop());
	    int nextPiece = s.getNextPiece();
	    
	    //height if the first column makes contact
        int height = top[slot]-State.getpBottom()[nextPiece][orient][0];
        //for each column beyond the first in the piece
        for(int c = 1; c < State.getpWidth()[nextPiece][orient];c++) {
            height = Math.max(height,top[slot+c]-State.getpBottom()[nextPiece][orient][c]);
        }
        
        //for each column in the piece - fill in the appropriate blocks
        for(int i = 0; i < State.getpWidth()[nextPiece][orient]; i++) {
            
            //from bottom to top of brick
            for(int h = height+State.getpBottom()[nextPiece][orient][i]; h < height+State.getpTop()[nextPiece][orient][i]; h++) {
                if (h < field.length && i + slot < field[0].length) {
                    field[h][i+slot] = 1;
                }
            }
        }
        
        //adjust top
        for(int c = 0; c < State.getpWidth()[nextPiece][orient]; c++) {
            top[slot+c]=height+State.getpTop()[nextPiece][orient][c];
        }
    
	    return top;
	}
	
	//The height that the piece will land at
	public double getLandingHeight(int[] top, int nextPiece, int orient, int slot) {
	    //get placement row
	    int row = 0;
	    int curCol = slot;
	    int pieceWidth = State.getpWidth()[nextPiece][orient];
	    while (pieceWidth-- > 0) {
	        if (top[curCol] > row) {
	            row = top[curCol];
	        }
	        curCol++;
	    }
	    
	    if (row >= State.ROWS) row = 1000;
	    return row;
	}
	
	//The number of empty cells that are adjacent to a filled cell in the same row
	public int getRowTransitions(int[][] field) {
	    int transitions = 0;
	    
	    for (int i = 0; i < field.length; i++) {
	        int lastBit = 1;
	        int bit = -1;
	        for (int j = 0; j < field[i].length; j++) {
	            if (field[i][j] > 0) bit = 1;
	            else bit = 0;
	            
	            if (bit != lastBit) {
	                transitions++;
	            }
	            lastBit = bit;
	        }
	        
	        if (bit == 0) {
	            transitions++;
	        }
	    }
	    return transitions-2;
	}
	
	// The number of empty cells that are adjacent to a filled cell in the same column
	public int getColTransitions(int[][] field) {
	    int transitions = 0;
        
        for (int i = 0; i < field[0].length; i++) {
            int lastBit = 1;
            int bit = -1;
            for (int j = 0; j < field.length; j++) {
                if (field[j][i] > 0) bit = 1;
                else bit = 0;
                
                if (bit != lastBit) {
                    transitions++;
                }
                lastBit = bit;
            }
            
            if (bit == 0) {
                transitions++;
            }
        }
	    
        return transitions-10;
	}
	
	// The number of lines cleared
	public double getCompleteLines(int[][] field) {
	    int sum = 0;
        for (int i = 0; i < field.length; i++) {
            boolean isFull = true;
            for (int j = 0; j < field[i].length; j++) {
                if (field[i][j] == 0)
                    isFull = false;
            }
            
            if (isFull) sum++;
        }
        return sum;
	}
	
	//The number of holes (considered a hole if a filled cell is on top os an empty cell)
	public int getHoles(int[][] field, int[] top) {
	    int holes = 0;
	    for (int i = 0; i < field[0].length; i++) {	        
	        for (int j = 0; j < field.length; j++) {
	            if (j >= top[i]) {
	                break;
	            }
	            if (field[j][i] == 0) {
	                holes++;
	            }
	        }
	    }
	    return holes;
	}
	
	//Measures how deep an area with empty cells is
	public int getWellSum(int[][] field) {
	    int wellSum = 0;
	    //inner well
	    for (int i = 1; i < field[0].length - 1; i++) {
	        for (int j = field.length - 1; j >= 0; j--) {
	            if ((field[j][i] == 0) && (field[j][i-1] != 0) && (field[j][i+1] != 0)) {
	                wellSum++;
	                
	                for (int k = j - 1; k >= 0; k--) {
	                    if (field[k][i] == 0) {
	                        wellSum++;
	                    } else {
	                        break;
	                    }
	                }
	            }
	        }
	    }
	    
	    //left well
	    for (int j = field.length - 1; j >= 0; j--) {
	        if ((field[j][0] == 0) && (field[j][1] != 0)) {
                wellSum++;
                
                for (int k = j - 1; k >= 0; k--) {
                    if (field[k][0] == 0) {
                        wellSum++;
                    } else {
                        break;
                    }
                }
            }
	    }
	    
	    //right well
	    for (int j = field.length - 1; j >= 0; j--) {
            if ((field[j][field[j].length-1] == 0) && (field[j][field[j].length-2] != 0)) {
                wellSum++;
                
                for (int k = j - 1; k >= 0; k--) {
                    if (field[k][field[k].length-1] == 0) {
                        wellSum++;
                    } else {
                        break;
                    }
                }
            }
        }
	    return wellSum;
	}
	
    public int[][] copy(int[][] toCopy) {
        int[][] copy = new int[toCopy.length][toCopy[0].length];
        
        for (int i = 0; i < toCopy.length; i++) {
            for (int j = 0; j < toCopy[i].length; j++) {
                copy[i][j] = toCopy[i][j];
            }
        }
        return copy;
    }
    
    public int[] copy(int[] toCopy) {
        int[] copy = new int[toCopy.length];
        
        for (int i = 0; i < toCopy.length; i++) { 
            copy[i] = toCopy[i];
        }
        return copy;
    }
	
    //Original
    public static void main(String[] args) {
        while (true) {
        State s = new State();
        new TFrame(s);
        PlayerSkeleton p = new PlayerSkeleton();
        while(!s.hasLost()) {
            s.makeMove(p.pickMove(s,s.legalMoves()));
            s.draw();
            s.drawNext(0,0);
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("You have completed "+s.getRowsCleared()+" rows.");
        }
    }

    /* Codes for Genetic Algorithm
     * Need to add a seed to State constructor 
	public static void main(String[] args) throws Exception {
		File file = null;
		FileWriter fw = null;
		try {
			file = new  File("GAOut.txt");
	        // if file doesnt exists, then create it
	        if (!file.exists()) {
	            file.createNewFile();
	        }
	        fw = new FileWriter(file.getAbsoluteFile(), true);
	        bw = new BufferedWriter(fw);
		} catch (IOException e) {
	        e.printStackTrace();
	    }
        

		ai = new AI();
		while(true){
		    //seed the ai with 5 random long values for each generation
		    //can increase this value to make GA run more games
            seed = System.currentTimeMillis();
            long[] seeds = new long[5];
            Random r = new Random(seed);
            for (int i = 0; i < seeds.length; i++) {
                seeds[i] = r.nextLong();
            }
            System.out.println("Generation " + ai.generation + " seed:" + seed);
           
            //run each chromosome in parallel
            ai.chromosomes.stream()
                .parallel()
                .forEach(i -> runTetris(ai.chromosomes.indexOf(i), seeds));
                
            //once all chromosomes have finished running, declare a new generation
            ai.newGeneration();
		}
	}
	
	//method to run headless
	public static void runTetris(int i, long[] seeds) {
	    int total = 0;
	    for (int j = 0; j < seeds.length; j++) {
    	    State s = new State(seeds[j]);
            PlayerSkeleton p = new PlayerSkeleton();
            ai.setAIValues(p, i);
            
            while(!s.hasLost()) {
                s.makeMove(p.pickMove(s,s.legalMoves()));
            }
            if(s.getRowsCleared()>0){
                total += s.getRowsCleared();
            }
	    }
        String val = ai.sendScore(total, i) + "\n";
        try {
            bw.write(val);
            bw.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
	}*/
	
}

/*//Classes for Genetic Algorithm
public class AI {
    // If false, just use the default values
    final boolean USE_GENETIC = true;
    
    // Start from generation 1
    int generation = 1;
    
    // How many candidates are there in a generation?
    // Must be a multiple of 4.
    final int population = 16;
    
    // Chromosomes mutatation rate
    double mutation_rate = 0.05;
    
    // A chromosome is just an array of 6 doubles.
    ArrayList<Gene> chromosomes = new ArrayList<Gene>();

    public AI() {

        // Randomize starting chromosomes with values between -10 and 0.
        for (int i = 0; i < population; i++) {
            chromosomes.add(new Gene());
            for (int j = 0; j < 6; j++) {
                chromosomes.get(i).weights[j] = Math.random() * 10 - 10;
            }
        }

    }

    void newGeneration() {
        Random r = new Random(System.currentTimeMillis());
        // Calculate average fitness
        int[] scores_ = new int[population];
        for (int i = 0; i < chromosomes.size(); i++) {
            scores_[i] = chromosomes.get(i).score;
        }
        Arrays.sort(scores_);
        System.out.println("Generation " + generation
                + "; min = " + scores_[0]
                + "; med = " + scores_[population / 2]
                + "; max = " + scores_[population - 1]);

        List<double[]> winners = new ArrayList<double[]>();

        // Pair 1 with 2, 3 with 4, etc.
        for (int i = 0; i < population; i += 2) {

            // Pick the more fit of the two pairs
            int c1score = chromosomes.get(i).score;
            int c2score = chromosomes.get(i+1).score;
            int winner = c1score > c2score ? i : i + 1;

            // Keep the winner, discard the loser.
            winners.add(chromosomes.get(winner).weights);
        }


        List<double[]> new_population = new ArrayList<double[]>();

        // Pair up two winners at a time
        for (int i = 0; i < winners.size(); i += 2) {
            double[] winner1 = winners.get(i);
            double[] winner2 = winners.get(i + 1);
            
            // Generate four new offspring
            for (int off = 0; off < 4; off++) {

                double[] child = new double[6];

                // Pick at random a mixed subset of the two winners and make it the new chromosome
                for (int j = 0; j < 6; j++) {
                    int gen = r.nextInt(2);
                    //System.out.print(gen + ",");
                    child[j] = gen == 1 ? winner1[j] : winner2[j];

                    // Chance of mutation
                    boolean mutate = r.nextDouble() < mutation_rate;
                    if (mutate) {
                        // Change this value anywhere from -10 to 10
                        double change = r.nextDouble() * 20 - 10;
                        child[j] += change;
                    }
                }
                new_population.add(child);
            }
        }

        // Shuffle the new population.
        Collections.shuffle(new_population, new Random());

        // Copy them over
        for (int i = 0; i < population; i++) {
            for (int j = 0; j < 6; j++) {
                chromosomes.get(i).weights[j] = new_population.get(i)[j];
            }
        }

        System.out.println("Population size: " + chromosomes.size());
        generation++;

    }

    void setAIValues(PlayerSkeleton ai, int index) {
        if (!USE_GENETIC) {
            return;
        }

        ai.a = chromosomes.get(index).weights[0];
        ai.b = chromosomes.get(index).weights[1];
        ai.c = chromosomes.get(index).weights[2];
        ai.d = chromosomes.get(index).weights[3];
        ai.e = chromosomes.get(index).weights[4];
        ai.g = chromosomes.get(index).weights[5];
    }

    String sendScore(int score, int index) {
        if (!USE_GENETIC) {
            return "";
        }

        String s = aToS(chromosomes.get(index).weights);
        s = "Generation " + generation + "; Candidate " + (index + 1) + ": " + s + " Score = " + score;
        System.out.println(s);
        chromosomes.get(index).score = score;
        return s;
    }

    // Double array to string
    private String aToS(double[] a) {
        String s = "";
        for (int i = 0; i < a.length; i++) {
            s += Double.toString(a[i]);
            if (i != a.length - 1) {
                s += ", ";
            }
        }
        return "[" + s + "]";
    }
}

class Gene {
    double[] weights;
    int score;
    
    public Gene() {
        weights = new double[6];
        score = 0;
    }
}*/
