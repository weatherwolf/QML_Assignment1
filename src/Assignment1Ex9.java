import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBEnv;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBLinExpr;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;

public class Assignment1Ex9  {

	public static void main(String[] args) throws FileNotFoundException {
        int[] x = new int[50];
        int[] y = new int[50];
        int i = 0;
        Scanner scan = new Scanner(new File("src/Metropolica_coordinates.txt"));
        while (scan.hasNextLine()) {
            String data = scan.nextLine();
            if (data.contains(",")) {
				String[] coordinates = data.split(",");
                x[i] = Integer.parseInt(coordinates[0].trim());
                y[i] = Integer.parseInt(coordinates[1].trim());
                i++;
            }
        }

		// Set the values for the variables
		double vE = 60;
		double vA = 300;
		double sE = 7;
		double sA = 10;
		double cE = 11;
		double cA = 35;
		double T = 22;
        double tMaxE = 4;
        double tMaxA = 1.1;
        double[] betas = {0.05, 0.10, 0.20, 0.35, 0.5, 0.6, 0.75, 0.9, 0.95, 0.99, 1};
		// Run the optimization
        try {
            for(int j = 0; j < betas.length; j++) {
            solveAssignment1Ex9(x, y, vE, vA, sE, sA, cE, cA, T, tMaxE, tMaxA, betas[j]);
            }
        } catch (GRBException e) {
            System.out.println("A Gurobi exception occured: " + e.getMessage());
            e.printStackTrace();
        }
        
	}

	public static void solveAssignment1Ex9(int[] x, int[] y, double vE, double vA, double sE, double sA, 
                                           double cE_, double cA_, double T, double tMaxE, double tMaxA, double beta) throws GRBException {
		                                           
        // Create the environment
		GRBEnv env = new GRBEnv();
		env.set(GRB.IntParam.OutputFlag, 0);
		env.start();

		// Create empty model
		GRBModel model = new GRBModel(env);

		// Set some parameters
		model.set(GRB.DoubleParam.TimeLimit, 100.0);

		// create some basic variables
		int n = x.length;
		double[] cE = Functions.doubleToArr(cE_, n);
		double[] cA = Functions.doubleToArr(cA_, n);


		// Create the variables and their domain restrictions
		GRBVar[] pE = new GRBVar[n];
		GRBVar[] pA = new GRBVar[n];
		
        GRBVar[][] hE = new GRBVar[n][n];
		GRBVar[][] hA = new GRBVar[n][n];

		for (int i = 0; i < n; i++) {
            pE[i] = model.addVar(0, 1, 0.0, GRB.BINARY, "pE(" + i + ")");
			pA[i] = model.addVar(0, 1, 0.0, GRB.BINARY, "pA(" + i + ")");
            for(int j = 0; j < n; j++) {
                hE[i][j] = model.addVar(0, 1, 0.0, GRB.BINARY, "hE(" + i + "," + j + ")");
                hA[i][j] = model.addVar(0, 1, 0.0, GRB.BINARY, "hA(" + i + "," + j + ")");
            }
		}

		// Create the objective
		GRBLinExpr objExpr = new GRBLinExpr();
		objExpr.addTerms(cE, pE);
		objExpr.addTerms(cA, pA);
		model.setObjective(objExpr, GRB.MINIMIZE);

        // Add the restiction that at least beta cities need to evacuate in T time.
        GRBLinExpr betaExpr = new GRBLinExpr();
		for(int i = 0; i < n; i++) {
            for(int j = 0; j < n; j++) {
                // calculating the time to travel to a normal evacuation center
                double valueE = sE + 60*Functions.distE(x[i], y[i], x[j], y[j])/(vE);
				int boolE = Functions.binF(valueE, T);
				betaExpr.addTerm(boolE, hE[i][j]);

				// calculating the time to travel to an advanced evacuation center
				double valueA = sA + 60*Functions.distA(x[i], y[i], x[j], y[j])/(0.5*vA);
				int boolA= Functions.binF(valueA, T);
				betaExpr.addTerm(boolA, hA[i][j]);
            }
        }
        double temp = beta*n;
        model.addConstr(betaExpr, GRB.GREATER_EQUAL, temp, "beta Constraint");

		// Add constraint that the city can go to exactly one destination in 2T/tMax 
        for(int j = 0; j < n; j++) {
            GRBLinExpr destinationExpr = new GRBLinExpr();
            for(int i = 0; i < n; i++) {
                double valueE = sE + 60*Functions.distE(x[i], y[i], x[j], y[j])/(vE);
				int boolE = Functions.binF(valueE, (2*T/tMaxE));
				destinationExpr.addTerm(boolE, hE[i][j]);

				// calculating the time to travel to an advanced evacuation center
				double valueA = sA + 60*Functions.distA(x[i], y[i], x[j], y[j])/(0.5*vA);
				int boolA= Functions.binF(valueA, (2*T/tMaxA));
				destinationExpr.addTerm(boolA, hA[i][j]);

                // destinationExpr.addTerm(1.0, hE[i][j]);
                // destinationExpr.addTerm(1.0, hA[i][j]);
            }
            model.addConstr(destinationExpr, GRB.EQUAL, 1.0, "destination constraint: " + j);
        }

        // Add constraints that every city can only go to an open evacuation center 
          for(int i = 0; i < n; i++) {
            for(int j = 0; j < n; j++) {
                model.addConstr(hE[i][j], GRB.LESS_EQUAL, pE[i], null);
                model.addConstr(hA[i][j], GRB.LESS_EQUAL, pA[i], null);
            }
        }

        // Add constraint that every city only has one destination:
        for(int j = 0; j < n; j++) {
            GRBLinExpr cityExpr = new GRBLinExpr();
            for(int i = 0; i < n; i++) {
                cityExpr.addTerm(1.0, hE[i][j]);
                cityExpr.addTerm(1.0, hA[i][j]);
            }
            model.addConstr(cityExpr, GRB.EQUAL, 1, null);
        }

		// Solve the model
		model.optimize();

		// Query the solution
		if (model.get(GRB.IntAttr.Status) == GRB.Status.OPTIMAL) {
			System.out.println("Found optimal solution!");
			System.out.println("Objective = " + model.get(GRB.DoubleAttr.ObjVal) + " beta = " + beta);
			System.out.print("Used items: ");
			for (int i = 0; i < n; i++) {
				// To print out the coordinates and the type of the build evacuation center
				if (pE[i].get(GRB.DoubleAttr.X) + pA[i].get(GRB.DoubleAttr.X) >= 0.5) {
					System.out.println("coordinates: (" + x[i] + ", " + y[i] + "). Type: [" + 
										pE[i].get(GRB.DoubleAttr.X) + ", " + pA[i].get(GRB.DoubleAttr.X) + "]");

                    // To print out the coordinates of the build evacuation centers
                    System.out.println("(" + x[i] + ", " + y[i] + ")");

                    // To check to how many cities are going to a particular city
                    int sumHE = 0;
                    int sumHA = 0;
                    for(int j = 0; j < n; j++) {
                        if(hE[i][j].get(GRB.DoubleAttr.X) >= 0.5) {
                            sumHE++;
                        }
                        if(hA[i][j].get(GRB.DoubleAttr.X) >= 0.5) {
                            sumHA++;
                        }   
                    }
                    if(sumHE + sumHA > 0) {
                        System.out.println("hE: " + sumHE + " hA: " + sumHA);
                    }
				}
			}
		} else {
			System.out.println("No optimal solution found");
		}

		// Close the model
		model.dispose();
		env.dispose();
	}
}
