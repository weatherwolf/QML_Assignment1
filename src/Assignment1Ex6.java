import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBEnv;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBLinExpr;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;

public class Assignment1Ex6  {

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
        double[] Capacity = {25,20,15,10,5};
		// Run the optimization
        try {
            for(int j = 0; j < Capacity.length; j++) {
            solveAssignment1Ex6(x, y, vE, vA, sE, sA, cE, cA, T, Capacity[j]);
            }
        } catch (GRBException e) {
            System.out.println("A Gurobi exception occured: " + e.getMessage());
            e.printStackTrace();
        }
        
	}

	public static void solveAssignment1Ex6(int[] x, int[] y, double vE, double vA, double sE, double sA, 
                                           double cE_, double cA_, double T, double cap) throws GRBException {
		                                           
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

		// Add the time to travel to evacuation center restriction
		for (int city = 0; city < n; city++) {
			GRBLinExpr travelExpr = new GRBLinExpr();
			for(int i = 0; i < n; i++) {
				// calculating the time to travel to a normal evacuation center
				double valueE = sE + 60*Functions.distE(x[i], y[i], x[city], y[city])/(vE);
				int boolE = Functions.binF(valueE, T);
				travelExpr.addTerm(boolE, pE[i]);

				// calculating the time to travel to an advanced evacuation center
				double valueA = sA + 60*Functions.distA(x[i], y[i], x[city], y[city])/(0.5*vA);
				int boolA= Functions.binF(valueA, T);
				travelExpr.addTerm(boolA, pA[i]);
			}
			model.addConstr(travelExpr, GRB.GREATER_EQUAL, 1, "Constrain T city: " + city);
		}

        // Add constraint that the city can go to exactly one destination (evaucation center)
        for(int j = 0; j < n; j++) {
            GRBLinExpr destinationExpr = new GRBLinExpr();
            for(int i = 0; i < n; i++) {
                double valueE = sE + 60*Functions.distE(x[i], y[i], x[j], y[j])/(vE);
				int boolE = Functions.binF(valueE, T);
				destinationExpr.addTerm(boolE, hE[i][j]);

				// calculating the time to travel to an advanced evacuation center
				double valueA = sA + 60*Functions.distA(x[i], y[i], x[j], y[j])/(0.5*vA);
				int boolA= Functions.binF(valueA, T);
				destinationExpr.addTerm(boolA, hA[i][j]);

                // destinationExpr.addTerm(1.0, hE[i][j]);
                // destinationExpr.addTerm(1.0, hA[i][j]);
            }
            model.addConstr(destinationExpr, GRB.EQUAL, 1.0, "destination constraint: " + j);
        }

        // Add the capacity constraint
        for(int i = 0; i < n; i++) {
            GRBLinExpr capExprElhs = new GRBLinExpr();
            GRBLinExpr capExprAlhs = new GRBLinExpr();

            GRBLinExpr capExprErhs = new GRBLinExpr();
            GRBLinExpr capExprArhs = new GRBLinExpr();

            capExprErhs.addTerm(cap, pE[i]);
            capExprArhs.addTerm(cap, pA[i]);
            for(int j = 0; j < n; j++) {
                capExprElhs.addTerm(1.0, hE[i][j]);
				capExprAlhs.addTerm(1.0, hA[i][j]);  

            }
            model.addConstr(capExprElhs, GRB.LESS_EQUAL, capExprErhs, "capacity constraint E " + i);
            model.addConstr(capExprAlhs, GRB.LESS_EQUAL, capExprArhs, "capacity constraint A " + i);
        }

		// Solve the model
		model.optimize();

		// Query the solution
		if (model.get(GRB.IntAttr.Status) == GRB.Status.OPTIMAL) {
			System.out.println("Found optimal solution!");
			System.out.println("Objective = " + model.get(GRB.DoubleAttr.ObjVal));
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
