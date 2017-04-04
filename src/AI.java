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
    double[][] chromosomes = new double[population][6];
    int[] scores = new int[population];
    int current = 0;

    public AI() {

        // Randomize starting chromosomes with values between -10 and 0.
        for (int i = 0; i < population; i++) {
            for (int j = 0; j < 6; j++) {
                chromosomes[i][j] = Math.random() * 10 - 10;
            }
        }

    }

    void newGeneration() {

        // Calculate average fitness
        int[] scores_ = new int[population];
        for (int i = 0; i < scores.length; i++) {
            scores_[i] = scores[i];
        }
        Arrays.sort(scores_);
        System.out.println("Generation " + generation
                + "; min = " + scores_[0]
                + "; med = " + scores_[population / 2]
                + "; max = " + scores_[population - 1]);

        List<double[]> winners = new ArrayList<double[]>();

        // Pair 1 with 2, 3 with 4, etc.
        for (int i = 0; i < (population / 2); i++) {

            // Pick the more fit of the two pairs
            int c1score = scores[i];
            int c2score = scores[i + 1];
            int winner = c1score > c2score ? i : i + 1;

            // Keep the winner, discard the loser.
            winners.add(chromosomes[winner]);
        }


        int counter = 0;
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
                    child[j] = (Math.random()*2) > 1 ? winner1[j] : winner2[j];

                    // Chance of mutation
                    boolean mutate = Math.random() < mutation_rate;
                    if (mutate) {
                        // Change this value anywhere from -10 to 10
                        double change = Math.random() * 20 - 10;
                        child[j] += change;
                    }
                }

                new_population.add(child);
                counter++;
            }
        }

        // Shuffle the new population.
        Collections.shuffle(new_population, new Random());

        // Copy them over
        for (int i = 0; i < population; i++) {
            for (int j = 0; j < 6; j++) {
                chromosomes[i][j] = new_population.get(i)[j];
            }
        }

        generation++;
        current = 0;

    }

    void setAIValues(PlayerSkeleton ai) {
        if (!USE_GENETIC) {
            return;
        }

        ai.a = chromosomes[current][0];
        ai.b = chromosomes[current][1];
        ai.c = chromosomes[current][2];
        ai.d = chromosomes[current][3];
        ai.e = chromosomes[current][4];
        ai.g = chromosomes[current][5];
    }

    String sendScore(int score) {
        if (!USE_GENETIC) {
            return "";
        }

        String s = aToS(chromosomes[current]);
        s = "Generation " + generation + "; Candidate " + (current + 1) + ": " + s + " Score = " + score;
        System.out.println(s);
        scores[current] = score;
        current++;

        if (current == population) {
            newGeneration();
        }
        return s;
    }

    // Double array to string, two decimal places
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