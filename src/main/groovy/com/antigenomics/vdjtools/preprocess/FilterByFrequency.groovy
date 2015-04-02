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
 */

package com.antigenomics.vdjtools.preprocess

import com.antigenomics.vdjtools.io.SampleWriter
import com.antigenomics.vdjtools.sample.*
import com.antigenomics.vdjtools.sample.metadata.MetadataTable

import static com.antigenomics.vdjtools.util.ExecUtil.formOutputPath

def DEFAULT_FREQ_THRESHOLD = "0.01", DEFAULT_QUANTILE_THRESHOLD = "0.25"
def cli = new CliBuilder(usage: "FilterByFrequency [options] " +
        "[sample1 sample2 sample3 ... if -m is not specified] output_prefix")
cli.h("display help message")
cli.m(longOpt: "metadata", argName: "filename", args: 1,
        "Metadata file. First and second columns should contain file name and sample id. " +
                "Header is mandatory and will be used to assign column names for metadata.")
cli.f(longOpt: "freq-threshold", argName: "double", args: 1,
        "Default clonotype frequency threshold. Set it to 0 to disable. " +
                "[default = $DEFAULT_FREQ_THRESHOLD]")
cli.q(longOpt: "quantile-threshold", argName: "double", args: 1,
        "Default quantile threshold. Will retain a set of top clonotypes " +
                "with a total frequency specified by this threhsold. " +
                "Set it to 1 to disable." +
                "[default = $DEFAULT_QUANTILE_THRESHOLD]")
cli.c(longOpt: "compress", "Compress output sample files.")

def opt = cli.parse(args)

if (opt == null)
    System.exit(-1)

if (opt.h || opt.arguments().size() == 0) {
    cli.usage()
    System.exit(-1)
}

// Check if metadata is provided

def metadataFileName = opt.m

if (metadataFileName ? opt.arguments().size() != 1 : opt.arguments().size() < 2) {
    if (metadataFileName)
        println "Only output prefix should be provided in case of -m"
    else
        println "At least 1 sample files should be provided if not using -m"
    cli.usage()
    System.exit(-1)
}

// Remaining arguments

def outputFilePrefix = opt.arguments()[-1],
    freqThreshold = (opt.f ?: DEFAULT_FREQ_THRESHOLD).toDouble(),
    quantileThreshold = (opt.q ?: DEFAULT_QUANTILE_THRESHOLD).toDouble(),
    compress = (boolean) opt.c

def scriptName = getClass().canonicalName.split("\\.")[-1]

//
// Batch load all samples (lazy)
//

println "[${new Date()} $scriptName] Reading sample(s)"

def sampleCollection = metadataFileName ?
        new SampleCollection((String) metadataFileName) :
        new SampleCollection(opt.arguments()[0..-2])

println "[${new Date()} $scriptName] ${sampleCollection.size()} sample(s) loaded"

//
// Iterate over samples & filter
//

def writer = new SampleWriter(compress)

def filter = new CompositeClonotypeFilter(
        new FrequencyFilter(freqThreshold),
        new QuantileFilter(quantileThreshold)
)

new File(formOutputPath(outputFilePrefix, "freqfilter", "summary")).withPrintWriter { pw ->
    def header = "#$MetadataTable.SAMPLE_ID_COLUMN\t" +
            sampleCollection.metadataTable.columnHeader + "\t" +
            ClonotypeFilter.ClonotypeFilterStats.HEADER

    pw.println(header)

    sampleCollection.eachWithIndex { sample, ind ->
        def sampleId = sample.sampleMetadata.sampleId
        println "[${new Date()} $scriptName] Filtering $sampleId.."

        def filteredSample = new Sample(sample, filter)

        // print output
        writer.writeConventional(filteredSample, outputFilePrefix)

        def stats = filter.getStatsAndFlush()

        pw.println([sampleId, sample.sampleMetadata, stats].join("\t"))
    }
}

sampleCollection.metadataTable.storeWithOutput(outputFilePrefix, compress,
        "freqfilter:$freqThreshold:$quantileThreshold")

println "[${new Date()} $scriptName] Finished"