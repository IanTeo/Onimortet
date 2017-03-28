
public class PlayerSkeleton {

    //implement this function to have a working system
    public int[] pickMove(State s, int[][] legalMoves) {
        int popSize = legalMoves.length;
        for (int i = 0; i < popSize; i++) {
            legalMoves = evolve(s, legalMoves);
        }
        return getFittest(s, legalMoves);
    }

    public int[][] evolve(State s, int[][] legalMoves) {
        int[][] newMoves = new int[legalMoves.length][2];
        int k = 0;
        for (int i = 0; i < legalMoves.length; i++) {
            int[] parentState1 = randomSelection(s, legalMoves);
            int[] parentState2 = randomSelection(s, legalMoves);
            int[] childState = crossover(s, parentState1, parentState2);
            childState = mutate(s.getNextPiece(), childState);
            if (isValidMove(s, childState)) {
                newMoves[k++] = childState;
            }
        }
        return newMoves;
    }

    //Randomly select state for crossover
    private int[] randomSelection(State s, int[][] possibleStates) {
        int[][] selectedStates = new int[possibleStates.length][2];
        for (int i = 0; i < possibleStates.length; i++) {
            int rnd = (int) (Math.random() * possibleStates.length);
            selectedStates[i] = possibleStates[rnd];
        }
        //Get fittest state out of randomly selected ones
        return getFittest(s, selectedStates);
    }

    //Get fittest state
    private int[] getFittest(State s, int[][] legalMoves) {
        double bestEvaluation = f(s, legalMoves[0]);
        int[] bestMove = legalMoves[0];
        for (int i = 1; i < legalMoves.length; i++) {
            double evaluation = f(s, legalMoves[i]);
            if (bestEvaluation < evaluation) {
                bestEvaluation = evaluation;
                bestMove = legalMoves[i];
            }
        }
        return bestMove;
    }

    //Crossover parent states to produce a child state
    private int[] crossover(State s, int[] state1, int[] state2) {
        int[] childState = new int[state1.length];
        for (int i = 0; i < state1.length; i++) {
            if (Math.random() <= 0.5) {
                childState[i] = state1[i];
            } else {
                childState[i] = state2[i];
            }
        }
        if (!isValidMove(s, childState)) {
            return state1;
        } else {
            return childState;
        }
    }

    //Mutate a state
    private int[] mutate(int currentId, int[] state) {
        for (int i = 0; i < state.length; i++) {
            if (Math.random() <= 0.01) {
                if (i == 0) {
                    state[i] = (int) (Math.random() * State.getpOrients()[currentId]);
                } else {
                    state[i] = (int) (Math.random() * (State.COLS - State.getpWidth()[currentId][state[0]]));
                }
            }
        }
        return state;
    }

    //Checks if mutated piece is still valid
    private boolean isValidMove(State s, int[] move) {
        return move[1] + State.pWidth[s.nextPiece][move[0]] - 1 < 10;
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
        int height = top[slot] - State.getpBottom()[nextPiece][orient][0];
        //for each column beyond the first in the piece
        for (int c = 1; c < State.getpWidth()[nextPiece][orient]; c++) {
            //System.out.println("H:" + height + " S:" + slot + " C:" + c + " P:" + nextPiece + " O:" + orient);
            height = Math.max(height, top[slot + c] - State.getpBottom()[nextPiece][orient][c]);
        }

        //for each column in the piece - fill in the appropriate blocks
        for (int i = 0; i < State.getpWidth()[nextPiece][orient]; i++) {

            //from bottom to top of brick
            for (int h = height + State.getpBottom()[nextPiece][orient][i]; h < height + State.getpTop()[nextPiece][orient][i]; h++) {
                if (h < field.length && i + slot < field[0].length) {
                    field[h][i + slot] = 1;
                }
            }
        }

        //adjust top
        for (int c = 0; c < State.getpWidth()[nextPiece][orient]; c++) {
            top[slot + c] = height + State.getpTop()[nextPiece][orient][c];
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
        if (row >= State.ROWS) {
            row = 1000;
        }
        return row;
    }

    public int getRowTransitions(int[][] field) {
        int transitions = 0;
        for (int i = 0; i < field.length; i++) {
            int lastBit = 1;
            int bit = -1;
            for (int j = 0; j < field[i].length; j++) {
                if (field[i][j] > 0) {
                    bit = 1;
                } else {
                    bit = 0;
                }
                if (bit != lastBit) {
                    transitions++;
                }
                lastBit = bit;
            }
            if (bit == 0) {
                transitions++;
            }
        }
        return transitions - 2;
    }

    public int getColTransitions(int[][] field) {
        int transitions = 0;
        for (int i = 0; i < field[0].length; i++) {
            int lastBit = 1;
            int bit = -1;
            for (int j = 0; j < field.length; j++) {
                if (field[j][i] > 0) {
                    bit = 1;
                } else {
                    bit = 0;
                }
                if (bit != lastBit) {
                    transitions++;
                }
                lastBit = bit;
            }
            if (bit == 0) {
                transitions++;
            }
        }
        return transitions - 10;
    }

    public double getCompleteLines(int[][] field) {
        int sum = 0;
        for (int i = 0; i < field.length; i++) {
            boolean isFull = true;
            for (int j = 0; j < field[i].length; j++) {
                if (field[i][j] == 0) {
                    isFull = false;
                }
            }

            if (isFull) {
                sum++;
            }
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
                if ((field[j][i] == 0) && (field[j][i - 1] != 0) && (field[j][i + 1] != 0)) {
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
            if ((field[j][field[j].length - 1] == 0) && (field[j][field[j].length - 2] != 0)) {
                wellSum++;

                for (int k = j - 1; k >= 0; k--) {
                    if (field[k][field[k].length - 1] == 0) {
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
                System.out.print(toPrint[i][j] + " ");
            }
            System.out.println();
        }
        System.out.println("========================");
    }

    public static void main(String[] args) {
        State s = new State();
        new TFrame(s);
        PlayerSkeleton p = new PlayerSkeleton();
        while (!s.hasLost()) {
            s.makeMove(p.pickMove(s, s.legalMoves()));
            s.draw();
            s.drawNext(0, 0);
            if (s.getRowsCleared() % 10 == 0) {
                System.out.println("Rows Cleared: " + s.getRowsCleared());
            }
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("You have completed " + s.getRowsCleared() + " rows.");
    }

}
