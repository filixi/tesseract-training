
// To prepare the environment of tesseract training
// docu: https://github.com/tesseract-ocr/tesseract/wiki/Compiling-%E2%80%93-GitInstallation
// - clone tesseract git: https://github.com/tesseract-ocr/tesseract repo: https://github.com/tesseract-ocr/tesseract.git
// - install additional library
//    - sudo apt-get install libicu-dev
//      sudo apt-get install libpango1.0-dev
//      sudo apt-get install libcairo2-dev
// - In the repo's root directory
//    - ./autogen.sh
//      ./configure
//      make
//      sudo make install
//      sudo ldconfig
//      make training
//      sudo make training-install
//    - make sure every steps succeed!!!
// - Prepare
//    - render image (tif)
//    - box file (box) : https://github.com/tesseract-ocr/tesseract/issues/2357#issuecomment-477239316
//        - must be tab at the end of line
//        - must be a space at end of each word
//    - unicharset file (unicharset) : http://manpages.ubuntu.com/manpages/bionic/man5/unicharset.5.html https://github.com/tesseract-ocr/langdata_lstm/blob/master/HanS/HanS.unicharset
//
//
//
//
// Other material:
// https://groups.google.com/forum/#!topic/tesseract-ocr/97SzDEE--F0
// http://manpages.ubuntu.com/manpages/bionic/man5/unicharset.5.html
//

//#=== CHECK THAT TESSERACT AND TRAINING TOOLS ARE INSTALLED
//
//tesseract -v
//text2image -v
//unicharset_extractor -v
//set_unicharset_properties -v
//combine_lang_model -v
//lstmtraining -v
//lstmeval -v
//
//#===  MAKE DIRECTORIES AND DOWNLOAD REQUIRED FILES
//
//mkdir -p ~/tessscratch
//cd ~/tessscratch
//wget -O lstm.train https://raw.githubusercontent.com/tesseract-ocr/tesseract/master/tessdata/configs/lstm.train
//wget -O radical-stroke.txt https://raw.githubusercontent.com/tesseract-ocr/langdata_lstm/master/radical-stroke.txt
//mkdir -p mylangdata
//  mkdir -p mylangdata/foo
//
//#=== CREATE YOUT TRAINING TEXT FOR NEW LANGUAGE foo.
//  #=== FOR TRAINING FROM SCRATCH, IT SHOULD BE THOSANDS OF LINES.
//#=== HERE A COPY OF ENGLISH TRAINING TEXT (72 LINES) IS MADE AS AN ILLUSTRATION.
//
//wget -O mylangdata/foo/foo.training_text https://raw.githubusercontent.com/tesseract-ocr/langdata/master/eng/eng.training_text
//
//#=== MAKE BOX/TIFF PAIRS USING TRAINING TEXT AND TWO FONTS.
//
//text2image --strip_unrenderable_words --leading=32 --xsize=3600 --char_spacing=0.0 --exposure=0  --max_pages=0 \
//  --fonts_dir=/usr/share/fonts \
//--font="Arial Unicode MS" \
//  --text=mylangdata/foo/foo.training_text \
//  --outputbase=foo.Arial.exp0
//
//text2image --strip_unrenderable_words --leading=32 --xsize=3600 --char_spacing=0.0 --exposure=0  --max_pages=0 \
//  --fonts_dir=/usr/share/fonts \
//--font="Courier New" \
//  --text=mylangdata/foo/foo.training_text \
//  --outputbase=foo.Courier.exp0
//
//#=== EXTRACT UNICHARSET & SET PROPERTIES FROM BOX FILES.
//
//  unicharset_extractor --output_unicharset foo.unicharset --norm_mode 1  foo.Arial.exp0.box  foo.Courier.exp0.box
//set_unicharset_properties -U foo.unicharset -O foo.unicharset -X foo.xheights --script_dir=.
//
//#=== CREATE LSTMF FILES.
//
//tesseract foo.Arial.exp0.tif foo.Arial.exp0 --psm 6 lstm.train
//tesseract foo.Courier.exp0.tif foo.Courier.exp0 --psm 6 lstm.train
//ls -1 *.lstmf > foo.training_files.txt
//
//#=== CREATE STARTER TRAINEDDATA
//
//mkdir -p fooscratch
//
//combine_lang_model \
//  --input_unicharset foo.unicharset \
//  --script_dir . \
//--output_dir fooscratch \
//  --lang foo
//
//#=== RUN LSTM TRAINING -
//  #=== hundreds of thousands of iterations may be needed for real training_text.
//
//lstmtraining \
//  --model_output  han-scratch/layer \
//  --net_spec '[1,36,0,1 Ct3,3,16 Mp3,3 Lfys48 Lfx96 Lrx96 Lfx256 O1c111]' \
//  --learning_rate 20e-4 \
//  --traineddata  han-scratch/han/han.traineddata \
//  --train_listfile  han.training_files.txt   \
//  --debug_interval 0 \
//  --max_iterations 5000
//

import javax.imageio.ImageIO
import java.awt.{Color, Font, Graphics2D, RenderingHints}
import java.awt.image.BufferedImage
import java.io.{File, FileWriter, PrintWriter}

import scala.collection.mutable.ArrayBuffer
import scala.io.Source

case class Rect(left: Int, bottom: Int, right: Int, top: Int, text: String)

case

object DrawTextOnImage {
  def apply(text: String, characterPerLine: Int, font: Font): (BufferedImage, Array[Rect]) = {
    val lines = text.replaceAll("\\s", "")
      .filter(c => !c.isSurrogate)
      .grouped(characterPerLine - 1).map(line => line + "\t").toArray

    val extendedLines = text.replaceAll("\\s", "")
      .filter(c => c.isSurrogate)
      .grouped((characterPerLine - 1)*2).map(line => line + "\t").toArray

    val fontSize = font.getSize
    val width = characterPerLine * fontSize + 20
    val height = lines.length * fontSize + 20

    val bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val graphics = bufferedImage.getGraphics.asInstanceOf[Graphics2D]

    graphics.setColor(Color.WHITE)
    graphics.fillRect(0, 0, width, height)

    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
    graphics.setColor(Color.BLACK)
    graphics.setFont(font)

    for ((key, idx) <- lines.zipWithIndex)
      graphics.drawString(key, 10, (idx + 1) * fontSize + 10)

    // top: 10 + row * fontSize + 3
    // bottom: 10 + row * fontSize + fontSize + 3
    // left: 10 + col * fontSize /// 10
    // right: 10 + col * fontSize + fontSize /// 10 + lines(row).size * fontSize

    def characterPos(row: Int, col: Int): Rect = {
      Rect(10 + col * fontSize, 10 + row * fontSize + fontSize + 3, 10 + col * fontSize + fontSize, 10 + row * fontSize + 3, lines(row)(col).toString)
    }

    def linePos(row: Int, col: Int): Rect = {
      Rect(10, 10 + row * fontSize + fontSize + 3, 10 + lines(row).length * fontSize, 10 + row * fontSize + 3, lines(row)(col).toString)
    }

    // each character
    val coord = new ArrayBuffer[Rect]()
    for (row <- lines.indices)
      for (col <- lines(row).indices)
        coord.append(linePos(row, col))

    // line

    graphics.setColor(Color.GREEN)
    for (rect <- coord)
      graphics.drawRect(rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top)

    (bufferedImage, coord.map(v => Rect(v.left, height - v.bottom, v.right, height - v.top, v.text)).toArray)
  }
}

class TessTrainedData(val directory: String, val lang: String, val font: String, val radical_stroke: String, val config: String) {
  private def directoryCheck(): Boolean = {
    if (directory.length == 0)
      return false

    val f = new File(directory)
    f.list().length == 0
  }

  require(directoryCheck(), "directory must be empty!")

  import sys.process._

  def prepareStage0(textFilePath: String): Unit = {
    val file = Source.fromFile(textFilePath)
    val content = file.getLines().toArray.apply(0)
    file.close()

    val stagePath = s"$directory/stage0/"
    if (!new File(stagePath).exists())
      Process(s"mkdir $stagePath").!

    val image = DrawTextOnImage(content, 25,
      new Font("SimHei", Font.PLAIN, 30))

    tifPath = stagePath + s"$lang.$font.exp0.tif"
    ImageIO.write(image._1, "tif", new File(tifPath))

    boxPath = stagePath + s"$lang.$font.exp0.box"
    val output = new PrintWriter(new File(boxPath))
    for (line <- image._2) {
      output.write(line.text + " " + line.left + " " + line.bottom + " " + line.right + " " + line.top + " " + 0 + "\n")
      if (line.text != "\t")
        output.write("  " + line.left + " " + line.bottom + " " + line.right + " " + line.top + " " + 0 + "\n") // space at the end of each word
    }
    output.flush()
    output.close()
  }

  private var tifPath: String = ""
  private var boxPath: String = ""

  def prepareStage1(): Unit = {
    val stagePath = s"$directory/stage0/"
    if (!new File(stagePath).exists())
      Process(s"mkdir $stagePath").!

    unicharsetPath = stagePath + s"$lang.unicharset"
    Process(s"unicharset_extractor --output_unicharset $unicharsetPath --norm_mode 1 $boxPath").!

    xheightsPath = stagePath + s"$lang.xheights"
    Process(s"set_unicharset_properties -U $unicharsetPath -O $unicharsetPath -X $xheightsPath --script_dir=.").!
  }

  private var unicharsetPath: String = ""
  private var xheightsPath: String = ""

  def prepareStage2(): Unit = {
    val stagePath = s"$directory/stage0/"
    if (!new File(stagePath).exists())
      Process(s"mkdir $stagePath").!

    val command = s"tesseract $tifPath $boxPath --psm 6 lstm.train"
    Process(command).!

    val writer = new FileWriter(new File(s"$stagePath$lang.training_files.txt"))
    var dir = new File(stagePath)
    for (f <- dir.listFiles(_.getName.endsWith(".lstmf")))
      writer.write(f.getName + "\n")
    writer.close()
  }

  def CreateTrainedData(): Unit = {
    val stagePath = s"$directory/stage0/"

    val targetPath = s"$stagePath$lang-scratch"
    if (!new File(targetPath).exists() )
      Process(s"mkdir $targetPath").!

    val configPath = s"$stagePath/$lang/"
    if (!new File(configPath).exists())
      Process(s"mkdir $configPath").!
    Process(s"cp $config $lang.config", new File(configPath)).!

    Process(s"cp $radical_stroke radical-stroke.txt", new File(stagePath)).!

    Process(s"combine_lang_model --input_unicharset $unicharsetPath --script_dir . --output_dir $lang-scratch --lang $lang",
      new File(stagePath)).!
  }

}

object Main {
  def main(args: Array[String]): Unit = {
    val tess = new TessTrainedData(
      "/home/yifei/tess-yifei",
      "han",
      "simhei",
      "/home/yifei/tess-data/radical-stroke.txt",
      "/home/yifei/tess-data/chi_sim.config")

    tess.prepareStage0("/home/yifei/repo/tesseract-train/first.txt")
    tess.prepareStage1()
    tess.prepareStage2()
    tess.CreateTrainedData()
  }
}
