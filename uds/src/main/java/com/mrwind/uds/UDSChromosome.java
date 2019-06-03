package com.mrwind.uds;

import io.jenetics.IntegerChromosome;
import io.jenetics.IntegerGene;
import io.jenetics.internal.math.random;
import io.jenetics.util.ISeq;
import io.jenetics.util.IntRange;
import io.jenetics.util.MSeq;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class UDSChromosome extends IntegerChromosome {

    public Response response;
    private static int _xxx = 0;
    public int xxx = _xxx++;

    protected UDSChromosome(ISeq<IntegerGene> genes, IntRange lengthRange) {
        super(genes, lengthRange);
    }

    public UDSChromosome(Integer min, Integer max, IntRange lengthRange) {
        super(min, max, lengthRange);
    }

    public UDSChromosome(Integer min, Integer max, int length) {
        super(min, max, length);
    }

    public UDSChromosome(Integer min, Integer max) {
        super(min, max);
    }

    public static UDSChromosome of(
            final int min,
            final int max,
            final int length
    ) {
        return new UDSChromosome(min, max, length);
    }

    public static UDSChromosome of(final int[] genes, final int min, final int max) {
        List<IntegerGene> geneList = new ArrayList<>(genes.length);
        for (int gen : genes) {
            geneList.add(IntegerGene.of(gen, min, max));
        }
        return new UDSChromosome(ISeq.of(geneList), IntRange.of(genes.length));
    }

    public static UDSChromosome of(final IntegerGene... genes) {
        return new UDSChromosome(ISeq.of(genes), IntRange.of(genes.length));
    }

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    @Override
    public UDSChromosome newInstance(final ISeq<IntegerGene> genes) {
        return new UDSChromosome(genes, lengthRange());
    }

    @Override
    public UDSChromosome newInstance() {
        return new UDSChromosome(getMin(), getMax(), lengthRange());
    }
}
