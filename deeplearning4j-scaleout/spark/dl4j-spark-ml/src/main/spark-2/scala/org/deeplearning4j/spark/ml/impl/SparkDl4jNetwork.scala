package org.deeplearning4j.spark.ml.impl

import java.util

import org.apache.spark.ml.param.ParamMap
import org.apache.spark.ml.linalg.Vector
import org.apache.spark.ml.util.Identifiable
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.sql.{Dataset, Row}
import org.deeplearning4j.nn.conf.MultiLayerConfiguration
import org.deeplearning4j.optimize.api.IterationListener
import org.deeplearning4j.spark.impl.multilayer.SparkDl4jMultiLayer
import org.deeplearning4j.spark.ml.utils.{DatasetFacade, ParamSerializer}
import org.nd4j.linalg.api.ndarray.INDArray


final class SparkDl4jNetwork(
                                override val multiLayerConfiguration: MultiLayerConfiguration,
                                override val numLabels: Int,
                                override val trainingMaster: ParamSerializer,
                                override val listeners: util.Collection[IterationListener],
                                override val collectStats: Boolean = false,
                                override val uid: String = Identifiable.randomUID("dl4j"))
    extends SparkDl4jNetworkWrapper[Vector, SparkDl4jNetwork, SparkDl4jModel](
        uid, multiLayerConfiguration, numLabels, trainingMaster, listeners, collectStats) {

    def this(multiLayerConfiguration: MultiLayerConfiguration, numLabels: Int, trainingMaster: ParamSerializer, listeners: util.Collection[IterationListener]) {
        this(multiLayerConfiguration, numLabels, trainingMaster, listeners, false, Identifiable.randomUID("dl4j"))
    }

    def this(multiLayerConfiguration: MultiLayerConfiguration, numLabels: Int, trainingMaster: ParamSerializer, listeners: util.Collection[IterationListener], collectStats: Boolean) {
        this(multiLayerConfiguration, numLabels, trainingMaster, listeners, collectStats, Identifiable.randomUID("dl4j"))
    }

    override val mapVectorFunc: Row => LabeledPoint = row => new LabeledPoint(row.getAs[Double]($(labelCol)), Vectors.fromML(row.getAs[Vector]($(featuresCol))))

    override def train(dataset: Dataset[_]): SparkDl4jModel = {
        val spn = trainer(DatasetFacade.dataRows(dataset))
        new SparkDl4jModel(uid, spn)
    }
}

class SparkDl4jModel(override val uid: String, network: SparkDl4jMultiLayer)
    extends SparkDl4jModelWrapper[Vector, SparkDl4jModel](uid, network) {

    override def copy(extra: ParamMap) : SparkDl4jModel = {
        copyValues(new SparkDl4jModel(uid, network)).setParent(parent)
    }

    override def predict(features: Vector) : Double = {
        predictor(Vectors.fromML(features))
    }

    /**
      * Output wrapper around spark multilayer network. Does not flatten tensors if using rnn.
      * @param vector spark-ml vector
      * @return spark-ml vector
      */
    def output(vector: Vector): Vector = org.apache.spark.ml.linalg.Vectors.dense(super.output(Vectors.fromML(vector)).toArray)

    /**
      * returns a flattened tensor or a transformation from tensor to vector.
      * @param vector spark-ml vector
      * @return Vector
      */
    def outputFlattenedTensor(vector: Vector) : Vector = org.apache.spark.ml.linalg.Vectors.dense(super.outputFlattenedTensor(Vectors.fromML(vector)).toArray)

    /**
      * Outputs the tensor from the multiLayer network
      * @param vector spark-ml vector
      * @return and INDArray
      */
    def outputTensor(vector: Vector) : INDArray = super.outputTensor(Vectors.fromML(vector))

}

object SparkDl4jModel extends SparkDl4jModelWrap
