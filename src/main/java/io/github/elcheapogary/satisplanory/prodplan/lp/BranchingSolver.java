/*
 * Copyright (c) 2022 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.prodplan.lp;

import io.github.elcheapogary.satisplanory.util.BigFraction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.RecursiveAction;

class BranchingSolver
{
    private BranchingSolver()
    {
    }


    public static Tableau maximize(Tableau initialTableau, Expression objectiveFunction, Collection<? extends BranchingConstraint> constraints)
            throws UnboundedSolutionException, InterruptedException
    {
        BestSolutionHolder bestSolutionHolder = new BestSolutionHolder();

        Tableau.Objective objective = initialTableau.addObjective(objectiveFunction);

        initialTableau.maximize(objective);

        new FindUnsatisfiedConstraintsAction(initialTableau, objective, constraints, bestSolutionHolder).fork().join();

        initialTableau.removeObjective(objective);

        Tableau retv = bestSolutionHolder.getBestTableau();

        if (retv != initialTableau){
            retv.removeObjective(objective);
        }

        return retv;
    }

    private static class FindUnsatisfiedConstraintsAction
            extends RecursiveAction
    {
        private final Tableau tableau;
        private final Tableau.Objective objective;
        private final Collection<? extends BranchingConstraint> branchingConstraints;
        private final BestSolutionHolder bestSolutionHolder;

        public FindUnsatisfiedConstraintsAction(Tableau tableau, Tableau.Objective objective, Collection<? extends BranchingConstraint> branchingConstraints, BestSolutionHolder bestSolutionHolder)
        {
            this.tableau = tableau;
            this.objective = objective;
            this.branchingConstraints = branchingConstraints;
            this.bestSolutionHolder = bestSolutionHolder;
        }

        @Override
        protected void compute()
        {
            for (BranchingConstraint branchingConstraint : branchingConstraints){
                Collection<? extends Constraint> branches = branchingConstraint.getConstraints(tableau);

                if (branches == null || branches.isEmpty()){
                    continue;
                }

                List<BoundAction> actions = new ArrayList<>(branches.size());

                boolean first = true;
                for (Constraint constraint : branches){
                    Tableau tmpTableau = tableau;
                    if (first){
                        first = false;
                    }else{
                        tmpTableau = new Tableau(tmpTableau);
                    }
                    actions.add(new BoundAction(tmpTableau, objective, branchingConstraints, bestSolutionHolder, constraint));
                }

                invokeAll(actions);

                return;
            }

            BigFraction objectiveValue = tableau.getValue(objective);
            bestSolutionHolder.submitCompleteSolution(objectiveValue, tableau);
        }
    }

    private static class BoundAction
            extends RecursiveAction
    {
        private final Tableau tableau;
        private final Tableau.Objective objective;
        private final Collection<? extends BranchingConstraint> branchingConstraints;
        private final BestSolutionHolder bestSolutionHolder;
        private final Constraint constraint;

        public BoundAction(Tableau tableau, Tableau.Objective objective, Collection<? extends BranchingConstraint> branchingConstraints, BestSolutionHolder bestSolutionHolder, Constraint constraint)
        {
            this.tableau = tableau;
            this.objective = objective;
            this.branchingConstraints = branchingConstraints;
            this.bestSolutionHolder = bestSolutionHolder;
            this.constraint = constraint;
        }

        @Override
        protected void compute()
        {
            tableau.addConstraint(constraint);
            try {
                tableau.solveFeasibility();
                BigFraction objectiveValue = tableau.maximize(objective);
                if (bestSolutionHolder.isIncompleteSolutionWorthContinuing(objectiveValue)){
                    invokeAll(Collections.singleton(new FindUnsatisfiedConstraintsAction(tableau, objective, branchingConstraints, bestSolutionHolder)));
                }
            }catch (InfeasibleSolutionException ignore){
            }catch (UnboundedSolutionException e){
                bestSolutionHolder.setError(e);
            }catch (InterruptedException e){
                Thread.currentThread().interrupt();
            }
        }
    }

    private static class BestSolutionHolder
    {
        private BigFraction bestObjectiveValue;
        private Tableau bestTableau;
        private UnboundedSolutionException error;

        public synchronized Tableau getBestTableau()
                throws UnboundedSolutionException
        {
            if (error != null){
                throw error;
            }
            return bestTableau;
        }

        public synchronized boolean isIncompleteSolutionWorthContinuing(BigFraction objectiveValue)
        {
            if (error != null){
                return false;
            }else if (bestObjectiveValue == null){
                return true;
            }else{
                return objectiveValue.compareTo(bestObjectiveValue) > 0;
            }
        }

        public synchronized void setError(UnboundedSolutionException error)
        {
            this.error = error;
        }

        public synchronized void submitCompleteSolution(BigFraction objectiveValue, Tableau tableau)
        {
            if (bestObjectiveValue == null){
                bestObjectiveValue = objectiveValue;
                bestTableau = tableau;
            }
        }
    }
}
