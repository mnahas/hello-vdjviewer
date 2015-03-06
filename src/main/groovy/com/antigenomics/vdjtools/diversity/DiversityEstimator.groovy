/*
 * Copyright 2013-2015 Mikhail Shugay (mikhail.shugay@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Last modified on 7.3.2015 by mikesh
 */

package com.antigenomics.vdjtools.diversity

/**
 * Base class for implementations that compute various species richness estimates and diversity indices.
 * @see com.antigenomics.vdjtools.diversity.DiversityIndex
 * @see com.antigenomics.vdjtools.diversity.SpeciesRichness
 */
abstract class DiversityEstimator {
    protected final FrequencyTable frequencyTable
    protected final EstimationMethod estimationMethod

    /**
     * Protected constructor.
     * @param frequencyTable a {@link com.antigenomics.vdjtools.diversity.FrequencyTable} summary for sample of interest.
     * @param estimationMethod specifies the method that is used in this estimator implementation. 
     */
    protected DiversityEstimator(FrequencyTable frequencyTable, EstimationMethod estimationMethod) {
        this.frequencyTable = frequencyTable
        this.estimationMethod = estimationMethod
    }

    /*
     * Diversity indices
     */

    /**
     * Computes the Shannon-Weiner diversity index.
     * @return {@link com.antigenomics.vdjtools.diversity.DiversityEstimate} object handling the result.
     */
    abstract DiversityIndex getShannonWeinerIndex()

    /**
     * Computes the Inverse Simpson diversity index.
     * @return {@link com.antigenomics.vdjtools.diversity.DiversityEstimate} object handling the result.
     */
    abstract DiversityIndex getInverseSimpsonIndex()

    /**
     * Computes the diversity index DXX, where XX is a specified fraction.
     * The estimate is the minimum number of clonotypes accounting for at least XX% of the total reads.
     * @param fraction a fraction of reads at which the diversity estimate will be computed.
     * @return {@link com.antigenomics.vdjtools.diversity.DiversityEstimate} object handling the result.
     */
    abstract DiversityIndex getDxxIndex(double fraction)

    /**
     * Computes is the minimum number of clonotypes accounting for at least 50% of the total reads.
     * @return {@link com.antigenomics.vdjtools.diversity.DiversityEstimate} object handling the result.
     */
    DiversityEstimate getD50Index() {
        getDxxIndex(0.5)
    }

    /*
     * Richness estimates
     */

    /**
     * Computes the Efron-Thisted lower bound estimate of total diversity.
     * @param maxDepth max number of terms in Euler series to compute.
     * @param cvThreshold coefficient of variance threshold, after reaching it higher order terms will be ignored.
     * @return {@link com.antigenomics.vdjtools.diversity.DiversityEstimate} object handling the result.
     */
    abstract SpeciesRichness getEfronThisted(int maxDepth, double cvThreshold)

    /**
     * Computes the Efron-Thisted lower bound estimate of total diversity.
     * @return {@link com.antigenomics.vdjtools.diversity.DiversityEstimate} object handling the result.
     */
    SpeciesRichness getEfronThisted() {
        getEfronThisted(20, 0.05)
    }

    /**
     * Computes the Chao1 lower bound estimate of total diversity.
     * @return {@link com.antigenomics.vdjtools.diversity.DiversityEstimate} object handling the result.
     */
    abstract SpeciesRichness getChao1()

    /**
     * Computes the extrapolated Chao diversity estimate.
     * @return {@link com.antigenomics.vdjtools.diversity.DiversityEstimate} object handling the result.
     */
    abstract DiversityEstimate getChaoE()

    /**
     * Computes the observed diversity, i.e. the number of clonotypes in the sample.
     * @return {@link com.antigenomics.vdjtools.diversity.DiversityEstimate} object handling the result.
     */
    abstract SpeciesRichness getObservedDiversity()

    /*
     * end estimates
     */

    /**
     * Gets the underlying frequency table.
     * @return {@link com.antigenomics.vdjtools.diversity.DiversityEstimate} object handling the result.
     */
    FrequencyTable getFrequencyTable() {
        frequencyTable
    }

    /**
     * Gets the computation method that is used in this estimator implementation.
     * @return computation method that is used in this estimator implementation.
     */
    EstimationMethod getEstimationMethod() {
        return estimationMethod
    }

    /**
     * List of fields that will be included in tabular output .
     */
    private static final String[] FIELDS = ["observedDiversity",
                                            "chaoE",
                                            "efronThisted", "chao1",
                                            "d50Index", "shannonWeinerIndex", "inverseSimpsonIndex"]

    /**
     * Computes all the diversity estimates.
     * @return list of computed diversity estimates.
     */
    List<DiversityEstimate> computeAll() {
        FIELDS.collect { this."$it" }
    }

    /**
     * Header string, used for tabular output.
     */
    static final String HEADER = FIELDS.collect { [it + "_mean", it + "_std"] }.flatten().join("\t")

    /**
     * Plain text row for tabular output.
     */
    @Override
    String toString() {
        FIELDS.collect { this."$it" }.join("\t")
    }
}