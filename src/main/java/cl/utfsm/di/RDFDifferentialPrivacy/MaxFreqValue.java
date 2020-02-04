/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cl.utfsm.di.RDFDifferentialPrivacy;

import javafx.util.Pair;

/**
 *
 * @author cbuil
 */
public class MaxFreqValue
{

    private Pair<StarQuery, String> queryLeft;
    private Pair<StarQuery, String> queryRight;

    public MaxFreqValue(Pair<StarQuery, String> queryLeft, Pair<StarQuery, String> queryRight)
    {
        this.queryLeft = queryLeft;
        this.queryRight = queryRight;
    }

    public Pair<StarQuery, String> getStarQueryLeft()
    {
        return queryLeft;
    }

    public Pair<StarQuery, String> getStarQueryRight()
    {
        return queryRight;
    }
}
