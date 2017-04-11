import java.util.*;

/*
 * A genetic algorithm to find combinations for AI values. This is an interface
 * to the rest of JTetris: they start by calling setAIValues() to let us set
 * some values for the AI, then they call sendScore() to give us what they got.
 */
public class AI {
    // If false, just use the default values
    final boolean USE_GENETIC = true;
    
    // Which generation are we in?
    int generation = 1;
    
    // How many candidates are there in a generation?
    // Must be a multiple of 4.
    final int population = 16;
    
    // How often do chromosomes mutate?
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

/*        // Pair 1 with 2, 3 with 4, etc.
        for (int i = 0; i < population; i += 2) {

            // Pick the more fit of the two pairs
            int c1score = chromosomes.get(i).score;
            int c2score = chromosomes.get(i+1).score;
            int winner = c1score > c2score ? i : i + 1;

            // Keep the winner, discard the loser.
            winners.add(chromosomes.get(winner).weights);
        }*/
        
        //pick the best 8 scores instead

        for (int j = population-1; j >= population/2; j--) { //start from the highest score, stops at half size of pop size
        	for (int i = 0; i < population; i++) {
	        	if(chromosomes.get(i).score == scores_[j]){
	        		winners.add(chromosomes.get(i).weights);
	        		System.out.println("Score taken: " + chromosomes.get(i).score);
	        	}
	        }
        }
        
        List<double[]> new_population = new ArrayList<double[]>();

        // Pair up two winners at a time
        for (int i = 0; i < winners.size(); i += 2) {
            double[] winner1 = winners.get(i);
            double[] winner2 = winners.get(i + 1);
            
            //System.out.println(winner1[0] + "," + winner1[1] + "," + winner1[2] + "," + winner1[3] + "," + winner1[4] + "," + winner1[5]);
            //System.out.println(winner2[0] + "," + winner2[1] + "," + winner2[2] + "," + winner2[3] + "," + winner2[4] + "," + winner2[5]);;

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
                //System.out.println(child[0] + "-" + child[1] + "-" + child[2] + "-" + child[3] + "-" + child[4] + "-" + child[5]);
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
}