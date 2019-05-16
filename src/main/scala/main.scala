import java.io.File

import tessTraining.TessTrainedData
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.lept._
import org.bytedeco.javacpp.tesseract.TessBaseAPI

import scala.sys.process.Process

object TessTest {
  val baseDirectory: String = "/home/tess/"

  def CreateTrainedDataFromCheckPoint(trainSetName: String): Unit = {
    print("checkpoint name:")
    val line = scala.io.StdIn.readLine()
    val name = line.reverse.takeWhile(_ != '/').reverse

    assert(!name.isEmpty)

    val command =
      "lstmtraining --stop_training " +
      s"--continue_from $baseDirectory$trainSetName/$name " +
      s"--traineddata $baseDirectory$trainSetName/stage0/han-scratch/han/han.traineddata " +
      s"--model_output $baseDirectory$trainSetName/han_simhei.traineddata"

    println(command)
    Process(command).!
  }

  def FirstTraining(trainSetName: String, lang: String): Unit = {
    val layerDir = s"$baseDirectory$trainSetName"
    val targetDir = s"${layerDir}/stage0"

    val command =
      "lstmtraining " +
      s"--model_output $layerDir/layer_ \\\n" +
      s"--traineddata $targetDir/$lang-scratch/$lang/$lang.traineddata \\\n" +
      "--net_spec '[1,36,0,1 Ct3,3,16 Mp3,3 Lfys48 Lfx96 Lrx96 Lfx256 O1c111]' \\\n" +
      "--learning_rate 20e-4 \\\n" +
      s"--train_listfile $targetDir/$lang.training_files.txt \\\n" +
      s"--eval_listfile $targetDir/$lang.eval_files.txt \\\n" +
      "--debug_interval 0 \\\n" +
      "--max_iterations 500"


    val eval =
      "lstmeval " +
      "--model /home/tess/new-font-test/layer_5.524_601.checkpoint \\\n" +
      s"--traineddata $targetDir/$lang-scratch/$lang/$lang.traineddata \\\n" +
      s"--eval_listfile $targetDir/$lang.eval_files.txt"

    println(command)
    println()
    println(eval)
    // Process(command).!
  }


  def tesseractOCR(): Unit = {
    var outText: BytePointer = new BytePointer

    val api = new TessBaseAPI()
    // Initialize tesseract-ocr with English, without specifying tessdata path
    if (api.Init("/home/tess/tess-trained", "han_simhei") != 0) {
      System.err.println("Could not initialize tesseract.")
      System.exit(1)
    }

    assert(api.SetVariable("tessedit_ocr_engine_mode", "1"))
    assert(api.SetVariable("tessedit_pageseg_mode", "6"))

    // Open input image with leptonica library

    val image = pixRead("/home/tess/new-font-test/stage0/han.simhei.exp0.tif")
    api.SetImage(image)

    val result = api.GetBoxText(0)
    println(result.getString())
    result.deallocate()

    // Get OCR result
    outText = api.GetUTF8Text
    System.out.println("OCR output:\n" + outText.getString)

    // Destroy used object and release memory
    api.End()
    outText.deallocate()

  }
}

object Main {
  def main(args: Array[String]): Unit = {
    // TessTrainedData.Example()
//    TessTest.CreateTrainedDataFromCheckPoint(
//      "/home/tess/tess-test/stage0/han-scratch/layer6.416_225118.checkpoint",
//      "/home/tess/tess-test/stage0/han-scratch/han/han.traineddata",
//      "/home/tess/tess-trained/chi_simhei.traineddata"
//    )

    TessTest.tesseractOCR()

    // TessTrainedData.default("new-font-test")

    // TessTest.FirstTraining("new-font-test","han")

    // TessTest.CreateTrainedDataFromCheckPoint("new-font-test")
  }
}
