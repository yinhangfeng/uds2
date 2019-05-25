package com.mrwind.uds;

import io.jenetics.IntegerChromosome;
import io.jenetics.IntegerGene;
import io.jenetics.util.ISeq;
import io.jenetics.util.IntRange;

public class UDSChromosome extends IntegerChromosome {

    public Response response;

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
