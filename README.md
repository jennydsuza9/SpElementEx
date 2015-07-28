# SpElementEx
SpElementEx: Spatial Element Extraction System

A sequence labeling system for automatically annotating spatial element type and role information in text, following the ISO-Space (Pustejovsky et al., 2013) annotation specifications. 

The SpElementEx tool has been written in Java and is released as free software.

#### Prerequisites:

1) Input data must be in `xml` format.

2) External libraries used by the tool are [Apache Commons IO v2.4](https://commons.apache.org/proper/commons-io/download_io.cgi) and [Stanford CoreNLP](http://nlp.stanford.edu/software/corenlp.shtml#Download). Before running the tool, these libraries must be downloaded and the Java classpath must be set with the paths to the library `jar` files.

### Usage:

1) To train and develop a new spatial element type extraction model, and annotate test data with spatial element types using the newly developed model.

    java main.java.spelementex.Main -annotators type -train <YOUR TRAIN DIRECTORY> -dev <YOUR DEVELOPMENT DIRECTORY> -test <YOUR TEST DIRECTORY>
    
2) To train and develop a new spatial element role extraction model, and annotate test data with spatial element roles using the newly developed model.

    java main.java.spelementex.Main -annotators role -train <YOUR TRAIN DIRECTORY> -dev <YOUR DEVELOPMENT DIRECTORY> -test <YOUR TEST DIRECTORY>
    
`Please note: In this usage, the system requires the input training, development and test data to already have spatial element type annotations. If the input data does not have spatial element type annotations, then follow Usage 3 of the tool below.`
    
3) To train and develop a new spatial element type and role extraction model, and annotate test data with spatial element types and roles using the newly developed model.

    java main.java.spelementex.Main -annotators type,role -train <YOUR TRAIN DIRECTORY> -dev <YOUR DEVELOPMENT DIRECTORY> -test <YOUR TEST DIRECTORY>
    
4) To annotate test data using our pre-trained spatial element type extraction model.

    java main.java.spelementex.Main -annotators type -test <YOUR TEST DIRECTORY>
    
5) To annotate test data using our pre-trained spatial element role extraction model.

    java main.java.spelementex.Main -annotators role -test <YOUR TEST DIRECTORY>
    
`Please note: In this usage, the system requires the input test data to already have spatial element type annotations. If the input test data does not have spatial element type annotations, then follow Usage 6 of the tool below.`

6) To annotate test data using our pre-trained spatial element type and role extraction models.

    java main.java.spelementex.Main -annotators type,role -test <YOUR TEST DIRECTORY>

#### About Sequence Labeling in SpElementEx:

SpElementEx employs [CRF++](https://taku910.github.io/crfpp/), a sequence labeling tool based on conditional random fields, for learning and annotating the spatial element information. It is included in SpElementEx's `main\resources\crfpp\` folder. 

Since [CRF++](https://taku910.github.io/crfpp/) requires feature template files for training a model, files `type-template.txt` and `role-template.txt` in the `main\resources\templates\` folder are created as the features' templates for type and role extraction, respectively.

