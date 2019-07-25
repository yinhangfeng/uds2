package com.mrwind.uds.util;

import com.mrwind.uds.Response;
import com.mrwind.uds.UDSChromosome;
import io.jenetics.Genotype;
import io.jenetics.IntegerGene;
import io.jenetics.engine.EvolutionInit;
import io.jenetics.util.ISeq;

import java.util.ArrayList;
import java.util.List;

public class Utils {

    public static EvolutionInit<IntegerGene> responsesToEvolutionInit(List<Response> responseList) {
        List<Genotype<IntegerGene>> initGenotypes = new ArrayList<>();
        for (Response response : responseList) {
            UDSChromosome udsChromosome = UDSChromosome.of(response.allocation, 0, response.drivers.size() - 1);
            udsChromosome.response = response;
            initGenotypes.add(Genotype.of(udsChromosome));
        }

        return EvolutionInit.of(ISeq.of(initGenotypes), 1);
    }
}
