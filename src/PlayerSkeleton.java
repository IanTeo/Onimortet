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
	    
	    int front = max - 9; //find lowest the frontier
        
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
	    
	    /*if (randomize(top) >= 0.2) {
	        moveToMake[0] = (int) (Math.random() * State.getpOrients()[s.getNextPiece()]);
	        int possibleMove = 0;
	        for (int i = 0; i < legalMoves.length; i++) {
	            if (legalMoves[i][0] == moveToMake[0]) {
	                possibleMove++;
	            }
	        }
	        moveToMake[1] = (int) (Math.random() * (possibleMove-1));
	        //System.out.println("Making the random move: " + moveToMake[0] + "," + moveToMake[1]);
	    }*/
	    
	    int[][] field = copy(s.getField());
        simulateField(s, field, moveToMake[0], moveToMake[1]);
        int completeLines = (int) getCompleteLines(field);
        backtrack.push(new Tuple(key, moveToMake));
        
	    calculatePayoff(completeLines);
	    
	    return moveToMake;
	   
	}
	
	public double randomize(int[] top) {
	    double sum = 0;
	    for (int i = 0; i < top.length; i++) {
	        if (top[i] >= State.ROWS-3) return -1; //never randomize
	        sum += top[i];
	    }
	    
	    double normalizedSum = ((sum / State.ROWS-1) / State.COLS);
	    return (Math.random() - normalizedSum) / 2;
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
	    
        double a = -4.500158825082766;
        double b = 3.4181268101392694;
        double c = -3.2178882868487753;
        double d = -9.348695305445199;
        double e = -7.899265427351652;
        double g = -3.3855972247263626;
	    
	    //TODO make it a linear combination
	    double f = a * landingHeight + b * completeLines + c * rowTransitions + d * colTransitions + e * holes + g * wellSum;
	    //System.out.println(move[0] + "," + move[1] + ": "
	    //        + landingHeight + " + " + completeLines + " + " + rowTransitions + " + " + colTransitions + " + " + holes + " + " + wellSum + " = " + f);
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
	    
	    //get landing height
	    //double landingHeight = row + ((State.getpHeight()[nextPiece][orient] - 1) / 2.0);
	    if (row >= State.ROWS) row = 1000;
	    return row;
	}
	
	public int getRowTransitions(int[][] field) {
	    int transitions = 0;
	    
	    /*for (int i = 0; i < field.length; i++) {
	        for (int j = 0; j < field[i].length-1; j++) {
	            if (field[i][j] > 0 && field[i][j+1] == 0) {
	                transitions++;
	            } else if (field[i][j] == 0 && field[i][j+1] > 0) {
	                transitions++;
	            }
	        }
	    }*/
	    
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
	
	public int getColTransitions(int[][] field) {
	    int transitions = 0;
	    
	    /*for (int i = 0; i < field.length-1; i++) {
            for (int j = 0; j < field[i].length; j++) {
                if (field[i][j] > 0 && field[i+1][j] == 0) {
                    transitions++;
                } else if (field[i][j] == 0 && field[i+1][j] > 0) {
                    transitions++;
                }
            }
        }*/
        
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
	    while (true) {
		State s = new State();
		TFrame t = new TFrame(s);
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
		t.dispose();
		System.out.println("You have completed "+s.getRowsCleared()+" rows.");
	}
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
