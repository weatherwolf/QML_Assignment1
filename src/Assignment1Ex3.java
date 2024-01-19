import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBEnv;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBLinExpr;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;

public class Assignment1Ex3 {

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

		// Run the optimization
		try {
			solveAssignment1(x, y, vE, vA, sE, sA, cE, cA, T);
		} catch (GRBException e) {
			System.out.println("A Gurobi exception occured: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void solveAssignment1(int[] x, int[] y, double vE, double vA, double sE, double sA, double cE_, double cA_, double T) throws GRBException {
		
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
		for (int i = 0; i < n; i++) {
			pE[i] = model.addVar(0, 1, 0.0, GRB.BINARY, "pE(" + i + ")");
			pA[i] = model.addVar(0, 1, 0.0, GRB.BINARY, "pA(" + i + ")");
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
				double valueA = sA + 60*Functions.distA(x[i], y[i], x[city], y[city])/(vA);
				int boolA= Functions.binF(valueA, T);
				travelExpr.addTerm(boolA, pA[i]);
			}
			model.addConstr(travelExpr, GRB.GREATER_EQUAL, 1, "Constrain T city: " + city);
		}

		// Solve the model
		model.optimize();

		// Query the solution
		if (model.get(GRB.IntAttr.Status) == GRB.Status.OPTIMAL) {
			System.out.println("Found optimal solution!");
			System.out.println("Objective = " + model.get(GRB.DoubleAttr.ObjVal));
			System.out.print("Used items: ");
			for (int i = 0; i < n; i++) {
				// printing the coordinates of the places where there will be an (advanced) evacuation center
				if (pE[i].get(GRB.DoubleAttr.X) + pA[i].get(GRB.DoubleAttr.X) >= 0.5) {
					System.out.println("coordinates: (" + x[i] + ", " + y[i] + "). Type: [" + 
										pE[i].get(GRB.DoubleAttr.X) + ", " + pA[i].get(GRB.DoubleAttr.X) + "]");

					// System.out.println("(" + x[i] + ", " + y[i] + "), [" + 
					// 					pE[i].get(GRB.DoubleAttr.X) + ", " + pA[i].get(GRB.DoubleAttr.X) + "]");
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
