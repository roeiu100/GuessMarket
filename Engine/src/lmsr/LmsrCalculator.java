package lmsr;

public class LmsrCalculator {

    public static double calculatePool(int qYes, int qNo, int b) {
        double expYes = Math.exp((double) qYes / b);
        double expNo = Math.exp((double) qNo / b);
        return b * Math.log(expYes + expNo);
    }

    public static double calculatePrice(int qTarget, int qOther, int b) {
        double expTarget = Math.exp((double) qTarget / b);
        double expOther = Math.exp((double) qOther / b);
        return expTarget / (expTarget + expOther);
    }
    
    public static double calculateTradeCost(int currentQYes, int currentQNo, int b, int buyYesAmount, int buyNoAmount) {
        double costBefore = calculatePool(currentQYes, currentQNo, b);
        double costAfter = calculatePool(currentQYes + buyYesAmount, currentQNo + buyNoAmount, b);
        return costAfter - costBefore;
    }
}