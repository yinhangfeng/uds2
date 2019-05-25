package com.mrwind.uds.stat;

import io.jenetics.Phenotype;
import io.jenetics.engine.EvolutionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class EvolutionResultStatistics<C extends Double> implements Consumer<EvolutionResult<?, C>> {

    private List<EvolutionResult<?, C>> results = new ArrayList<>();
    private List<SimpleEvolutionResult> simpleEvolutionResults = new ArrayList<>();

    @Override
    public void accept(EvolutionResult<?, C> gcEvolutionResult) {
        results.add(gcEvolutionResult);
        List<Double> fitnessList = gcEvolutionResult.getPopulation().stream().map(Phenotype::getFitness).collect(Collectors.toList());

        SimpleEvolutionResult simpleEvolutionResult = new SimpleEvolutionResult();
        simpleEvolutionResult.fitnessList = fitnessList;
        simpleEvolutionResults.add(simpleEvolutionResult);
    }

    public List<EvolutionResult<?, C>> getResults() {
        return results;
    }

    public List<SimpleEvolutionResult> getSimpleEvolutionResults() {
        return simpleEvolutionResults;
    }
}
