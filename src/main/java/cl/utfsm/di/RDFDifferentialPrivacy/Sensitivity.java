package cl.utfsm.di.RDFDifferentialPrivacy;

import symjava.symbolic.Expr;

public class Sensitivity
{
    private double sensitivity;
    private Expr s;
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
