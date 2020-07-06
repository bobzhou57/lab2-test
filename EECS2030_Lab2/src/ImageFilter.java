
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.Math;


import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * @author Andriy Pavlovych
 * The class is meant to illustrate a couple of image-processing algorithms:
 * Gaussian blurring and simple sharpening
 * Java Swing is used to implement the GUI of the application
 * Limitations: image sizes are limited by computer screen resolution (no scaling is implemented)
 */
@SuppressWarnings("serial")
public class ImageFilter extends JFrame implements ActionListener {
    private String fileName;
    private BufferedImage image;
    private JButton blurButton, sharpenButton;
    private JLabel sourceImageLabel, resultImageLabel;
    private JTextField sigmaField;
    private JPanel sourcePanel, middlePanel, resultPanel, blurPanel, sharpenPanel;
    private double[] kernel1D =
            {0.006, 0.061, 0.242, 0.383, 0.242, 0.061, 0.006}; //sigma = 1;

    //	{0.063327,	0.093095,	0.122589,	0.144599,	0.152781,	0.144599,	0.122589,	0.093095,	0.063327}; //sigma = 3

    //	{0.034619,	0.044859,	0.055857,	0.066833,	0.076841,	0.084894,	0.090126,	0.09194,	0.090126,
    //		0.084894, 0.076841,	0.066833,	0.055857,	0.044859,	0.034619}; //sigma = 5

    /**
     * @param fileName name of the image file to process
     *                 loads the image with the filename provided
     */
    public ImageFilter(String fileName) {
        this.fileName = fileName;
        try {
            image = ImageIO.read(new File(fileName));
        } catch (IOException e) {
        }
        initUI();
    }

    private void initUI() {
        sourceImageLabel = new JLabel(new ImageIcon(fileName));

        resultImageLabel = new JLabel(new ImageIcon(image));

        sigmaField = new JTextField(4);
        sigmaField.setText("1.0");
        blurButton = new JButton("Blur");
        blurButton.setPreferredSize(new Dimension(84, 24));
        blurButton.addActionListener(this);
        sharpenButton = new JButton("Sharpen");
        sharpenButton.setPreferredSize(new Dimension(84, 24));
        sharpenButton.addActionListener(this);

        getContentPane().setLayout(new BorderLayout());
        sourcePanel = new JPanel();
        middlePanel = new JPanel();
        resultPanel = new JPanel();
        sourcePanel.setLayout(new BoxLayout(sourcePanel, BoxLayout.Y_AXIS));
        sourcePanel.add(new JLabel("Source"));
        sourcePanel.add(sourceImageLabel);
        add(sourcePanel, BorderLayout.WEST);

        middlePanel.setLayout(new BorderLayout(10, 10));

        blurPanel = new JPanel();
        blurPanel.setLayout(new FlowLayout(FlowLayout.TRAILING, 5, 5));
        blurPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        blurPanel.add(new JLabel("Sigma "));
        blurPanel.add(sigmaField);
        blurPanel.add(blurButton);

        sharpenPanel = new JPanel();
        sharpenPanel.setLayout(new FlowLayout(FlowLayout.TRAILING, 5, 5));
        sharpenPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        sharpenPanel.add(sharpenButton);
        middlePanel.add(new JLabel(" "), BorderLayout.NORTH);
        middlePanel.add(blurPanel, BorderLayout.CENTER);
        middlePanel.add(sharpenPanel, BorderLayout.SOUTH);
        add(middlePanel, BorderLayout.CENTER);

        resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.Y_AXIS));
        resultPanel.add(new JLabel("Result"));
        resultPanel.add(resultImageLabel);
        add(resultPanel, BorderLayout.EAST);

        pack();
        setTitle("ImageFilter");
        setLocationRelativeTo(null); //place in the centre of the screen
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    public static void main(String[] args) {

        //test if there is exactly one argument
        if(args.length != 1){
            System.out.println("Error: There must be exactly one argument");
            System.exit(-1);
        }

        String fileName = args[0];

        ImageFilter ex = new ImageFilter(fileName);
        ex.setVisible(true);

    }

    public static double[] getGaussian(double sigma) {
        //according to wikipedia, we can ignore values that are far away from the centre because they're practically 0
        //set the # of values we want to keep, and thus the function array's length
        int FNC_LENGTH = (int) Math.ceil(sigma * 5 * 2 + 1);
        int HALF_LENGTH = (int) Math.ceil(sigma * 5 + 1);
        double[] result = new double[FNC_LENGTH];
        double[] temp = new double[HALF_LENGTH];

        //generate one half of the gaussian curve
        for (int i = 0; i < (5 * sigma + 1); i++) {
            //this is the function for gaussian distribution
            temp[i] = (1 / Math.sqrt(2 * Math.PI * sigma * sigma)) * (Math.exp(-1 * ((i * i) / (2 * sigma * sigma))));
        }

        //now we're gonna make the front half by reversing the temp array
        int i = 0;
        //this gives us the index of the last element in the temp half of the gaussian distr.
        int j = HALF_LENGTH - 1;

        //traverse the half-function in reverse and fill the result array
        while (j > 0) {
            result[i] = temp[j];
            i++;
            j--;
        }

        //go forwards
        while (j < temp.length) {
            result[i] = temp[j];
            j++;
            i++;
        }
        return result;
    }

    /**
     * Method implements Gaussian blurring
     *
     * @param imageData array of image pixels
     * @param width     image width
     * @param sigma     parameter of the Gaussian distribution
     */
    private void blur(int[] imageData, int width, double sigma) {
        //hold the pixels in a 2d array
        int[][] pixels = new int[image.getHeight()][image.getWidth()];
        //array for holding the result
        int[][] result = new int[image.getHeight()][image.getWidth()];

        //scan the image into the 2d array
        int counter = 0;
        for (int i = 0; i < image.getHeight(); i++) {
            for (int j = 0; j < image.getWidth(); j++) {
                pixels[i][j] = imageData[counter];
                counter++;
            }
        }

        //get a gaussian fnc based on desired sigma
        double[] kernel = getGaussian(sigma);
        //tells us how far the kernel goes in each direction from the centre
        int G_WIDTH = (int) sigma * 3;

        //this mess applies the kernel to the pixels horizontally while checking for out of bounds
        for (int i = 0; i < image.getHeight(); i++) {
            for (int j = 0; j < image.getWidth(); j++) {
                //store each rgb value seperately
                double sumr = 0;
                double sumg = 0;
                double sumb = 0;
                for (int k = -G_WIDTH; k <= G_WIDTH; k++) {
                    if ((j + k >= 0) && (j + k < image.getWidth())) {
                        //sum for each of the r,g,b components
                        sumr += kernel[k + G_WIDTH] * (((pixels[i][j + k]) & 0x00FF0000) >> 16);
                        sumg += kernel[k + G_WIDTH] * (((pixels[i][j + k]) & 0x0000FF00) >> 8);
                        sumb += kernel[k + G_WIDTH] * (((pixels[i][j + k]) & 0x000000FF));
                    } else if (j + k < 0) {
                        sumr += kernel[k + G_WIDTH] * (((pixels[i][0]) & 0x00FF0000) >> 16);
                        sumg += kernel[k + G_WIDTH] * (((pixels[i][0]) & 0x0000FF00) >> 8);
                        sumb += kernel[k + G_WIDTH] * (((pixels[i][0]) & 0x000000FF));
                    } else if (j + k >= image.getWidth()) {
                        sumr += kernel[k + G_WIDTH] * (((pixels[i][image.getWidth() - 1]) & 0x00FF0000) >> 16);
                        sumg += kernel[k + G_WIDTH] * (((pixels[i][image.getWidth() - 1]) & 0x0000FF00) >> 8);
                        sumb += kernel[k + G_WIDTH] * (((pixels[i][image.getWidth() - 1]) & 0x000000FF));
                    }
                }
                //re-assemble rgb value
                result[i][j] = (int) sumr << 16 | (int) sumg << 8 | (int) sumb;
            }
        }

        //do the same thing vertically
        for (int i = 0; i < image.getWidth(); i++) {
            for (int j = 0; j < image.getHeight(); j++) {
                double sumr = 0;
                double sumg = 0;
                double sumb = 0;
                for (int k = -G_WIDTH; k <= G_WIDTH; k++) {
                    if ((j + k >= 0) && (j + k < image.getHeight())) {
                        sumr += kernel[k + G_WIDTH] * (((result[j + k][i]) & 0x00FF0000) >> 16);
                        sumg += kernel[k + G_WIDTH] * (((result[j + k][i]) & 0x0000FF00) >> 8);
                        sumb += kernel[k + G_WIDTH] * (((result[j + k][i]) & 0x000000FF));
                    } else if (j + k < 0) {
                        sumr += kernel[k + G_WIDTH] * (((result[0][i]) & 0x00FF0000) >> 16);
                        sumg += kernel[k + G_WIDTH] * (((result[0][i]) & 0x0000FF00) >> 8);
                        sumb += kernel[k + G_WIDTH] * (((result[0][i]) & 0x000000FF));
                    } else if (j + k >= image.getWidth()) {
                        sumr += kernel[k + G_WIDTH] * (((result[image.getWidth() - 1][i]) & 0x00FF0000) >> 16);
                        sumg += kernel[k + G_WIDTH] * (((result[image.getWidth() - 1][i]) & 0x0000FF00) >> 8);
                        sumb += kernel[k + G_WIDTH] * (((result[image.getWidth() - 1][i]) & 0x000000FF));
                    }
                }
                result[j][i] = (int) sumr << 16 | (int) sumg << 8 | (int) sumb;
            }
        }

        //convert back to 1d array and store in original variable
        int c = 0;
        for (int i = 0; i < image.getHeight(); i++) {
            for (int j = 0; j < image.getWidth(); j++) {
                imageData[c] = result[i][j];
                c++;
            }
        }
    }

    /**
     * Method implements simple sharpening
     *
     * @param imageData imageData array of image pixels
     * @param width     image width
     */
    private void sharpen(int[] imageData, int width) {

        //make 2d storage arrays
        int[][] pixels = new int[image.getHeight()][image.getWidth()];
        //array for holding the result
        int[][] result = new int[image.getHeight()][image.getWidth()];

        //hardcoded kernel
        int[][] kernel = {
                {0, -1, 0},
                {-1, 5, -1},
                {0, -1, 0}
        };

        //scan image into 2d array
        int counter = 0;
        for (int i = 0; i < image.getHeight(); i++) {
            for (int j = 0; j < image.getWidth(); j++) {
                pixels[i][j] = imageData[counter];
                counter++;
            }
        }


        //handling the edge with 2d is a pain
        //instead i shall just ignore a 1px border around the image. this will work in this case since the kernel is
        //narrow and therefore there will be no out of bounds
        //otherwise, we're just applying the kernel iteratively
        for (int i = 1; i < image.getHeight() - 1; i++) {
            for (int j = 1; j < image.getWidth() - 1; j++) {
                //store each rgb value separately        int[] temp = new int[imageData.length];
                double sumr = 0;
                double sumg = 0;
                double sumb = 0;

                for (int ii = -1; ii < 2; ii++) {
                    for (int jj = -1; jj < 2; jj++) {
                        //sum for each of the r,g,b components
                        sumr += kernel[ii + 1][jj + 1] * (((pixels[i + ii][j + jj]) & 0x00FF0000) >> 16);
                        sumg += kernel[ii + 1][jj + 1] * (((pixels[i + ii][j + jj]) & 0x0000FF00) >> 8);
                        sumb += kernel[ii + 1][jj + 1] * (((pixels[i + ii][j + jj]) & 0x000000FF));
                    }
                }
                //re-assemble rgb value
                result[i][j] = (int) sumr << 16 | (int) sumg << 8 | (int) sumb;
            }
        }

        //convert back into 1d array and store in original variable
        int c = 0;
        for (int i = 0; i < image.getHeight(); i++) {
            for (int j = 0; j < image.getWidth(); j++) {
                imageData[c] = result[i][j];
                c++;
            }
        }
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == blurButton) {
            int[] rgbData = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
            blur(rgbData, image.getWidth(), Double.parseDouble(sigmaField.getText()));
            image.setRGB(0, 0, image.getWidth(), image.getHeight(), rgbData, 0, image.getWidth());
            resultImageLabel.setIcon(new ImageIcon(image));

        } else if (e.getSource() == sharpenButton) {
            int[] rgbData = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
            sharpen(rgbData, image.getWidth());
            image.setRGB(0, 0, image.getWidth(), image.getHeight(), rgbData, 0, image.getWidth());
            resultImageLabel.setIcon(new ImageIcon(image));
        }
    }


}
