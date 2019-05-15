import java.io.File

import tessTraining.TessTrainedData
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.lept._
import org.bytedeco.javacpp.tesseract.TessBaseAPI

import scala.sys.process.Process

object TessTest {
  def CreateTrainedDataFromCheckPoint(checkpoint: String, trainedData: String, targetFile: String) = {
    val command =
      "lstmtraining --stop_training " +
      s"--continue_from $checkpoint " +
      s"--traineddata $trainedData " +
      s"--model_output $targetFile";

    println(command)
    Process(command).!
  }

  def tesseractOCR(): Unit = {
    var outText: BytePointer = new BytePointer

    val api = new TessBaseAPI()
    // Initialize tesseract-ocr with English, without specifying tessdata path
    if (api.Init("/home/tess/tess-trained/", "chi_sim") != 0) {
      System.err.println("Could not initialize tesseract.")
      System.exit(1)
    }

    // Open input image with leptonica library

    val image = pixRead("/usr/src/tesseract/testing/phototest.tif")
    api.SetImage(image)
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
  }
}
