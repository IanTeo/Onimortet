import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class PlayerSkeleton {
	
    double a = -4.500158825082766; //landingHeight
    double b = 3.4181268101392694; //completeLines
    double c = -3.2178882868487753; //rowTransitions
    double d = -9.348695305445199; //colTransitions
    double e = -7.899265427351652; //holes
    double g = -3.3855972247263626; //wellSum
    
    static long seed = 0;
    static BufferedWriter bw = null;
    
    static AI ai;

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
	    
	    //TODO make it a linear combination
	    double f = a * landingHeight - b * completeLines + c * rowTransitions + d * colTransitions + e * holes + g * wellSum;
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
    
    /*public void print(int[][] toPrint) {
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
    }*/
	
	public static void main(String[] args) {
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
            seed = System.currentTimeMillis();
            System.out.println("Generation " + ai.generation + " seed:" + seed);
		    ai.scores.stream().parallel().forEach(i -> runTetris(ai.scores.indexOf(i)));
            /*for (int i = 0; i < 16; i++) {
    			State s = new State(seed);
    			//new TFrame(s);
    			PlayerSkeleton p = new PlayerSkeleton();
    			ai.setAIValues(p, i);
    			
    			while(!s.hasLost()) {
    				s.makeMove(p.pickMove(s,s.legalMoves()));
    			}
    			if(s.getRowsCleared()>0){
    		        String val = ai.sendScore(s.getRowsCleared(), i) + "\n";
    		        try {
    					bw.write(val);
    			        bw.flush();
    				} catch (IOException e) {
    					// TODO Auto-generated catch block
    					e.printStackTrace();
    				}
    				
    			}
		    }*/
		}
	}
	
	public static void runTetris(int i) {
	    State s = new State(seed);
        //new TFrame(s);
        PlayerSkeleton p = new PlayerSkeleton();
        ai.setAIValues(p, i);
        
        while(!s.hasLost()) {
            s.makeMove(p.pickMove(s,s.legalMoves()));
        }
        if(s.getRowsCleared()>0){
            String val = ai.sendScore(s.getRowsCleared(), i) + "\n";
            try {
                bw.write(val);
                bw.flush();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
        }
	}
	
}
