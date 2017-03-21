import java.util.*;
public class PlayerSkeleton {
    
    //key is top[] values concat into a String with nextPiece as the last character
    //value is the reward
    HashMap<String, Double[]> rewardMap = new HashMap<String, Double[]>();
    Stack<Tuple> backtrack = new Stack<Tuple>();
    double reward = 0;

	//implement this function to have a working system
	public int[] pickMove(State s, int[][] legalMoves) {
	   int[] top = s.getTop();
	        
	    int max = top[0];
	    for (int i = 1; i < top.length; i++) {
	        max = Math.max(max, top[i]);
	    }
	    
	    int front = max - 6; //find lowest the frontier
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < top.length; i++) {
            int next = top[i] - front;
            if (next <= 0) next = 1;
            sb.append(next-1); //ensure everything is 0-based
        }
        sb.append(s.getNextPiece());
	        
	    String key = sb.toString();
	    Double[] r = rewardMap.get(key);
	    int[] moveToMake;
	    if (r == null) {
	        moveToMake = bestMove(s, legalMoves);
	        //System.out.println("Making the greedy move");
	    } else {
	        System.out.println("Making my optimal move");
	        moveToMake = new int[2];
	        moveToMake[0] = r[0].intValue();
	        moveToMake[1] = r[1].intValue();
	    }
	    
	    int[][] field = copy(s.getField());
        simulateField(s, field, moveToMake[0], moveToMake[1]);
        int completeLines = (int) getCompleteLines(field);
        backtrack.push(new Tuple(key, moveToMake));
        
	    calculatePayoff(completeLines);
	    
	    reward -= 0.1;
	    
	    return moveToMake;
	   
	}
	
	public void calculatePayoff(double payoff) {
	    if (payoff > 0) {
	        //System.out.println("Payday!");
	        while (!backtrack.isEmpty()) {
    	        Tuple t = backtrack.pop();
    	        Double[] r = rewardMap.get(t.key);
                Double[] v = { (double) t.move[0], (double) t.move[1], payoff };
    	        if (r == null) {
    	            rewardMap.put(t.key, v);
    	        } else {
    	            //if current reward is smaller than new payoff
    	            if (r[2] < payoff) {
    	                rewardMap.put(t.key, v);
    	            }
    	            
                    //reinforce that this is a good state
                    if (r[0] == v[0] && r[1] ==  v[1]) {
                        r[2] *= 1.1;
                    }
    	        }
    	        //System.out.println(t.key + " got paid: " + payoff);
    	        payoff *= 0.9; //reduce payoff as we backtrack
	        }
	    }
	}
	
	public int[] bestMove(State s, int[][] legalMoves) {
	    //System.out.println("==== Choosing Best Move for Piece " + s.getNextPiece() + " ====");
        double best = f(s, legalMoves[0]);
        int[] bestMove = legalMoves[0];
        for (int i = 1; i < legalMoves.length; i++) {
            double next = f(s, legalMoves[i]);
            //we want to maximize f().
            if (best < next) {
                best = next;
                bestMove = legalMoves[i];
            }
        }
        //System.out.println("==== Best Move Found: " + bestMove[0] + "," + bestMove[1] + " ====");
        return bestMove;
	}
	
	public double f(State s, int[] move) {
	    int[][] field = copy(s.getField());
	    int[] top = simulateField(s, field, move[0], move[1]);
	    
	    //heuristics
	    double aggregateHeight = getAggregateHeight(top); //average of all heights
	    double completeLines = getCompleteLines(field); //number of lines completed
	    double holes = getHoles(field, top); //number of holes present
	    double bumpiness = getBumpiness(top); //sum of difference in height
	    
        double a = -0.510066;
        double b = 0.760666;
        double c = -0.35663;
        double d = -0.184483;
	    
	    //TODO make it a linear combination
	    double f = a * aggregateHeight + b * completeLines + c * holes + d * bumpiness;
	    //System.out.println(move[0] + "," + move[1] + ": "
	    //        + (a*aggregateHeight) + " + " + (b*completeLines) + " + " + (c*holes) + " + " + (d*bumpiness) + " = " + f);
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
	
	public double getAggregateHeight(int[] top) {
	    double sum = 0;
	    for (int i = 0; i < top.length; i++) {
	        if (top[i] >= State.ROWS) return 1000;
	        sum += top[i];
	    }
	    return sum;
	}
	
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
	
	public double getHoles(int[][] field, int[] top) {
	    int max = top[0];
	    for (int i = 1; i < top.length; i++) {
	        max = Math.max(max, top[i]);
	    }
	    if (max >= State.ROWS) max = State.ROWS-1;
	    
	    int sum = 0;
	    //last row cannot have holes
	    for (int i = 0; i < max-1; i++) {
	        for (int j = 0; j < field[i].length; j++) {
	            if (field[i][j] == 0 && i < top[j])
	                sum++;
	        }
	    }
	    return sum;
	}
	
	public double getBumpiness(int[] top) {
	    double sum = 0;
	    for (int i = 0; i < top.length-1; i++) {
	        int bump = Math.abs(top[i] - top[i+1]);
	        sum += bump;
	    }
	    return sum;
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
    
    public void print(int[][] toPrint) {
        for (int i = 0; i < toPrint.length; i++) {
            for (int j = 0; j < toPrint[i].length; j++) {
                if(toPrint[i][j] > 0) System.out.print("1 ");
                else System.out.print("0 ");
            }
            System.out.println();
        }
        System.out.println("========================");
    }
    
    public void print(int[] toPrint) {
        for (int i = 0; i < toPrint.length; i++) {
            System.out.print(toPrint[i] + " ");
        }
        System.out.println();
    }
	
	public static void main(String[] args) {
		State s = new State();
		new TFrame(s);
		PlayerSkeleton p = new PlayerSkeleton();
		while(!s.hasLost()) {
			s.makeMove(p.pickMove(s,s.legalMoves()));
			//s.makeMove(p.bestMove(s, s.legalMoves()));
		    s.draw();
			s.drawNext(0,0);
			try {
				Thread.sleep(0);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("You have completed "+s.getRowsCleared()+" rows.");
	}
	
}

class Tuple {
    protected String key;
    protected int[] move;
    
    public Tuple (String key, int[] move) {
        this.key = key;
        this.move = move;
    }
}
