package cl.utfsm.di.RDFDifferentialPrivacy.symbolic;

public class SmoothResult
{
    private final double sensitivity;
    private final int k;

    public SmoothResult(double sensitivity, int k)
    {
        this.sensitivity = sensitivity;
        this.k = k;
    }

    public double getSensitivity()
    {
        return sensitivity;
    }

    public int getK()
    {
        return k;
    }
}
