public class Functions {
    public static int abs(int x) {
        return (x < 0) ? -x : x;
    }

    public static int distE(int xi,int yi,int xj,int yj) {
        return abs(xi - xj) + abs(yi-yj);
    }

    public static double distA(int xi,int yi,int xj,int yj) {
        return Math.sqrt((xi-xj)*(xi-xj) + (yi-yj)*(yi-yj));
    }

    public static int binF(double i, double j) {
        return (i <= j) ? 1 : 0;
    }

    public static double[] doubleToArr(double value, int size) {
        double[] data = new double[size];
        for(int i = 0; i < size; i++) {
            data[i] = value;
        }
        return data;
    }
}