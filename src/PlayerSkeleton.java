public class PlayerSkeleton {

	//implement this function to have a working system
	public int[] pickMove(State s, int[][] legalMoves) {
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
