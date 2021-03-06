package cl.utfsm.di.RDFDifferentialPrivacy;

import symjava.symbolic.Expr;

public class Sensitivity
{
    /*
    @sensitivity double the value of the query sensitivity
    */
    private double sensitivity;
    /*
    @s Expr the polinomial for the sensitivity
    */
    private Expr s;
    /*
    @maxK int the max iteration needed to get to the sensitivity
    */
    private int maxK;

    public Sensitivity(double sensitivity, Expr s)
    {
        this.sensitivity = sensitivity;
        this.s = s;
    }

    public Expr getS()
    {
        return s;
    }

    public double getSensitivity()
    {
        return sensitivity;
    }
    
    public void setSensitivity(double newSensitivity)
    {
        this.sensitivity = newSensitivity;
    }
    
    public int getMaxK()
    {
        return maxK;
    }
    
    public void setMaxK(int maxK)
    {
        this.maxK = maxK;
    }
}
